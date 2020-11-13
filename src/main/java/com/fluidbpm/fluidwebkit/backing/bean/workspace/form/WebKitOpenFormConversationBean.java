package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.ws.client.v1.flowitem.FlowItemClient;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
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
	private WorkspaceFluidItem workspaceFluidItem;

	@Inject
	private WebKitAccessBean accessBean;

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
		this.setWorkspaceFluidItem(null);
		if (wfiParam == null) return;
		try {
			String formType = wfiParam.getFluidItemFormType();
			List<Field> editable = this.accessBean.getFieldsEditableForFormDef(formType);
			List<Field> viewable = this.accessBean.getFieldsViewableForFormDef(formType);

			FormContainerClient fcClient = this.getFluidClientDS().getFormContainerClient();

			FluidItem fluidItem = new FluidItem();
			if (wfiParam.getFluidItemId() != null && wfiParam.getFluidItemId().longValue() > 0) {
				FlowItemClient flowItemClient = this.getFluidClientDS().getFlowItemClient();
				fluidItem = flowItemClient.getFluidItemByFormId(wfiParam.getFluidItemFormId());
			}

			Form freshFetchForm = fcClient.getFormContainerById(wfiParam.getFluidItemFormId());
			fluidItem.setForm(freshFetchForm);

			this.setWorkspaceFluidItem(new WorkspaceFluidItem(wfiParam.getBaseWeb().cloneVO(
					fluidItem, viewable, editable, null)));
		} catch (Exception except) {
			this.raiseError(except);
		}
	}
}
