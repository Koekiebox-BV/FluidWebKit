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

package com.fluidbpm.fluidwebkit.backing.utility.field;

import com.fluidbpm.program.api.vo.field.Field;

/**
 * TODO Description comes here.
 *
 * @author jasonbruwer on 24/09/2019.
 * @since 1.0
 */
public interface IConvertMapping {

	/**
	 *
	 * @return
	 */
	public abstract String getSQLColumnName();

	/**
	 * 
	 * @return
	 */
	public abstract String getFluidFieldName();

	/**
	 * 
	 * @return
	 */
	public abstract Field.Type getFluidFieldType();
}
