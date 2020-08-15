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

package com.fluidbpm.fluidwebkit.ds;

import com.fluidbpm.fluidwebkit.exception.WebSessionExpiredException;
import com.fluidbpm.fluidwebkit.qualifier.WebKitResource;
import com.fluidbpm.program.api.util.cache.exception.FluidCacheException;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.ABaseClientWS;
import com.fluidbpm.ws.client.v1.config.ConfigurationClient;
import com.fluidbpm.ws.client.v1.sqlutil.wrapper.SQLUtilWebSocketRESTWrapper;
import com.fluidbpm.ws.client.v1.user.PersonalInventoryClient;
import com.fluidbpm.ws.client.v1.user.UserClient;
import com.google.common.cache.Cache;

import javax.inject.Inject;
import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Source for all the Fluid API's.
 */
public class FluidClientPool {
	@Inject
	@WebKitResource
	private Cache<String, FluidClientDS> fluidDS;

	/**
	 * Init communication with Fluid for session.
	 *
	 * @param uiSessionId
	 * @param serviceTicket
	 * @param endpoint
	 */
	public void init(
		String uiSessionId,
		String serviceTicket,
		String endpoint
	) {
		this.fluidDS.put(uiSessionId, new FluidClientDS(serviceTicket, endpoint));
	}

	/**
	 * @param uiSessionId
	 * @return
	 */
	public FluidClientDS get(String uiSessionId) {
		if (uiSessionId == null) {
			return null;
		}
		FluidClientDS returnVal = this.fluidDS.getIfPresent(uiSessionId);
		if (returnVal == null) {
			throw new WebSessionExpiredException(
					String.format("Session '%s' expired or not yet initialized.", uiSessionId));
		}
		return returnVal;
	}

	/**
	 * Invalidate the session.
	 * @param uiSessionId The session to invalidate.
	 */
	public void invalidate(String uiSessionId) {
		this.fluidDS.invalidate(uiSessionId);
	}
}
