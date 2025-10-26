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
import com.google.common.cache.Cache;

import jakarta.inject.Inject;

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
	 * @param uiSessionId The web session id
	 * @param serviceTicket The service ticket
	 * @param endpoint The URL endpoint
	 */
	public void init(
		String uiSessionId,
		String serviceTicket,
		String endpoint
	) {
		this.fluidDS.put(uiSessionId, new FluidClientDS(serviceTicket, endpoint));
	}

	public FluidClientDS get(String uiSessionId) {
		if (uiSessionId == null) return null;
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
