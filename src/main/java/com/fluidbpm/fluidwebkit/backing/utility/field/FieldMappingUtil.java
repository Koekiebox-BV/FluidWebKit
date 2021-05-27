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
import com.fluidbpm.program.api.vo.field.MultiChoice;
import com.fluidbpm.program.api.vo.sqlutil.sqlnative.SQLColumn;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Field mapping utility for mapping
 * SQLColumn fields to Form Fields.
 *
 * @author jasonbruwer on 24/09/2019.
 * @since 1.0
 */
public class FieldMappingUtil {
	
	public static List<Field> convertToFields(
			IConvertMapping[] mappingParam,
			List<SQLColumn> sqlColumnsParam
	) {
		if (sqlColumnsParam == null) return null;

		return sqlColumnsParam.stream()
				.map(itm -> convertToField(mappingParam, itm))
				.filter(itm -> itm != null)
				.collect(Collectors.toList());
	}

	public static Field convertToField(
		IConvertMapping[] mappingParam,
		SQLColumn sqlColumnParam
	) {
		if (sqlColumnParam == null) return null;

		String fieldName = sqlColumnParam.getColumnName();
		Object objValue = sqlColumnParam.getSqlValue();
		Field.Type type = null;
		String sqlColumnName = sqlColumnParam.getColumnName();
		if (sqlColumnName != null && mappingParam != null) {
			String sqlColTrimLower = sqlColumnName.trim().toLowerCase();
			for (IConvertMapping mapping : mappingParam) {
				String sqlFieldNameIter = mapping.getSQLColumnName();
				if (sqlFieldNameIter == null) continue;

				String sqlFieldNameIterTrimLower = sqlFieldNameIter.trim().toLowerCase();
				if (!sqlColTrimLower.equals(sqlFieldNameIterTrimLower)) continue;

				fieldName = mapping.getFluidFieldName();
				type = mapping.getFluidFieldType();
			}
		}

		if (type == null) return null;

		if (objValue != null) {
			switch (type) {
				case MultipleChoice:
					objValue = new MultiChoice(objValue.toString());
			}
		}

		return new Field(fieldName, objValue, type);
	}
}
