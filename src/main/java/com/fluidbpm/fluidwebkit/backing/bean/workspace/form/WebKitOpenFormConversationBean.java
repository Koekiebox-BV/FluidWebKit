package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.attachment.WebKitAttachmentBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitConfigBean;
import com.fluidbpm.fluidwebkit.backing.bean.som.GlobalIssuerBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.field.WebKitField;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.lf.WebKitWorkspaceLookAndFeelBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.pi.PersonalInventoryItemVO;
import com.fluidbpm.fluidwebkit.backing.utility.WebUtil;
import com.fluidbpm.fluidwebkit.backing.utility.field.FieldMappingUtil;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.fluidwebkit.exception.CustomExecutionException;
import com.fluidbpm.fluidwebkit.exception.MandatoryFieldsException;
import com.fluidbpm.program.api.util.GeoUtil;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.ABaseListing;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import com.fluidbpm.program.api.vo.field.DecimalMetaFormat;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.field.TableField;
import com.fluidbpm.program.api.vo.flow.Flow;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.form.FormListing;
import com.fluidbpm.program.api.vo.form.TableRecord;
import com.fluidbpm.program.api.vo.item.CustomWebAction;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.payment.PaymentLinkAdyen;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.webkit.form.WebKitForm;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.flowitem.FlowItemClient;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import com.fluidbpm.ws.client.v1.payment.PaymentClient;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.map.GeocodeEvent;
import org.primefaces.event.map.ReverseGeocodeEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.map.*;
import org.primefaces.model.menu.*;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.fluidbpm.fluidwebkit.backing.bean.workspace.pi.PersonalInventoryItemVO.PLACEHOLDER_TITLE;

@ConversationScoped
@Named("webKitOpenFormConversationBean")
public class WebKitOpenFormConversationBean extends ABaseManagedBean {
	public static final String MAP_CENTER_DEFAULT = "41.850033, -87.6500523";
	public static final String MAP_ZOOM_DEFAULT = "1";
	public static final String MAP_ZOOM_CLOSE = "19";
	public static final String VISIBLE_FOR_CHECKER = "Visible For Checker";

	@Inject
	@Getter
	private Conversation conversation;

	@Inject
	private WebKitOpenFormFieldHistoryConversationBean fieldHistoryConvBean;

	@Inject
	private WebKitConfigBean webKitConfigBean;

	@Inject
	private GlobalIssuerBean globalIssuerBean;

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

	@Getter
	@Setter
	private boolean inputIncludeCompanyLogo;

	@Getter
	@Setter
	private boolean inputIncludeProperties;

	@Getter
	@Setter
	private boolean inputIncludeAncestor;

	@Getter
	@Setter
	private boolean inputIncludeDescendant;

	@Getter
	@Setter
	private MapModel conversationMapModel;

	@Getter
	@Setter
	private WebKitField activeGeoField;

	@Getter
	@Setter
	private GeoUtil activeGeo;

	@Getter
	@Setter
	private String mapCenter;

	@Getter
	@Setter
	private String mapZoom;
	
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

		wfiParam.resetFormFieldsEditMandatoryAndEmpty();

		try {
			String formType = wfiParam.getFluidItemFormType();
			String titleToUse = wfiParam.getFluidItemTitle();
			if ((formType != null && titleToUse != null) && !titleToUse.contains(formType)) {
				titleToUse = String.format("%s - %s", formType, titleToUse);
			}

			this.setDialogHeaderTitle(titleToUse);

			WebKitForm webKitForm = this.getWebKitFormWithFormDef(wfiParam);
			if (webKitForm == null && WebKitForm.EMAIL_FORM_TYPE.equals(formType)) webKitForm = WebKitForm.emailWebKitForm();
			else if (webKitForm == null) throw new ClientDashboardException(
					String.format("Form Type '%s' WebKit Form not configured.", formType),
					ClientDashboardException.ErrorCode.VALIDATION);

			List<Field> editable = this.accessBean.getFieldsEditableForFormDef(formType);
			List<Field> viewable = this.accessBean.getFieldsViewableForFormDef(formType);

			FormContainerClient fcClient = this.getFluidClientDS().getFormContainerClient();
			FluidItem fluidItem = new FluidItem();
			FlowItemClient flowItemClient = this.getFluidClientDS().getFlowItemClient();
			try {
				Long fluidItemFormId = wfiParam.getFluidItemFormId();
				if (fluidItemFormId != null && fluidItemFormId > 0) {
					fluidItem = flowItemClient.getFluidItemByFormId(fluidItemFormId, true, webKitForm.isEnableCalculatedLabels());
				}
			} catch (FluidClientException fce) {
				if (FluidClientException.ErrorCode.NO_RESULT != fce.getErrorCode()) throw fce;
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

			Map<String, List<Form>> existingTableRecords = new HashMap<>();

			//Set the associated workflows...
			List<Form> formDefs = this.accessBean.getFormDefinitionsCanCreateInstanceOfSorted();
			if (formDefs == null) formDefs = new ArrayList<>();
			List<Flow> associatedFlowsForFormDef = formDefs.stream()
					.filter(itm -> itm.getFormType().equals(wfiParam.getFluidItemFormType()))
					.findFirst()
					.map(Form::getAssociatedFlows)
					.orElse(new ArrayList<>());
			associatedFlowsForFormDef.forEach(flowItm -> this.getInputWorkflowsForFormDef().add(flowItm.getName()));

			Form freshFetchForm = new Form();
			if (wfiParam.isFluidItemFormSet()) {
				freshFetchForm = fluidItem.getForm();
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
								.map(ABaseListing::getListing)
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
					if (associatedFlowsForFormDef != null && associatedFlowsForFormDef.size() == 1) {
						this.setInputSelectedWorkflow(associatedFlowsForFormDef.get(0).getName());
					}
				}
			}
			fluidItem.setForm(freshFetchForm);

			this.setWsFluidItem(new WorkspaceFluidItem(wfiParam.getBaseWeb().cloneVO(fluidItem, viewable, editable, null)));
			this.getWsFluidItem().setWebKitForm(webKitForm);
			this.getWsFluidItem().setLoggedInUser(this.getLoggedInUserSafe());
			this.getWsFluidItem().setJobView(wfiParam.getJobView());
			this.getWsFluidItem().refreshFormFieldsEdit();
			//  Set the Default Field Values:
			if (!wfiParam.isFluidItemFormSet()) {
				this.setDefaultFieldValues(this.getWsFluidItem().getFormFieldsEdit(), webKitForm);
			}

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
			this.applyCurrencyFormatting();
		} catch (Exception except) {
			this.raiseError(except);
		}

		//The Context Menu...
		this.buildContextMenuForConversation();
	}

	private void applyCurrencyFormatting() {
		if (!this.webKitConfigBean.getEnableSOMPostCardFeatures()) return;

		Currency currencyOverride = null;
		try {
			currencyOverride = this.webKitConfigBean.getIssuerVelocityCurrency(
					this.getFluidClientDSConfig().getSQLUtilWrapper(),
					this.globalIssuerBean.getActiveIssuer()
			);
		} catch (Exception exc) {
			this.getLogger().error(exc.getMessage(), exc);
		}
		if (currencyOverride == null) return;

		final Currency constCurr = currencyOverride;
		// Main Form fields (edit and view fields):
		this.getWsFluidItem().getFormFieldsEdit().stream()
				.filter(this::filterCurrencyFieldCanBeNullValue)
				.forEach(field -> this.processCurrencyOverride(field, constCurr));
		this.getWsFluidItem().getFormFieldsViewable().stream()
				.filter(this::filterCurrencyFieldCanBeNullValue)
				.forEach(field -> this.processCurrencyOverride(field, constCurr));

		// Update the table field viewable map:
		if (this.tableRecordViewableFields != null) {
			this.tableRecordViewableFields.forEach((key, val) -> {
				val.stream()
						.filter(this::filterCurrencyFieldCanBeNullValue)
						.forEach(field -> {
							this.processCurrencyOverride(field, constCurr);
						});
			});
		}

		// Only Table fields:
		this.getWsFluidItem().getFormFieldsEdit().stream()
				.filter(itm -> itm.getTypeAsEnum() == Field.Type.Table)
				.map(itm -> itm.getFieldValueAsTableField())
				//Only where there is table records...
				.filter(tblField -> tblField != null && (tblField.getTableRecords() != null && !tblField.getTableRecords().isEmpty()))
				.map(tblRecords -> tblRecords.getTableRecords())
				.flatMap(Collection::stream)
				.forEach(tableRecord -> {tableRecord.getFormFields().stream()
						.filter(this::filterCurrencyFieldCanBeNullValue)
						.forEach(field -> this.processCurrencyOverride(field, constCurr));
				});
		this.getWsFluidItem().getFormFieldsViewable().stream()
				.filter(itm -> itm.getTypeAsEnum() == Field.Type.Table)
				.map(itm -> itm.getFieldValueAsTableField())
				//Only where there is table records...
				.filter(tblField -> tblField != null && (tblField.getTableRecords() != null && !tblField.getTableRecords().isEmpty()))
				.map(tblRecords -> tblRecords.getTableRecords())
				.flatMap(Collection::stream)
				.forEach(tableRecord -> {tableRecord.getFormFields().stream()
						.filter(this::filterCurrencyFieldCanBeNullValue)
						.forEach(field -> this.processCurrencyOverride(field, constCurr));
				});
	}

	private void processCurrencyOverride(Field field, Currency currencyOverride) {
		String currForOverride = DecimalMetaFormat.format(field.getDecimalMetaFormat(), currencyOverride);
		field.setTypeMetaData(currForOverride);
	}

	private void buildContextMenuForConversation() {
		this.contextMenuModel = new DefaultMenuModel();

		if (this.getWsFluidItem() == null) return;

		Separator sep = new DefaultSeparator();
		//Save Button...
		if (!this.getWsFluidItem().isFormLocked() || this.getWsFluidItem().isFormLockedByLoggedInUser()) {

			final String areaToUpdate;
			if (UtilGlobal.isBlank(this.getAreaToUpdateAfterSave())) areaToUpdate = UtilGlobal.EMPTY;
			else areaToUpdate = String.format(" %s", this.getAreaToUpdateAfterSave());
			DefaultMenuItem itemSave = DefaultMenuItem.builder()
					.value("Save")
					.icon("pi pi-save")
					.command("#{webKitOpenFormConversationBean.actionSaveForm('varFormDialog', '')}")
					.update(String.format("manage-product-content growl%s", areaToUpdate))
					.build();
			this.contextMenuModel.getElements().add(itemSave);
		}

		String formDef = this.getWsFluidItem().getFluidItemFormType();
		WebKitForm webKitForm = this.getWebKitFormWithFormDef(this.getWsFluidItem());
		if (webKitForm == null) {
			getLogger().error(String.format("WebKitForm is not set for Def [%s].", formDef),
					new IllegalStateException());
			return;
		}

		//Enable attachment upload components....
		String attDisplayType = webKitForm.getAttachmentDisplayType();

		if (UtilGlobal.isBlank(attDisplayType) || "galleria".equals(attDisplayType) &&
				(this.accessBean.attachmentCanEdit(formDef) && !"none".equals(webKitForm.getAttachmentDisplayLocation()))) {
			this.contextMenuModel.getElements().add(sep);
			
			DefaultMenuItem itemUploadNewAtt = DefaultMenuItem.builder()
					.value("Upload New Attachment")
					.icon("pi pi-upload")
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
							.icon("pi pi-share-alt")
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
								.icon("pi pi-trash")
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

		//Goto...
		this.contextMenuModel.getElements().add(sep);
		DefaultMenuItem itemGotoFields = DefaultMenuItem.builder()
				.value("Goto 'Input Form'")
				.url("#frmOpenForm:outPanelFormBdy")
				.icon("pi pi-link")
				.build();

		// TODO need to look at all label fields, and add placeholders for them here...

		DefaultMenuItem itemGotoAttachments = DefaultMenuItem.builder()
				.value("Goto 'Attachments'")
				.url("#attachmentsAsListingGridList")
				.icon("pi pi-link")
				.build();
		
		DefaultMenuItem itemPrint = DefaultMenuItem.builder()
				.value("Print")
				.command("#{webKitOpenFormConversationBean.actionPrepToPrint()}")
				.process("@this")
				.update(":frmPrintForm")
				.oncomplete("PF('varPrintFormDialog').show();")
				.icon("pi pi-print")
				.build();

		this.contextMenuModel.getElements().add(itemGotoFields);

		if (!webKitForm.isAttachmentDisplayLocationNone()) {
			this.contextMenuModel.getElements().add(itemGotoAttachments);
		}

		if (this.doesUserHavePermission("print_forms")) {
			this.contextMenuModel.getElements().add(sep);
			this.contextMenuModel.getElements().add(itemPrint);
		}
	}

	public void actionPrepToUploadNewAttachment() {

	}

	public StreamedContent getPrintFileContent() {
		Long formId = this.getLongRequestParam("formId");
		Boolean inputIncludeCompanyLogo = this.getBooleanRequestParam("inputIncludeCompanyLogo");
		Boolean inputIncludeProperties = this.getBooleanRequestParam("inputIncludeProperties");

		try {
			FormContainerClient fcc = this.getFluidClientDS().getFormContainerClient();
			Attachment pdfPrint = fcc.printForm(
				this.wsFluidItem.getFluidItemForm(),
				this.inputIncludeCompanyLogo,
				this.inputIncludeAncestor,
				this.inputIncludeDescendant,
				this.inputIncludeProperties
			);

			return WebUtil.pfStreamContentFrom(
					new ByteArrayInputStream(pdfPrint.getAttachmentDataRAW()),
					"image/svg+xml",
					pdfPrint.getName());
		} catch (Exception except) {
			this.raiseError(except);
			return null;
		}
	}

	public void actionOnBoolChange(AjaxBehaviorEvent boolEvent) {

	}

	public void actionPrepToPrint() {
		this.inputIncludeCompanyLogo = true;
		this.inputIncludeProperties = false;
		this.inputIncludeAncestor = false;
		this.inputIncludeDescendant = false;
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

		String attName = uploaded.getFileName();

		try {
			if (this.getFreshAndExistingAttachments() != null) {
				Attachment existingWithName = this.getFreshAndExistingAttachments().stream()
						.filter(itm -> attName.equalsIgnoreCase(itm.getName()))
						.findFirst()
						.orElse(null);
				if (existingWithName != null) {
					throw new ClientDashboardException(String.format(
						"Attachment with name '%s' already exists. Upload new version or delete existing.",
							attName
					), ClientDashboardException.ErrorCode.VALIDATION);
				}
			}

			Attachment toAdd = new Attachment();
			toAdd.setContentType(uploaded.getContentType());
			toAdd.setAttachmentDataBase64(UtilGlobal.encodeBase64(uploaded.getContent()));
			toAdd.setName(attName);

			this.getFreshAttachments().add(toAdd);
			this.attachmentBean.addAttachmentFreshToCache(toAdd, this.conversation.getId(), this.getFreshAttachments().size() - 1);

			//Rebuild the context menu...
			this.buildContextMenuForConversation();

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", String.format("Uploaded '%s'. Remember to Save!", uploaded.getFileName()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception except) {
			this.raiseError(except);
		}
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

		// Rebuild the context menu:
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
			toAdd.setFormFields(wfi.getFormFieldsEditAsFields());
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
			toAdd.setFormFields(wfi.getFormFieldsEditAsFields());
			if (tableField.getTableRecords() == null) tableField.setTableRecords(new ArrayList<>());
			tableField.getTableRecords().add(toAdd);
		});
		fieldToAddTo.setFieldValue(tableField);
	}

	public void actionModifyTableRecord(Form tableRecordToSave, Field tableFieldToAddFor) {
		try {
			if (tableRecordToSave.isAllFormFieldsEmpty()) {
				throw new MandatoryFieldsException("All table fields are empty. Not allowed.");
			}
			
			String prevTitle = tableRecordToSave.getTitle();
			WebKitForm wkForm = this.lookAndFeelBean.getWebKitFormWithFormDef(tableRecordToSave.getFormType());
			tableRecordToSave.setTitle(FieldMappingUtil.generateNewFormTitle(
				this.getDateFormat(),
				wkForm == null ? null : wkForm.getNewFormTitleFormula(),
				tableRecordToSave.getFormType(),
				tableRecordToSave.getFormFields())
			);
			tableRecordToSave.setCurrentUser(this.getLoggedInUser());

			Field fieldBy = this.accessBean.getFieldBy(this.getWsFluidItem().getFluidItemFormType(), tableFieldToAddFor.getFieldName());
			if (fieldBy != null) tableRecordToSave.setTableFieldParentId(fieldBy.getId());

			//Apply the Custom Action when applicable...
			String customActionSave = this.getThirdPartyWebActionSaveForFormType(tableRecordToSave.getFormType());
			if (customActionSave != null) {
				FormContainerClient fcc = this.getFluidClientDS().getFormContainerClient();
				CustomWebAction execActionResult = fcc.executeCustomWebAction(
						customActionSave, true, fieldBy.getId(), tableRecordToSave);
				this.mergeFormFieldsFromCustomAction(execActionResult, tableRecordToSave);
			}

			//Add another placeholder...
			if (PLACEHOLDER_TITLE.equals(prevTitle)) {
				this.actionAddPlaceholderRowOfType(tableFieldToAddFor);
			}
		} catch (Exception except) {
			if (except instanceof MandatoryFieldsException) {
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_WARN, "Mandatory.", except.getMessage());
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
			} else if (except instanceof CustomExecutionException) {
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error.", except.getMessage());
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
			} else this.raiseError(except);
		}
	}

	public void actionDeleteTableRecord(Form tableRecordToDel, Field tableFieldToDelFor, int tblRecordIndex) {
		try {
			FormContainerClient fcc = this.getFluidClientDS().getFormContainerClient();

			if (tableRecordToDel.getId() != null && tableRecordToDel.getId().longValue() > 0) {
				fcc.deleteFormContainer(tableRecordToDel);
			}
			TableField tblField = tableFieldToDelFor.getFieldValueAsTableField();
			List<Form> tbRecords = tblField == null ? null : tblField.getTableRecords();
			if (tbRecords != null && tblRecordIndex > -1) tbRecords.remove(tblRecordIndex);

			String prevTitle = tableRecordToDel.getTitle();
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", String.format("Deleted '%s'.", prevTitle));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public void actionOnRowEdit(RowEditEvent<Form> event) {
		FacesMessage msg = new FacesMessage("Edited", String.format("%s - %s",
				event.getObject().getFormType(), event.getObject().getTitle()));
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}

	public void actionOnRowCancel(RowEditEvent<Form> event) {
		FacesMessage msg = new FacesMessage("Cancelled",
				String.format("%s", event.getObject().getFormType()));
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}

	public void actionPerformCustomWebActionOnForm(String customAction, String varBtnToEnableFailedSave, int buttonIndex) {
		if (this.getFluidClientDS() == null) return;
		
		try {
			FormContainerClient fcClient = this.getFluidClientDS().getFormContainerClient();
			WorkspaceFluidItem wsFlItem = this.getWsFluidItem();
			if (wsFlItem == null) {
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_WARN,
						"Failure", String.format("Workspace Item is not set. Not allowed to execute '%s'.", customAction));
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
				return;
			}

			Form updatedForm = this.toFormToSave(wsFlItem, fcClient, customAction);

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", String.format("'%s' %s.", updatedForm.getTitle(), customAction));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception except) {
			this.raiseError(except);
		} finally {
			if (UtilGlobal.isNotBlank(varBtnToEnableFailedSave)) {
				this.executeJavaScript(String.format("PF('%s').enable();", String.format("%s%s", varBtnToEnableFailedSave, buttonIndex)));
			}
		}
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
				// Always lock the form first on a save:
				if (!wsFlItem.isFormLockedByLoggedInUser()) {
					//Lock then send it on:
					fcClient.lockFormContainer(wsFlItem.getFluidItemForm(), wsFlItem.getJobView());
					this.lockByLoggedInUser(wsFlItem.getFluidItemForm());
				}

				// Update the Table Records:
				Form formToSave = this.toFormToSave(wsFlItem, fcClient);
				this.upsertTableRecordsInFluid(formToSave);

				// Existing Item:
				Long formId = fcClient.updateFormContainer(formToSave).getId();
				this.handleAttachmentStorageForForm(formId);

				// Locked by current user:
				if (wsFlItem.isFormLockedByLoggedInUser()) {
					//Set to Send on After and Form in Workflow state
					if (webKitForm.isSendOnAfterSave() && wsFlItem.isFluidItemInWIPState()) {
						// Send on in the workflow:
						fiClient.sendFlowItemOn(wsFlItem.getFluidItem(), true);
						actionString = String.format("%s and placed successfully. Please verify processed results for final status.", actionString);
					} else if (!wsFlItem.isFluidItemInWIPState() && UtilGlobal.isNotBlank(this.inputSelectedWorkflow)) {
						// Not in workflow and a workflow route has been selected:
						fiClient.sendFormToFlow(wsFlItem.getFluidItemForm(), this.inputSelectedWorkflow);
						actionString = String.format("%s and placed on '%s' successfully. Please verify processed results for final status.",
								actionString, this.inputSelectedWorkflow);
					} else if (webKitForm.isUnlockFormOnSave()) {
						// Unlock form on save:
						fcClient.unLockFormContainer(wsFlItem.getFluidItemForm());
					}
				}
			} else {
				//Create a new item:
				boolean sendingOn = (webKitForm.isSendToWorkflowAfterCreate() &&
						UtilGlobal.isNotBlank(this.inputSelectedWorkflow));
				Form formToSave = this.toFormToSave(wsFlItem, fcClient);
				final Form createdForm = fcClient.createFormContainer(formToSave, !sendingOn);
				formToSave.setId(createdForm.getId());

				//Update the table records:
				this.upsertTableRecordsInFluid(formToSave);

				//Update attachments:
				this.handleAttachmentStorageForForm(createdForm.getId());

				if (sendingOn) {
					fiClient.sendFormToFlow(createdForm, this.inputSelectedWorkflow);
					actionString = String.format("%s and Sent to '%s'", actionString, this.inputSelectedWorkflow);
				} else if (webKitForm.isUnlockFormOnSave()) {
					fcClient.unLockFormContainer(createdForm);
				}
			}

			// After 'Save' Processing:
			if (UtilGlobal.isBlank(dialogToHideAfterSuccess)) {
				if (this.conversationCallback != null) {
					this.conversationCallback.afterSaveProcessing(wsFlItem);
				}
			} else {
				// The close event will execute the workspace update:
				this.executeJavaScript(String.format("PF('%s').hide();", dialogToHideAfterSuccess));
			}

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", String.format("'%s' %s.", this.wsFluidItem.getFluidItemTitle(), actionString));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception except) {
			if (UtilGlobal.isNotBlank(varBtnToEnableFailedSave)) {
				this.executeJavaScript(String.format("PF('%s').enable();", varBtnToEnableFailedSave));
			}

			if (except instanceof MandatoryFieldsException) {
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_WARN,
						"Mandatory.", String.format("'%s' %s.", this.wsFluidItem.getFluidItemTitle(), except.getMessage()));
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
			} else if (except instanceof CustomExecutionException) {
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error.", except.getMessage());
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
			} else this.raiseError(except);
		}
	}

	public void actionSetAttachmentToPreviewWithDialog(Attachment attachment) {
		this.attachmentToReplaceDelete = attachment;
	}

	public void actionSendOn(String dialogToHideAfterSuccess, String varBtnToEnableFailedSendOn) {
		try {
			FlowItemClient fiClient = this.getFluidClientDS().getFlowItemClient();
			WorkspaceFluidItem wsFlItem = this.getWsFluidItem();
			fiClient.sendFlowItemOn(wsFlItem.getFluidItem(), true);

			if (UtilGlobal.isBlank()) {
				if (this.conversationCallback != null) {
					this.conversationCallback.afterSendOnProcessing(wsFlItem);
				}
			} else {
				this.executeJavaScript(String.format("PF('%s').hide();", dialogToHideAfterSuccess));
			}
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", String.format("'%s' %s.", this.wsFluidItem.getFluidItemTitle(), "Send On."));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception except) {
			if (UtilGlobal.isNotBlank(varBtnToEnableFailedSendOn))
				this.executeJavaScript(String.format("PF('%s').enable();", varBtnToEnableFailedSendOn));
			this.raiseError(except);
		}
	}

	public void actionCloseDialog() {
		try {
			if (this.conversationCallback != null) this.conversationCallback.closeFormDialog();
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public void actionLoadFormHistoricData(String btnToReEnable, String dialogToShowIfSuccess) {
		String jsToExec = "";
		if (UtilGlobal.isNotBlank(btnToReEnable)) {
			jsToExec = String.format("PF('%s').enable();", btnToReEnable);
		}
		try {
			this.fieldHistoryConvBean.setHistoryForm(this.getWsFluidItem().getFluidItemForm());
			this.fieldHistoryConvBean.actionLoadFormHistoricData(true);

			jsToExec = String.format("%sPF('%s').show();", jsToExec, dialogToShowIfSuccess);
		} finally {
			this.executeJavaScript(jsToExec);
		}
	}

	public void actionLoadFormHistoricDataTableRecord(
		String btnToReEnable,
		String dialogToShowIfSuccess,
		Long recordFormId,
		String formType
	) {
		String jsToExec = UtilGlobal.EMPTY;
		try {
			Form frm = new Form(recordFormId);
			frm.setFormType(formType);
			this.fieldHistoryConvBean.setHistoryForm(frm);
			this.fieldHistoryConvBean.actionLoadFormHistoricData(false);

			jsToExec = String.format("%sPF('%s').show();", jsToExec, dialogToShowIfSuccess);
		} finally {
			this.executeJavaScript(jsToExec);
		}
	}

	public void actionCloseFormHistoricData() {
		
	}

	private void handleAttachmentStorageForForm(Long formId) {
		this.getFreshAttachments().forEach(attItm -> {
			attItm.setFormId(formId);
			Attachment created = this.getFluidClientDS().getAttachmentClient().createAttachment(attItm);
			attItm.setId(created.getId());
			this.attachmentBean.removeAttachmentFromRAWCache(attItm.getId());
		});
		this.getDeletedAttachments().forEach(attItm -> {
			attItm.setFormId(formId);
			this.attachmentBean.removeAttachmentFromRAWCache(attItm.getId());
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
				// Only table fields:
				.filter(itm -> itm.getTypeAsEnum() == Field.Type.Table)
				.map(itm -> itm.getFieldValueAsTableField())
				// Only where there is table records:
				.filter(tblField -> tblField != null && (tblField.getTableRecords() != null && !tblField.getTableRecords().isEmpty()))
				.map(tblRecords -> tblRecords.getTableRecords())
				.flatMap(Collection::stream)
				// Only where modified by currently logged in user and not a placeholder:
				.filter(tblRecordItm -> loggedInUser.equals(tblRecordItm.getCurrentUser()) &&
						!PLACEHOLDER_TITLE.equals(tblRecordItm.getTitle()))
				.forEach(tableRecord -> {
					try {
						// Remap the Currency Fields to store as minor denomination:
						if (tableRecord.getFormFields() != null) {
							tableRecord.getFormFields().stream()
									.filter(this::filterCurrencyField)
									.filter(field -> this.accessBean.isFieldEditable(tableRecord.getFormType(), field.getFieldName()))
									.forEach(decimalFieldCurrMinor -> {
										Currency currency = decimalFieldCurrMinor.getDecimalMetaFormat().getAmountCurrency();
										decimalFieldCurrMinor.getDecimalMetaFormat().setAmountCurrency(null);
										Double oldVal = decimalFieldCurrMinor.getFieldValueAsDouble();
										decimalFieldCurrMinor.getDecimalMetaFormat().setAmountCurrency(currency);

										Double newVal = new BigDecimal(oldVal).movePointRight(
												currency.getDefaultFractionDigits()
										).doubleValue();
										decimalFieldCurrMinor.setFieldValueAsDouble(newVal);
									});
						}

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

	private Form toFormToSave(
			WorkspaceFluidItem fluidItem,
			FormContainerClient formContClient
	) {
		return this.toFormToSave(fluidItem, formContClient, null);
	}

	private Form toFormToSave(
		WorkspaceFluidItem fluidItem,
		FormContainerClient formContClient,
		String customActionForceUsage
	) {
		Form returnVal = new Form(fluidItem.getFluidItemFormType());
		returnVal.setId(fluidItem.getFluidItemFormId());
		returnVal.setFormType(fluidItem.getFluidItemFormType());

		fluidItem.resetFormFieldsEditMandatoryAndEmpty();
		List<Field> editFormFields = fluidItem.getFormFieldsEditAsFields();
		// Remap the Currency Fields to store as minor denomination:
		if (editFormFields != null) {
			editFormFields.stream()
					.filter(this::filterCurrencyField)
					.filter(field -> this.accessBean.isFieldEditable(returnVal.getFormType(), field.getFieldName()))
					.forEach(decimalFieldCurrMinor -> {
						Currency currency = decimalFieldCurrMinor.getDecimalMetaFormat().getAmountCurrency();
						decimalFieldCurrMinor.getDecimalMetaFormat().setAmountCurrency(null);
						Double oldVal = decimalFieldCurrMinor.getFieldValueAsDouble();
						decimalFieldCurrMinor.getDecimalMetaFormat().setAmountCurrency(currency);

						Double newVal = new BigDecimal(oldVal).movePointRight(
							currency.getDefaultFractionDigits()
						).doubleValue();
						decimalFieldCurrMinor.setFieldValueAsDouble(newVal);
					});
		}

		returnVal.setFormFields(editFormFields);

		WebKitForm wkForm = this.lookAndFeelBean.getWebKitFormWithFormDef(returnVal.getFormType());
		if (wkForm == null) {
			throw new ClientDashboardException(
					String.format("No WebKit Form of type '%s'.", returnVal.getFormType()),
					ClientDashboardException.ErrorCode.VALIDATION);
		}
		returnVal.setFormTypeId(fluidItem.getFluidItemForm().getFormTypeId());
		returnVal.setFormType(fluidItem.getFluidItemForm().getFormType());
		returnVal.setTitle(FieldMappingUtil.generateNewFormTitle(
				this.getDateFormat(),
				wkForm.getNewFormTitleFormula(),
				fluidItem.getFluidItemFormType(),
				fluidItem.getFormFieldsEditAsFields())
		);

		//Apply the Custom Action when applicable...
		String customAction = null;
		if (UtilGlobal.isBlank(customActionForceUsage)) {
			customAction = this.getThirdPartyWebActionSaveForFormType(returnVal.getFormType());
			//Ensure the mandatory fields are set...
			AtomicBoolean anyEmpty = new AtomicBoolean(false);
			if (wkForm.getMandatoryFields() != null && !wkForm.getMandatoryFields().isEmpty()) {
				wkForm.getMandatoryFields().forEach(manField -> {
					WebKitField wkField = fluidItem.getFormFieldsEditWithName(manField);

					if (wkField != null && wkField.isFieldValueEmpty()) {
						anyEmpty.set(true);
						wkField.setMandatoryAndEmpty(true);
					}
				});
			}
			if (anyEmpty.get()) throw new MandatoryFieldsException("Please populate mandatory fields.");
		} else {
			List<String> allCustomActions = this.getThirdPartyWebActionTaskIdsForFormType(returnVal.getFormType());
			customAction = allCustomActions.stream()
					.filter(itm -> itm.equals(customActionForceUsage))
					.findFirst()
					.orElse(null);
		}

		if (customAction != null) {
			try {
				CustomWebAction execActionResult = formContClient.executeCustomWebAction(customAction, returnVal);
				this.mergeFormFieldsFromCustomAction(execActionResult, returnVal);
			} catch (Exception err) {
				throw new CustomExecutionException(err.getMessage(), err);
			}
		}

		return returnVal;
	}

	private boolean filterCurrencyFieldCanBeNullValue(Field field) {
		if (field == null) return false;
		return field.getTypeAsEnum() == Field.Type.Decimal &&
				(field.getDecimalMetaFormat() != null && field.getDecimalMetaFormat().isAmountMinorWithCurrency());
	}

	private boolean filterCurrencyField(Field field) {
		if (field == null) return false;
		return (field.getTypeAsEnum() == Field.Type.Decimal && field.getFieldValueAsLong() != null) &&
				(field.getDecimalMetaFormat() != null && field.getDecimalMetaFormat().isAmountMinorWithCurrency());
	}

	private void mergeFormFieldsFromCustomAction(CustomWebAction execActionResult, Form formToUpdate) {
		Form execForm = execActionResult.getForm();
		if (execForm != null && (execForm.getTitle() != null && !execForm.getTitle().trim().isEmpty())) {
			formToUpdate.setTitle(execForm.getTitle());
		}
		List<Field> execFields = (execForm == null) ? null : execForm.getFormFields();
		if (execFields == null || execFields.isEmpty()) return;

		execFields.stream()
				.filter(itm -> itm.getTypeAsEnum() != Field.Type.Table)//We do not update table records...
				.forEach(fieldItemFromExec -> {
					//Ignore Table Records...
					if (fieldItemFromExec.getTypeAsEnum() == null || fieldItemFromExec.getTypeAsEnum() == Field.Type.Table) return;

					// Set field values from Exec result:
					formToUpdate.setFieldValue(
							fieldItemFromExec.getFieldName(),
							fieldItemFromExec.getFieldValue(),
							fieldItemFromExec.getTypeAsEnum()
					);
		});
	}

	private void lockByLoggedInUser(Form form) {
		form.setCurrentUser(this.getLoggedInUser());
		form.setState(Form.State.LOCKED);
	}

	public int getInputWorkflowsForFormDefCount() {
		return this.inputWorkflowsForFormDef == null ? 0 : this.inputWorkflowsForFormDef.size();
	}

	public boolean getRenderWorkflowSelectDropdown() {
		int workflowOptionCount = this.getInputWorkflowsForFormDefCount();
		if (workflowOptionCount < 2) return false;

		WebKitForm webKitForm = this.getWebKitFormWithFormDef(this.getWsFluidItem());
		//if configured to send to workflow after created, but it should not be created...
		if ((webKitForm != null && !webKitForm.isSendToWorkflowAfterCreate())) return false;
		//Or, the item is not in WIP state and locked by current user...
		return (!this.getWsFluidItem().isFluidItemInWIPState());
	}

	public boolean getRenderSendOnButton() {
		if (this.getWsFluidItem() == null) return false;
		WebKitForm webKitForm = this.getWebKitFormWithFormDef(this.getWsFluidItem());
		if ((webKitForm != null && webKitForm.isSendOnAfterSave()) ||
				!this.getWsFluidItem().isFluidItemFormSet()) return false;
		return (this.getWsFluidItem().isFluidItemInWIPState() && this.getWsFluidItem().isFormLockedByLoggedInUser());
	}

	public WebKitForm getWebKitFormWithFormDef(WorkspaceFluidItem workspaceFluidItem) {
		if (workspaceFluidItem == null) return null;
		return this.lookAndFeelBean.getWebKitFormWithFormDef(workspaceFluidItem.getFluidItemFormType());
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
			List<Attachment> existingAttachments = this.attachmentBean.actionFetchAttachmentsForForm(
					wsFlItem.getFluidItemForm());
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
		if ((toCheck == null || this.getFreshAttachments() == null) || this.getFreshAttachments().isEmpty()) return false;

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

	public void actionPrepareForMapDisplay() {
		this.mapCenter = null;
		this.conversationMapModel = new DefaultMapModel();

		FacesContext context = FacesContext.getCurrentInstance();
		WebKitField webKitField = (WebKitField) UIComponent.getCurrentComponent(context).getAttributes().get("geoLocationField");
		GeoUtil geo = null;
		if (webKitField == null && this.wsFluidItem != null) {
			Long paramFormId = this.getLongRequestParam("gMapPreviewFormId");
			String paramFieldName = this.getStringRequestParam("gMapPreviewFieldName");
			Long paramFieldValId = this.getLongRequestParam("gMapPreviewFieldValueId");

			Field formFieldById = this.wsFluidItem.getFormFieldsEdit().stream()
					.filter(field -> field.getTypeAsEnum() == Field.Type.Table)
					.map(field -> field.getFieldValueAsTableField().getTableRecords())
					.flatMap(Collection::stream)
					.filter(tableRecordForm -> paramFormId.equals(tableRecordForm.getId()))
					.map(tableForm -> tableForm.getFormFields())
					.flatMap(Collection::stream)
					.filter(tableField -> paramFieldName.equals(tableField.getFieldName()))
					.findFirst()
					.orElse(null);
			getLogger().info(String.format("Found a field by id '%s'", formFieldById));
		} else {
			geo = webKitField.getFieldValueAsGeo();
		}

		//Set the Markers on the Map...
		LatLng coord = new LatLng(geo.getLatitude(), geo.getLongitude());
		this.mapCenter = geo.getMapCenterLatLong();

		//Basic marker
		this.conversationMapModel.addOverlay(new Marker(coord, this.getWsFluidItem().getFluidItemTitle()));

		this.addRadiusCircle(geo);
	}

	public void actionPrepareForMapAddressLookup() {
		this.mapCenter = MAP_CENTER_DEFAULT;
		this.mapZoom = MAP_ZOOM_DEFAULT;
		this.conversationMapModel = new DefaultMapModel();

		FacesContext context = FacesContext.getCurrentInstance();
		this.activeGeoField = (WebKitField) UIComponent.getCurrentComponent(context).getAttributes().get("geoLocationField");
		this.activeGeo = this.activeGeoField.getFieldValueAsGeo();

		//Set the Markers on the Map...
		if (!this.getActiveGeo().isLatOrLonNotSet()) {//Is set...
			this.mapCenter = this.getActiveGeo().getMapCenterLatLong();
			this.mapZoom = MAP_ZOOM_CLOSE;
		}

		this.addRadiusCircle(this.activeGeo);
	}

	public void actionUpdateRadiusForMap(AjaxBehaviorEvent abe) {
		//Update the field value with the active geo value...
		this.activeGeoField.setFieldValueAsGeo(this.activeGeo);
	}

	public void actionOnGeoCode(GeocodeEvent eventParam) {
		this.conversationMapModel.getMarkers().clear();
		this.conversationMapModel.getCircles().clear();

		this.mapCenter = MAP_CENTER_DEFAULT;
		this.mapZoom = MAP_ZOOM_DEFAULT;

		List<GeocodeResult> results = eventParam.getResults();
		if (results == null || results.isEmpty()) {
			FacesMessage fMsg = new FacesMessage(
					FacesMessage.SEVERITY_ERROR, "No Results.",
					"No Coordinates found from Address '"+ eventParam.getQuery()+"'.");
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} else if (results.size() == 1) {
			//We found one result...
			GeocodeResult result = results.get(0);
			this.conversationMapModel.addOverlay(new Marker(result.getLatLng(), result.getAddress()));

			this.getActiveGeo().setLatitude(result.getLatLng().getLat());
			this.getActiveGeo().setLongitude(result.getLatLng().getLng());
			this.getActiveGeo().setAddress(result.getAddress());

			//Set the Markers on the Map...
			if (!this.getActiveGeo().isLatOrLonNotSet()) {
				this.mapCenter = this.getActiveGeo().getMapCenterLatLong();
				this.mapZoom = MAP_ZOOM_CLOSE;

				this.activeGeoField.setFieldValueAsGeo(this.getActiveGeo());
				this.addRadiusCircle(this.getActiveGeo());
				
				FacesMessage fMsg = new FacesMessage(
						FacesMessage.SEVERITY_INFO, "Address Located.", result.getAddress());
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
			}
		} else {
			for (int index = 0; index < results.size(); index++) {
				GeocodeResult result = results.get(index);
				this.conversationMapModel.addOverlay(new Marker(result.getLatLng(), result.getAddress()));
			}
			FacesMessage fMsg = new FacesMessage(
					FacesMessage.SEVERITY_ERROR, "Too many results.",
					String.format("Review Markers and refine lookup. Total '%s'.", results.size()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		}
	}
	
	public void actionOnGeoCodeReverse(ReverseGeocodeEvent eventParam) {
		this.mapCenter = MAP_CENTER_DEFAULT;
		this.mapZoom = MAP_ZOOM_DEFAULT;

		List<String> addresses = eventParam.getAddresses();
		LatLng coord = eventParam.getLatlng();

		if (addresses == null || addresses.isEmpty()) {
			FacesMessage fMsg = new FacesMessage(
					FacesMessage.SEVERITY_ERROR, "No Results.",
					"No Address found from Latitude '"+ coord.getLat()+"' and Longitude '"+coord.getLng()+"'.");
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} else {
			String address = addresses.get(0);
			this.conversationMapModel.addOverlay(new Marker(coord, address));

			this.getActiveGeo().setLatitude(coord.getLat());
			this.getActiveGeo().setLongitude(coord.getLng());
			this.getActiveGeo().setAddress(address);

			//Set the Markers on the Map...
			if (!this.getActiveGeo().isLatOrLonNotSet()) {
				this.mapCenter = this.getActiveGeo().getMapCenterLatLong();
				this.mapZoom = MAP_ZOOM_CLOSE;

				this.activeGeoField.setFieldValueAsGeo(this.getActiveGeo());
				this.addRadiusCircle(this.getActiveGeo());
				
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Address Located.", address);
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
			}
		}
	}

	private void addRadiusCircle(GeoUtil model) {
		if (model.getRadius() < 1) return;

		Circle circle = new Circle(
				new LatLng(model.getLatitude(), model.getLongitude()),
				model.getRadius());
		circle.setStrokeColor("#d93c3c");//red
		circle.setFillColor("#d93c3c");
		circle.setFillOpacity(0.5);

		this.conversationMapModel.addOverlay(circle);
	}

	public void actionRequestAdyenPayment() {
		try {
			PaymentClient payClient = this.getFluidClientDS().getPaymentClient();
			WorkspaceFluidItem wsFlItem = this.getWsFluidItem();

			PaymentLinkAdyen paymentLinkReqAdyen = payClient.obtainPaymentLink(wsFlItem.getFluidItem());
			String linkLocation = paymentLinkReqAdyen.getPaymentLink();

			this.executeJavaScript(String.format("window.location = '%s';", linkLocation));
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public boolean customDateFormatWithNoDay(String format) {
		if (UtilGlobal.isBlank(format)) return false;
		return (!format.contains("d"));
	}

	public List<Form> filterOutRowsWhereCheckerCannotView(List<Form> records) {
		if (records == null) return null;

		boolean enableFilterFeature = records.stream()
				.filter(itm -> itm.getFieldValueAsBoolean(VISIBLE_FOR_CHECKER) != null &&
						itm.getFieldValueAsBoolean(VISIBLE_FOR_CHECKER).booleanValue())
				.findFirst()
				.isPresent();
		if (enableFilterFeature) {
			return records.stream()
					.filter(itm -> {
						Boolean visForChecker = itm.getFieldValueAsBoolean(VISIBLE_FOR_CHECKER);
						return (visForChecker != null && visForChecker.booleanValue());
					})
					.collect(Collectors.toList());
		}
		return records;
	}

	public String destinationNavigationForCancel() {
		return this.getDestinationNavigationForCancel();
	}

	public String getDestinationNavigationForCancel() {
		if (this.conversationCallback != null) {
			return this.conversationCallback.getDestinationNavigationForCancel();
		}
		return null;
	}
}
