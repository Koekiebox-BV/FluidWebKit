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

package com.fluidbpm.fluidwebkit.infrastructure;

import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.fluidwebkit.infrastructure.cache.ExitEventForFluidAPI;
import com.fluidbpm.fluidwebkit.qualifier.WebKitResource;
import com.fluidbpm.fluidwebkit.qualifier.cache.FormAttachmentsCache;
import com.fluidbpm.fluidwebkit.qualifier.cache.FormImageCache;
import com.fluidbpm.fluidwebkit.servlet.content.ImageStreamedContent;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import javax.enterprise.inject.Produces;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * CDI for resources.
 */
public class Resources {
	private static final Cache<String, FluidClientDS> cacheFluidDS = CacheBuilder.newBuilder()
			.expireAfterAccess(3, TimeUnit.DAYS)
			.removalListener(new ExitEventForFluidAPI())
			.build();

	private static Cache<String, ImageStreamedContent> imageServletImageCache =
			CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES).build();

	public static Cache<Long, List<Attachment>> imageAttachmentsByFormCache =
			CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES).build();

	@Produces
	@WebKitResource
	public Cache<String, FluidClientDS> getFluidClientDSCache() {
		return Resources.cacheFluidDS;
	}

	@Produces
	@FormImageCache
	public Cache<String, ImageStreamedContent> getFormImageServletCache(){
		return Resources.imageServletImageCache;
	}

	@Produces
	@FormAttachmentsCache
	public Cache<Long, List<Attachment>> getFormAttachmentsCache(){
		return Resources.imageAttachmentsByFormCache;
	}

}
