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
import lombok.Getter;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

@SessionScoped
@Named("webKitHealthBean")
public class HealthBean extends ABaseManagedBean {

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
		JSONObject returnVal = new JSONObject();

		returnVal.put("connectStatus", this.connectStatus.toJsonObject());
		returnVal.put("extendedServerHealth", this.extendedServerHealth.toJsonObject());

		return returnVal.toString(2);
	}
	
}
