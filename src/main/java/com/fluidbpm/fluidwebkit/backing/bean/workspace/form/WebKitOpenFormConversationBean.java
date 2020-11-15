package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.lf.WebKitWorkspaceLookAndFeelBean;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.form.Form;
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

	@PostConstruct
	public void init() {

	}

	/**
	 * Start the conversation scope.
	 */
	public void startConversation() {
		//Start the transaction...
		if (this.conversation.isTransient()) {
			this.conversation.setTimeout(
					TimeUnit.MINUTES.toMillis(120));
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
		if (wfiParam == null) return;
		if (this.getFluidClientDS() == null) return;

		try {
			String formType = wfiParam.getFluidItemFormType();
			this.setDialogHeaderTitle(wfiParam.getFluidItemTitle());

			WebKitForm webKitForm = this.lookAndFeelBean.getWebKitFormWithFormDef(formType);
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
			}
			fluidItem.setForm(freshFetchForm);

			this.setWsFluidItem(new WorkspaceFluidItem(wfiParam.getBaseWeb().cloneVO(
					fluidItem, viewable, editable, null)));
			this.getWsFluidItem().setWebKitForm(webKitForm);
			this.getWsFluidItem().refreshFormFieldsEdit();
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public void actionSaveForm(String dialogToHideAfterSuccess) {
		if (this.getFluidClientDS() == null) return;
		try {

			this.executeJavaScript(String.format("PF('%s').hide();", dialogToHideAfterSuccess));
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", String.format("'%s' Updated.", this.wsFluidItem.getFluidItemTitle()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception except) {
			this.raiseError(except);
		}
	}
}
