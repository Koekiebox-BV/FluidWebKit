/*
 * Koekiebox CONFIDENTIAL
 *
 * [2012] - [2020] Koekiebox (Pty) Ltd
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

package com.fluidbpm.fluidwebkit.backing.bean.config;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.utility.Globals;
import com.fluidbpm.fluidwebkit.backing.utility.WebUtil;
import com.fluidbpm.program.api.util.UtilGlobal;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * Bean storing all access for the logged in user.
 */
@RequestScoped
@Named("webKitConfHelpBean")
public class WebKitConfigHelperBean extends ABaseManagedBean {
	@Inject
	private WebKitConfigBean webKitConfigBean;

	/**
	 * Request params.
	 */
	private static final class WebParam {
		private static final String ErrorMessage = "errorMessage";
		private static final String InfoMessageHeader = "infoMessageHeader";
		private static final String InfoMessage = "infoMessage";
	}

	/**
	 * Retrieve the FusionReactor script for tracking user experience.
	 * @return JS for user tracking
	 */
	public String getFRUserTrackingScript() {
		return Globals.getFRUserTrackingScript();
	}


	/**
	 * Get the configured or server URL.
	 *
	 * @return {@code FluidServerURL} value or server url.
	 */
	public String getFluidWebAppServletPushURL() {
		String fluidServerUrlConfig = this.webKitConfigBean.getFluidServerURL();

		if (fluidServerUrlConfig != null && (!fluidServerUrlConfig.trim().isEmpty()) &&
				fluidServerUrlConfig.toLowerCase().startsWith("https")) {
			return new WebUtil().getConstructCompleteCallbackURL(true);
		}

		//From the request...
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();

		String serverName = externalContext.getRequestServerName();
		int port = externalContext.getRequestServerPort();
		String contextPath = externalContext.getRequestContextPath();

		return externalContext.getRequestScheme().concat("://").concat(
				serverName).concat(":").concat(Integer.toString(port)).concat(contextPath).toString();
	}

	/**
	 *
	 */
	public void actionDisplayErrorRemote() {
		String value = this.getFacesRequestParameterMap().get(WebParam.ErrorMessage);
		FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, value, UtilGlobal.EMPTY);
		FacesContext.getCurrentInstance().addMessage(null, fMsg);
	}

	/**
	 *
	 */
	public void actionDisplayInformationRemote() {
		Map<String,String> reqParamMap = this.getFacesRequestParameterMap();

		String infoMessageHeader = reqParamMap.get(WebParam.InfoMessageHeader);
		String infoMessage = reqParamMap.get(WebParam.InfoMessage);

		FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, infoMessageHeader, infoMessage);
		FacesContext.getCurrentInstance().addMessage(null, fMsg);
	}
	
	public int getUserBrowserSessionDialogHeight() {
		return (this.getUserBrowserSessionWindowHeight() - 370);
	}

	/**
	 *
	 * @return
	 */
	public int getUserBrowserSessionScrollableHeight() {
		return (this.getUserBrowserSessionWindowHeight() - 100);
	}

	/**
	 *
	 * @return
	 */
	public int getUserBrowserSessionWindowHeight() {
		Object httpSessionObj =
				FacesContext.getCurrentInstance().getExternalContext().getSession(false);
		if (httpSessionObj instanceof HttpSession) {
			HttpSession casted = (HttpSession)httpSessionObj;

			Object heightValue = casted.getAttribute(
					SessionVariable.USER_BROWSER_WINDOW_HEIGHT);
			if (heightValue != null && heightValue instanceof Number) {
				return ((Number)heightValue).intValue();
			}
		}
		return 900;
	}

	public void actionDoNothing() {
		//Do nothing...
		this.getLogger().debug("Action fired to do nothing.");
	}

	public String convertToUpper(String toConvert) {
		return toConvert.toUpperCase();
	}
}
