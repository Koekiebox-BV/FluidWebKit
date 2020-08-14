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

package com.fluidbpm.fluidwebkit.backing.bean;

import com.fluidbpm.GitDescribe;
import com.fluidbpm.fluidwebkit.backing.bean.logger.ILogger;
import com.fluidbpm.fluidwebkit.backing.utility.Globals;
import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.fluidwebkit.ds.FluidClientPool;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.fluidwebkit.exception.WebSessionExpiredException;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.ws.auth.AppRequestToken;
import lombok.Getter;
import org.primefaces.PrimeFaces;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base backing bean for all JSF beans.
 *
 * @author jasonbruwer on 2018-06-03
 * @since 1.0
 */
public abstract class ABaseManagedBean implements Serializable {
	private final int pageSize = 15;

	//App Request Token...
	protected AppRequestToken configUserAppRequestToken;

	@Inject
	@Getter
	private transient FluidClientPool fcp;

	public static final String CONFIG_USER_GLOBAL_ID = "FCP_CONFIG_USER";

	/**
	 * Outcome JSF navigation outcomes.
	 */
	protected static class Outcome {
		public static final String LOGIN = "login";
		public static final String SESSION_EXPIRED = "session_expired";
		public static final String DASHBOARD = "dashboard";
	}

	/**
	 * Any column model.
	 */
	public static class ColumnModel implements Serializable {
		private String header;
		private String fluidFieldName;
		private Field.Type fluidFieldColumnType;

		/**
		 *
		 * @param header
		 * @param fluidFieldNameParam
		 * @param fluidFieldColumnTypeParam
		 */
		public ColumnModel(
				String header,
				String fluidFieldNameParam,
				Field.Type fluidFieldColumnTypeParam
		) {
			this.header = header;
			this.fluidFieldName = fluidFieldNameParam;
			this.fluidFieldColumnType = fluidFieldColumnTypeParam;
		}

		/**
		 *
		 * @return
		 */
		public String getHeader() {
			return this.header;
		}

		/**
		 *
		 * @return
		 */
		public String getFluidFieldName() {
			return this.fluidFieldName;
		}

		/**
		 *
		 * @return
		 */
		public Field.Type getFluidFieldColumnType() {
			return this.fluidFieldColumnType;
		}

		/**
		 *
		 * @return
		 */
		public String getFluidFieldColumnTypeTxt() {
			return (this.getFluidFieldColumnType() == null) ? null:
					this.getFluidFieldColumnType().toString();
		}
	}

	/**
	 * Session related variables.
	 */
	public static class SessionVariable {
		//User...
		public static final String USER = "user";
	}

	/**
	 * Default logger through;
	 * {@code LoggerFactory.getLogger(this.getClass().getName())}
	 * @return New logger instance.
	 */
	public ILogger getLogger() {
		return new ILogger() {
			@Override
			public void info(String details, Object... args) {
				System.out.print(getPrefix());
				System.out.printf(details, args);
				System.out.println();
			}

			@Override
			public void debug(String details, Object... args) {
				System.out.print(getPrefix());
				System.out.printf(details, args);
				System.out.println();
			}

			@Override
			public void error(String details, Exception exception) {
				System.err.print(getPrefix());
				System.err.printf("%s . %s", details, exception.getMessage());
				System.err.println();
			}

			private String getPrefix() {
				return this.getClass().getSimpleName();
			}
		};
	}

	/**
	 * 
	 * @return
	 */
	public String getSoftwareVersion() {
		return GitDescribe.GIT_DESCRIBE;
	}

	/**
	 *
	 * @return
	 */
	public int getPageSize() {
		return this.pageSize;
	}

	/**
	 *
	 * @return
	 */
	public String getDateAndTimeFormat() {
		try {
			String returnVal = this.getLoggedInUser().getDateFormat();
			returnVal += " ";
			returnVal += this.getLoggedInUser().getTimeFormat();
			return returnVal;
		} catch (WebSessionExpiredException webExp) {
			return "yyyy-MMM-dd HH:mm";
		} catch (Exception except) {
			this.getLogger().error("Unable to get 'Date and Time' Format! "+except.getMessage(),except);
			return "yyyy-MMM-dd HH:mm";
		}
	}

	/**
	 *
	 * @return
	 */
	public String getDateFormat() {
		try {
			return this.getLoggedInUser().getDateFormat();
		} catch (WebSessionExpiredException webExp) {
			return "yyyy-MMM-dd";
		} catch (Exception except) {
			this.getLogger().error("Unable to get 'Date' Format! "+except.getMessage(),except);
			return "yyyy-MMM-dd";
		}
	}

	/**
	 *
	 * @return
	 */
	public String getNumberFormat() {
		return "###,###,###.000";
	}

	/**
	 *
	 * @return
	 */
	public int getDateAndTimeFormatLength() {
		return this.getDateAndTimeFormat().length();
	}

	/**
	 *
	 * @return
	 */
	public int getDateFormatLength()
	{
		return getDateFormat().length();
	}

	/**
	 *
	 * @return
	 */
	public String getLoggedInUserUsername() {
		return this.getLoggedInUserSafe().getUsername();
	}

	/**
	 *
	 * @return
	 */
	public User getLoggedInUserSafe() {
		try {
			return this.getLoggedInUser();
		} catch (WebSessionExpiredException wsee) {
			return new User();
		}
	}

	/**
	 *
	 * @param fieldNameParam
	 * @return
	 */
	protected String getCustomUserFieldByName(String fieldNameParam){
		if (fieldNameParam == null || fieldNameParam.isEmpty()){
			return null;
		}

		try {
			List<Field> userFields = this.getLoggedInUser().getUserFields();
			if (userFields == null || userFields.isEmpty()){
				return null;
			}

			for (Field userField : userFields) {
				if (fieldNameParam.equals(userField.getFieldName())) {
					if (userField.getFieldValue() == null) {
						return null;
					}

					return userField.getFieldValue().toString();
				}
			}

			return null;
		} catch (WebSessionExpiredException wsee) {
			return null;
		}
	}

	/**
	 *
	 * @return
	 */
	public User getLoggedInUser() {
		FacesContext facCont = FacesContext.getCurrentInstance();
		ExternalContext externalContext = (facCont == null) ? null : facCont.getExternalContext();

		Map<String, Object> sessionMap = (externalContext == null) ? null :externalContext.getSessionMap();
		if (sessionMap == null) {
			throw new WebSessionExpiredException("No session. Session expired.");
		}

		if (sessionMap.containsKey(SessionVariable.USER)) {
			User returnVal = (User) sessionMap.get(SessionVariable.USER);
			return returnVal;
		}

		if (this.getHttpSession() != null) {
			this.getHttpSession().invalidate();
		}

		throw new WebSessionExpiredException("No session. Session expired. User logged out.");
	}

	/**
	 * Return the http session.
	 *
	 * @return {@code HttpSession}
	 */
	public HttpSession getHttpSession() {
		FacesContext facCont = FacesContext.getCurrentInstance();
		ExternalContext externalContext = (facCont == null) ? null : facCont.getExternalContext();
		Object sessObj = (externalContext == null) ? null :externalContext.getSession(false);

		if (sessObj instanceof HttpSession) {
			return (HttpSession)sessObj;
		}

		return null;
	}

	protected String getSessionId() {
		HttpSession session = this.getHttpSession();
		if (session == null) {
			return null;
		}
		return session.getId();
	}

	public FluidClientDS getFluidClientDS() {
		return this.fcp.get(this.getSessionId());
	}

	public FluidClientDS getFluidClientDSConfig() {
		return this.fcp.get(CONFIG_USER_GLOBAL_ID);
	}

	public void bindFluidClientDSToSession(String serviceTicket) {
		this.fcp.init(this.getSessionId(), serviceTicket, Globals.getConfigURLFromSystemProperty());
	}

	public void bindConfigFluidClientDS(String serviceTicket) {
		this.fcp.init(CONFIG_USER_GLOBAL_ID, serviceTicket, Globals.getConfigURLFromSystemProperty());
	}

	/**
	 *
	 * @return
	 */
	public boolean isSessionExpired() {
		FacesContext facCont = FacesContext.getCurrentInstance();
		ExternalContext externalContext = (facCont == null) ? null : facCont.getExternalContext();
		Object sessObj = (externalContext == null) ? null :externalContext.getSession(false);

		return (sessObj == null);
	}

	/**
	 *
	 * @return
	 */
	public String generateUUID() {
		return UUID.randomUUID().toString();
	}

	/**
	 *
	 * @return
	 */
	protected Map<String, String> getFacesRequestParameterMap() {
		Map<String, String> params =
				FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

		if (params == null) {
			params = new HashMap<>();
		}

		return params;
	}

	/**
	 *
	 * @param permissionsParam
	 * @return
	 */
	public boolean doesUserHavePermission(String permissionsParam) {
		String permissionLower = permissionsParam.toLowerCase();
		return this.doesUserHavePermissionArray(permissionLower.split("\\|"));
	}

	/**
	 *
	 * @param permissionParam
	 * @return
	 */
	public boolean doesUserHavePermissionArray(String... permissionParam) {
		if (permissionParam == null || permissionParam.length == 0) {
			return false;
		}

		User loggedInUser = null;
		try {
			loggedInUser = this.getLoggedInUser();
		} catch (ClientDashboardException except) {
			if (except.getErrorCode() != ClientDashboardException.ErrorCode.SESSION_EXPIRED) {
				this.getLogger().error("Unable to get logged in user. "+except.getMessage(), except);
			}
		}

		if (loggedInUser == null) {
			return false;
		}

		//When not the admin user...
		if(!"admin".equals(loggedInUser.getUsername().toLowerCase())) {
			FacesContext facCont = FacesContext.getCurrentInstance();
			ExternalContext externalContext = (facCont == null) ? null : facCont.getExternalContext();
			Map<String, Object> sessionMap = (externalContext == null) ? null : externalContext.getSessionMap();

			if (sessionMap == null) {
				return false;
			}

			sessionMap.put(SessionVariable.USER, loggedInUser);
		}

		return false;
	}

	/**
	 *
	 * @param paramName
	 * @return
	 */
	protected long getLongRequestParam(String paramName) {
		Map<String, String> params = getFacesRequestParameterMap();
		if (params == null || params.isEmpty()) {
			return -1;
		}

		try {
			return Long.parseLong(params.get(paramName));
		} catch (NumberFormatException nfe) {
			return -1;
		}
	}

	/**
	 * 
	 * @param paramName
	 * @return
	 */
	protected boolean getBooleanRequestParam(String paramName) {
		Map<String, String> params = getFacesRequestParameterMap();

		if (params == null || params.isEmpty()) {
			return false;
		}

		try {
			return Boolean.parseBoolean(params.get(paramName));
		} catch (NumberFormatException nfe) {
			return false;
		}
	}


	/**
	 *
	 * @param paramName
	 * @return
	 */
	protected int getIntRequestParam(String paramName) {
		Map<String, String> params = getFacesRequestParameterMap();
		if (params == null || params.isEmpty()) {
			return -1;
		}

		try {
			return Integer.parseInt(params.get(paramName));
		} catch (NumberFormatException nfe) {
			return -1;
		}
	}

	/**
	 * 
	 * @param paramName
	 * @return
	 */
	protected String getStringRequestParam(String paramName) {
		Map<String, String> params = getFacesRequestParameterMap();
		if (params == null || params.isEmpty()) {
			return null;
		}

		return params.get(paramName);
	}

	/**
	 *
	 * @param pageParam
	 */
	protected void redirectToPage(String pageParam) {
		this.redirectToPage(pageParam, false);
	}

	/**
	 *
	 * @param pageParam
	 */
	protected void redirectToPage(String pageParam,boolean isRootParam) {
		String pagesFolder = "/pages/";
		if (isRootParam) {
			pagesFolder = "/";
		}

		FacesContext facesContext = FacesContext.getCurrentInstance();
		HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
		String servletContextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();

		try {
			response.sendRedirect(servletContextPath + pagesFolder + pageParam + ".xhtml");
		} catch (IOException ioExcept) {
			throw new ClientDashboardException(
					"Unable to dispatch. " + ioExcept.getMessage(), ioExcept,
					ClientDashboardException.ErrorCode.IO_ERROR);
		}
	}

	/**
	 * Forward to the {@code dashboard} if user already logged in.
	 */
	public void actionForwardIfUserLoggedIn() {
		try {
			this.getLoggedInUser();
			this.redirectToPage(this.getLandingPageIfLoggedIn(),true);
		} catch (ClientDashboardException resDashException) {
			if (resDashException.getErrorCode() != ClientDashboardException.ErrorCode.SESSION_EXPIRED) {
				throw resDashException;
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	public String getLandingPageIfLoggedIn() {
		return Outcome.DASHBOARD;
	}

	/**
	 *
	 * @return
	 */
	public void actionConfirmUserLoggedIn() {
		this.checkLoggedIn();
	}

	/**
	 * Confirm whether user is logged in.
	 *
	 * @return
	 */
	protected boolean checkLoggedIn() {
		return checkLoggedIn("/index.xhtml");
	}

	/**
	 * Confirm whether user is logged in.
	 *
	 * @return
	 */
	protected boolean checkLoggedIn(String pagePath) {
		try {
			this.getLoggedInUser();
			return true;
		} catch (ClientDashboardException pgwExcept) {
			if (pgwExcept.getErrorCode() == ClientDashboardException.ErrorCode.SESSION_EXPIRED) {
				try {
					FacesContext facesContext = FacesContext.getCurrentInstance();
					HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
					String servletContextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();

					response.sendRedirect(servletContextPath + pagePath);
					return false;
				} catch (IOException ioExcept) {
					throw new ClientDashboardException(
							"Unable to dispatch. "
									+ ioExcept.getMessage(),
							ioExcept, ClientDashboardException.ErrorCode.IO_ERROR);
				}
			} else {
				//rethrow the error...
				throw pgwExcept;
			}
		}
	}

	/**
	 *
	 * @return
	 */
	protected String getConfigURLFromSystemProperty() {
		return Globals.getConfigURLFromSystemProperty();
	}

	/**
	 *
	 * @return
	 */
	protected String getConfigUserProperty() {
		return Globals.getConfigUserProperty();
	}

	/**
	 *
	 * @return
	 */
	protected String getConfigUserPasswordProperty() {
		return Globals.getConfigUserPasswordProperty();
	}

	/**
	 *
	 * @param roleParam
	 * @return
	 */
	protected boolean doesUserHaveAccessToRole(String roleParam) {
		if (this.getLoggedInUser().getUsername().equals("admin")) {
			return true;
		}

		return this.getLoggedInUser().doesUserHaveAccessToRole(roleParam);
	}

	/**
	 *
	 * @return
	 */
	public AppRequestToken getConfigUserAppRequestToken() {
		return this.configUserAppRequestToken;
	}

	/**
	 * Execute the JavaScript on the users browser.
	 *
	 * @param javascriptParam
	 */
	public void executeJavaScript(String javascriptParam) {
		if (javascriptParam == null || javascriptParam.trim().isEmpty()) {
			return;
		}

		//For PF 6.1
		//RequestContext.getCurrentInstance().execute(javascriptParam);

		//For PF 6.2
		PrimeFaces.current().executeScript(javascriptParam);
	}

	/**
	 * Retrieve the FusionReactor script for tracking user experience.
	 * @return JS for user tracking
	 */
	public String getFRUserTrackingScript() {
		return Globals.getFRUserTrackingScript();
	}
}
