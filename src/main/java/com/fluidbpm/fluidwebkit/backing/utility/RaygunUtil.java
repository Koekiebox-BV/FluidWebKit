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

import com.fluidbpm.GitDescribe;
import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.user.User;
import com.mindscapehq.raygun4java.core.RaygunClient;
import com.mindscapehq.raygun4java.core.messages.RaygunIdentifier;
import com.mindscapehq.raygun4java.webprovider.RaygunServletClient;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for Raygun error reporting.
 *
 * @author jasonbruwer on 2019-01-20.
 * @since 1.0
 */
public class RaygunUtil {
	private String[] tags;

	public static boolean isRaygunEnabled() {
		return (UtilGlobal.Raygun.API_KEY != null &&
				!UtilGlobal.Raygun.API_KEY.trim().isEmpty());
	}

	public RaygunUtil(String tagParam) {
		this.tags = new String[]{tagParam};
	}

	public RaygunUtil(String ... tagsParam) {
		this.tags = tagsParam;
	}

	public void raiseErrorToRaygun(ExceptionQueuedEventContext contextParam) {
		if (!RaygunUtil.isRaygunEnabled()) {
			return;
		}

		Throwable throwable = contextParam.getException();
		ExternalContext externalContext = contextParam.getContext().getExternalContext();
		//Report error through Raygun...
		HttpServletRequest httpServletRequest = null;
		HttpServletResponse httpServletResponse = null;
		Throwable throwableReal = throwable;
		if (throwable instanceof FacesException) {
			throwableReal = throwableReal.getCause();
			if (throwableReal instanceof ELException) {
				throwableReal = throwableReal.getCause();
			}
		}

		if (externalContext.getRequest() instanceof HttpServletRequest) {
			httpServletRequest = (HttpServletRequest)externalContext.getRequest();
			httpServletResponse = (HttpServletResponse)externalContext.getResponse();
		}

		Map<String,Object> sessionMap = externalContext.getSessionMap();
		this.raiseErrorToRaygun(
				throwableReal,
				httpServletRequest,
				null,
				sessionMap);
	}

	public void raiseErrorToRaygun(
		Throwable throwableParam,
		HttpServletRequest httpServletRequestParam,
		HttpServletResponse httpServletResponseParam,
		Map<String,Object> additionalPropertiesMapParam
	) {
		if (!RaygunUtil.isRaygunEnabled()) {
			return;
		}

		RaygunClient client = this.getRaygunClient(
				httpServletRequestParam,
				httpServletResponseParam);

		if (additionalPropertiesMapParam != null &&
				additionalPropertiesMapParam.containsKey(ABaseManagedBean.SessionVariable.USER)) {
			User loggedInUser = (User)additionalPropertiesMapParam.get(
					ABaseManagedBean.SessionVariable.USER);
			RaygunIdentifier raygunIdentifier =
					new RaygunIdentifier(loggedInUser.getUsername());
			client.setUser(raygunIdentifier);
		}

		client.send(throwableParam, this.generateMap(additionalPropertiesMapParam));
	}

	public void raiseErrorToRaygun(Throwable exceptionThrownParam) {
		this.raiseErrorToRaygun(exceptionThrownParam, null);
	}

	public void raiseErrorToRaygun(
		Throwable exceptionThrownParam,
		User loggedInUserParam
	) {
		if ( !RaygunUtil.isRaygunEnabled()) return;

		RaygunClient client = this.getRaygunClient(null,null);
		if(loggedInUserParam != null && loggedInUserParam.getUsername() != null) {
			client.setUser(new RaygunIdentifier(loggedInUserParam.getUsername()));
		}

		client.send(exceptionThrownParam, this.generateMap(null));
	}

	public void raiseErrorToRaygun(
			Throwable throwableParam,
			HttpServletRequest httpServletRequestParam,
			HttpServletResponse httpServletResponseParam) {
		if (!RaygunUtil.isRaygunEnabled()) return;

		RaygunClient client = this.getRaygunClient(
						httpServletRequestParam,
						httpServletResponseParam);

		client.send(throwableParam, this.generateMap(null));
	}
	
	private Map<String,String> generateMap(Map existingMapParam) {
		Map<String, String> addData = new HashMap<>();
		if (existingMapParam != null){
			if (existingMapParam.containsKey(
							ABaseManagedBean.SessionVariable.USER)) {
				User loggedInUser = (User)existingMapParam.get(
						ABaseManagedBean.SessionVariable.USER);
				addData.put("Fluid User", loggedInUser.getUsername());
			}
		}
		
		addData.put("Fluid-API Version", GitDescribe.GIT_DESCRIBE);
		return addData;
	}

	private RaygunClient getRaygunClient(
		HttpServletRequest httpServletRequestParam,
		HttpServletResponse httpServletResponseParam
	) {
		RaygunClient client = null;
		if (httpServletRequestParam != null) {
			RaygunServletClient servletClient = new RaygunServletClient(
					UtilGlobal.Raygun.API_KEY,
					httpServletRequestParam);

			if (httpServletResponseParam != null) {
				servletClient.setResponse(httpServletResponseParam);
			}
			client = servletClient;
		} else {
			client = new RaygunClient(UtilGlobal.Raygun.API_KEY);
		}

		if (this.tags != null) {
			Set<String> tags = new HashSet<>();
			for (String tag : this.tags) {
				if (tag != null && !tag.trim().isEmpty()) {
					tags.add(tag);
				}
			}
			client.setTags(tags);
		}

		client.setVersion(GitDescribe.GIT_DESCRIBE);

		return client;
	}
}
