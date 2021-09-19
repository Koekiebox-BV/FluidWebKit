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

package com.fluidbpm.fluidwebkit.backing.bean.performance.system;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.performance.PerformanceBean;
import com.fluidbpm.program.api.vo.report.system.SystemUpHourMin;
import com.fluidbpm.program.api.vo.report.system.SystemUptimeReport;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RequestScoped
@Named("webKitSystemUptimeBean")
public class SystemUptimeBean extends ABaseManagedBean {

	@Inject
	private PerformanceBean performanceBean;

	/**
	 * Return a mapping of downtime. If value for day is {@code true}, there was downtime.
	 * @return Map
	 */
	public Map<String, Integer> getDowntimeMapping() {
		SystemUptimeReport sysUptimeReport = this.performanceBean.getSystemUptimeReport();
		Map<String, Integer> returnVal = new HashMap<>();
		if (sysUptimeReport.getUptimeEntries() != null) {
			sysUptimeReport.getUptimeEntries().forEach(itmYearDay -> {
				AtomicInteger downtimeMinsForDay = new AtomicInteger(0);
				if (itmYearDay.getSystemUpHourMins() != null && !itmYearDay.getSystemUpHourMins().isEmpty()) {
					itmYearDay.getSystemUpHourMins().forEach(hourMinItm -> {
						if (hourMinItm.getState() == SystemUpHourMin.State.Down) {
							downtimeMinsForDay.incrementAndGet();//Add 1 minute for downtime...
							return;
						}
					});
				}
				returnVal.put(itmYearDay.getYearDayKey(), downtimeMinsForDay.get());
			});
		}
		return returnVal;
	}

	public List<Integer> getDowntimeMappingValues() {
		Map<String, Integer> downtimeMapping = this.getDowntimeMapping();
		List<Integer> returnVal = new ArrayList<>();
		downtimeMapping.keySet().stream()
				.sorted(Comparator.comparing(String::toString))
				.forEach(yearDayItm -> {
					returnVal.add(downtimeMapping.get(yearDayItm));
				});
		Collections.reverse(returnVal);//Last day at the end...
		return returnVal;
	}

	public List<String> getDowntimeMappingKeys() {
		Map<String, Integer> downtimeMapping = this.getDowntimeMapping();
		List<String> returnVal = new ArrayList<>();
		return downtimeMapping.keySet().stream()
				.sorted(Comparator.comparing(String::toString))
				.collect(Collectors.toList());
	}

	public int getDowntimeMappingValuesSize() {
		return this.getDowntimeMappingValues() == null ? 0 :
				this.getDowntimeMappingValues().size();
	}

	public int getNumberOfDowntimeMinutes() {
		SystemUptimeReport sysUptimeReport = this.performanceBean.getSystemUptimeReport();
		AtomicInteger returnVal = new AtomicInteger(0);
		if (sysUptimeReport.getUptimeEntries() != null) {
			sysUptimeReport.getUptimeEntries().forEach(itmYearDay -> {
				if (itmYearDay.getSystemUpHourMins() != null && !itmYearDay.getSystemUpHourMins().isEmpty()) {
					itmYearDay.getSystemUpHourMins().forEach(hourMinItm -> {
						if (hourMinItm.getState() == SystemUpHourMin.State.Down) {
							returnVal.incrementAndGet();
							return;
						}
					});
				}
			});
		}
		return returnVal.get();
	}

	public String getNumberOfDowntimeLabel() {
		int downtimeTotal = this.getNumberOfDowntimeMinutes();
		if (downtimeTotal > 480) {
			int downtimeHours = (downtimeTotal / 60);
			if (downtimeHours > 48) {
				return String.format("%d days", (downtimeHours / 24));
			}
			return String.format("%d hours", downtimeHours);
		}
		return String.format("%d minutes", downtimeTotal);
	}
}
