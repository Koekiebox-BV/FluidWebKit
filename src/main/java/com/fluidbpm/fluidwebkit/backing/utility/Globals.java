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

import com.fluidbpm.program.api.vo.ws.auth.AppRequestToken;
import com.fluidbpm.ws.client.v1.sqlutil.wrapper.SQLUtilWebSocketRESTWrapper;
import com.fluidbpm.ws.client.v1.user.LoginClient;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static java.lang.Class.forName;

/**
 * Global accessible variables.
 *
 * @author jasonbruwer on 4/14/18.
 * @since 1.0
 */
public class Globals {
	public static String CHARSET_UTF8 = "UTF-8";
	public static int WEB_SOCKET_TIMEOUT_MILLIS = 10000;
	
	public static final String NOT_SET = "[Not Set]";

	public static String FLUID_WS_URL = "http://localhost:80/fluid-ws/";
	public static String FLUID_CONFIG_USER = "admin";
	public static String FLUID_CONFIG_USER_PASSWORD = "12345";

	/**
	 *
	 * @param elementParam
	 * @return
	 */
	private static String getTextContentOnlyFrom(Element elementParam) {
		return elementParam.getTextContent();
	}

	public static String EXCEL_WARNING = "WARNING: ";


	/**
	 * Create and return a new instance of the SQLUtil wrapper.
	 * 
	 * @return new instance of {@code SQLUtilWebSocketRESTWrapper}
	 */
	public static SQLUtilWebSocketRESTWrapper getConfigWrapperInstance() {
		LoginClient loginClient = new LoginClient(Globals.getConfigURLFromSystemProperty());

		try {
			AppRequestToken requestToken = loginClient.login(
					Globals.getConfigUserProperty(), Globals.getConfigUserPasswordProperty());

			return new SQLUtilWebSocketRESTWrapper(
					Globals.getConfigURLFromSystemProperty(),
					requestToken.getServiceTicket(),
					TimeUnit.SECONDS.toMillis(60));
		} finally {
			loginClient.closeAndClean();
		}
	}

	/**
	 *
	 * @return
	 */
	public static String getConfigURLFromSystemProperty() {
		return System.getProperty(
				"FLUID_WS_URL",
				FLUID_WS_URL);
	}

	/**
	 *
	 * @return
	 */
	public static String getConfigUserProperty() {
		return System.getProperty(
				"CONFIG_USER",
				FLUID_CONFIG_USER);
	}

	/**
	 *
	 * @return
	 */
	public static String getConfigUserPasswordProperty() {
		return System.getProperty(
				"CONFIG_USER_PASSWORD",
				FLUID_CONFIG_USER_PASSWORD);
	}

	/**
	 * @return
	 */
	public static boolean isConfigBasicAuthFromSystemProperty() {
		String useBasicAuth = System.getProperty(
				"USE_BASIC_AUTH",
				"false");
		try {
			return Boolean.parseBoolean(useBasicAuth);
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

	private static String JS_USER_TRACKING_FR = null;

	/**
	 * Retrieve the FusionReactor script for tracking user experience.
	 * @return JS for user tracking
	 */
	public static String getFRUserTrackingScript() {
		if (JS_USER_TRACKING_FR != null) {
			return JS_USER_TRACKING_FR;
		}

		try {
			Class clazz = forName("com.intergral.fusionreactor.api.FRAPI");
			Method methodGetInst = clazz.getMethod("getInstance");
			Method methodGetScript = clazz.getMethod("getUemTrackingScript");
			Object frAPIInstance = methodGetInst.invoke(null, new Object[]{});
			Object respObj = methodGetScript.invoke(frAPIInstance, new Object[]{});

			String toString = respObj.toString();
			JS_USER_TRACKING_FR = toString;
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException eParam) {
			JS_USER_TRACKING_FR = "";
		}
		return JS_USER_TRACKING_FR;
	}
}
