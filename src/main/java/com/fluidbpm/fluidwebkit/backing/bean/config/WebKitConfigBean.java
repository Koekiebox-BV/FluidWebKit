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
import com.fluidbpm.program.api.vo.config.Configuration;
import com.fluidbpm.program.api.vo.config.ConfigurationListing;
import com.fluidbpm.program.api.vo.ws.auth.AppRequestToken;
import com.fluidbpm.ws.client.v1.config.ConfigurationClient;
import com.fluidbpm.ws.client.v1.user.LoginClient;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * Bean to take care of the config login.
 */
@ApplicationScoped
@Named("webKitConfigBean")
public class WebKitConfigBean extends ABaseManagedBean {
	private boolean enableAuth0;
	private boolean disableTraditionalLogin;

	private String clientId;
	private String clientSecret;
	private String domain;
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
	}

	/**
	 * User Queries used for configurations.
	 */
	public static final class ConfigUserQuery {
		public static final String ALL_CONF_ASSET_CHANNELS =
				"All Config Asset Type Channels";
		public static final String OFFICE_SETTL_CONFIG_BY =
				"Retrieve Office Settlement Config By";
	}

	/**
	 * Log a user into the Design Dashboard.
	 *
	 * @return navigation {@code dashboard}
	 */
	@PostConstruct
	public void actionPopulateInit() {
		this.configUserLoginSuccess = false;
		LoginClient loginClient = new LoginClient(this.getConfigURLFromSystemProperty());
		try {
			//App Request Token ...
			AppRequestToken appReqToken =
					loginClient.login(
							this.getConfigUserProperty(),
							this.getConfigUserPasswordProperty());
			this.bindConfigFluidClientDS(appReqToken.getServiceTicket());

			//CONFIGS...
			ConfigurationListing configurationListing =
					this.getFluidClientDSConfig().getConfigurationClient().getAllConfigurations();
			//Populate...
			this.setPropertiesBasedOnListing(configurationListing);
			this.configUserLoginSuccess = true;
		} catch (Exception fce) {
			//We have a problem...
			this.getLogger().error(fce.getMessage(),fce);
		} finally {
			loginClient.closeAndClean();
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
					if(ConfigKey.DisableTraditionalLogin.equals(configName)) {
						this.setDisableTraditionalLogin(
								Boolean.parseBoolean(configuration.getValue()));
					} else if(ConfigKey.Auth0_Enabled.equals(configName)) {
						this.setEnableAuth0(
								Boolean.parseBoolean(configuration.getValue()));
					} else if(ConfigKey.Auth0_ClientId.equals(configName)) {
						this.setClientId(configuration.getValue());
					} else if(ConfigKey.Auth0_ClientSecret.equals(configName)) {
						this.setClientSecret(configuration.getValue());
					} else if(ConfigKey.Auth0_Domain.equals(configName)) {
						this.setDomain(configuration.getValue());
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

	/**
	 *
	 * @return
	 */
	public boolean isEnableAuth0() {
		return this.enableAuth0;
	}

	/**
	 * @param enableAuth0Param
	 */
	private void setEnableAuth0(boolean enableAuth0Param) {
		this.enableAuth0 = enableAuth0Param;
	}

	/**
	 *
	 * @return
	 */
	public boolean isDisableTraditionalLogin() {
		return this.disableTraditionalLogin;
	}

	/**
	 *
	 * @param disableTraditionalLoginParam
	 */
	private void setDisableTraditionalLogin(boolean disableTraditionalLoginParam) {
		this.disableTraditionalLogin = disableTraditionalLoginParam;
	}

	/**
	 *
	 * @return
	 */
	public String getClientId() {
		return this.clientId;
	}

	/**
	 *
	 * @param clientIdParam
	 */
	private void setClientId(String clientIdParam) {
		this.clientId = clientIdParam;
	}

	/**
	 *
	 * @return
	 */
	public String getClientSecret() {
		return this.clientSecret;
	}

	/**
	 *
	 * @param clientSecretParam
	 */
	private void setClientSecret(String clientSecretParam) {
		this.clientSecret = clientSecretParam;
	}

	/**
	 *
	 * @return
	 */
	public String getDomain() {
		return this.domain;
	}

	/**
	 *
	 * @param domainParam
	 */
	private void setDomain(String domainParam) {
		this.domain = domainParam;
	}

	/**
	 *
	 * @return
	 */
	public boolean isConfigUserLoginSuccess() {
		return this.configUserLoginSuccess;
	}
}
