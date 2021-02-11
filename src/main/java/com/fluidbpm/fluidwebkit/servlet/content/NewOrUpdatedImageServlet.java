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

import com.fluidbpm.fluidwebkit.backing.utility.ImageUtil;
import com.fluidbpm.fluidwebkit.qualifier.cache.FormImageCache;
import com.fluidbpm.program.api.vo.user.User;
import com.google.common.cache.Cache;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet used to retrieve image content for a form.
 */
@WebServlet(value = "/get_new_or_updated_image",
		name = "Get New Or Updated Image Servlet",
		asyncSupported = false,
		displayName = "Get New or Updated Image Servlet",
		loadOnStartup = 1)
public class NewOrUpdatedImageServlet extends ImageServlet {

	@Inject
	@FormImageCache
	private Cache<String, ImageStreamedContent> formImageCache;

	private static final String CONVERSATION_ID = "conversationId";
	private static final String ATTACHMENT_INDEX = "attachmentIndex";

	@Override
	protected void doGet(HttpServletRequest reqParam, HttpServletResponse respParam) throws ServletException, IOException {
		User loggedInUser = this.getLoggedInUser(reqParam);
		if (loggedInUser == null) return;

		String conversationId = reqParam.getParameter(CONVERSATION_ID);
		if ("null".equals(conversationId)) return;
		String attachmentIndex = reqParam.getParameter(ATTACHMENT_INDEX);
		if ("null".equals(attachmentIndex)) return;
		if ((conversationId == null || conversationId.trim().isEmpty()) ||
				(attachmentIndex == null || attachmentIndex.trim().isEmpty())) {
			return;
		}
		int attachmentIndexInt = Integer.valueOf(attachmentIndex);
		String imageKey = this.getImageCacheKey(conversationId, attachmentIndexInt);
		ImageStreamedContent cacheImageStreamedContent = this.formImageCache.getIfPresent(imageKey);
		if (cacheImageStreamedContent != null) {
			byte[] nonImageData = this.getNonImagePreviewForContentType(cacheImageStreamedContent.getContentType());
			if (nonImageData == null) {
				this.writeImageToResponseOutput(respParam, cacheImageStreamedContent.cloneAsDefaultStreamedContent());
			} else {
				ImageStreamedContent noPreview = new ImageStreamedContent(
						nonImageData, "image/png", "preview.pdf");
				this.writeImageToResponseOutput(respParam, noPreview);
			}
			return;
		}

		byte[] placeholderImageBytes = ImageUtil.getThumbnailPlaceholderImageForNoAccess();
		ImageStreamedContent noAccessContent = new ImageStreamedContent(
				placeholderImageBytes, "image/png", "no_access.png");
		this.writeImageToResponseOutput(respParam, noAccessContent);
	}

	private String getImageCacheKey(
		String conversationId,
		int index
	) {
		return String.format("%s_%d", conversationId, index);
	}
}
