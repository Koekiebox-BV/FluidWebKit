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

package com.fluidbpm.fluidwebkit.backing.bean.performance;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import org.primefaces.model.chart.ChartModel;

public abstract class ABasePerformanceBean extends ABaseManagedBean {

	/**
	 * @param chartModelParam The chart model.
	 */
	protected void setChartBasics(ChartModel chartModelParam) {
		if (chartModelParam == null) return;
		
		chartModelParam.setSeriesColors("1399F1,E41751,3B9F3F,FDB309,864CD2,FD8B47,A8CC53,58629F,149C9C,237CDE,EB953C");
	}

	protected int getFloatAsPercentage(float floatParam) {
		if (floatParam >= 1.0) {
			return 100;
		}

		String toString = Float.toString(floatParam).substring(2);
		if (toString.length() == 1) {
			return toIntSafe((toString).concat("0"));
		}

		return toIntSafe(toString.substring(0,2));
	}

	/**
	 * Returns -1 if there is a problem with conversion.
	 *
	 * @param toParseParam The txt to parse.
	 *
	 * @return -1 or int value of {@code toParseParam}
	 */
	public int toIntSafe(String toParseParam) {
		if (toParseParam == null || toParseParam.trim().isEmpty()) {
			return -1;
		}
		try {
			return Double.valueOf(toParseParam).intValue();
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
