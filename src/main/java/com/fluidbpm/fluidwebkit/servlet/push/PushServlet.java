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

package com.fluidbpm.fluidwebkit.servlet.push;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.login.WebKitNotificationsBean;
import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.fluidwebkit.exception.WebSessionExpiredException;
import com.fluidbpm.fluidwebkit.servlet.ABaseFWKServlet;
import com.fluidbpm.program.api.vo.user.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * Background servlet used to check for login status.
 */
@WebServlet(value = "/webkit_push",
		name = "WebKit Push Servlet",
		asyncSupported = false,
		displayName = "WebKit Push Servlet",
		loadOnStartup = 1)
public class PushServlet extends ABaseFWKServlet {
	public static final String APPLICATION_JSON_STRING = "application/json";

	@Inject
	private transient WebKitNotificationsBean webKitNotificationsBean;

	private static final long WAIT_TIME_NOTI_FETCH_MILLIS = TimeUnit.SECONDS.toMillis(5);

	public static final class ErrorCode {
		public static final int USER_NOT_LOGGED_IN = 1;
	}

	/**
	 *
	 */
	public static class Commands {
		public static final String NONE = "none";
		//Refresh User Notification...
		public static final String REFRESH_USER_NOTIFICATION = "refresh_user_notification";
	}

	/**
	 *
	 */
	public static class FieldMapping {
		//Error...
		public static final String ERROR = "error";
		public static final String ERROR_CODE = "error_code";

		public static final String DELAY = "delay";

		public static final String COMMANDS = "commands";
		public static final String COMMAND = "command";

		//Chat...
		public static final String TEXT_HEADER = "textHeader";
		public static final String TEXT = "text";
		public static final String MAKE_USE_OF_REMOTE_COMMAND = "makeUseOfRemoteCommand";
	}

	public FluidClientDS getFluidClientDS(String sessionId) {
		try {
			return this.fcp.get(sessionId);
		} catch (WebSessionExpiredException weExcept) {
			return null;
		}
	}

	@Override
	protected void doGet(HttpServletRequest httpServletRequestParam, HttpServletResponse httpServletResponseParam) throws ServletException, IOException {
		this.doPost(httpServletRequestParam, httpServletResponseParam);
	}

	@Override
	protected void doPost(
		HttpServletRequest httpServletRequestParam,
		HttpServletResponse httpServletResponseParam
	) throws ServletException, IOException {
		this.webKitNotificationsBean.setSessionIdFallback(null);
		HttpSession httpSession = httpServletRequestParam.getSession(false);
		User loggedInUser = this.getLoggedInUser(httpServletRequestParam);
		JSONObject returnObject = new JSONObject();
		try {
			//User not logged in...
			if (loggedInUser == null) {
				returnObject.put(FieldMapping.ERROR, "User not logged in. Refresh browser.");
				returnObject.put(FieldMapping.ERROR_CODE, ErrorCode.USER_NOT_LOGGED_IN);
				//Invalidate...
				if (httpSession != null) httpSession.invalidate();
			} else {
				if (httpSession == null) return;

				//User Logged in...
				this.webKitNotificationsBean.setSessionIdFallback(httpSession.getId());
				boolean anyProcessed = false;

				//Check and Notify of any new User Notifications...
				anyProcessed = this.processWindowSize(httpServletRequestParam, httpSession) ? true: anyProcessed;
				anyProcessed = this.processUserNotifications(returnObject) ? true: anyProcessed;
				if (anyProcessed) {
					//Do something...
				}
			}
		} catch (JSONException jsonExcept) {
			//JSON issue...
			this.webKitNotificationsBean.raiseError(jsonExcept);
		}

		PrintWriter printWriter = httpServletResponseParam.getWriter();
		httpServletResponseParam.setContentType(APPLICATION_JSON_STRING);
		httpServletResponseParam.setHeader("Access-Control-Allow-Origin","*");
		httpServletResponseParam.setHeader("Access-Control-Allow-Methods","POST, GET");

		printWriter.write(returnObject.toString());
		printWriter.flush();
		printWriter.close();
	}

	/**
	 *
	 * @param jsonCmdObject
	 * @return
	 * @throws JSONException
	 */
	private boolean processUserNotifications(
		JSONObject jsonCmdObject
	) throws JSONException {
		switch (this.webKitNotificationsBean.getNotificationState()) {
			case UnreadNotificationsFull:
				return false;
			case UnreadNotificationsNowRead:
				this.webKitNotificationsBean.actionUpdateNotifications();
				this.addRefreshNotificationCommandObjectToArray(jsonCmdObject, TimeUnit.SECONDS.toMillis(20));
				return false;
			default:
				if ((this.webKitNotificationsBean.getLastNotificationFetch() + WAIT_TIME_NOTI_FETCH_MILLIS) < System.currentTimeMillis()) {
					int lastUnreadCount = this.webKitNotificationsBean.getNumberOfUnreadNotifications();
					int lastReadCount = this.webKitNotificationsBean.getNumberOfReadNotifications();
					this.webKitNotificationsBean.actionUpdateNotifications();
					int lastUnreadCountUpdated = this.webKitNotificationsBean.getNumberOfUnreadNotifications();
					int lastReadCountUpdated = this.webKitNotificationsBean.getNumberOfReadNotifications();

					if ((lastUnreadCount != lastUnreadCountUpdated) || lastReadCount != lastReadCountUpdated) {
						this.addRefreshNotificationCommandObjectToArray(jsonCmdObject, 0);
						/*this.webKitNotificationsBean.getLogger().debug(String.format(
							"%s: Sent update for '%s'.",
							new Date().toString(), Commands.REFRESH_USER_NOTIFICATION
						));*/
					}
				}
		}
		return false;
	}

	/**
	 *
	 * @param returnObjectParam
	 * @throws JSONException
	 */
	private void addRefreshNotificationCommandObjectToArray(
		JSONObject returnObjectParam,
		long delayMillis
	) throws JSONException {
		if (!returnObjectParam.has(FieldMapping.COMMANDS)) {
			returnObjectParam.put(FieldMapping.COMMANDS, new JSONArray());
		}
		JSONArray jsonCommandsArray = returnObjectParam.getJSONArray(FieldMapping.COMMANDS);
		JSONObject commandObject = new JSONObject();
		commandObject.put(
				FieldMapping.COMMAND,
				Commands.REFRESH_USER_NOTIFICATION);
		commandObject.put(FieldMapping.DELAY, delayMillis);

		//Add to the array...
		jsonCommandsArray.put(commandObject);
	}
	
	private boolean processWindowSize(
		HttpServletRequest httpServletRequestParam, 
		HttpSession httpSessionParam
	) {
		//Window width and height...
		String userBrowserWindWidth =
				httpServletRequestParam.getParameter(
						ABaseManagedBean.SessionVariable.USER_BROWSER_WINDOW_WIDTH);
		if (userBrowserWindWidth != null) {
			httpSessionParam.setAttribute(
					ABaseManagedBean.SessionVariable.USER_BROWSER_WINDOW_WIDTH,
					Integer.parseInt(userBrowserWindWidth));
		}

		String userBrowserWindHeight =
				httpServletRequestParam.getParameter(
						ABaseManagedBean.SessionVariable.USER_BROWSER_WINDOW_HEIGHT);
		if (userBrowserWindHeight != null) {
			httpSessionParam.setAttribute(
					ABaseManagedBean.SessionVariable.USER_BROWSER_WINDOW_HEIGHT,
					Integer.parseInt(userBrowserWindHeight));
		}

		return false;
	}
}
