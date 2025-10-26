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
import com.fluidbpm.program.api.vo.report.system.SystemUptimeReport;
import com.fluidbpm.program.api.vo.report.userstats.UserStatsReport;
import com.fluidbpm.ws.client.v1.report.ReportSystemClient;
import com.fluidbpm.ws.client.v1.report.ReportUserClient;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;

import jakarta.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;

@SessionScoped
@Named("webKitPerformanceBean")
public class PerformanceBean extends ABaseManagedBean {

	@Getter
	private UserStatsReport userStatsReport;

	@Getter
	private SystemUptimeReport systemUptimeReport;

	@PostConstruct
	public void init() {
		this.actionUpdatePerformanceResult();
	}

	/**
	 * Update the data.
	 */
	public void actionUpdatePerformanceResult() {
		this.userStatsReport = new UserStatsReport();
		this.systemUptimeReport = new SystemUptimeReport();

		if (this.getFluidClientDS() == null) return;

		//Fluid Clients...
		ReportUserClient userClient = this.getFluidClientDS().getReportUserClient();
		ReportSystemClient systemClient = this.getFluidClientDS().getReportSystemClient();
		try {
			this.userStatsReport = userClient.getLoggedInUserStats();
		} catch (Exception except) {
			this.raiseError(except);
		}

		try {
			this.systemUptimeReport = systemClient.getSystemReport(true, null);
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	//TODO These are from VO's...
	public static String INT_DATE_DAY_FORMAT = "dd";
	public static int getDayAsInt(Date dateParam) {
		if (dateParam == null) {
			return 0;
		}

		String formatted = new SimpleDateFormat(
				INT_DATE_DAY_FORMAT).format(dateParam);
		return Integer.parseInt(formatted);
	}

	public static String DOUBLE_DATE_HOUR = "kk.mm";
	public static double getHourAndMinuteAsDouble(Date dateParam) {
		if (dateParam == null) {
			return 0.0;
		}

		String formatted = new SimpleDateFormat(DOUBLE_DATE_HOUR).format(dateParam);
		return Double.parseDouble(formatted);
	}
	
	public static String INT_DATE_MONTH_FORMAT = "MM";
	public static int getMonthAsInt(Date dateParam) {
		if (dateParam == null) {
			return 0;
		}

		String formatted = new SimpleDateFormat(INT_DATE_MONTH_FORMAT).format(dateParam);
		return Integer.parseInt(formatted);
	}

	public static String DATE_FORMAT_YEAR_MONTH = "yyyy MMM";
	public static String getYearMonthAsString(Date dateParam) {
		if (dateParam == null) return null;

		return new SimpleDateFormat(DATE_FORMAT_YEAR_MONTH).format(dateParam);
	}
}
