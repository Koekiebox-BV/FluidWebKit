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

import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitConfigBean;
import com.fluidbpm.program.api.util.UtilGlobal;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Servlet used to retrieve the custom company logo.
 */
@WebServlet(value = "/get_company_logo_small",
		name = "Get Company Logo Small Image Servlet",
		asyncSupported = false,
		displayName = "Get Company Logo Small Image Servlet",
		loadOnStartup = 1)
public class CompanyLogoServlet extends ImageServlet {

	@Inject
	private WebKitConfigBean webKitConfigBean;

	@Override
	protected void doGet(HttpServletRequest reqParam, HttpServletResponse respParam) throws ServletException, IOException {
		if (!this.webKitConfigBean.getCompanyLogoSmallPathExists()) return;
		
		this.writeImageToResponseOutput(respParam, new ImageStreamedContent(
				UtilGlobal.readFileBytes(new File(this.webKitConfigBean.getCompanyLogoSmallFilepath())),
				"image/svg+xml",
				String.format("comp_logo_%s.svg", UUID.randomUUID().toString().substring(0, 5))));
	}


}
