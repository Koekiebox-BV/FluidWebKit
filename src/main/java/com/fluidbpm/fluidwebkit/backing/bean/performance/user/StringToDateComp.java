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

package com.fluidbpm.fluidwebkit.backing.bean.performance.user;

import com.fluidbpm.program.api.util.UtilGlobal;

import java.util.Comparator;
import java.util.Date;

/**
 * Created by jasonbruwer on 2015/11/27.
 */
public class StringToDateComp implements Comparator<PunchCardChartVO> {

	private String dateFormat = null;


	public StringToDateComp(String dateFormatParam) {
		this.dateFormat = dateFormatParam;
	}


	@Override
	public int compare(PunchCardChartVO objectOneParam, PunchCardChartVO objectTwoParam) {
		if (objectOneParam == null || objectTwoParam == null) return 0;

		Date objOneDate = this.toDate(objectOneParam.getYearMonthLabel());
		Date objTwoDate = this.toDate(objectTwoParam.getYearMonthLabel());

		if (objOneDate == null || objTwoDate == null) return 0;

		return objOneDate.compareTo(objTwoDate);
	}

	private Date toDate(String stringValParam) {
		return UtilGlobal.fromStringToDateSafe(stringValParam , this.dateFormat);
	}
}
