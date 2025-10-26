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
import com.fluidbpm.fluidwebkit.backing.bean.attachment.sheet.FluidWorkspaceUtil;
import com.fluidbpm.fluidwebkit.backing.bean.attachment.sheet.Worksheet;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.utility.WebUtil;
import com.fluidbpm.fluidwebkit.qualifier.cache.FormAttachmentsCache;
import com.fluidbpm.fluidwebkit.qualifier.cache.FormImageCache;
import com.fluidbpm.fluidwebkit.qualifier.cache.RAWAttachmentsCache;
import com.fluidbpm.fluidwebkit.servlet.content.ImageStreamedContent;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.ws.client.v1.attachment.AttachmentClient;
import com.google.common.cache.Cache;
import com.google.common.io.BaseEncoding;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseId;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Request-scoped managed bean responsible for handling form attachments in the WebKit interface.
 * 
 * This bean provides functionality for:
 * - Fetching attachments associated with forms
 * - Managing attachment caches to improve performance
 * - Generating URLs for thumbnails and raw attachment content
 * - Handling attachment data retrieval and processing
 * - Supporting Excel worksheet extraction from attachments
 * 
 * The bean uses various caches to optimize attachment handling and reduce server load
 * when working with potentially large attachment files.
 *
 * @author jasonbruwer
 * @since 1.0
 */
@RequestScoped
@Named("webKitAttachmentBean")
public class WebKitAttachmentBean extends ABaseManagedBean {
    @Inject
    @FormAttachmentsCache
    private Cache<Long, List<Attachment>> formAttachmentCache;

    @Inject
    @RAWAttachmentsCache
    private Cache<Long, Attachment> attachmentCache;

    @Inject
    @FormImageCache
    private Cache<String, ImageStreamedContent> formImageStreamedCache;

    /**
     * Fetches a paginated subset of attachments for the specified form.
     * 
     * @param form The form for which to fetch attachments
     * @param fromIndex The starting index for pagination (0-based)
     * @param maxAllowed The maximum number of attachments to return
     * @return A list of attachments for the form, limited by the pagination parameters,
     *         or an empty list if no attachments are available or the pagination is out of bounds
     */
    public List<Attachment> actionFetchAttachmentsForForm(Form form, int fromIndex, int maxAllowed) {
        List<Attachment> returnVal = this.actionFetchAttachmentsForForm(form);
        if (maxAllowed < 1) maxAllowed = (returnVal == null) ? 0 : returnVal.size();

        if (returnVal == null || (fromIndex + 1) > maxAllowed) return new ArrayList<>();

        List<Attachment> subList = returnVal.subList(fromIndex, maxAllowed);
        return subList;
    }

    /**
     * Fetches all attachments for the specified form.
     * 
     * This method first checks the cache for attachments. If found, it returns clones of the cached
     * attachments with the base64 data cleared to reduce memory usage. If not found in the cache,
     * it fetches the attachments from the server, caches them, and returns clones with the base64 data cleared.
     * 
     * @param form The form for which to fetch attachments
     * @return A list of attachments for the form, or null if an error occurs or the form is invalid
     */
    public List<Attachment> actionFetchAttachmentsForForm(Form form) {
        try {
            if (this.getFluidClientDS() == null || (form == null || form.getId() == null)) return null;

            List<Attachment> attachments = this.formAttachmentCache.getIfPresent(form.getId());
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
            attachments = attachmentClient.getAttachmentsByForm(form, false);
            if (attachments == null) return null;

            attachments.sort(Comparator.comparing(Attachment::getId));

            this.formAttachmentCache.put(form.getId(), attachments);

            List<Attachment> returnVal = attachments.stream()
                    .map(toMap -> {
                        Attachment returnValClone = toMap.clone();
                        returnValClone.setAttachmentDataBase64(null);
                        return returnValClone;
                    })
                    .sorted(Comparator.comparing(Attachment::getId))
                    .collect(Collectors.toList());
            return returnVal;
        } catch (Exception except) {
            this.raiseError(except);
            return null;
        }
    }

    public String actionFetchTxtBasedAttachmentData(Attachment attachment) {
        try {
            if (this.getFluidClientDS() == null || (attachment == null || attachment.getId() == null)) return null;

            final AttachmentClient attachmentClient = this.getFluidClientDS().getAttachmentClient();

            Attachment attByIdWithData = attachmentClient.getAttachmentById(
                    attachment.getId(), true);
            return new String(attByIdWithData.getAttachmentDataRAW());
        } catch (Exception except) {
            this.raiseError(except);
            return null;
        }
    }

    /**
     * Clears all attachment caches associated with the specified form.
     * 
     * This method removes:
     * - All attachments for the form from the form attachment cache
     * - Each individual attachment from the RAW attachment cache
     * - All entries from the form image streamed cache
     * 
     * This is typically called when a form is updated or when attachments are added, 
     * modified, or removed to ensure that subsequent requests fetch fresh data.
     * 
     * @param formToClearFor The form for which to clear attachment caches
     */
    public void clearAttachmentCacheFor(Form formToClearFor) {
        if (formToClearFor == null || formToClearFor.getId() == null) return;

        List<Attachment> currentInCache =
                this.formAttachmentCache.getIfPresent(formToClearFor.getId());
        if (currentInCache != null) {
            currentInCache.stream().forEach(attItm -> {
                this.removeAttachmentFromRAWCache(attItm.getId());
            });
        }

        this.formImageStreamedCache.invalidateAll();

        this.formAttachmentCache.invalidate(formToClearFor.getId());
    }

    public boolean actionDoesAttachmentExist(Form formToCheck) {
        return (this.actionGetAttachmentCount(formToCheck) > 0);
    }

    public int actionGetAttachmentCount(Form formToCheck) {
        List<Attachment> att = this.actionFetchAttachmentsForForm(formToCheck);
        return (att == null) ? 0 : att.size();
    }

    public int actionGetColumnCountForAttachment(Form formToCheck) {
        return 1;
    }

    /**
     * Generates a URL for retrieving a thumbnail image of a form.
     * 
     * This method creates a URL that can be used to fetch a thumbnail image of the form
     * at the specified scale. The URL includes the form ID, form definition, and thumbnail scale.
     * 
     * @param wfItem The workspace fluid item containing form information
     * @param thumbnailScale The scale factor for the thumbnail (higher values produce larger thumbnails)
     * @return A URL string for retrieving the form thumbnail
     */
    public String actionGenerateURLForThumbnail(WorkspaceFluidItem wfItem, int thumbnailScale) {
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

    /**
     * Generates a URL for retrieving a thumbnail image of a specific attachment.
     * 
     * This method creates a URL that can be used to fetch a thumbnail image of the specified
     * attachment at the specified scale. The URL includes the form ID, form definition,
     * attachment ID, and thumbnail scale.
     * 
     * @param wfItem The workspace fluid item containing form information
     * @param attachment The attachment for which to generate a thumbnail URL
     * @param thumbnailScale The scale factor for the thumbnail (higher values produce larger thumbnails)
     * @return A URL string for retrieving the attachment thumbnail, or null if either wfItem or attachment is null
     */
    public String actionGenerateURLForThumbnail(WorkspaceFluidItem wfItem, Attachment attachment, int thumbnailScale) {
        if (wfItem == null || attachment == null) return null;

        String postFix = String.format(
                "/get_form_image?formId=%d&formDefinition=%s&attachmentId=%d&thumbnailScale=%d",
                wfItem.getFluidItemFormId(),
                wfItem.getFluidItemFormTypeURLSafe(),
                attachment.getId(),
                thumbnailScale);
        return postFix;
    }

    public String actionGenerateURLForRAW(WorkspaceFluidItem wfItem, Attachment attachment) {
        if (wfItem == null || attachment == null) return null;

        String postFix = String.format(
                "/get_attachment_raw?formId=%d&formDefinition=%s&attachmentId=%d",
                wfItem.getFluidItemFormId(),
                wfItem.getFluidItemFormTypeURLSafe(),
                attachment.getId());
        return postFix;
    }

    public String actionGenerateURLForAttToPDF(WorkspaceFluidItem wfItem, Attachment attachment) {
        String postFix = String.format(
                "/convert_attachment_raw_to_pdf?formId=%d&formDefinition=%s&attachmentId=%d",
                wfItem.getFluidItemFormId(),
                wfItem.getFluidItemFormTypeURLSafe(),
                attachment.getId());
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
        this.formImageStreamedCache.put(key, streamedContent);
    }

    public void removeAttachmentFreshFromCache(
        String conversationId,
        int numberOfAttachments
    ) {
        for (int index = 0; index < numberOfAttachments; index++) {
            String key = String.format("%s_%d", conversationId, index);
            this.formImageStreamedCache.invalidate(key);
        }
    }

    public void removeAttachmentFromRAWCache(Long attachmentId) {
        if (attachmentId == null || attachmentId < 1) return;

        //Need to optimise later. We need only remove the items for specific att...
        this.attachmentCache.invalidate(attachmentId);
    }

    public List<Worksheet> fetchExcelWorksheets(Long attachmentIdParam, String excelPasswordParam) {
        InputStream inputStream = null;
        try {
            StreamedContent sc = this.fetchAttachmentData(new Attachment(attachmentIdParam));
            inputStream = sc.getStream().get();
            FluidWorkspaceUtil fluidWorkspaceUtil = new FluidWorkspaceUtil();
            List<Worksheet> returnVal = fluidWorkspaceUtil.getWorksheetsFromExcel(inputStream, excelPasswordParam);
            return returnVal;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException eParam) {
                    this.raiseError(eParam);
                }
            }
        }
    }

    public StreamedContent fetchAttachmentData(Attachment attachment) {
        FacesContext context = FacesContext.getCurrentInstance();
        if ((context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) && false) {
            return new DefaultStreamedContent();
        } else {
            Attachment attForData = this.getFluidClientDS().getAttachmentClient().getAttachmentById(
                    attachment.getId(), true);
            byte[] rawData = BaseEncoding.base64().decode(attForData.getAttachmentDataBase64());

            return WebUtil.pfStreamContentFrom(
                new ByteArrayInputStream(rawData),
                attachment.getContentType(),
                attachment.getName()
            );
        }
    }

}
