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

package com.fluidbpm.fluidwebkit.listener;

import com.fluidbpm.fluidwebkit.ds.FluidClientPool;

import javax.inject.Inject;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Cleanup the FCP.
 */
@WebListener("Cleanup-FCP-Session")
public class CleanupFCPSessionListener implements HttpSessionListener {

	@Inject
	private FluidClientPool fcp;

	/**
	 * When the session is destroyed, we want to close all the Fluid clients.
	 *
	 * @param se Session event.
	 */
	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		this.fcp.invalidate(se.getSession().getId());
	}
}
