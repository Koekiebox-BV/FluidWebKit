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

package com.fluidbpm.fluidwebkit.backing.utility;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by jasonbruwer on 2015/11/08.
 */
public class WebUtil {

	public static final String HTTPS = "https";

	// Converts a relative path into a full path
	// Taken from http://stackoverflow.com/posts/5212336/revisions
	/**
	 * Generate a callback URL for the current page using the
	 * servlet context.
	 *
	 * @param forceHTTPSParam Force HTTPS protocol.
	 *
	 * @return Redirect URL.
	 */
	public String getConstructCompleteCallbackURL(boolean forceHTTPSParam) {
		Object request = FacesContext.getCurrentInstance().getExternalContext().getRequest();
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpServletRequest = ((HttpServletRequest) request);

			String scheme      =    httpServletRequest.getScheme();        // http
			String serverName  =    httpServletRequest.getServerName();    // hostname.com
			int serverPort     =    httpServletRequest.getServerPort();    // 80
			String contextPath =    httpServletRequest.getContextPath();   // /mywebapp (/fluid)

			if (forceHTTPSParam) {
				scheme = HTTPS;
				if (serverPort == 80) {
					serverPort = 443;
				} else if(serverPort == 8080) {
					serverPort = 8443;
				}
			}

			// Reconstruct original requesting URL
			StringBuffer url =  new StringBuffer();
			url.append(scheme).append("://").append(serverName);
			
			//Add the port if not configured...
			if ((serverPort != 80) && (serverPort != 443)) {
				url.append(":").append(serverPort);
			}

			url.append(contextPath).append("/");

			return url.toString();
		}

		return null;
	}

	/**
	 * Generate a callback URL for the current page using the
	 * servlet context.
	 *
	 * @return Redirect URL.
	 */
	public String getConstructCompleteCallbackURL() {

		return this.getConstructCompleteCallbackURL(true);
	}

	/**
	 * Return the http session (Servlet).
	 *
	 * If no existing session exists, a {@code null} value is returned.
	 *
	 * @return {@code HttpSession}
	 *
	 * @see HttpSession
	 */
	public HttpSession getExistingHttpSession() {

		FacesContext facCont = FacesContext.getCurrentInstance();
		ExternalContext externalContext = (facCont == null) ? null : facCont.getExternalContext();
		Object sessObj = (externalContext == null) ? null :externalContext.getSession(false);

		if(sessObj instanceof HttpSession)
		{
			return (HttpSession)sessObj;
		}

		return null;
	}
}
