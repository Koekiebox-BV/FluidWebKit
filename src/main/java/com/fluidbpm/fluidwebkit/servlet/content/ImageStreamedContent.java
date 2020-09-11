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

import org.primefaces.model.DefaultStreamedContent;

import java.io.ByteArrayInputStream;

/**
 * The image steamed content used for caching.
 *
 * @author jasonbruwer on 2019-03-06.
 * @since 1.0
 */
public class ImageStreamedContent extends DefaultStreamedContent {
	private byte[] imageBytes = null;

	/**
	 *
	 * @param imageBytesParam
	 * @param contentType
	 * @param name
	 */
	public ImageStreamedContent(
			byte[] imageBytesParam,
			String contentType,
			String name
	) {
		super(new ByteArrayInputStream(imageBytesParam), contentType, name);
		this.imageBytes = imageBytesParam;
	}

	/**
	 *
	 * @return
	 */
	public DefaultStreamedContent cloneAsDefaultStreamedContent() {
//		return DefaultStreamedContent.builder()
//				.stream(new ByteArrayInputStream(this.imageBytes))
//				.contentType(this,getContentType())
//				.name(this.getName());
		return new DefaultStreamedContent(
				new ByteArrayInputStream(this.imageBytes),
				this.getContentType(),
				this.getName());
	}
}
