/*----------------------------------------------------------------------------*
 *
 * Copyright (c) 2018 by Montant Limited. All rights reserved.
 *
 * This software is the confidential and proprietary property of
 * Montant Limited and may not be disclosed, copied or distributed
 * in any form without the express written permission of Montant Limited.
 *----------------------------------------------------------------------------*/

package com.fluidbpm.fluidwebkit.backing.bean.config;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.program.api.vo.config.ConfigurationListing;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * Bean to take care of the config login.
 */
@ApplicationScoped
@Named("webKitConfigBean")
@Getter
@Setter
public class WebKitConfigBean extends ABaseManagedBean {
	private boolean enableAuth0;
	private boolean disableTraditionalLogin;
	private String clientId;
	private String clientSecret;
	private String domain;
	private String fluidServerURL;

	private boolean configUserLoginSuccess = false;

	/**
	 * Login related configuration keys.
	 */
	private static final class ConfigKey {
		//Disable Traditional...
		public static final String DisableTraditionalLogin = "DisableTraditionalLogin";

		//Auth0 items...
		public static final String Auth0_Enabled = "Auth0_Enabled";

		public static final String Auth0_ClientId = "Auth0_ClientId";
		public static final String Auth0_ClientSecret = "Auth0_ClientSecret";
		public static final String Auth0_Domain = "Auth0_Domain";

		//Fluid URL
		public static final String FluidServerURL = "System Server URL";
	}

	/**
	 * Log a user into the Design Dashboard.
	 *
	 * @return navigation {@code dashboard}
	 */
	@PostConstruct
	public void actionPopulateInit() {
		this.configUserLoginSuccess = false;
		try {
			this.bindConfigFluidClientDS();

			//CONFIGS...
			ConfigurationListing configurationListing =
					this.getFluidClientDSConfig().getConfigurationClient().getAllConfigurations();
			//Populate...
			this.setPropertiesBasedOnListing(configurationListing);
			this.configUserLoginSuccess = true;
		} catch (Exception fce) {
			this.raiseError(fce);
		}
	}

	/**
	 *
	 * @param propertiesBasedOnListingParam
	 */
	private void setPropertiesBasedOnListing(ConfigurationListing propertiesBasedOnListingParam) {
		propertiesBasedOnListingParam.getListing().forEach(
				configuration -> {
					String configName = configuration.getKey();
					if (ConfigKey.DisableTraditionalLogin.equals(configName)) {
						this.setDisableTraditionalLogin(
								Boolean.parseBoolean(configuration.getValue()));
					} else if (ConfigKey.Auth0_Enabled.equals(configName)) {
						this.setEnableAuth0(
								Boolean.parseBoolean(configuration.getValue()));
					} else if (ConfigKey.Auth0_ClientId.equals(configName)) {
						this.setClientId(configuration.getValue());
					} else if (ConfigKey.Auth0_ClientSecret.equals(configName)) {
						this.setClientSecret(configuration.getValue());
					} else if (ConfigKey.Auth0_Domain.equals(configName)) {
						this.setDomain(configuration.getValue());
					} else if (ConfigKey.FluidServerURL.equals(configName)) {
						this.setFluidServerURL(configuration.getValue());
					}
				}
		);
	}

	/**
	 *
	 * @return
	 */
	public String getDirectFromConfigHtmlContainerId()
	{
		return "loginFrm";
	}
}
