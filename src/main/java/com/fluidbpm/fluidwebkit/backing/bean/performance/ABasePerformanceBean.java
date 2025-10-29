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

public abstract class ABasePerformanceBean extends ABaseManagedBean {

	/**
	 * Backward-compatibility no-op for legacy chart setup. New PrimeFaces charts
	 * configure colors via datasets/options, so this method intentionally does nothing.
	 * Kept to avoid breaking existing callers during the migration.
	 *
	 * @param chartModelParam The chart model (ignored).
	 */
	protected void setChartBasics(Object chartModelParam) {
		// No-op with newer PrimeFaces Charts (ChartJS). Colors are set per-dataset.
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
