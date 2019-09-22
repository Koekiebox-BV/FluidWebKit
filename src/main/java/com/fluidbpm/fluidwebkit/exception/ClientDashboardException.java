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
 * Base exception class for client specific Dashboard.
 *
 * @author jasonbruwer on 2018-05-30
 * @since v1.0
 */
public class ClientDashboardException extends RuntimeException {
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
	}

	/**
	 * Constructs a new runtime exception with the specified detail message. The
	 * cause is not initialized, and may subsequently be initialized by a call
	 * to {@link #initCause}.
	 *
	 * @param message the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 */
	public ClientDashboardException(String message, int errorCodeParam) {
		super(message);
		this.errorCode = errorCodeParam;
	}

	/**
	 * Constructs a new runtime exception with the specified detail message and
	 * cause. <p> Note that the detail message associated with {@code cause} is
	 * <i>not</i> automatically incorporated in this runtime exception's detail
	 * message.
	 *
	 * @param message the detail message (which is saved for later retrieval by
	 *            the {@link #getMessage()} method).
	 * @param cause the cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A <tt>null</tt> value is
	 *            permitted, and indicates that the cause is nonexistent or
	 *            unknown.)
	 *
	 * @since 1.4
	 */
	public ClientDashboardException(String message, Throwable cause, int errorCodeParam) {
		super(message, cause);
		this.errorCode = errorCodeParam;
	}

	/**
	 *
	 * @return
	 */
	public int getErrorCode() {
		return this.errorCode;
	}
}
