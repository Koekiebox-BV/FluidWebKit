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
import com.fluidbpm.fluidwebkit.qualifier.cache.FormAttachmentsCache;
import com.fluidbpm.fluidwebkit.qualifier.cache.FormImageCache;
import com.fluidbpm.fluidwebkit.servlet.content.ImageStreamedContent;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.ws.client.v1.attachment.AttachmentClient;
import com.google.common.cache.Cache;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.stream.Collectors;


@RequestScoped
@Named("webKitAttachmentBean")
public class AttachmentBean extends ABaseManagedBean {
	@Inject
	@FormAttachmentsCache
	private Cache<Long, List<Attachment>> attachmentCache;

	@Inject
	@FormImageCache
	private Cache<String, ImageStreamedContent> formImageCache;

	public List<Attachment> actionFetchAttachmentsForForm(Form form, int fromIndex) {
		List<Attachment> returnVal = this.actionFetchAttachmentsForForm(form);
		if (returnVal == null || (fromIndex + 1) > returnVal.size()) return returnVal;
		return returnVal.subList(fromIndex, returnVal.size());
	}

	public List<Attachment> actionFetchAttachmentsForForm(Form form) {
		try {
			if (this.getFluidClientDS() == null || (form == null || form.getId() == null)) return null;

			List<Attachment> attachments = this.attachmentCache.getIfPresent(form.getId());
			if (attachments != null) {
				return attachments.stream()
						.map(toMap -> {
							Attachment returnValClone = toMap.clone();
							returnValClone.setAttachmentDataBase64(null);
							return returnValClone;
						})
						.collect(Collectors.toList());
			}

			final AttachmentClient attachmentClient = this.getFluidClientDS().getAttachmentClient();
			attachments = attachmentClient.getAttachmentsByForm(form, true);
			if (attachments == null) {
				return null;
			}

			this.attachmentCache.put(form.getId(), attachments);
			return attachments.stream()
					.map(toMap -> {
						Attachment returnValClone = toMap.clone();
						returnValClone.setAttachmentDataBase64(null);
						return returnValClone;
					})
					.collect(Collectors.toList());
		} catch (Exception except) {
			this.raiseError(except);
			return null;
		}
	}

	public boolean actionDoesAttachmentExist(Form formToCheck) {
		return (this.actionGetAttachmentCount(formToCheck) > 0);
	}

	public int actionGetAttachmentCount(Form formToCheck) {
		List<Attachment> att = this.actionFetchAttachmentsForForm(formToCheck);
		return (att == null) ? 0 : att.size();
	}

	public String actionGenerateURLForThumbnail(WorkspaceFluidItem wfItem, int thumbnailScale) {
		//String contextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
		StringBuffer buffer = new StringBuffer();
		String postFix = String.format(
				"/get_form_image?formId=%d&formDefinition=%s&thumbnailScale=%d",
				wfItem == null ? null : wfItem.getFluidItemFormId(),
				wfItem == null ? null : wfItem.getFluidItemFormTypeURLSafe(),
				thumbnailScale);
		buffer.append(postFix);
		String returnVal = buffer.toString();
		return returnVal;
	}

	public String actionGenerateURLForThumbnail(WorkspaceFluidItem wfItem, Attachment attachment, int thumbnailScale) {
		String postFix = String.format(
				"/get_form_image?formId=%d&formDefinition=%s&attachmentId=%d&thumbnailScale=%d",
				wfItem.getFluidItemFormId(),
				wfItem.getFluidItemFormTypeURLSafe(),
				attachment.getId(),
				thumbnailScale);
		return postFix;
	}

	public void addAttachmentFreshToCache(
		Attachment attachment,
		String conversationId,
		int attachmentIndex
	) {
		ImageStreamedContent streamedContent = new ImageStreamedContent(
				UtilGlobal.decodeBase64(attachment.getAttachmentDataBase64()),
				attachment.getContentType(),
				attachment.getName());
		String key = String.format("%s_%d", conversationId, attachmentIndex);
		this.formImageCache.put(key, streamedContent);
	}

}
