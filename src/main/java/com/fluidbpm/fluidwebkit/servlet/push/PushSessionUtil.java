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
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for pushing data to the web dashboard servlet.
 *
 * @author jasonbruwer on 12/18/17.
 * @since 1.3
 */
public class PushSessionUtil {

	public static final String KEY_EVENT_BUS = "event_bus";

	public static final String COMMAND = "command";

	/**
	 *
	 */
	public static class CommandField {

		public static final String VIEW_ID = "view_id";
	}

	/**
	 *
	 */
	public static class Commands {
		public static final String NONE = "none";

		//Chat...
		public static final String CHAT_USER_LOGIN_LOGOFF = "chat_user_login_logoff";
		public static final String CHAT_MESSAGE_SENT_TO_YOU = "chat_message_sent_to_you";

		//View
		public static final String REFRESH_VIEW = "refresh_view";

		//Unlock...
		public static final String ECHO_TEST_ADD_OR_LOCK_UNLOCK_3_SECONDS =
				"echo_test_add_or_lock_unlock_3_seconds";

		//Refresh User Notification...
		public static final String REFRESH_USER_NOTIFICATION = "refresh_user_notification";

		public static final String REFRESH_PERSONAL_INBOX_3_SECONDS =
				"refresh_personal_inbox_3_seconds";

		//Remove from inventory...
		public static final String ECHO_TEST_ADD_OR_LOCK_UNLOCK = "echo_test_add_or_lock_unlock";
		public static final String ECHO_TEST_REMOVE_FROM_INVENTORY = "echo_test_remove_from_inventory";
	}

	/**
	 *
	 * @param httpSessionParam
	 * @param viewIdToRefreshParam
	 */
	public void publishRefreshViewCommandToEventBus(
			HttpSession httpSessionParam,
			Long viewIdToRefreshParam)
	{
		if(httpSessionParam == null || viewIdToRefreshParam == null)
		{
			return;
		}

		Map<String,String> mapping = new HashMap<>();
		mapping.put(CommandField.VIEW_ID,String.valueOf(viewIdToRefreshParam));

		this.publishCommandToEventBus(
				httpSessionParam,
				Commands.REFRESH_VIEW,
				mapping);
	}

	/**
	 *
	 * @param httpSessionParam
	 * @param commandValueToPushParam
	 */
	public void publishCommandToEventBus(
			HttpSession httpSessionParam,
			String commandValueToPushParam) {
		this.publishCommandToEventBus(
				httpSessionParam,
				commandValueToPushParam,
				null);
	}

	/**
	 *
	 * @param httpSessionParam
	 * @param commandValueToPushParam
	 * @param additionalFieldMappingParam
	 */
	public void publishCommandToEventBus(
		HttpSession httpSessionParam,
		String commandValueToPushParam,
		Map<String,String> additionalFieldMappingParam
	) {
		if (commandValueToPushParam == null) {
			return;
		}

		JSONObject jsonObject = new JSONObject();
		jsonObject.put(COMMAND, commandValueToPushParam);

		if (additionalFieldMappingParam != null && !additionalFieldMappingParam.isEmpty()) {
			for (String key : additionalFieldMappingParam.keySet()) {
				jsonObject.put(key, additionalFieldMappingParam.get(key));
			}
		}

		//Publish the command to the event bus...
		this.publishCommandToEventBus(
				httpSessionParam,
				jsonObject);
	}

	/**
	 *
	 * @param httpSessionParam
	 * @param objectToAddToEventBusParam
	 */
	public void publishCommandToEventBus(HttpSession httpSessionParam, JSONObject objectToAddToEventBusParam) {
		if (httpSessionParam == null || objectToAddToEventBusParam == null) {
			return;
		}

		Object eventBusObj = httpSessionParam.getAttribute(
				ABaseManagedBean.SessionVariable.PUSH_SERVLET_EVENT_BUS);

		JSONObject existingObject = null;
		if (eventBusObj != null && eventBusObj instanceof String) {
			existingObject = new JSONObject((String)eventBusObj);
		}

		//New JSON Object...
		JSONObject newObject = this.addObjectToListing(
				existingObject,
				objectToAddToEventBusParam);

		httpSessionParam.setAttribute(
				ABaseManagedBean.SessionVariable.PUSH_SERVLET_EVENT_BUS,
				newObject.toString());
	}

	/**
	 * Remove all the events from the event bus.
	 *
	 * @param httpSessionParam
	 */
	public void clearEventBus(HttpSession httpSessionParam) {
		if (httpSessionParam == null) {
			return;
		}

		httpSessionParam.removeAttribute(ABaseManagedBean.SessionVariable.PUSH_SERVLET_EVENT_BUS);
	}

	/**
	 * @param httpSessionParam
	 * @param commandToFilterParam
	 * @return
	 */
	public JSONArray getAllEventsFromBusForCommand(
		HttpSession httpSessionParam,
		String commandToFilterParam
	) {
		if (httpSessionParam == null || commandToFilterParam == null) {
			return null;
		}

		JSONArray jsonArray =
				this.getAllEventsFromBus(httpSessionParam);

		if(jsonArray == null)
		{
			return null;
		}

		JSONArray returnVal = new JSONArray();
		for (int index = 0;index < jsonArray.length();index++) {
			JSONObject toCheck = jsonArray.getJSONObject(index);
			String command = toCheck.optString(COMMAND);
			if (command == null || command.trim().isEmpty()) {
				continue;
			}

			//Match..
			if (command.equals(commandToFilterParam)) {
				returnVal.put(toCheck);
			}
		}

		return returnVal;
	}

	/**
	 *
	 * @param httpSessionParam
	 * @return
	 */
	public JSONArray getAllEventsFromBus(HttpSession httpSessionParam) {
		if (httpSessionParam == null) {
			return null;
		}

		Object eventBusObj =
				httpSessionParam.getAttribute(
						ABaseManagedBean.SessionVariable.PUSH_SERVLET_EVENT_BUS);
		if(eventBusObj == null || !(eventBusObj instanceof String)) {
			return null;
		}

		JSONObject existingObject = new JSONObject((String)eventBusObj);

		return existingObject.optJSONArray(KEY_EVENT_BUS);
	}

	/**
	 *
	 * @param objectToAddToParam
	 * @param objectToAddParam
	 *
	 * @return result object.
	 */
	public JSONObject addObjectToListing(
		JSONObject objectToAddToParam,
		JSONObject objectToAddParam
	) {
		if (objectToAddParam == null) {
			return objectToAddToParam;
		}

		if (objectToAddToParam == null) {
			objectToAddToParam = new JSONObject();
		}

		JSONArray existingBus =
				objectToAddToParam.optJSONArray(KEY_EVENT_BUS);
		if (existingBus == null) {
			existingBus = new JSONArray();
		}

		existingBus.put(objectToAddParam);

		objectToAddToParam.put(KEY_EVENT_BUS, existingBus);

		return objectToAddToParam;
	}
}
