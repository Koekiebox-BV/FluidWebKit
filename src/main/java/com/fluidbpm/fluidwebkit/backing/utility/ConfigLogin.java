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
import com.fluidbpm.ws.client.v1.user.LoginClient;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.fluidbpm.program.api.util.UtilGlobal.sysOut;

/**
 * Utility class for logging in 
 *
 * @author jasonbruwer on 2019-01-17.
 * @since 1.0
 */
@Deprecated
public class ConfigLogin {
	public static String FLUID_WS_URL = "http://localhost:80/fluid-ws/";
	public static String FLUID_CONFIG_USER = "admin";
	public static String FLUID_CONFIG_USER_PASSWORD = "12345";

	private static Long LAST_TIME_CONF_LOGGED_IN = 0L;
	private static AppRequestToken CONFIG_USER_APP_REQ_TOKEN = null;
	private static int LOGIN_DURATION_HOURS = 4;

	/**
	 * Log in the config user.
	 * If the config user is already logged in, the existing session will be used.
	 *
	 * @return AppRequestToken
	 */
	public AppRequestToken loginConfigUser() {
		Properties existingProps = null;
		long now = System.currentTimeMillis();
		if ((LAST_TIME_CONF_LOGGED_IN + TimeUnit.HOURS.toMillis(LOGIN_DURATION_HOURS)) < now) {
			synchronized (LAST_TIME_CONF_LOGGED_IN) {
				LoginClient loginClient = new LoginClient(UtilGlobal.getConfigURLFromSystemProperty(existingProps));
				try {
					CONFIG_USER_APP_REQ_TOKEN = loginClient.login(
							UtilGlobal.getConfigUserProperty(existingProps),
							UtilGlobal.getConfigUserPasswordProperty(existingProps));
					LAST_TIME_CONF_LOGGED_IN = System.currentTimeMillis();
					sysOut("\n\nConfig User Logged in!!!\n\n");
				} catch (Exception except){
					CONFIG_USER_APP_REQ_TOKEN = null;
					System.err.println(except.getMessage());
					except.printStackTrace();
					throw new IllegalStateException(except.getMessage(), except);
				} finally {
					loginClient.closeAndClean();
				}
			}
		}

		if (CONFIG_USER_APP_REQ_TOKEN == null) {
			throw new IllegalStateException(
					String.format(
						"User '%s' was never logged in to '%s'.",
						UtilGlobal.getConfigUserProperty(existingProps),
						UtilGlobal.getConfigURLFromSystemProperty(existingProps)
					));
		}

		return CONFIG_USER_APP_REQ_TOKEN;
	}

}
