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

import com.fluidbpm.fluidwebkit.backing.utility.WebUtil;
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
	private String contentType = null;
	private String name = null;

	public ImageStreamedContent(
		byte[] imageBytes,
		String contentType,
		String name
	) {
		super();
		this.imageBytes = imageBytes;
		this.contentType = contentType;
		this.name = name;
	}
	
	public DefaultStreamedContent cloneAsDefaultStreamedContent() {
		return WebUtil.pfStreamContentFrom(
			new ByteArrayInputStream(this.imageBytes),
			this.getContentType(),
			this.getName()
		);
	}
}
