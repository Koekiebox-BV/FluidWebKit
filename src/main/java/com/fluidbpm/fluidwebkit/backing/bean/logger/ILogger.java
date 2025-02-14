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

package com.fluidbpm.fluidwebkit.backing.bean.logger;

import java.io.Serializable;

/**
 * Interface for logging implementation.
 *
 * @author jasonbruwer on 08/03/2020.
 * @since 1.0
 */
public interface ILogger extends Serializable {
	/**
	 * Log data with trace log level.
	 *
	 * @param details Message to log.
	 * @param args Additional arguments in case of template.
	 */
	void trace(String details, Object ... args);
	
	/**
	 * Log data with debug log level.
	 *
	 * @param details Message to log.
	 * @param args Additional arguments in case of template.
	 */
	void debug(String details, Object ... args);

	/**
	 * Log data with info log level.
	 *
	 * @param details Message to log.
	 * @param args Additional arguments in case of template.
	 */
	void info(String details, Object ... args);

	/**
	 * Log data with warn log level.
	 *
	 * @param details Message to log.
	 * @param args Additional arguments in case of template.
	 */
	void warn(String details, Object ... args);

	/**
	 * Log data with error log level.
	 *
	 * @param details Message to log.
	 * @param exception Error to log.
	 */
	void error(String details, Exception exception);

	
}
