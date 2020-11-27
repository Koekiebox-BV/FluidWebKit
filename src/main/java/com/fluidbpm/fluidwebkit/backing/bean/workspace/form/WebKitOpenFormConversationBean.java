package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.lf.WebKitWorkspaceLookAndFeelBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.Flow;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.form.FormListing;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.webkit.WebKitForm;
import com.fluidbpm.ws.client.v1.flowitem.FlowItemClient;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ConversationScoped
@Named("webKitOpenFormConversationBean")
public class WebKitOpenFormConversationBean extends ABaseManagedBean {
	@Inject
	@Getter
	private Conversation conversation;

	@Getter
	@Setter
	private WorkspaceFluidItem wsFluidItem;

	@Inject
	private WebKitAccessBean accessBean;

	@Inject
	private WebKitWorkspaceLookAndFeelBean lookAndFeelBean;

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

		//Send to a specific workflow...
		this.setInputWorkflowsForFormDef(new ArrayList<>());
		this.setInputSelectedWorkflow(null);

		if (wfiParam == null) return;
		if (this.getFluidClientDS() == null) return;

		try {
			String formType = wfiParam.getFluidItemFormType();
			this.setDialogHeaderTitle(wfiParam.getFluidItemTitle());

			WebKitForm webKitForm = this.lookAndFeelBean.getWebKitFormWithFormDef(formType);
			if (webKitForm == null && WebKitForm.EMAIL_FORM_TYPE.equals(formType))
				webKitForm = WebKitForm.emailWebKitForm();

			List<Field> editable = this.accessBean.getFieldsEditableForFormDef(formType);
			List<Field> viewable = this.accessBean.getFieldsViewableForFormDef(formType);

			FormContainerClient fcClient = this.getFluidClientDS().getFormContainerClient();

			FluidItem fluidItem = new FluidItem();
			if (wfiParam.isFluidItemInWIPState()) {
				FlowItemClient flowItemClient = this.getFluidClientDS().getFlowItemClient();
				fluidItem = flowItemClient.getFluidItemByFormId(wfiParam.getFluidItemFormId());
			}

			Form freshFetchForm = new Form();
			if (wfiParam.isFluidItemFormSet()) {
				freshFetchForm = fcClient.getFormContainerById(wfiParam.getFluidItemFormId());
				//Lock the Form as it is being opened...
				if (webKitForm.isLockFormOnOpen() && Form.State.OPEN.equals(freshFetchForm.getState())) {
					fcClient.lockFormContainer(freshFetchForm, wfiParam.getJobView());
					this.lockByLoggedInUser(freshFetchForm);
				}

				//Fetch the table field forms...
				if (webKitForm.isAnyTableFormsEnabled()) {
					List<FormListing> childForms =
							this.getFluidClientDS().getSQLUtilWrapper().getTableForms(true, freshFetchForm);
				}
			} else {
				freshFetchForm.setFormType(wfiParam.getFluidItemFormType());
				this.setDialogHeaderTitle(freshFetchForm.getTitle());

				if (webKitForm.isSendToWorkflowAfterCreate()) {
					List<Form> formDefs = this.accessBean.getFormDefinitionsCanCreateInstanceOfSorted();
					List<Flow> associatedFlowsForFormDef = formDefs.stream()
							.filter(itm -> itm.getFormType().equals(wfiParam.getFluidItemFormType()))
							.findFirst()
							.map(itm -> itm.getAssociatedFlows())
							.orElse(new ArrayList<>());
					if (associatedFlowsForFormDef.size() == 1)
						this.setInputSelectedWorkflow(associatedFlowsForFormDef.get(0).getName());
					associatedFlowsForFormDef.forEach(flowItm -> this.getInputWorkflowsForFormDef().add(flowItm.getName()));
				}
			}
			fluidItem.setForm(freshFetchForm);

			this.setWsFluidItem(new WorkspaceFluidItem(wfiParam.getBaseWeb().cloneVO(
					fluidItem, viewable, editable, null)));
			this.getWsFluidItem().setWebKitForm(webKitForm);
			this.getWsFluidItem().setLoggedInUser(this.getLoggedInUserSafe());
			this.getWsFluidItem().refreshFormFieldsEdit();
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public void actionUpdateSelectedWorkflow() {
		
	}

	public void actionSaveForm(
		String dialogToHideAfterSuccess,
		String varBtnToEnableFailedSave
	) {
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

				//Existing Item...
				fcClient.updateFormContainer(this.toFormToSave(wsFlItem));

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
				Form createdForm = fcClient.createFormContainer(this.toFormToSave(wsFlItem), !sendingOn);
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

	private Form toFormToSave(WorkspaceFluidItem fluidItem) {
		Form returnVal = new Form(fluidItem.getFluidItemFormType());
		returnVal.setId(fluidItem.getFluidItemFormId());
		returnVal.setFormType(fluidItem.getFluidItemForm().getFormType());
		returnVal.setFormFields(fluidItem.getFormFieldsEdit());

		WebKitForm wkForm = this.lookAndFeelBean.getWebKitFormWithFormDef(returnVal.getFormType());
		returnVal.setFormTypeId(fluidItem.getFluidItemForm().getFormTypeId());
		returnVal.setTitle(this.generateNewFormTitle(
				wkForm == null ? null : wkForm.getNewFormTitleFormula(), fluidItem));
		return returnVal;
	}

	protected String generateNewFormTitle(
		String formula,
		WorkspaceFluidItem wfiParam
	) {
		if (UtilGlobal.isBlank(formula)) return String.format("New '%s'", wfiParam.getFluidItemFormType());

		int lastIndexOfPipe = formula.lastIndexOf("|");
		String formFieldsString = formula.substring(lastIndexOfPipe);
		if (UtilGlobal.isBlank(formFieldsString)) return formula.substring(lastIndexOfPipe - 1);

		String[] formFieldNames = formFieldsString.split("\\,");
		if (formFieldNames == null || formFieldNames.length < 1) return formFieldsString;

		return String.format(formula, this.toObjs(formFieldNames, wfiParam.getFormFields()));
	}

	private Object[] toObjs(String[] formFieldNames, List<Field> formFields) {
		if (formFieldNames == null || formFieldNames.length == 0) return null;

		List<Object> returnVal = new ArrayList<>();
		for (String fieldName : formFieldNames) {
			String fieldNameLower = fieldName.toLowerCase();
			Field fieldWithName = formFields.stream()
					.filter(itm -> fieldNameLower.equals(itm.getFieldName().toLowerCase()))
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

}
