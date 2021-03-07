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
import com.fluidbpm.program.api.util.command.impl.DocumentToPDFConvert;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.ws.client.v1.attachment.AttachmentClient;
import com.google.common.cache.Cache;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * Servlet used to retrieve RAW content for an attachment.
 */
@WebServlet(value = "/convert_attachment_raw_to_pdf",
		name = "Get Fluid RAW Attachment Servlet",
		asyncSupported = false,
		displayName = "Get Attachment content RAW",
		loadOnStartup = 1)
public class RAWContentToPDFServlet extends RAWContentServlet {

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
		if ((formId == null || formId.trim().isEmpty()) || (formDef == null || formDef.trim().isEmpty())) return;

		String attachmentId = reqParam.getParameter(PARAM_ATTACHMENT_ID);
		Long attachmentIdLong = -1L;
		if (attachmentId == null) {
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
					placeholderImageBytes, "image/png", "no_access.png");
			this.writeImageToResponseOutput(respParam, noAccessContent);
			return;
		}

		User loggedInUser = this.getLoggedInUser(reqParam);
		if (loggedInUser == null) return;

		FluidClientDS fcp = this.getFcp().get(reqParam.getSession(false).getId());
		if (fcp == null) return;

		final AttachmentClient attachmentClient = fcp.getAttachmentClient();
		try {
			Attachment cacheAttachments =
					this.getRAWAttachmentByIdNoDataUseCache(attachmentClient, attachmentIdLong);
			if (cacheAttachments == null) return;

			//Write contents to file...
			String extension = cacheAttachments.getExtensionFromFilename();
			File tempOutForDocument = this.getTemporaryFile(
					reqParam, attachmentIdLong, extension);

			UtilGlobal.writeStreamToFile(new ByteArrayInputStream(cacheAttachments.getAttachmentDataRAW()), tempOutForDocument);

			//Convert the document...
			DocumentToPDFConvert docToPdfConvert = new DocumentToPDFConvert();

			File outputPDFFile = null;
			try {
				outputPDFFile = docToPdfConvert.convertDocumentToPDF(tempOutForDocument);
			} catch (Exception err) {
				this.raiseError(err, reqParam);
				return;
			} finally {
				//Remove Word Document...
				tempOutForDocument.delete();
			}

			byte[] pdfContentBytes = null;
			try {
				pdfContentBytes = UtilGlobal.readFileBytes(outputPDFFile);
			} catch (Exception err) {
				this.raiseError(err, reqParam);
				return;
			} finally {
				//Delete temp PDF file...
				outputPDFFile.delete();
			}

			this.writeImageToResponseOutput(respParam, new ImageStreamedContent(
					pdfContentBytes,
					"application/pdf",
					String.format("%s.pdf", cacheAttachments.getName())));
		} catch (Exception except) {
			this.raiseError(except, reqParam);
		}
	}

	private File getTemporaryFile(
		HttpServletRequest reqParam,
		Long attachmentId,
		String filenameExtension
	) {
		HttpSession httpSession = reqParam.getSession();

		if (filenameExtension == null) filenameExtension = "";

		String uuid = attachmentId.toString();

		String pathToTempFile = System.getProperty("user.dir");

		pathToTempFile += File.separator;
		pathToTempFile += "Fluid_Temp_HttpSession";
		pathToTempFile += File.separator;
		pathToTempFile += httpSession.getId();
		pathToTempFile += File.separator;
		pathToTempFile += (attachmentId);
		pathToTempFile += File.separator;

		File directory = new File(pathToTempFile);
		if (!directory.exists()) directory.mkdirs();

		int index = 1;
		File currentFileSeek = null;
		do {
			currentFileSeek = new File(String.format("%s%d%s", pathToTempFile, index, filenameExtension));
			index++;
		} while (currentFileSeek.exists());

		return currentFileSeek;
	}

	private Attachment getRAWAttachmentByIdNoDataUseCache(AttachmentClient attachmentClientParam, Long attId) {
		Attachment cacheAttachments = this.attachmentCache.getIfPresent(attId);
		if (cacheAttachments != null) return cacheAttachments;

		Attachment attachmentById =
				attachmentClientParam.getAttachmentById(attId, true);
		this.attachmentCache.put(attId, attachmentById);
		return attachmentById;
	}
}
