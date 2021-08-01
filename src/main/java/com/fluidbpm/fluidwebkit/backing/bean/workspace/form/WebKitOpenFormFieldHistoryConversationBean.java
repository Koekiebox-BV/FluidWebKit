package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.historic.FormHistoricData;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import lombok.Getter;
import lombok.Setter;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ConversationScoped
@Named("webKitOpenFormFieldHistoryConversationBean")
public class WebKitOpenFormFieldHistoryConversationBean extends ABaseManagedBean {
	@Inject
	@Getter
	private Conversation conversation;

	@Getter
	@Setter
	private Long historyFormId;

	@Getter
	@Setter
	private List<FormHistoricData> formHistoricData;

	public void startConversation() {
		if (this.conversation.isTransient()) {
			this.conversation.setTimeout(TimeUnit.MINUTES.toMillis(120));
			this.conversation.begin();
		}
	}

	public void endConversation() {
		if (!this.conversation.isTransient()) this.conversation.end();
	}

	public boolean isConversationClosed() {
		return (this.conversation == null);
	}


	public void actionLoadFormHistoricData() {
		if (this.getFluidClientDS() == null) return;

		if (this.formHistoricData != null) return;

		FormContainerClient fcc = this.getFluidClientDS().getFormContainerClient();

		try {
			this.setFormHistoricData(fcc.getFormAndFieldHistoricData(new Form(this.historyFormId), false));
		} catch (Exception except) {
			this.raiseError(except);
		}
	}
	
}
