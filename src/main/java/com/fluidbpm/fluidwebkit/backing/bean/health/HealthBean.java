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
import com.fluidbpm.program.api.vo.health.ConnectStatus;
import com.fluidbpm.ws.client.v1.health.HealthClient;
import lombok.Getter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

@SessionScoped
@Named("webKitHealthBean")
public class HealthBean extends ABaseManagedBean {

	@Getter
	private ConnectStatus connectStatus;

	@PostConstruct
	public void init() {
		this.actionUpdateHealthResult();
	}

	/**
	 * Update the data.
	 */
	public void actionUpdateHealthResult() {
		this.connectStatus = new ConnectStatus();

		if (this.getFluidClientDS() == null) return;

		HealthClient healthClient = this.getFluidClientDSConfig().getHealthClient();
		try {
			this.connectStatus = healthClient.getHealthAndServerInfo();
		} catch (Exception except) {
			this.raiseError(except);
		}
	}
	
}
