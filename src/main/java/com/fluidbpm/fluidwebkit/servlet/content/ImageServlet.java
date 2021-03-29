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
import com.fluidbpm.fluidwebkit.qualifier.cache.FormAttachmentsCache;
import com.fluidbpm.fluidwebkit.qualifier.cache.FormImageCache;
import com.fluidbpm.fluidwebkit.servlet.ABaseFWKServlet;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.ws.client.v1.attachment.AttachmentClient;
import com.google.common.cache.Cache;
import org.primefaces.model.StreamedContent;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servlet used to retrieve image content for a form.
 */
@WebServlet(value = "/get_form_image",
		name = "Get Fluid Form Image Servlet",
		asyncSupported = false,
		displayName = "Get Fluid Form Image Servlet",
		loadOnStartup = 1)
public class ImageServlet extends ABaseFWKServlet {

	@Inject
	@FormImageCache
	private Cache<String, ImageStreamedContent> formImageCache;

	@Inject
	@FormAttachmentsCache
	private Cache<Long, List<Attachment>> attachmentCache;

	@Inject
	private WebKitAccessBean webKitAccessBean;

	private static final String PARAM_FORM_ID = "formId";
	private static final String PARAM_FORM_DEFINITION = "formDefinition";
	private static final String PARAM_ATTACHMENT_ID = "attachmentId";
	private static final String PARAM_THUMBNAIL_SCALE = "thumbnailScale";

	@Override
	protected void doGet(HttpServletRequest reqParam, HttpServletResponse respParam) throws ServletException, IOException {
		String formId = reqParam.getParameter(PARAM_FORM_ID);
		if ("null".equals(formId)) return;
		String formDef = reqParam.getParameter(PARAM_FORM_DEFINITION);
		if ("null".equals(formDef)) return;
		if ((formId == null || formId.trim().isEmpty()) || (formDef == null || formDef.trim().isEmpty())) return;

		String attachmentId = reqParam.getParameter(PARAM_ATTACHMENT_ID);
		Long attachmentIdLong = -1L;
		if (attachmentId == null) {
			attachmentId = UtilGlobal.EMPTY;
		} else {
			attachmentIdLong = Long.valueOf(attachmentId.trim());
		}

		String thumbnailScale = reqParam.getParameter(PARAM_THUMBNAIL_SCALE);
		int thumbScale = -1;
		if (thumbnailScale != null && !thumbnailScale.trim().isEmpty()) thumbScale = Integer.parseInt(thumbnailScale.trim());

		Long formIdParam = null;
		try {
			formIdParam = Long.valueOf(formId.trim());
		} catch (NumberFormatException nfe) { return; }
		Form form = new Form(formIdParam);
		form.setFormType(formDef);

		if (this.webKitAccessBean == null || !this.webKitAccessBean.isCanUserAttachmentsView(formDef)) {
			byte[] placeholderImageBytes = ImageUtil.getThumbnailPlaceholderImageForNoAccess();
			ImageStreamedContent noAccessContent = new ImageStreamedContent(
					placeholderImageBytes, "image/png", "no_access.png");
			this.writeImageToResponseOutput(respParam, noAccessContent);
			return;
		}

		String imageKey = this.getImageCacheKey(formIdParam, attachmentIdLong, thumbScale);
		ImageStreamedContent cacheImageStreamedContent = this.formImageCache.getIfPresent(imageKey);
		if (cacheImageStreamedContent != null) {
			this.writeImageToResponseOutput(respParam, cacheImageStreamedContent.cloneAsDefaultStreamedContent());
			return;
		}

		User loggedInUser = this.getLoggedInUser(reqParam);
		if (loggedInUser == null) return;

		FluidClientDS fcp = this.getFcp().get(reqParam.getSession(false).getId());
		if (fcp == null) return;

		final AttachmentClient attachmentClient = fcp.getAttachmentClient();
		try {
			ImageStreamedContent imageStreamedContent = this.getImageStreamedContentForFirstImageAttachmentForForm(
					attachmentClient, new Form(formIdParam), attachmentIdLong, thumbScale);
			if (imageStreamedContent == null) return;

			//Store the Image...
			this.formImageCache.put(imageKey, imageStreamedContent);
			this.writeImageToResponseOutput(respParam, imageStreamedContent.cloneAsDefaultStreamedContent());
		} catch (Exception except) {
			this.raiseError(except, reqParam);
		}
	}

	protected void writeImageToResponseOutput(
		HttpServletResponse respParam,
		StreamedContent streamedContentParam
	) throws IOException {
		respParam.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", streamedContentParam.getName()));
		respParam.setContentType(streamedContentParam.getContentType());
		InputStream is = streamedContentParam.getStream();

		OutputStream os = respParam.getOutputStream();
		int readVal = -1;
		while ((readVal = is.read()) != -1) os.write(readVal);

		is.close();
		os.flush();
		os.close();
	}

	public ImageStreamedContent getImageStreamedContentForFirstImageAttachmentForForm(
		AttachmentClient attachmentClientParam,
		Form formToCheckForParam,
		Long attachmentId,
		int thumbnailScale
	) throws IOException {
		List<Attachment> attachmentsForForm =
				this.getImageAttachmentByFormNoDataUseCache(attachmentClientParam, formToCheckForParam);
		if (attachmentsForForm == null || attachmentsForForm.isEmpty()) {
			byte[] placeholderImageBytes = ImageUtil.getThumbnailPlaceholderImageForNone();
			return new ImageStreamedContent(
					placeholderImageBytes,
					"image/svg+xml",
					String.format("no_attachments_yet_%s.svg", UUID.randomUUID().toString().substring(0, 5)));
		}

		List<Attachment> imgAttachmentsForForm = attachmentsForForm.stream()
				.filter(itm -> itm.isFileTypeImage())
				.collect(Collectors.toList());
		if (imgAttachmentsForForm.isEmpty()) {
			byte[] placeholderImageBytes = ImageUtil.getNonImagePreviewForContentType(attachmentsForForm.get(0).getContentType());
			if (placeholderImageBytes == null) placeholderImageBytes = ImageUtil.getThumbnailPlaceholderImageForNone();
			
			return new ImageStreamedContent(
					placeholderImageBytes,
					"image/svg+xml",
					String.format("prev_for_non_image_attachment_%s.svg", UUID.randomUUID().toString().substring(0, 5)));
		}

		//First image...
		Attachment imageAttachmentById = null;
		if (attachmentId > 0) {
			imageAttachmentById = attachmentClientParam.getAttachmentById(
					attachmentId, true);
		} else {
			imageAttachmentById = attachmentClientParam.getAttachmentById(
					attachmentsForForm.get(0).getId(), true);
		}

		byte[] nonImageData = ImageUtil.getNonImagePreviewForContentType(imageAttachmentById.getContentType());
		if (nonImageData != null) {
			return new ImageStreamedContent(
					nonImageData,
					"image/svg+xml",
					String.format("type_icon_%s.svg", UUID.randomUUID().toString().substring(0, 5)));
		}

		byte[] imageBytes = UtilGlobal.decodeBase64(imageAttachmentById.getAttachmentDataBase64());
		if (thumbnailScale > 0) imageBytes = this.scale(imageBytes, thumbnailScale, 0);
		return new ImageStreamedContent(
				imageBytes,
				imageAttachmentById.getContentType(),
				imageAttachmentById.getName());
	}

	private List<Attachment> getImageAttachmentByFormNoDataUseCache(
		AttachmentClient attachmentClientParam,
		Form formToCheckForParam
	) {
		List<Attachment> cacheAttachments = this.attachmentCache.getIfPresent(formToCheckForParam.getId());
		if (cacheAttachments != null) return cacheAttachments;

		List<Attachment> attachmentsForForm =
				attachmentClientParam.getImageAttachmentsByForm(formToCheckForParam, false);
		this.attachmentCache.put(formToCheckForParam.getId(), attachmentsForForm);
		return attachmentsForForm;
	}

	public byte[] scale(
		byte[] fileDataParam,
		int widthParam,
		int heightParam
	) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(fileDataParam);
		BufferedImage img = ImageIO.read(in);
		//Lets not enlarge an image that is less than {widthParam}.
		if (widthParam > img.getWidth() || heightParam > img.getHeight()) return fileDataParam;

		if (heightParam == 0) heightParam = (widthParam * img.getHeight()) / img.getWidth();

		if (widthParam == 0) widthParam = (heightParam * img.getWidth()) / img.getHeight();

		Image scaledImage = img.getScaledInstance(widthParam, heightParam, Image.SCALE_SMOOTH);
		BufferedImage imageBuff = new BufferedImage(widthParam, heightParam, BufferedImage.TYPE_INT_RGB);
		Color black = new Color(0,0,0);
		Color white = new Color(255,255,255);

		imageBuff.getGraphics().drawImage(scaledImage, 0, 0, white, null);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ImageIO.write(imageBuff, "jpg", buffer);
		return buffer.toByteArray();
	}

	private String getImageCacheKey(
		Long formId,
		Long attachmentId,
		int scale
	) {
		return String.format("%d_%d_%d", formId, attachmentId, scale);
	}


}
