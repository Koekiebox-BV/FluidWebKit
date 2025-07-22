/*
 * Koekiebox CONFIDENTIAL
 *
 * [2012] - [2025] Koekiebox B.V.
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

package com.fluidbpm.fluidwebkit.backing.vo;

import com.fluidbpm.program.api.util.UtilGlobal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents the login session information for a given user. This class encapsulates
 * details such as the username, IP address, and login timestamp.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class LoginSessionInfoVO implements Serializable {
	private final String username;
	private final String ipAddress;
	private final Date loggedInAt;

	/**
	 * Checks if the IP address of the session has changed by comparing the given
	 * latest IP address with the current session's IP address. Returns true if
	 * the IP address is non-blank and does not match the current session's
	 * stored IP address.
	 *
	 * @param ipAddressLatest the latest IP address to compare against the current IP address
	 * @return true if the IP address has changed, false otherwise
	 */
	public boolean hasIpAddressChanged(String ipAddressLatest) {
		return !UtilGlobal.isBlank(ipAddressLatest) && !ipAddressLatest.equalsIgnoreCase(this.ipAddress);
	}
}
