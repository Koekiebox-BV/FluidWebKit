/*
 * Koekiebox CONFIDENTIAL
 *
 * [2012] - [2017] Koekiebox (Pty) Ltd
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property
 * of Koekiebox and its suppliers, if any. The intellectual and
 * technical concepts contained herein are proprietary to Koekiebox
 * and its suppliers and may be covered by South African and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material is strictly
 * forbidden unless prior written permission is obtained from Koekiebox Innovations.
 */

package com.fluidbpm.fluidwebkit.backing.bean.attachment;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import lombok.Getter;
import lombok.Setter;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


@ConversationScoped
@Named("webKitAttachmentPreviewBean")
@Getter
@Setter
public class WebKitAttachmentPreviewBean extends ABaseManagedBean {
	@Inject
	private WebKitAttachmentBean attachmentBean;

	@Inject
	private Conversation conversation;

	private List<Attachment> allAttachmentsForForm;
	private Attachment currentViewedAttachmentForForm;
	private WorkspaceFluidItem workspaceFluidItem;

	private int currentAttachmentIndex;
	private int dialogWidth;

	public void actionLoadAttachmentsForForm(WorkspaceFluidItem fluidItem, int dialogWidth) {
		if (this.conversation.isTransient()) {
			this.conversation.setTimeout(TimeUnit.MINUTES.toMillis(180));
			this.conversation.begin();
		}

		this.setDialogWidth(dialogWidth);
		this.setWorkspaceFluidItem(fluidItem);
		this.setDialogHeaderTitle("[Unknown]");

		try {
			this.setAllAttachmentsForForm(this.attachmentBean.actionFetchAttachmentsForForm(fluidItem.getFluidItemForm()));
		} catch (Exception except) {
			this.raiseError(except);
		} finally {
			this.setCurrentAttachmentIndex(0);

			if (this.getAllAttachmentsForForm() == null) this.setAllAttachmentsForForm(new ArrayList<>());

			if (this.getAllAttachmentsForForm().isEmpty()) this.setCurrentViewedAttachmentForForm(new Attachment());
			else this.setCurrentViewedAttachmentForForm(this.getAllAttachmentsForForm().get(0));

			this.setDialogHeaderTitle(String.format("%s - %d of %d",
					this.getCurrentViewedAttachmentForForm().getName(),
					this.getCurrentAttachmentIndex() + 1,
					this.getAllAttachmentsForForm().size()));
		}
	}

	public void actionNextAttachment() {

	}

	public void actionPreviousAttachment() {

	}

	public void actionLoadThumbnailClicked(int index) {

	}

	public boolean isAtFirstAttachment() {
		return this.currentAttachmentIndex == 0;
	}

	public boolean isAtLastAttachment() {
		if (this.getAllAttachmentsForForm() == null || this.getAllAttachmentsForForm().isEmpty()) return true;
		return (this.currentAttachmentIndex + 1) == this.getAllAttachmentsForForm().size();
	}

	public void actionCloseDialog() {
		if (!this.conversation.isTransient()) this.conversation.end();
	}

	
	
}
