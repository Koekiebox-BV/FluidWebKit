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

import lombok.Getter;

/**
 * Base exception class for client specific Dashboard.
 *
 * @author jasonbruwer on 2018-05-30
 * @since v1.0
 */
public class ClientDashboardException extends RuntimeException {
	@Getter
	private int errorCode;

	/**
	 * Error code mappings for Client Dashboard errors.
	 */
	public static class ErrorCode {
		public static final int SESSION_EXPIRED = 10001;
		public static final int IO_ERROR = 10002;
		public static final int VALIDATION = 10003;
		public static final int ILLEGAL_STATE = 10004;
		public static final int ACCESS_DENIED = 10005;
		public static final int IP_CHANGED = 10006;
	}

	public ClientDashboardException(String message, int errorCodeParam) {
		super(message);
		this.errorCode = errorCodeParam;
	}

	public ClientDashboardException(String message, Throwable cause, int errorCodeParam) {
		super(message, cause);
		this.errorCode = errorCodeParam;
	}
}
