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

import com.fluidbpm.fluidwebkit.backing.bean.performance.ABasePerformanceBean;
import com.fluidbpm.fluidwebkit.backing.bean.performance.PerformanceBean;
import com.fluidbpm.program.api.vo.report.userstats.PunchCardEntry;
import lombok.Getter;
import lombok.Setter;
import software.xdev.chartjs.model.charts.ScatterChart;
import software.xdev.chartjs.model.dataset.ScatterDataset;
import software.xdev.chartjs.model.datapoint.ScatterDataPoint;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RequestScoped
@Named("webKitPunchCardBean")
@Getter
@Setter
public class PunchCardBean extends ABasePerformanceBean {
	public static String DATE_FORMAT_YEAR_MONTH = "yyyy MMM";

	//CHARTS...
	//PunchCard...
	private List<PunchCardChartVO> userPunchcardCharts;

	private String earliestLogin;
	private String generalLogin;

	private String latestLogout;
	private String generalLogout;

	private int loggedInForATotalOfDays;
	private int loggedInForATotalOfHours;

	@Inject
	private PerformanceBean performanceBean;

	@PostConstruct
	public void createUserPunchCardModel() {
		this.userPunchcardCharts = new ArrayList();
		this.loggedInForATotalOfDays = 0;
		this.loggedInForATotalOfHours = 0;

		List<PunchCardEntry> punchCardEntries = this.performanceBean.getUserStatsReport().getPunchCardEntries();

		Map<String, List<PunchCardEntry>> entries = this.breakPunchCardUpMonthly(punchCardEntries);
		if (entries != null && !entries.isEmpty()) {
			boolean isFirst = true;
			for (String yearAndMonth : entries.keySet()) {
				List<PunchCardEntry> punchCardEntriesFromMap = entries.get(yearAndMonth);

				// Build ScatterChart using chartjs-java-model
				ScatterChart modelToAdd = new ScatterChart();
				// Title per month
				modelToAdd.getOptions().getPlugins().getTitle().setDisplay(true);
				modelToAdd.getOptions().getPlugins().getTitle().setText(yearAndMonth);

				// Create datasets representing login/logout moments
				ScatterDataset dsFirstLogin = new ScatterDataset();
				dsFirstLogin.setLabel("First Login");
				ScatterDataset dsSecondLastLogin = new ScatterDataset();
				dsSecondLastLogin.setLabel("Second Last Login");
				ScatterDataset dsSecondLastLogout = new ScatterDataset();
				dsSecondLastLogout.setLabel("Second Last Logout");
				ScatterDataset dsLastLogout = new ScatterDataset();
				dsLastLogout.setLabel("Last Logout");

				//Add all of the data to the datasets...
				for (PunchCardEntry entry: punchCardEntriesFromMap) {
					int date = PerformanceBean.getDayAsInt(entry.getPunchCardDay());
					double firstLogin = PerformanceBean.getHourAndMinuteAsDouble(entry.getFirstLoginForDay());
					double lastLogout = PerformanceBean.getHourAndMinuteAsDouble(entry.getLastLogout());
					double secondLastLogout = PerformanceBean.getHourAndMinuteAsDouble(entry.getSecondLastLogout());
					double secondLastLogin = PerformanceBean.getHourAndMinuteAsDouble(entry.getSecondLastLogin());

					dsFirstLogin.addData(new ScatterDataPoint((double)date, firstLogin));
					dsSecondLastLogin.addData(new ScatterDataPoint((double)date, secondLastLogin));
					dsSecondLastLogout.addData(new ScatterDataPoint((double)date, secondLastLogout));
					dsLastLogout.addData(new ScatterDataPoint((double)date, lastLogout));
				}

				// Attach datasets to the chart
				modelToAdd.getData().addDataset(dsFirstLogin);
				modelToAdd.getData().addDataset(dsSecondLastLogin);
				modelToAdd.getData().addDataset(dsSecondLastLogout);
				modelToAdd.getData().addDataset(dsLastLogout);

				// Backward-compatibility no-op
				this.setChartBasics(modelToAdd);
				this.userPunchcardCharts.add(new PunchCardChartVO(modelToAdd, yearAndMonth));
				isFirst = false;
			}

			Collections.sort(this.userPunchcardCharts, new StringToDateComp(DATE_FORMAT_YEAR_MONTH));

			//Add the Summary data...
			this.populateSummary(punchCardEntries);
		}
	}

	private void populateSummary(
		List<PunchCardEntry> punchCardEntriesParam
	) {
		if (punchCardEntriesParam == null || punchCardEntriesParam.isEmpty()) return;

		Date defaultFirstLogin = null,defaultLastLogout = null;
		for (PunchCardEntry entry:punchCardEntriesParam) {
			if (defaultFirstLogin != null && defaultLastLogout != null) break;

			defaultFirstLogin = (defaultFirstLogin == null) ?
					entry.getFirstLoginForDay(): defaultFirstLogin;

			defaultLastLogout = (defaultLastLogout == null) ?
					entry.getLastLogout(): defaultLastLogout;
		}

		if (defaultFirstLogin == null || defaultLastLogout == null) return;

		//First Login...
		Calendar calToWorkWithFirstLogin = Calendar.getInstance();
		calToWorkWithFirstLogin.setTime(defaultFirstLogin);
		int calHour = calToWorkWithFirstLogin.get(Calendar.HOUR_OF_DAY),
			calMinute = calToWorkWithFirstLogin.get(Calendar.MINUTE);

		Calendar calToWorkWithLastLogout = Calendar.getInstance();
		calToWorkWithLastLogout.setTime(defaultLastLogout);
		int calLastLogHour = calToWorkWithLastLogout.get(Calendar.HOUR_OF_DAY),
			calLastLogMinute = calToWorkWithLastLogout.get(Calendar.MINUTE);

		Calendar entryCalFirstLogin = Calendar.getInstance();
		Calendar entryCalLastLogin = Calendar.getInstance();

		int daysLoggedIn = 0, minutesLoggedIn = 0,
				//Login...
		avgHourLoginMinute = 0,avgHourLoginHour = 0,
				//Logout
		avgHourLogoutMinute = 0,avgHourLogoutHour = 0;
		for (PunchCardEntry entry : punchCardEntriesParam) {
			if (entry.isLogInsForDayEmpty()) continue;

			daysLoggedIn++;

			//The first login....
			entryCalFirstLogin.setTime(entry.getFirstLoginForDay());

			int entryHour = entryCalFirstLogin.get(Calendar.HOUR_OF_DAY),
			entryMinute = entryCalFirstLogin.get(Calendar.MINUTE);
			if ((entryHour < calHour) || (entryHour == calHour && entryMinute < calMinute)) {
				calHour = entryHour;
				calMinute = entryMinute;
			}

			avgHourLoginHour += entryHour;
			avgHourLoginMinute += entryMinute;

			//The last login....
			entryCalLastLogin.setTime(entry.getLastLogout());
			int entryLastHour = entryCalLastLogin.get(Calendar.HOUR_OF_DAY),
					entryLastMinute = entryCalLastLogin.get(Calendar.MINUTE);
			if((entryLastHour > calLastLogHour) ||
					(entryLastHour == calLastLogHour && entryLastMinute > calLastLogMinute))
			{
				calLastLogHour = entryLastHour;
				calLastLogMinute = entryLastMinute;
			}

			avgHourLogoutHour += entryLastHour;
			avgHourLogoutMinute += entryLastMinute;

			//Minutes logged in...
			minutesLoggedIn += entry.getNumberOfMinutesLoggedInForDay();
		}

		avgHourLoginHour = (avgHourLoginHour / daysLoggedIn);
		avgHourLoginMinute = (avgHourLoginMinute / daysLoggedIn);

		avgHourLogoutHour = (avgHourLogoutHour / daysLoggedIn);
		avgHourLogoutMinute = (avgHourLogoutMinute / daysLoggedIn);

		this.earliestLogin =
				("" +
						this.formatForZeros(calHour)
						 + ":" +
						this.formatForZeros(calMinute));

		this.latestLogout = ("" +
				this.formatForZeros(calLastLogHour) +
				":"+this.formatForZeros(calLastLogMinute));
		this.loggedInForATotalOfDays = daysLoggedIn;
		this.loggedInForATotalOfHours = (int)TimeUnit.MINUTES.toHours(minutesLoggedIn);

		this.generalLogin = ("" +
				this.formatForZeros(avgHourLoginHour) +
				":"+this.formatForZeros(avgHourLoginMinute));
		this.generalLogout = ("" +
				this.formatForZeros(avgHourLogoutHour) +
				":" +
				this.formatForZeros(avgHourLogoutMinute));
	}

	private String formatForZeros(int intValueParam)
	{
		return String.format("%02d", intValueParam);
	}


	private Map<String,List<PunchCardEntry>> breakPunchCardUpMonthly(
		List<PunchCardEntry> punchCardEntriesParam
	) {
		Map<String,List<PunchCardEntry>> returnVal = new HashMap<String,List<PunchCardEntry>>();
		if (punchCardEntriesParam == null || punchCardEntriesParam.isEmpty()) return returnVal;

		int currentMonth = PerformanceBean.getMonthAsInt(
				punchCardEntriesParam.get(0).getPunchCardDay());
		String yearAndMonth = PerformanceBean.getYearMonthAsString(
				punchCardEntriesParam.get(0).getPunchCardDay());

		List<PunchCardEntry> keyValues = new ArrayList<>();
		Date punchCardDayForEntry = null;
		for (PunchCardEntry entry: punchCardEntriesParam) {
			punchCardDayForEntry = entry.getPunchCardDay();
			int currentEntryDateMonth = PerformanceBean.getMonthAsInt(punchCardDayForEntry);
			if (currentMonth != currentEntryDateMonth) {
				returnVal.put(yearAndMonth, keyValues);

				//Refresh...
				yearAndMonth = PerformanceBean.getYearMonthAsString(punchCardDayForEntry);
				currentMonth = currentEntryDateMonth;
				keyValues = new ArrayList<>();
			}

			keyValues.add(entry);
		}

		if (keyValues != null && !keyValues.isEmpty()) {
			returnVal.put(PerformanceBean.getYearMonthAsString(punchCardDayForEntry), keyValues);
		}

		return returnVal;
	}

	public int getNumberOfLogins() {
		return this.performanceBean.getUserStatsReport().getNumberOfLogins();
	}

	public int getNumberOfLoginsPrevCycle() {
		return this.performanceBean.getUserStatsReport().getNumberOfLoginsPrevCycle();
	}

}
