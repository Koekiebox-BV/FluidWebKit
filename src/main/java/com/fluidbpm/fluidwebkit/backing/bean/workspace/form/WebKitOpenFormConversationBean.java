package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.attachment.WebKitAttachmentBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.lf.WebKitWorkspaceLookAndFeelBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.pi.PersonalInventoryItemVO;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.field.TableField;
import com.fluidbpm.program.api.vo.flow.Flow;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.form.FormListing;
import com.fluidbpm.program.api.vo.form.TableRecord;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.webkit.form.WebKitForm;
import com.fluidbpm.ws.client.v1.flowitem.FlowItemClient;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.menu.*;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.fluidbpm.fluidwebkit.backing.bean.workspace.pi.PersonalInventoryItemVO.PLACEHOLDER_TITLE;

@ConversationScoped
@Named("webKitOpenFormConversationBean")
public class WebKitOpenFormConversationBean extends ABaseManagedBean {
	@Inject
	@Getter
	private Conversation conversation;

	@Getter
	@Setter
	private WorkspaceFluidItem wsFluidItem;

	@Getter
	@Setter
	private Map<String, List<WorkspaceFluidItem>> tableRecordWSFluidItems;

	@Getter
	@Setter
	private Map<String, List<Field>> tableRecordViewableFields;

	@Getter
	@Setter
	private Map<String, List<Field>> tableRecordEditableFields;

	@Getter
	@Setter
	private String inputSelectedWorkflow;

	@Getter
	@Setter
	private List<String> inputWorkflowsForFormDef;

	@Getter
	@Setter
	private String areaToUpdateAfterSave;

	@Setter
	private IConversationCallback conversationCallback;

	@Getter
	@Setter
	private List<Attachment> freshAttachments;

	@Getter
	@Setter
	private List<Attachment> updatedAttachments;

	@Getter
	@Setter
	private List<Attachment> deletedAttachments;

	@Getter
	@Setter
	private Attachment attachmentToReplaceDelete;

	@Getter
	@Setter
	private MenuModel contextMenuModel;

	//----BEANS-----
	@Inject
	private WebKitAccessBean accessBean;

	@Inject
	private WebKitWorkspaceLookAndFeelBean lookAndFeelBean;

	@Inject
	private WebKitAttachmentBean attachmentBean;

	@PostConstruct
	public void init() {

	}

	/**
	 * Start the conversation scope.
	 */
	public void startConversation() {
		if (this.conversation.isTransient()) {
			this.conversation.setTimeout(TimeUnit.MINUTES.toMillis(120));
			this.conversation.begin();
		}
	}

	/**
	 * End the conversation.
	 */
	public void endConversation() {
		if (!this.conversation.isTransient()) this.conversation.end();
	}

	public boolean isConversationClosed() {
		return (this.conversation == null);
	}

	public void actionFreshLoadFormAndSet(WorkspaceFluidItem wfiParam) {
		this.setDialogHeaderTitle(null);
		this.setWsFluidItem(null);
		this.setTableRecordWSFluidItems(null);
		this.setTableRecordViewableFields(new HashMap<>());
		this.setTableRecordEditableFields(new HashMap<>());
		//Attachments...
		this.setFreshAttachments(new ArrayList<>());
		this.setUpdatedAttachments(new ArrayList<>());
		this.setDeletedAttachments(new ArrayList<>());

		//Send to a specific workflow...
		this.setInputWorkflowsForFormDef(new ArrayList<>());
		this.setInputSelectedWorkflow(null);

		if (wfiParam == null) return;
		if (this.getFluidClientDS() == null) return;

		try {
			String formType = wfiParam.getFluidItemFormType();
			this.setDialogHeaderTitle(wfiParam.getFluidItemTitle());

			WebKitForm webKitForm = this.lookAndFeelBean.getWebKitFormWithFormDef(formType);
			if (webKitForm == null && WebKitForm.EMAIL_FORM_TYPE.equals(formType)) webKitForm = WebKitForm.emailWebKitForm();
			else if (webKitForm == null) {
				throw new ClientDashboardException(
						String.format("Form Type '%s' WebKit Form not configured.", formType),
						ClientDashboardException.ErrorCode.VALIDATION);
			}

			List<Field> editable = this.accessBean.getFieldsEditableForFormDef(formType);
			List<Field> viewable = this.accessBean.getFieldsViewableForFormDef(formType);

			FormContainerClient fcClient = this.getFluidClientDS().getFormContainerClient();

			FluidItem fluidItem = new FluidItem();
			if (wfiParam.isFluidItemInWIPState()) {
				FlowItemClient flowItemClient = this.getFluidClientDS().getFlowItemClient();
				fluidItem = flowItemClient.getFluidItemByFormId(wfiParam.getFluidItemFormId());
			}

			boolean tableFormsEnabled = webKitForm.isAnyTableFormsEnabled();
			Map<String, String> fieldNameToFormDef = new HashMap<>();
			List<String> tableFieldsToAddPlaceholdersFor = new ArrayList<>();
			if (tableFormsEnabled) {
				webKitForm.getTableFieldsToInclude().forEach(tableField -> {
					Field tableFieldWithName = viewable.stream()
							.filter(itm -> itm.getFieldName().equals(tableField))
							.findFirst()
							.orElse(null);
					if (tableFieldWithName == null) return;

					String formDef = this.accessBean.getFormDefinitionFromTableField(tableFieldWithName);
					List<Field> tblEditable = this.accessBean.getFieldsEditableForFormDef(formDef);
					if (tblEditable == null) tblEditable = new ArrayList<>();
					this.getTableRecordEditableFields().put(tableField, tblEditable);

					List<Field> tblViewable = this.accessBean.getFieldsViewableForFormDef(formDef);
					if (tblViewable == null) tblViewable = new ArrayList<>();
					this.getTableRecordViewableFields().put(tableField, tblViewable);

					fieldNameToFormDef.put(tableField, formDef);

					//Ensure the logged in user has edit access to the field
					if (editable == null || editable.isEmpty()) return;
					String fieldLower = tableField.trim().toLowerCase();
					Field fieldWithNameEditable = editable.stream()
							.filter(itm -> itm.getFieldName() != null &&
									fieldLower.equals(itm.getFieldName().trim().toLowerCase()))
							.findFirst()
							.orElse(null);
					if (fieldWithNameEditable == null) return;

					//Add the first placeholder row...
					tableFieldsToAddPlaceholdersFor.add(tableField);
				});
			}

			Form freshFetchForm = new Form();
			Map<String, List<Form>> existingTableRecords = new HashMap<>();

			//Set the associated workflows...
			List<Form> formDefs = this.accessBean.getFormDefinitionsCanCreateInstanceOfSorted();
			if (formDefs == null) formDefs = new ArrayList<>();
			List<Flow> associatedFlowsForFormDef = formDefs.stream()
					.filter(itm -> itm.getFormType().equals(wfiParam.getFluidItemFormType()))
					.findFirst()
					.map(itm -> itm.getAssociatedFlows())
					.orElse(new ArrayList<>());
			if (associatedFlowsForFormDef == null) associatedFlowsForFormDef = new ArrayList<>();
			associatedFlowsForFormDef.forEach(flowItm -> this.getInputWorkflowsForFormDef().add(flowItm.getName()));
			
			if (wfiParam.isFluidItemFormSet()) {
				freshFetchForm = fcClient.getFormContainerById(wfiParam.getFluidItemFormId());
				//Lock the Form as it is being opened...
				if (webKitForm.isLockFormOnOpen() && Form.State.OPEN.equals(freshFetchForm.getState())) {
					fcClient.lockFormContainer(freshFetchForm, wfiParam.getJobView());
					this.lockByLoggedInUser(freshFetchForm);
				}

				//Fetch the table field forms...
				if (tableFormsEnabled) {
					List<FormListing> childForms =
							this.getFluidClientDSConfig().getSQLUtilWrapper().getTableForms(true, freshFetchForm);
					webKitForm.getTableFieldsToInclude().forEach(tableFieldName -> {
						List<Form> tableRecordsForField = new ArrayList<>();
						childForms.stream()
								.filter(itm -> !itm.isListingEmpty())
								.map(itm -> itm.getListing())
								.flatMap(List::stream)
								.filter(itm -> itm.getFormType() != null &&
										itm.getFormType().equals(fieldNameToFormDef.get(tableFieldName)))
								.forEach(formItm -> {
									tableRecordsForField.add(formItm);
								});
						existingTableRecords.put(tableFieldName, tableRecordsForField);
					});
				}
			} else {
				freshFetchForm.setTitle(String.format("New '%s'", wfiParam.getFluidItemFormType()));
				freshFetchForm.setFormType(wfiParam.getFluidItemFormType());
				this.setDialogHeaderTitle(freshFetchForm.getTitle());

				if (webKitForm.isSendToWorkflowAfterCreate()) {
					if (associatedFlowsForFormDef != null && associatedFlowsForFormDef.size() == 1)
						this.setInputSelectedWorkflow(associatedFlowsForFormDef.get(0).getName());
				}
			}
			fluidItem.setForm(freshFetchForm);

			this.setWsFluidItem(new WorkspaceFluidItem(wfiParam.getBaseWeb().cloneVO(fluidItem, viewable, editable, null)));
			this.getWsFluidItem().setWebKitForm(webKitForm);
			this.getWsFluidItem().setLoggedInUser(this.getLoggedInUserSafe());
			this.getWsFluidItem().setJobView(wfiParam.getJobView());
			this.getWsFluidItem().refreshFormFieldsEdit();

			//Add Placeholders...
			tableFieldsToAddPlaceholdersFor.forEach(tableFieldName -> {
				this.getWsFluidItem().getFormFieldsEdit().stream()
						.filter(itm -> tableFieldName.equals(itm.getFieldName()))
						.findFirst()
						.ifPresent(fieldWithName -> this.actionAddPlaceholderRowOfType(fieldWithName));
			});
			//Add all the table records...
			existingTableRecords.forEach((key, val) -> {
				this.getWsFluidItem().getFormFieldsEdit().stream()
						.filter(itm -> key.equals(itm.getFieldName()))
						.findFirst()
						.ifPresent(fieldWithName -> this.addTableRecordsForField(fieldWithName, val));

			});
		} catch (Exception except) {
			this.raiseError(except);
		}

		//The Context Menu...
		this.buildContextMenuForConversation();
	}

	private void buildContextMenuForConversation() {
		this.contextMenuModel = new DefaultMenuModel();

		Separator sep = new DefaultSeparator();
		//Save Button...
		if (!this.getWsFluidItem().isFormLocked() || this.getWsFluidItem().isFormLockedByLoggedInUser()) {
			DefaultMenuItem itemSave = DefaultMenuItem.builder()
					.value("Save")
					.icon("pi pi-save")
					.command("#{webKitOpenFormConversationBean.actionSaveForm('varFormDialog', '')}")
					.update("manage-product-content growl")
					.icon("pi pi-home")
					.build();
			this.contextMenuModel.getElements().add(itemSave);
			this.contextMenuModel.getElements().add(sep);
		}

		String formDef = this.getWsFluidItem().getFluidItemFormType();
		WebKitForm webKitForm = this.lookAndFeelBean.getWebKitFormWithFormDef(formDef);
		
		if (this.accessBean.attachmentCanEdit(formDef) && !"none".equals(webKitForm.getAttachmentDisplayLocation())) {
			DefaultMenuItem itemUploadNewAtt = DefaultMenuItem.builder()
					.value("Upload New Attachment")
					.icon("pi pi-cloud-upload")
					.command("#{webKitOpenFormConversationBean.actionPrepToUploadNewAttachment}")
					.process("@this :frmOpenForm")
					.update(":frmUploadAttachmentForm")
					.oncomplete("PF('varNewAttachmentUploadDialog').show();")
					.build();
			this.contextMenuModel.getElements().add(itemUploadNewAtt);

			if (this.getFreshAndExistingAttachmentCount() > 0) {
				this.contextMenuModel.getElements().add(sep);
				this.getFreshAndExistingAttachments().forEach(attItm -> {
					String command = (attItm.getId() == null || attItm.getId() < 1) ?
							String.format("#{webKitOpenFormConversationBean.actionPrepToReplaceExistingAttachmentByName('%s')}", attItm.getName()) :
							String.format("#{webKitOpenFormConversationBean.actionPrepToReplaceExistingAttachmentById(%d)}", attItm.getId());

					DefaultMenuItem itmReplAtt = DefaultMenuItem.builder()
							.value(String.format("Replace '%s'", attItm.getName()))
							.icon("fa fa-file")
							.command(command)
							.process("@this")
							.update(":frmReplaceAttachmentForm")
							.oncomplete("PF('varReplaceAttachmentUploadDialog').show();")
							.build();
					this.contextMenuModel.getElements().add(itmReplAtt);
				});

				if (this.doesUserHavePermission("delete_electronic_form")) {
					this.contextMenuModel.getElements().add(sep);
					this.getFreshAndExistingAttachments().forEach(attItm -> {
						String command = (attItm.getId() == null || attItm.getId() < 1) ?
								String.format("#{webKitOpenFormConversationBean.actionPrepToReplaceExistingAttachmentByName('%s')}", attItm.getName()) :
								String.format("#{webKitOpenFormConversationBean.actionPrepToReplaceExistingAttachmentById(%d)}", attItm.getId());
						
						DefaultMenuItem itmReplAtt = DefaultMenuItem.builder()
								.value(String.format("Delete '%s'", attItm.getName()))
								.icon("fa fa-trash")
								.command(command)
								.process("@this")
								.update(":frmDeleteAttachmentForm")
								.oncomplete("PF('varDeleteAttachmentDialog').show();")
								.build();
						this.contextMenuModel.getElements().add(itmReplAtt);
					});
				}
			}
		}
	}

	public void actionPrepToUploadNewAttachment() {

	}

	public void actionPrepToReplaceExistingAttachmentByName(String attachmentName) {
		List<Attachment> all = this.getFreshAndExistingAttachments();
		this.attachmentToReplaceDelete = all.stream()
				.filter(itm -> attachmentName.equals(itm.getName()))
				.findFirst()
				.orElse(new Attachment());
	}
	
	public void actionPrepToReplaceExistingAttachmentById(Long attachmentId) {
		List<Attachment> all = this.getFreshAndExistingAttachments();
		this.attachmentToReplaceDelete = all.stream()
				.filter(itm -> attachmentId.equals(itm.getId()))
				.findFirst()
				.orElse(new Attachment());
	}

	public void actionUploadNewAttachment(FileUploadEvent fileUploadEvent) {
		UploadedFile uploaded = fileUploadEvent.getFile();

		Attachment toAdd = new Attachment();
		toAdd.setContentType(uploaded.getContentType());
		toAdd.setAttachmentDataBase64(UtilGlobal.encodeBase64(uploaded.getContent()));
		toAdd.setName(uploaded.getFileName());

		this.getFreshAttachments().add(toAdd);
		this.attachmentBean.addAttachmentFreshToCache(toAdd, this.conversation.getId(), this.getFreshAttachments().size() - 1);

		//Rebuild the context menu...
		this.buildContextMenuForConversation();
		
		FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
				"Success", String.format("Uploaded '%s'. Remember to Save!", uploaded.getFileName()));
		FacesContext.getCurrentInstance().addMessage(null, fMsg);
	}

	public void actionReplaceExistingAttachment(FileUploadEvent fileUploadEvent) {
		try {
			UploadedFile uploaded = fileUploadEvent.getFile();

			String newAttContentType = uploaded.getContentType();
			String existingContentType = this.getAttachmentToReplaceDelete().getContentType();
			if (!newAttContentType.equals(existingContentType))
				throw new ClientDashboardException(String.format(
						"New attachment content type is '%s', expected '%s' for '%s'.",
						newAttContentType, existingContentType, this.attachmentToReplaceDelete.getName()),
						ClientDashboardException.ErrorCode.VALIDATION);

			this.attachmentToReplaceDelete.setContentType(this.getAttachmentToReplaceDelete().getContentType());
			this.attachmentToReplaceDelete.setAttachmentDataBase64(UtilGlobal.encodeBase64(uploaded.getContent()));

			//If not part of fresh attachments...Add to Update List...
			boolean isInFresh = this.getFreshAttachments().stream()
					.filter(itm -> itm.getName().equals(this.attachmentToReplaceDelete.getName()))
					.findFirst()
					.isPresent();
			if (!isInFresh) {
				//Replace Already Updated Attachment...
				Attachment inUpdated = this.getUpdatedAttachments().stream()
						.filter(itm -> itm.getName().equals(this.attachmentToReplaceDelete.getName()))
						.findFirst()
						.orElse(null);
				if (inUpdated == null) this.getUpdatedAttachments().add(this.attachmentToReplaceDelete);
			}

			//Update the Fresh Cache...
			this.attachmentBean.removeAttachmentFreshFromCache(
					this.conversation.getId(), this.getFreshAttachments().size());
			this.attachmentBean.addAttachmentFreshToCache(
					this.attachmentToReplaceDelete,
					this.conversation.getId(),
					this.getFreshAttachments().size() - 1);

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", String.format("Replaced '%s' with '%s'. Remember to Save!",
					this.attachmentToReplaceDelete.getName(), uploaded.getFileName()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public void actionDeleteExistingAttachment() {
		//Only perform delete if set...
		if (this.attachmentToReplaceDelete.getId() != null && this.attachmentToReplaceDelete.getId() > 0)
			this.getDeletedAttachments().add(this.attachmentToReplaceDelete);

		this.getFreshAttachments().stream()
				.filter(itm -> itm.getName().equals(this.attachmentToReplaceDelete.getName()))
				.findFirst()
				.ifPresent(attFomFresh -> {
					this.getFreshAttachments().remove(attFomFresh);
				});

		this.getUpdatedAttachments().stream()
				.filter(itm -> itm.getName().equals(this.attachmentToReplaceDelete.getName()))
				.findFirst()
				.ifPresent(attFomFresh -> {
					this.getUpdatedAttachments().remove(attFomFresh);
				});

		//Rebuild the context menu...
		this.buildContextMenuForConversation();

		FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
				"Success", String.format("Deleted '%s'. Remember to Save!",
				this.attachmentToReplaceDelete.getName()));
		FacesContext.getCurrentInstance().addMessage(null, fMsg);
	}

	public void actionUpdateSelectedWorkflow() {
		
	}

	public void actionAddPlaceholderRowOfType(Field tableFieldToAddFor) {
		try {
			TableField tableField = tableFieldToAddFor.getFieldValueAsTableField();
			if (tableField == null) tableField = new TableField(new ArrayList<>());
			if (tableField.isTableRecordsEmpty()) tableField.setTableRecords(new ArrayList<>());

			String formDefForNewTableRecord = this.accessBean.getFormDefinitionFromTableField(tableFieldToAddFor);
			String fieldName = tableFieldToAddFor.getFieldName();

			List<Field> tableRecordFieldsView = this.tableRecordViewableFields.get(fieldName);
			List<Field> tableRecordFieldsEditable = this.tableRecordEditableFields.get(fieldName);

			//New Form Instance...
			Form toAdd = new Form(formDefForNewTableRecord);
			FluidItem fluidItmToAdd = new FluidItem(toAdd);
			WorkspaceFluidItem wfi = new WorkspaceFluidItem(
					new PersonalInventoryItemVO(fluidItmToAdd).cloneVO(
							fluidItmToAdd, tableRecordFieldsView, tableRecordFieldsEditable, null));
			wfi.refreshFormFieldsEdit();
			toAdd.setFormFields(wfi.getFormFieldsEdit());
			toAdd.setCurrentUser(this.getLoggedInUserSafe());
			toAdd.setTitle(PLACEHOLDER_TITLE);
			
			tableField.getTableRecords().add(0, toAdd);//always on top...
			tableFieldToAddFor.setFieldValue(tableField);
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public void addTableRecordsForField(Field fieldToAddTo, List<Form> tableRecords) {
		if (tableRecords == null || tableRecords.isEmpty()) return;
		
		final TableField tableField = (fieldToAddTo.getFieldValueAsTableField() == null) ?
				new TableField() : fieldToAddTo.getFieldValueAsTableField();
		String fieldName = fieldToAddTo.getFieldName();
		List<Field> tableRecordFieldsView = this.tableRecordViewableFields.get(fieldName);
		List<Field> tableRecordFieldsEditable = this.tableRecordEditableFields.get(fieldName);

		tableRecords.stream().forEach(toAdd -> {
			FluidItem fluidItmToAdd = new FluidItem(toAdd);
			WorkspaceFluidItem wfi = new WorkspaceFluidItem(
					new PersonalInventoryItemVO(fluidItmToAdd).cloneVO(
							fluidItmToAdd, tableRecordFieldsView, tableRecordFieldsEditable, null));
			wfi.refreshFormFieldsEdit();
			toAdd.setFormFields(wfi.getFormFieldsEdit());
			if (tableField.getTableRecords() == null) tableField.setTableRecords(new ArrayList<>());
			tableField.getTableRecords().add(toAdd);
		});
		fieldToAddTo.setFieldValue(tableField);
	}

	public void actionModifyTableRecord(Form tableRecordToSave, Field tableFieldToAddFor) {
		try {
			String prevTitle = tableRecordToSave.getTitle();
			WebKitForm wkForm = this.lookAndFeelBean.getWebKitFormWithFormDef(tableRecordToSave.getFormType());
			tableRecordToSave.setTitle(this.generateNewFormTitle(
					wkForm == null ? null : wkForm.getNewFormTitleFormula(),
					tableRecordToSave.getFormType(), tableRecordToSave.getFormFields()));
			tableRecordToSave.setCurrentUser(this.getLoggedInUser());

			Field fieldBy = this.accessBean.getFieldBy(
					this.getWsFluidItem().getFluidItemFormType(),
					tableFieldToAddFor.getFieldName());
			if (fieldBy != null) tableRecordToSave.setTableFieldParentId(fieldBy.getId());

			//Add another placeholder...
			if (PLACEHOLDER_TITLE.equals(prevTitle))
				this.actionAddPlaceholderRowOfType(tableFieldToAddFor);
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public void actionOnRowEdit(RowEditEvent<Form> event) {
		FacesMessage msg = new FacesMessage("Edited",
				String.format("%s", event.getObject().getFormType()));
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}

	public void actionOnRowCancel(RowEditEvent<Form> event) {
		FacesMessage msg = new FacesMessage("Cancelled",
				String.format("%s", event.getObject().getFormType()));
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}

	public void actionSaveForm(String dialogToHideAfterSuccess, String varBtnToEnableFailedSave) {
		if (this.getFluidClientDS() == null) return;

		try {
			String actionString = "Created";
			FormContainerClient fcClient = this.getFluidClientDS().getFormContainerClient();
			FlowItemClient fiClient = this.getFluidClientDS().getFlowItemClient();
			WorkspaceFluidItem wsFlItem = this.getWsFluidItem();
			WebKitForm webKitForm = wsFlItem.getWebKitForm();

			if (wsFlItem.isFluidItemFormSet()) {
				actionString = "Updated";
				//Always lock the form first on a save...
				if (wsFlItem.isFormLockedByLoggedInUser()) {
					//Already locked by the logged in user...
				} else {
					//Lock then send it on...
					fcClient.lockFormContainer(wsFlItem.getFluidItemForm(), wsFlItem.getJobView());
					this.lockByLoggedInUser(wsFlItem.getFluidItemForm());
				}

				//Update the Table Records...
				Form formToSave = this.toFormToSave(wsFlItem);
				this.upsertTableRecordsInFluid(formToSave);

				//Existing Item...
				Long formId = fcClient.updateFormContainer(formToSave).getId();
				this.handleAttachmentStorageForForm(formId);

				if (webKitForm.isSendOnAfterSave() &&
						(wsFlItem.isFluidItemInWIPState() && wsFlItem.isFormLockedByLoggedInUser())) {
					//Send on in the workflow...
					fiClient.sendFlowItemOn(wsFlItem.getFluidItem(), true);
					actionString = String.format("%s and Sent On", actionString);
				} else if (webKitForm.isUnlockFormOnSave()) {
					//Unlock form on save...
					fcClient.unLockFormContainer(wsFlItem.getFluidItemForm());
				}
			} else {
				//Create a new item...
				boolean sendingOn = (webKitForm.isSendToWorkflowAfterCreate() &&
						UtilGlobal.isNotBlank(this.inputSelectedWorkflow));
				Form formToSave = this.toFormToSave(wsFlItem);
				final Form createdForm = fcClient.createFormContainer(formToSave, !sendingOn);
				formToSave.setId(createdForm.getId());

				//Update the table records...
				this.upsertTableRecordsInFluid(formToSave);

				//Update attachments...
				this.handleAttachmentStorageForForm(createdForm.getId());

				if (sendingOn) {
					fiClient.sendFormToFlow(createdForm, this.inputSelectedWorkflow);
					actionString = String.format("%s and Sent to '%s'", actionString, this.inputSelectedWorkflow);
				} else if (webKitForm.isUnlockFormOnSave()) {
					fcClient.unLockFormContainer(wsFlItem.getFluidItemForm());
				}
			}

			//After 'Save' Processing...
			if (this.conversationCallback != null) this.conversationCallback.afterSaveProcessing(wsFlItem);

			this.executeJavaScript(String.format("PF('%s').hide();", dialogToHideAfterSuccess));
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", String.format("'%s' %s.", this.wsFluidItem.getFluidItemTitle(), actionString));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception except) {
			if (UtilGlobal.isNotBlank(varBtnToEnableFailedSave))
				this.executeJavaScript(String.format("PF('%s').enable();", varBtnToEnableFailedSave));
			this.raiseError(except);
		}
	}

	private void handleAttachmentStorageForForm(Long formId) {
		this.getFreshAttachments().forEach(attItm -> {
			attItm.setFormId(formId);
			this.getFluidClientDS().getAttachmentClient().createAttachment(attItm);
		});
		this.getDeletedAttachments().forEach(attItm -> {
			attItm.setFormId(formId);
			this.getFluidClientDS().getAttachmentClient().deleteAttachment(attItm);
		});
		this.getUpdatedAttachments().forEach(attItm -> {
			attItm.setFormId(formId);
			this.attachmentBean.removeAttachmentFromRAWCache(attItm.getId());
			this.getFluidClientDS().getAttachmentClient().createAttachment(attItm);
		});
		this.attachmentBean.clearAttachmentCacheFor(new Form(formId));
	}

	private void upsertTableRecordsInFluid(Form formToUpdateForm) {
		if (formToUpdateForm == null || formToUpdateForm.getFormFields() == null) return;

		FormContainerClient formContClient = this.getFluidClientDS().getFormContainerClient();

		User loggedInUser = this.getLoggedInUserSafe();
		if (loggedInUser == null) return;

		formToUpdateForm.getFormFields().stream()
				//Only table fields
				.filter(itm -> itm.getTypeAsEnum() == Field.Type.Table)
				.map(itm -> itm.getFieldValueAsTableField())
				//Only where there is table records...
				.filter(tblField -> tblField.getTableRecords() != null && !tblField.getTableRecords().isEmpty())
				.map(tblRecords -> tblRecords.getTableRecords())
				.flatMap(Collection::stream)
				//Only where modified by currently logged in user and not a placeholder...
				.filter(tblRecordItm -> loggedInUser.equals(tblRecordItm.getCurrentUser()) &&
						!PLACEHOLDER_TITLE.equals(tblRecordItm.getTitle()))
				.forEach(tableRecord -> {
					try {
						Form upsertForm = null;
						if (tableRecord.getId() == null || tableRecord.getId() < 1) {
							Field parentField = new Field(tableRecord.getTableFieldParentId());
							TableRecord tableRecordCreate = new TableRecord(tableRecord, formToUpdateForm, parentField);
							upsertForm = formContClient.createTableRecord(tableRecordCreate).getFormContainer();
						} else {
							tableRecord.setDateLastUpdated(new Date());
							upsertForm = formContClient.updateFormContainer(tableRecord);
						}
						tableRecord.setId(upsertForm.getId());
					} catch (Exception except) {
						this.raiseError(except);
					}
				});
	}

	private Form toFormToSave(WorkspaceFluidItem fluidItem) {
		Form returnVal = new Form(fluidItem.getFluidItemFormType());
		returnVal.setId(fluidItem.getFluidItemFormId());
		returnVal.setFormType(fluidItem.getFluidItemFormType());
		returnVal.setFormFields(fluidItem.getFormFieldsEdit());

		WebKitForm wkForm = this.lookAndFeelBean.getWebKitFormWithFormDef(returnVal.getFormType());
		returnVal.setFormTypeId(fluidItem.getFluidItemForm().getFormTypeId());
		returnVal.setTitle(this.generateNewFormTitle(wkForm == null ? null : wkForm.getNewFormTitleFormula(),
				fluidItem.getFluidItemFormType(), fluidItem.getFormFieldsEdit()));
		return returnVal;
	}

	protected String generateNewFormTitle(
		String formula,
		String formType,
		List<Field> formFields
	) {
		if (UtilGlobal.isBlank(formula))
			return String.format("%s - %s", formType, new Date().toString());

		int lastIndexOfPipe = formula.lastIndexOf("|");
		String formFieldsString = formula.substring(lastIndexOfPipe);
		if (UtilGlobal.isBlank(formFieldsString)) return formula.substring(lastIndexOfPipe - 1);

		String[] formFieldNames = formFieldsString.substring(1).split("\\,");
		if (formFieldNames == null || formFieldNames.length < 1) return formFieldsString;

		return String.format(formula.substring(0, lastIndexOfPipe), this.toObjs(formFieldNames, formFields));
	}

	private Object[] toObjs(String[] formFieldNames, List<Field> formFields) {
		if (formFieldNames == null || formFieldNames.length == 0) return null;

		List<Object> returnVal = new ArrayList<>();
		for (String fieldName : formFieldNames) {
			String fieldNameLower = fieldName.trim().toLowerCase();
			Field fieldWithName = formFields.stream()
					.filter(itm -> itm.getFieldName() != null && fieldNameLower.equals(itm.getFieldName().toLowerCase()))
					.findFirst()
					.orElse(null);
			if (fieldWithName == null) returnVal.add(UtilGlobal.EMPTY);
			else
				returnVal.add(fieldWithName.getFieldValue());
		}
		return returnVal.toArray(new Object[]{});
	}

	private void lockByLoggedInUser(Form form) {
		form.setCurrentUser(this.getLoggedInUser());
		form.setState(Form.State.LOCKED);
	}

	public int getInputWorkflowsForFormDefCount() {
		return this.inputWorkflowsForFormDef == null ? 0 :
				this.inputWorkflowsForFormDef.size();
	}

	public boolean getRenderWorkflowSelectDropdown() {
		int workflowOptionCount = this.getInputWorkflowsForFormDefCount();
		if (workflowOptionCount < 1) return false;

		WebKitForm webKitForm = this.lookAndFeelBean.getWebKitFormWithFormDef(
				this.getWsFluidItem().getFluidItemFormType());
		//if configured to send to workflow after created, but it should not be created...
		if ((webKitForm != null && webKitForm.isSendToWorkflowAfterCreate()) &&
				(workflowOptionCount == 1 && this.getWsFluidItem().isFluidItemFormSet())) return false;
		//Or, the item is not in WIP state and locked by current user...
		return (!this.getWsFluidItem().isFluidItemInWIPState() && this.getWsFluidItem().isFormLockedByLoggedInUser());
	}

	public boolean getRenderSendOnButton() {
		if (this.getWsFluidItem() == null) return false;
		WebKitForm webKitForm = this.lookAndFeelBean.getWebKitFormWithFormDef(
				this.getWsFluidItem().getFluidItemFormType());
		if ((webKitForm != null && webKitForm.isSendOnAfterSave()) ||
				!this.getWsFluidItem().isFluidItemFormSet()) return false;
		return (this.getWsFluidItem().isFluidItemInWIPState() && this.getWsFluidItem().isFormLockedByLoggedInUser());
	}

	public boolean isTableRecordPlaceholder(Form tableRecordForm) {
		if (tableRecordForm == null) return false;
		return PLACEHOLDER_TITLE.equals(tableRecordForm.getTitle());
	}

	public List<Attachment> getFreshAndExistingAttachments() {
		List<Attachment> freshAttachments = this.getFreshAttachments();
		List<Attachment> updatedAttachments = this.getUpdatedAttachments();
		List<Attachment> deletedAttachments = this.getDeletedAttachments();

		List<Attachment> returnVal = new ArrayList<>(freshAttachments);
		returnVal.addAll(updatedAttachments);

		WorkspaceFluidItem wsFlItem = this.getWsFluidItem();
		if (wsFlItem.isFluidItemFormSet()) {
			List<Attachment> existingAttachments = this.attachmentBean.actionFetchAttachmentsForForm(wsFlItem.getFluidItemForm());
			if (existingAttachments != null) {
				existingAttachments.forEach(existingAtt -> {
					Attachment replaced = updatedAttachments.stream()
							.filter(itm -> itm.getName().equals(existingAtt.getName()))
							.findFirst()
							.orElse(null);
					if (replaced == null) returnVal.add(existingAtt);
				});
			}
		}

		//Remove the Deleted Attachments from the listing...
		deletedAttachments.forEach(deleted -> {
			Attachment toRemoveFromReturnVal = returnVal.stream()
					.filter(itm -> deleted.getName().equals(itm.getName()))
					.findFirst()
					.orElse(null);
			if (toRemoveFromReturnVal != null) returnVal.remove(toRemoveFromReturnVal);
		});

		return returnVal;
	}

	public int getFreshAndExistingAttachmentCount() {
		return this.getFreshAndExistingAttachments().size();
	}

	public boolean actionIsAttachmentNewOrUpdated(Attachment toCheck) {
		if (this.getFreshAttachments().isEmpty()) return false;

		return this.getFreshAttachments().stream()
				.filter(itm -> itm.getName().equals(toCheck.getName()))
				.findFirst()
				.isPresent();
	}

	public StreamedContent actionFetchDataForFreshAttachment(Attachment toCheck) {
		return DefaultStreamedContent.builder()
				.name(toCheck.getName())
				.contentType(toCheck.getContentType())
				.stream(() -> {
					try {
						return new ByteArrayInputStream(UtilGlobal.decodeBase64(toCheck.getAttachmentDataBase64()));
					} catch (Exception err) {
						raiseError(err);
						return null;
					}
				}).build();
	}

	public String actionGenerateURLForThumbnail(WorkspaceFluidItem wfItem, Attachment attachment, int thumbnailScale) {
		if (this.actionIsAttachmentNewOrUpdated(attachment)) {
			int attachmentIndex = this.getFreshAttachments().indexOf(attachment);
			String postFix = String.format(
					"/get_new_or_updated_image?conversationId=%s&attachmentIndex=%d&thumbnailScale=%d",
					UtilGlobal.encodeURL(this.conversation.getId()),
					attachmentIndex,
					thumbnailScale);
			return postFix;
		} else {
			return this.attachmentBean.actionGenerateURLForThumbnail(wfItem, attachment, thumbnailScale);
		}
	}

}
