/*
 * Koekiebox CONFIDENTIAL
 *
 * [2012] - [2020] Koekiebox (Pty) Ltd
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

package com.fluidbpm.fluidwebkit.backing.bean.config;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.config.ConfigurationListing;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

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
	private String whiteLabel;
	private String companyLogoFilepath;
	private String webKitGlobalJSON;

	private boolean configUserLoginSuccess = false;

	/**
	 * Login related configuration keys.
	 */
	public static final class ConfigKey {
		//Disable Traditional...
		public static final String DisableTraditionalLogin = "DisableTraditionalLogin";

		//Auth0 items...
		public static final String Auth0_Enabled = "Auth0_Enabled";

		public static final String Auth0_ClientId = "Auth0_ClientId";
		public static final String Auth0_ClientSecret = "Auth0_ClientSecret";
		public static final String Auth0_Domain = "Auth0_Domain";

		//Fluid URL
		public static final String FluidServerURL = "FluidServerURL";

		//Other
		public static final String WhiteLabel = "WhiteLabel";
		public static final String CompanyLogoFilePath = "CompanyLogoFilePath";
		public static final String WebKit = "WebKit";
	}

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

	private void setPropertiesBasedOnListing(ConfigurationListing propertiesBasedOnListingParam) {
		propertiesBasedOnListingParam.getListing().forEach(
				configuration -> {
					String configName = configuration.getKey();
					if (ConfigKey.DisableTraditionalLogin.equals(configName)) {
						this.setDisableTraditionalLogin(Boolean.parseBoolean(configuration.getValue()));
					} else if (ConfigKey.Auth0_Enabled.equals(configName)) {
						this.setEnableAuth0(Boolean.parseBoolean(configuration.getValue()));
					} else if (ConfigKey.Auth0_ClientId.equals(configName)) {
						this.setClientId(configuration.getValue());
					} else if (ConfigKey.Auth0_ClientSecret.equals(configName)) {
						this.setClientSecret(configuration.getValue());
					} else if (ConfigKey.Auth0_Domain.equals(configName)) {
						this.setDomain(configuration.getValue());
					} else if (ConfigKey.FluidServerURL.equals(configName)) {
						this.setFluidServerURL(configuration.getValue());
					} else if (ConfigKey.WhiteLabel.equals(configName)) {
						this.setWhiteLabel(configuration.getValue());
					} else if (ConfigKey.CompanyLogoFilePath.equals(configName)) {
						this.setCompanyLogoFilepath(configuration.getValue());
					} else if (ConfigKey.WebKit.equals(configName)) {
						this.setWebKitGlobalJSON(configuration.getValue());
					}
				}
		);
	}

	public String getDirectFromConfigHtmlContainerId()
	{
		return "loginFrm";
	}

	public String getWhiteLabelUpper() {
		return this.getWhiteLabel() == null ? null : this.getWhiteLabel().toUpperCase();
	}

	public boolean getCompanyLogoPathExists() {
		return UtilGlobal.isBlank(this.companyLogoFilepath) ?
				false : new File(this.companyLogoFilepath).exists();
	}

	public StreamedContent getCustomCompanyLogoStreamedContent() {
		File logoFile = new File(this.companyLogoFilepath);
		return DefaultStreamedContent.builder()
				.name(logoFile.getName())
				.stream(() -> {
					try {
						return new FileInputStream(logoFile);
					} catch (FileNotFoundException e) {
						this.raiseError(e);
						return null;
					}
				}).build();
	}
}
