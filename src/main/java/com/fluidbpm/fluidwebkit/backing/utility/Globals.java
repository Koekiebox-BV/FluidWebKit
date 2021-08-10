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

import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.ws.auth.AppRequestToken;
import com.fluidbpm.ws.client.v1.sqlutil.wrapper.SQLUtilWebSocketRESTWrapper;
import com.fluidbpm.ws.client.v1.user.LoginClient;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.lang.Class.forName;

/**
 *
 */
public class Globals {
	public static String CHARSET_UTF8 = "UTF-8";
	public static int WEB_SOCKET_TIMEOUT_MILLIS = 10000;
	
	public static final String NOT_SET = "[Not Set]";

	private static String getTextContentOnlyFrom(Element elementParam) {
		return elementParam.getTextContent();
	}

	public static String EXCEL_WARNING = "WARNING: ";

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

	@Deprecated
	public static SQLUtilWebSocketRESTWrapper getConfigWrapperInstance() {
		Properties existingProps = null;

		LoginClient loginClient = new LoginClient(UtilGlobal.getConfigURLFromSystemProperty(existingProps));

		try {
			AppRequestToken requestToken = loginClient.login(
					UtilGlobal.getConfigUserProperty(existingProps),
					UtilGlobal.getConfigUserPasswordProperty(existingProps));

			return new SQLUtilWebSocketRESTWrapper(
					UtilGlobal.getConfigURLFromSystemProperty(existingProps),
					requestToken.getServiceTicket(),
					TimeUnit.SECONDS.toMillis(60));
		} finally {
			loginClient.closeAndClean();
		}
	}
}
