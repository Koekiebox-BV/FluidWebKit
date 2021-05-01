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

package com.fluidbpm.fluidwebkit.exception;

/**
 * @author jasonbruwer Date: 2018-05-31
 * @since v1.0
 */
public class WebSessionExpiredException extends ClientDashboardException {

	/**
	 * Constructs a new runtime exception with the specified detail message. The
	 * cause is not initialized, and may subsequently be initialized by a call
	 * to {@link #initCause}.
	 *
	 * @param message the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 */
	public WebSessionExpiredException(String message) {
		super(message, ErrorCode.SESSION_EXPIRED);
	}

	public WebSessionExpiredException(String message, Throwable cause) {
		super(message, cause, ErrorCode.SESSION_EXPIRED);
	}

}
