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
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitConfigBean;
import com.fluidbpm.fluidwebkit.backing.bean.logger.ILogger;
import com.fluidbpm.fluidwebkit.backing.utility.RaygunUtil;
import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.fluidwebkit.ds.FluidClientPool;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.fluidwebkit.exception.WebSessionExpiredException;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.role.Role;
import com.fluidbpm.program.api.vo.thirdpartylib.ThirdPartyLibraryTaskIdentifier;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.ws.client.v1.role.RoleClient;
import com.fluidbpm.ws.client.v1.user.LoginClient;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.fluidbpm.program.api.vo.thirdpartylib.ThirdPartyLibraryTaskIdentifier.ThirdPartyLibraryTaskType.CustomWebAction;

/**
 * Base backing bean for all JSF beans.
 *
 * @author jasonbruwer on 2018-06-03
 * @since 1.0
 */
public abstract class ABaseManagedBean implements Serializable {
	private final int pageSize = 15;

	@Inject
	@Getter
	private transient FluidClientPool fcp;

	@Getter
	@Setter
	private String dialogHeaderTitle;

	@Getter
	@Setter
	private String sessionIdFallback;

	public static final String CONFIG_USER_GLOBAL_ID = "FCP_CONFIG_USER";
	private static Long LAST_TIME_CONF_LOGGED_IN = 0L;
	private static int LOGIN_DURATION_HOURS = 4;

	@Inject
	private WebKitConfigBean webKitConfigBean;

	/**
	 * Get the standard Raygun error tag.
	 *
	 * @return Standard Raygun UI tag.
	 */
	public String getRaygunUITag() {
		return "FluidWebKit";
	}

	public void raiseError(Exception exception) {
		this.getLogger().error(exception.getMessage(), exception);
		if (RaygunUtil.isRaygunEnabled()) {
			new RaygunUtil(this.getRaygunUITag()).raiseErrorToRaygun(exception, this.getLoggedInUserSafe());
		}

		if (FacesContext.getCurrentInstance() == null) return;

		String adminErrorMessage = UtilGlobal.getProperty(System.getProperties(), "ENABLE_ADMIN_MESSAGE", "false");
		FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed.", exception.getMessage());
		if (adminErrorMessage.trim().toLowerCase().equals("true")) {
			fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed.", "Please contact administrator.");
		}
		FacesContext.getCurrentInstance().addMessage(null, fMsg);
	}

	public void raiseError(
		Exception exception,
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse
	) {
		this.getLogger().error(exception.getMessage(), exception);
		if (!RaygunUtil.isRaygunEnabled()) return;

		new RaygunUtil(this.getRaygunUITag()).raiseErrorToRaygun(
				exception,
				httpServletRequest,
				httpServletResponse);
	}

	protected static class Outcome {
		public static final String LOGIN = "login";
		public static final String SESSION_EXPIRED = "session_expired";
		public static final String DASHBOARD = "dashboard";
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@EqualsAndHashCode
	public static class ColumnModel implements Serializable {
		private String header;
		private String fluidFieldName;
		private String fluidFieldDescription;
		private Field.Type fluidFieldColumnType;
		private String fluidFieldMetaData;

		private boolean enabled;
		private boolean visible;
		private boolean filterable;

		private ColumnModel(ColumnModel colMdl) {
			if (colMdl == null) return;

			this.header = colMdl.header;
			this.fluidFieldName = colMdl.fluidFieldName;
			this.fluidFieldDescription = colMdl.fluidFieldDescription;
			this.fluidFieldColumnType = colMdl.fluidFieldColumnType;
			this.fluidFieldMetaData = colMdl.fluidFieldMetaData;
			this.enabled = colMdl.enabled;
			this.visible = colMdl.visible;
			this.filterable = colMdl.filterable;
		}

		public ColumnModel(
			String header,
			String fluidFieldName,
			String fluidFieldDescription,
			Field.Type fluidFieldColumnType,
			String fluidFieldMetaData,
			boolean enabled,
			boolean visible,
			boolean filterable
		) {
			this.header = header;
			this.fluidFieldName = fluidFieldName;
			this.fluidFieldDescription = fluidFieldDescription;
			this.fluidFieldColumnType = (fluidFieldColumnType);
			this.fluidFieldMetaData = (fluidFieldMetaData);
			this.enabled = (enabled);
			this.visible = (visible);
			this.filterable = (filterable);
		}
		
		public ColumnModel(
			String header,
			String fluidFieldName,
			Field.Type fluidFieldColumnType,
			String fluidFieldMetaData,
			boolean enabled,
			boolean visible,
			boolean filterable
		) {
			this.header = header;
			this.fluidFieldName = (fluidFieldName);
			this.fluidFieldColumnType = (fluidFieldColumnType);
			this.fluidFieldMetaData = (fluidFieldMetaData);
			this.enabled = (enabled);
			this.visible = (visible);
			this.filterable = (filterable);
		}

		public void setVisible(boolean vis) {
			//this.visible = vis;
			//System.out.println("ignore: Hidden: "+vis);
		}

		public boolean isVisible() {
			return this.visible;
		}

		public boolean getVisible() {
			return this.isVisible();
		}

		public String getFluidFieldColumnTypeTxt() {
			return (this.getFluidFieldColumnType() == null) ? null: this.getFluidFieldColumnType().toString();
		}

		public ColumnModel cloneCM() {
			return new ColumnModel(this);
		}

		public void flipVisibility() {
			this.visible = !this.visible;
		}
	}

	/**
	 * Session related variables.
	 */
	public static class SessionVariable {
		//User...
		public static final String USER = "user";

		//User Browser
		public static final String USER_BROWSER_WINDOW_WIDTH = "ub_window_width";
		public static final String USER_BROWSER_WINDOW_HEIGHT = "ub_window_height";

		//Push Servlet...
		public static final String PUSH_SERVLET_EVENT_BUS = "push_servlet_event_bus";
	}

	/**
	 * Default logger through;
	 * {@code LoggerFactory.getLogger(this.getClass().getName())}
	 * @return New logger instance.
	 */
	public ILogger getLogger() {
		return new ILogger() {
			@Override
			public void info(String template, Object... args) {
				UtilGlobal.sysOut(template, args);
			}

			@Override
			public void debug(String template, Object... args) {
				UtilGlobal.sysOut(template, args);
			}

			@Override
			public void error(String details, Exception exception) {
				System.err.print(getPrefix());
				System.err.printf("%s . %s", details,
						exception == null ? "[Except-Not-Set]" : exception.getMessage());
				System.err.println();
				if (exception != null) exception.printStackTrace();
			}

			private String getPrefix() {
				return this.getClass().getSimpleName();
			}
		};
	}

	public String getSoftwareVersion() {
		return GitDescribe.GIT_DESCRIBE;
	}

	public int getPageSize() {
		return this.pageSize;
	}

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

	public String getNumberFormat() {
		return "###,###,###.000";
	}

	public int getDateAndTimeFormatLength() {
		return this.getDateAndTimeFormat().length();
	}

	public int getDateFormatLength() {
		return getDateFormat().length();
	}

	public String getLoggedInUserUsername() {
		return this.getLoggedInUserSafe().getUsername();
	}

	public String getLoggedInUserUsernameNoSpaces() {
		String username = this.getLoggedInUserUsername();
		if (UtilGlobal.isBlank(username)) return username;
		return UtilGlobal.removeWhitespace(username);
	}

	public User getLoggedInUserSafe() {
		try {
			return this.getLoggedInUser();
		} catch (WebSessionExpiredException wsee) {
			return new User();
		}
	}

	protected String getCustomUserFieldByName(String fieldNameParam){
		if (fieldNameParam == null || fieldNameParam.isEmpty()) return null;

		try {
			List<Field> userFields = this.getLoggedInUser().getUserFields();
			if (userFields == null || userFields.isEmpty()) return null;

			for (Field userField : userFields) {
				if (fieldNameParam.equals(userField.getFieldName())) {
					if (userField.getFieldValue() == null) return null;

					return userField.getFieldValue().toString();
				}
			}
			return null;
		} catch (WebSessionExpiredException wsee) {
			return null;
		}
	}

	public User getLoggedInUser() {
		FacesContext facCont = FacesContext.getCurrentInstance();
		ExternalContext externalContext = (facCont == null) ? null : facCont.getExternalContext();

		Map<String, Object> sessionMap = (externalContext == null) ? null :externalContext.getSessionMap();
		if (sessionMap == null) throw new WebSessionExpiredException("No session. Session expired.");

		if (sessionMap.containsKey(SessionVariable.USER)) {
			User returnVal = (User) sessionMap.get(SessionVariable.USER);
			return returnVal;
		}

		if (this.getHttpSession() != null) this.getHttpSession().invalidate();

		throw new WebSessionExpiredException("No session. Session expired. User logged out.");
	}

	public HttpSession getHttpSession() {
		FacesContext facCont = FacesContext.getCurrentInstance();
		ExternalContext externalContext = (facCont == null) ? null : facCont.getExternalContext();
		Object sessObj = (externalContext == null) ? null :externalContext.getSession(false);

		if (sessObj instanceof HttpSession) return (HttpSession)sessObj;

		return null;
	}

	protected String getSessionId() {
		HttpSession session = this.getHttpSession();
		if (session == null) {
			if (this.getSessionIdFallback() != null) {
				return this.getSessionIdFallback();
			}
			return null;
		}
		return session.getId();
	}

	public FluidClientDS getFluidClientDS() {
		try {
			return this.fcp.get(this.getSessionId());
		} catch (WebSessionExpiredException weExcept) {
			return null;
		}
	}

	public FluidClientDS getFluidClientDSConfig() {
		return this.fcp.get(CONFIG_USER_GLOBAL_ID);
	}

	public void bindFluidClientDSToSession(String serviceTicket) {
		this.fcp.init(this.getSessionId(), serviceTicket, UtilGlobal.getConfigURLFromSystemProperty(null));
	}

	public void bindConfigFluidClientDS() {
		Properties existingProps = null;
		long now = System.currentTimeMillis();
		if ((LAST_TIME_CONF_LOGGED_IN + TimeUnit.HOURS.toMillis(LOGIN_DURATION_HOURS)) < now) {
			synchronized (LAST_TIME_CONF_LOGGED_IN) {
				LoginClient loginClient = new LoginClient(UtilGlobal.getConfigURLFromSystemProperty(existingProps));
				try {
					String serviceTicket = loginClient.login(
							UtilGlobal.getConfigUserProperty(existingProps),
							UtilGlobal.getConfigUserPasswordProperty(existingProps)).getServiceTicket();
					LAST_TIME_CONF_LOGGED_IN = System.currentTimeMillis();

					this.fcp.invalidate(CONFIG_USER_GLOBAL_ID);
					this.fcp.init(CONFIG_USER_GLOBAL_ID, serviceTicket, UtilGlobal.getConfigURLFromSystemProperty(existingProps));
				} catch (Exception except) {
					this.getLogger().error(except.getMessage(), except);
					throw new IllegalStateException(except.getMessage(), except);
				} finally {
					loginClient.closeAndClean();
				}
			}
		}
	}

	public boolean isSessionExpired() {
		FacesContext facCont = FacesContext.getCurrentInstance();
		ExternalContext externalContext = (facCont == null) ? null : facCont.getExternalContext();
		Object sessObj = (externalContext == null) ? null :externalContext.getSession(false);

		return (sessObj == null);
	}

	public String generateUUID() {
		return UUID.randomUUID().toString();
	}

	protected Map<String, String> getFacesRequestParameterMap() {
		Map<String, String> params =
				FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

		if (params == null) params = new HashMap<>();

		return params;
	}

	public boolean doesUserHavePermission(String permissionsParam) {
		String permissionLower = permissionsParam.toLowerCase();
		return this.doesUserHavePermissionArray(permissionLower.split("\\|"));
	}

	public boolean doesUserHavePermissionArray(String ... permissionParam) {
		if (permissionParam == null || permissionParam.length == 0) return false;

		User loggedInUser = null;
		try {
			loggedInUser = this.getLoggedInUser();
		} catch (ClientDashboardException except) {
			if (except.getErrorCode() != ClientDashboardException.ErrorCode.SESSION_EXPIRED) {
				this.getLogger().error("Unable to get logged in user. "+except.getMessage(), except);
			}
			return false;
		}

		if (User.ADMIN_USERNAME.equals(loggedInUser.getUsername().toLowerCase())) return true;

		//When not the admin user...
		List<Role> roles = loggedInUser.getRoles();
		if (roles == null || roles.isEmpty()) return false;

		Map<String, Boolean> accessMap = new HashMap<>();
		for (String param : permissionParam) accessMap.put(param, Boolean.FALSE);

		List<Role> updatedRolesForReCache = new ArrayList<>();
		for (Role role : roles) {
			if (role.getAdminPermissions() == null) {
				RoleClient roleClient = this.getFluidClientDSConfig().getRoleClient();
				Role roleById = roleClient.getRoleByName(role.getName());
				role.setAdminPermissions(roleById.getAdminPermissions());
				updatedRolesForReCache.add(roleById);
			}

			for (String param : permissionParam) {
				if (role.getAdminPermissions().contains(param)) accessMap.put(param, Boolean.TRUE);
			}
		}

		AtomicBoolean returnVal = new AtomicBoolean(true);
		accessMap.values().stream().forEach(bool -> {
			if (!bool) {
				returnVal.set(false);
				return;
			}
		});

		if (!updatedRolesForReCache.isEmpty()) {
			FacesContext facCont = FacesContext.getCurrentInstance();
			ExternalContext externalContext = facCont.getExternalContext();
			Map<String, Object> sessionMap = externalContext.getSessionMap();
			loggedInUser.setRoles(updatedRolesForReCache);
			sessionMap.put(SessionVariable.USER, loggedInUser);
		}

		return returnVal.get();
	}

	protected long getLongRequestParam(String paramName) {
		Map<String, String> params = getFacesRequestParameterMap();
		if (params == null || params.isEmpty()) return -1;

		try {
			return Long.parseLong(params.get(paramName));
		} catch (NumberFormatException nfe) {
			return -1;
		}
	}

	protected boolean getBooleanRequestParam(String paramName) {
		Map<String, String> params = getFacesRequestParameterMap();

		if (params == null || params.isEmpty()) return false;

		try {
			return Boolean.parseBoolean(params.get(paramName));
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

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

	protected String getStringRequestParam(String paramName) {
		Map<String, String> params = getFacesRequestParameterMap();
		if (params == null || params.isEmpty()) return null;

		return params.get(paramName);
	}

	protected void redirectToPage(String pageParam) {
		this.redirectToPage(pageParam, false);
	}

	protected void redirectToPage(String pageParam,boolean isRootParam) {
		String pagesFolder = "/pages/";
		if (isRootParam) pagesFolder = "/";

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

	public String getLandingPageIfLoggedIn() {
		return Outcome.DASHBOARD;
	}

	public void actionConfirmUserLoggedIn() {
		this.checkLoggedIn();
	}


	protected boolean checkLoggedIn() {
		return checkLoggedIn("/index.xhtml");
	}

	/**
	 * Confirm whether user is logged in.
	 *
	 * @param pagePath The path to the page.
	 *
	 * @return {@code true} if logged in.
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

	protected boolean doesUserHaveAccessToRole(String roleParam) {
		try {
			if (this.getLoggedInUser().getUsername().equals(User.ADMIN_USERNAME)) return true;
			return this.getLoggedInUser().doesUserHaveAccessToRole(roleParam);
		} catch (WebSessionExpiredException exp) {
			return false;
		}
	}

	/**
	 * Execute the JavaScript on the users browser.
	 * @param javascriptParam The js to exec.
	 */
	public void executeJavaScript(String javascriptParam) {
		if (javascriptParam == null || javascriptParam.trim().isEmpty()) return;

		//For PF 6.2 >
		PrimeFaces.current().executeScript(javascriptParam);
	}

	protected String getConfigURLFromSystemProperty() {
		return UtilGlobal.getConfigURLFromSystemProperty(null);
	}

	protected String getConfigUserProperty() {
		return UtilGlobal.getConfigUserProperty(null);
	}

	protected String getConfigUserPasswordProperty() {
		return UtilGlobal.getConfigUserPasswordProperty(null);
	}

	protected boolean isThirdPartyWebActionEnabledFormType(String formType) {
		List<String> actionForFormTypes = this.getThirdPartyWebActionTaskIdsForFormType(formType);
		return actionForFormTypes != null && !actionForFormTypes.isEmpty();
	}

	protected List<String> getThirdPartyWebActionTaskIdsForFormType(String formType) {
		List<ThirdPartyLibraryTaskIdentifier> thirdPartyLibs = this.webKitConfigBean.getThirdPartyLibTaskIdentifiers();
		if (thirdPartyLibs == null) return null;

		return thirdPartyLibs.stream()
				.filter(itm -> itm.getThirdPartyLibraryTaskType() == CustomWebAction && itm.getFormDefinitions() != null)
				.filter(itm -> {
					List<Form> formDefs = itm.getFormDefinitions();
					Form matchedForm = formDefs.stream()
							.filter(formDefItm -> formType.equals(formDefItm.getFormType()))
							.findFirst()
							.orElse(null);
					return matchedForm != null && itm.getTaskIdentifier() != null;
				})
				.map(itm -> itm.getTaskIdentifier())
				.collect(Collectors.toList());
	}

	private static final String save = "save";
	protected String getThirdPartyWebActionSaveForFormType(String formType) {
		List<String> allActions = this.getThirdPartyWebActionTaskIdsForFormType(formType);
		if (allActions == null || allActions.isEmpty()) return null;

		return allActions.stream()
				.filter(val -> val.toLowerCase().trim().equals(save))
				.findFirst()
				.orElse(null);
	}
	
}
