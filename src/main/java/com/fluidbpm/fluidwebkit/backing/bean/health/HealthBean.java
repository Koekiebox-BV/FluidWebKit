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

package com.fluidbpm.fluidwebkit.backing.bean.health;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.health.ConnectStatus;
import com.fluidbpm.program.api.vo.health.ExtendedServerHealth;
import com.fluidbpm.program.api.vo.health.Health;
import com.fluidbpm.ws.client.v1.health.HealthClient;
import com.google.gson.JsonObject;
import lombok.Getter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SessionScoped
@Named("webKitHealthBean")
public class HealthBean extends ABaseManagedBean {
	private static String REG_EX_SPLIT = "\\|";

	@Getter
	private ConnectStatus connectStatus;

	@Getter
	private ExtendedServerHealth extendedServerHealth;

	@PostConstruct
	public void init() {
		this.connectStatus = new ConnectStatus();
		this.extendedServerHealth = new ExtendedServerHealth();
	}

	/**
	 * Update the data.
	 */
	public void actionUpdateHealthResult() {
		if (this.getFluidClientDS() == null) return;

		HealthClient healthClient = this.getFluidClientDSConfig().getHealthClient();
		try {
			this.connectStatus = healthClient.getHealthAndServerInfo();
		} catch (Exception except) {
			this.raiseError(except);
		}

		try {
			this.extendedServerHealth = healthClient.getExtendedHealthAndServerInfo();
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	/**
	 * Update the data.
	 */
	public void actionUpdateHealthResultForBasicOnly() {
		if (this.getFluidClientDS() == null) return;

		HealthClient healthClient = this.getFluidClientDSConfig().getHealthClient();
		try {
			this.connectStatus = healthClient.getHealthAndServerInfo();
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public String calcStyleForHealth(Health health) {
		if (health == null) return UtilGlobal.EMPTY;
		switch (health) {
			case Healthy: return "background-color: #66FF66; color: black;";
			case Degraded: return "background-color: #FD8008; color: black;";
			case Unhealthy: return "background-color: #FC6666; color: black;";
			default: return UtilGlobal.EMPTY;
		}
	}

	public String getHealthCode() {
		JsonObject returnVal = new JsonObject();
		returnVal.add("connectStatus", this.connectStatus.toJsonObject());
		returnVal.add("extendedServerHealth", this.extendedServerHealth.toJsonObject());
		return returnVal.toString();
	}

	public String healthConsumptionExtract(String org, int index) {
		if (org == null) return null;
		String[] split = org.split(REG_EX_SPLIT);
		return split[index];
	}

	public List<String> orderCacheConsumptionBySizeOnlyWithCount(List<String> existing) {
		if (existing == null) return null;

		Comparator<String> consumptionComparator = (h1, h2) -> {
			String[] h1Split = h1.split(REG_EX_SPLIT);
			String[] h2Split = h2.split(REG_EX_SPLIT);

			Integer h1Int = UtilGlobal.toIntSafe(h1Split[1]);
			Integer h2Int = UtilGlobal.toIntSafe(h2Split[1]);
			return h1Int.compareTo(h2Int);
		};

		List<String> returnVal = existing.stream()
				.filter(itm -> {
					String[] split = itm.split(REG_EX_SPLIT);
					return UtilGlobal.toIntSafe(split[1]) > 0;
				})
				.sorted(consumptionComparator)
				.collect(Collectors.toList());
		Collections.reverse(returnVal);
		return returnVal;
	}
}
