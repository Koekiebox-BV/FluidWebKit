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

package com.fluidbpm.fluidwebkit.servlet.content;

import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.utility.ImageUtil;
import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.fluidwebkit.qualifier.cache.RAWAttachmentsCache;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.ws.client.v1.attachment.AttachmentClient;
import com.google.common.cache.Cache;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet used to retrieve RAW content for an attachment.
 */
@WebServlet(value = "/get_attachment_raw",
		name = "Get Fluid RAW Attachment Servlet",
		asyncSupported = false,
		displayName = "Get Attachment content RAW",
		loadOnStartup = 1)
public class RAWContentServlet extends ImageServlet {

	@Inject
	@RAWAttachmentsCache
	private Cache<Long, Attachment> attachmentCache;

	@Inject
	private WebKitAccessBean webKitAccessBean;

	private static final String PARAM_FORM_ID = "formId";
	private static final String PARAM_FORM_DEFINITION = "formDefinition";
	private static final String PARAM_ATTACHMENT_ID = "attachmentId";

	@Override
	protected void doGet(HttpServletRequest reqParam, HttpServletResponse respParam) throws ServletException, IOException {
		String formId = reqParam.getParameter(PARAM_FORM_ID);
		if ("null".equals(formId)) return;
		String formDef = reqParam.getParameter(PARAM_FORM_DEFINITION);
		if ("null".equals(formDef)) return;
		if ((formId == null || formId.trim().isEmpty()) ||
				(formDef == null || formDef.trim().isEmpty())) {
			return;
		}
		String attachmentId = reqParam.getParameter(PARAM_ATTACHMENT_ID);
		Long attachmentIdLong = -1L;
		if (attachmentId == null || "null".equals(attachmentId.trim())) {
			attachmentId = UtilGlobal.EMPTY;
		} else {
			attachmentIdLong = Long.valueOf(attachmentId.trim());
		}

		Long formIdParam = null;
		try {
			formIdParam = Long.valueOf(formId.trim());
		} catch (NumberFormatException nfe) { return; }
		Form form = new Form(formIdParam);
		form.setFormType(formDef);

		if (this.webKitAccessBean == null || !this.webKitAccessBean.isCanUserAttachmentsView(formDef)) {
			byte[] placeholderImageBytes = ImageUtil.getThumbnailPlaceholderImageForNoAccess();
			ImageStreamedContent noAccessContent = new ImageStreamedContent(
					placeholderImageBytes, "image/svg+xml", "no-access.svg");
			this.writeImageToResponseOutput(respParam, noAccessContent);
			return;
		}

		User loggedInUser = this.getLoggedInUser(reqParam);
		if (loggedInUser == null) return;

		FluidClientDS fcp = this.getFcp().get(reqParam.getSession(false).getId());
		if (fcp == null) return;

		final AttachmentClient attachmentClient = fcp.getAttachmentClient();
		try {
			ImageStreamedContent imageStreamedContent =
					this.getRAWAttachmentByIdNoDataUseCache(attachmentClient, attachmentIdLong);
			if (imageStreamedContent == null) return;
			
			this.writeImageToResponseOutput(respParam, imageStreamedContent.cloneAsDefaultStreamedContent());
		} catch (Exception except) {
			this.raiseError(except, reqParam);
		}
	}

	private ImageStreamedContent getRAWAttachmentByIdNoDataUseCache(AttachmentClient attachmentClientParam, Long attId) {
		Attachment cacheAttachments = this.attachmentCache.getIfPresent(attId);
		if (cacheAttachments != null) return new ImageStreamedContent(
			cacheAttachments.getAttachmentDataRAW(),
			cacheAttachments.getContentType(),
			cacheAttachments.getName()
		);

		Attachment attachmentById =
				attachmentClientParam.getAttachmentById(attId, true);
		this.attachmentCache.put(attId, attachmentById);
		return new ImageStreamedContent(
				attachmentById.getAttachmentDataRAW(),
				attachmentById.getContentType(),
				attachmentById.getName()
		);
	}
}
