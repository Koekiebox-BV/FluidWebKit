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
public class MandatoryFieldsException extends ClientDashboardException {

	public MandatoryFieldsException(String message) {
		super(message, ClientDashboardException.ErrorCode.VALIDATION);
	}

	public MandatoryFieldsException(String message, Throwable cause, int errorCodeParam) {
		super(message, cause, ClientDashboardException.ErrorCode.VALIDATION);
	}
}
