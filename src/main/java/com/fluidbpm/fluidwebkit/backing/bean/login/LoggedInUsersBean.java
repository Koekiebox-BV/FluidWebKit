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

package com.fluidbpm.fluidwebkit.backing.bean.login;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.utility.Globals;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.fluidwebkit.qualifier.cache.LoggedInUsersCache;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.google.common.cache.Cache;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;


@ApplicationScoped
@Named("webKitLoggedInUsersBean")
public class LoggedInUsersBean extends ABaseManagedBean {
	@Inject
	@LoggedInUsersCache
	private Cache<String, String> loggedInUsers;

	public LoggedInUsersBean() {
		super();
	}

	/**
	 * Clears all entries of logged-in users from the cache.
	 * This method invalidates the entire cache of logged-in users, effectively removing
	 * all stored username and IP address pairs, thereby resetting the logged-in state.
	 */
	public void clearLoggedInUsers() {
		this.loggedInUsers.invalidateAll();
	}

	/**
	 * Logs out the specified user by invalidating their entry in the logged-in users cache.
	 * @param username the username of the user to log out; must not be null, blank, or empty.
	 */
	public void logoutUser(String username) {
		this.loggedInUsers.invalidate(username);
	}

	/**
	 * Retrieves a map of currently logged-in users and their associated client IP addresses.
	 *
	 * @return a {@code Map<String, String>} where the key represents the username of a logged-in user
	 *         and the value represents their corresponding client IP address.
	 */
	public Map<String, String> getLoggedInUsers() {
		return this.loggedInUsers.asMap();
	}

	/**
	 * Authenticates and registers a user as logged in based on their username and client IP address.
	 * If the user is already logged in from a different IP address, an exception is thrown.
	 *
	 * @param username the username of the user attempting to log in;
	 *                 must not be null, blank, or empty, and is case-insensitively normalized.
	 * @throws ClientDashboardException if the user is already logged in from a different IP address.
	 */
	public void loginUser(String username) {
		if (UtilGlobal.isBlank(username)) return;

		FacesContext facesContext = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
		String clientIp = this.getClientIpAddress(request);

		username = username.trim().toLowerCase();

		String existingClientIp = this.loggedInUsers.getIfPresent(username);
		if (Globals.isLoginIPLocked() &&
				(UtilGlobal.isNotBlank(existingClientIp) && !existingClientIp.equalsIgnoreCase(clientIp))) {
			throw new ClientDashboardException(
					String.format("Not allowed, previously logged in from '%s'. Now '%s'.",existingClientIp, clientIp),
					ClientDashboardException.ErrorCode.IP_CHANGED
			);
		}

		this.loggedInUsers.put(username, clientIp);
	}

	/**
	 * Retrieves the client's IP address from the incoming HTTP request.
	 * The method first checks for the presence of the "X-Forwarded-For" header,
	 * which is typically used when the request is routed through a proxy or load balancer.
	 * If the header is not present or contains an invalid value, the method falls back to
	 * the remote address directly obtained from the request.
	 *
	 * @param request the {@code HttpServletRequest} object representing the client request.
	 *                Must not be null.
	 * @return the client's IP address as a {@code String}. If no valid IP address is found,
	 *         the method returns the remote address from the request.
	 */
	private String getClientIpAddress(HttpServletRequest request) {
		String ipAddress = request.getHeader("X-Forwarded-For");
		if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getRemoteAddr();
		}
		return ipAddress;
	}

}
