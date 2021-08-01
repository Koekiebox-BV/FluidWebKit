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
import com.fluidbpm.program.api.vo.thirdpartylib.ThirdPartyLibraryTaskIdentifier;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.config.ConfigurationClient;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
	private String companyLogoSmallFilepath;
	private String webKitGlobalJSON;
	private String webKitPersonalInventoryJSON;
	private String googleCloudPlatformAPIKey;

	private boolean configUserLoginSuccess = false;
	
	private List<ThirdPartyLibraryTaskIdentifier> thirdPartyLibTaskIdentifiers;
	private Map<String, List<ThirdPartyLibraryTaskIdentifier>> thirdPartyLibMap;

	private Boolean isGoogleMapsApiReachable = null;

	public static final String GOOGLE_MAPS_API_SERVER = "https://maps.google.com:443";

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
		public static final String CompanyLogoFilePathSmall = "CompanyLogoFilePathSmall";
		public static final String WebKit = "WebKit";
		public static final String WebKitPersonalInventory = "WebKitPersonalInventory";
		public static final String GoogleAPIKey = "GoogleAPIKey";
	}

	@PostConstruct
	public void actionPopulateInit() {
		this.configUserLoginSuccess = false;
		try {
			this.bindConfigFluidClientDS();

			//CONFIGS...
			ConfigurationClient configClient = this.getFluidClientDSConfig().getConfigurationClient();

			ConfigurationListing configurationListing = configClient.getAllConfigurations();
			//Populate...
			this.setPropertiesBasedOnListing(configurationListing);
			this.configUserLoginSuccess = true;
			
			//Third Party Libs...
			try {
				this.thirdPartyLibTaskIdentifiers = configClient.getAllThirdPartyTaskIdentifiers();
			} catch (FluidClientException fce) {
				if (FluidClientException.ErrorCode.NO_RESULT == fce.getErrorCode()) {
					this.thirdPartyLibTaskIdentifiers = new ArrayList<>();
				} else throw fce;
			}
		} catch (Exception fce) {
			this.raiseError(fce);
		}
	}

	public Map<String, List<ThirdPartyLibraryTaskIdentifier>> getThirdPartyLibMapCustomWebActions() {
		if (this.thirdPartyLibMap != null) return this.thirdPartyLibMap;

		this.thirdPartyLibMap = new HashMap<>();

		this.thirdPartyLibTaskIdentifiers.stream()
				.filter(itm -> itm.getThirdPartyLibraryTaskType() == ThirdPartyLibraryTaskIdentifier.ThirdPartyLibraryTaskType.CustomWebAction)
				.filter(itm -> !"save".equals(itm.getTaskIdentifier().toLowerCase().trim()))
				.filter(itm -> itm.getFormDefinitions() != null && !itm.getFormDefinitions().isEmpty())
				.forEach(thirdPartyLib -> {
					thirdPartyLib.getFormDefinitions().stream()
							.map(formDef -> formDef.getFormType())
							.forEach(formDef -> {
						List<ThirdPartyLibraryTaskIdentifier> listOfActionsForFormDef = this.thirdPartyLibMap.getOrDefault(formDef, new ArrayList<>());
						listOfActionsForFormDef.add(thirdPartyLib);
						this.thirdPartyLibMap.put(formDef, listOfActionsForFormDef);
					});
				});

		return this.thirdPartyLibMap;
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
					} else if (ConfigKey.CompanyLogoFilePathSmall.equals(configName)) {
						this.setCompanyLogoSmallFilepath(configuration.getValue());
					} else if (ConfigKey.WebKit.equals(configName)) {
						this.setWebKitGlobalJSON(configuration.getValue());
					} else if (ConfigKey.WebKitPersonalInventory.equals(configName)) {
						this.setWebKitPersonalInventoryJSON(configuration.getValue());
					} else if (ConfigKey.GoogleAPIKey.equals(configName)) {
						this.setGoogleCloudPlatformAPIKey(configuration.getValue());
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
		return UtilGlobal.isBlank(this.companyLogoFilepath) ? false : new File(this.companyLogoFilepath).exists();
	}

	public boolean getCompanyLogoSmallPathExists() {
		return UtilGlobal.isBlank(this.companyLogoSmallFilepath) ? false : new File(this.companyLogoSmallFilepath).exists();
	}
	
	public String getGoogleMapsAPIJSURL() {
		String prefix = String.format("%s/maps/api/js", GOOGLE_MAPS_API_SERVER);

		//Config set and Value...
		String fullURL = prefix;
		if (UtilGlobal.isNotBlank(this.googleCloudPlatformAPIKey)) {
			fullURL = String.format("%s?key=%s", fullURL, this.googleCloudPlatformAPIKey);
		}
		return fullURL;
	}

	public boolean isGoogleMapsAPIReachable() {
		if (this.isGoogleMapsApiReachable == null) {
			this.isGoogleMapsApiReachable = this.performCallToSeeIfGoogleReachable();
		}
		return this.isGoogleMapsApiReachable;
	}
	
	private boolean performCallToSeeIfGoogleReachable() {
		HttpsURLConnection con = null;
		try {
			URL url = new URL(GOOGLE_MAPS_API_SERVER);
			con = (HttpsURLConnection)url.openConnection();

			int timeout = (int) TimeUnit.SECONDS.toMillis(5);
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent","FluidWebKit Connect Tester");
			con.connect();
			return true;
		} catch (IOException except) {
			//IO Issue...
			System.err.println("Error Checking connection (Unavailable). Link is ["+ GOOGLE_MAPS_API_SERVER+"]. "+except.getMessage());
			return false;
		} finally {
			if (con != null) con.disconnect();
		}
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

	public StreamedContent getCustomCompanyLogoSmallStreamedContent() {
		File logoFile = new File(this.companyLogoSmallFilepath);
		return DefaultStreamedContent.builder()
				.name(logoFile.getName())
				.contentType("image/svg+xml")
				.stream(() -> {
					try {
						return new FileInputStream(logoFile);
					} catch (FileNotFoundException e) {
						this.raiseError(e);
						return null;
					}
				}).build();
	}

	public String getCustomCompanyLogoSmallURL() {
		return "/get_company_logo_small";
	}
}
