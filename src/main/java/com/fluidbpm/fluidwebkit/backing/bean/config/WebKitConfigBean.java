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
import com.fluidbpm.fluidwebkit.exception.WebSessionExpiredException;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.config.ConfigurationListing;
import com.fluidbpm.program.api.vo.role.Role;
import com.fluidbpm.program.api.vo.sqlutil.sqlnative.NativeSQLQuery;
import com.fluidbpm.program.api.vo.sqlutil.sqlnative.SQLColumn;
import com.fluidbpm.program.api.vo.sqlutil.sqlnative.SQLResultSet;
import com.fluidbpm.program.api.vo.sqlutil.sqlnative.SQLRow;
import com.fluidbpm.program.api.vo.thirdpartylib.ThirdPartyLibraryTaskIdentifier;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.config.ConfigurationClient;
import com.fluidbpm.ws.client.v1.sqlutil.wrapper.SQLUtilWebSocketRESTWrapper;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bean to take care of the config login.
 */
@SessionScoped
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
	private String titleLookupIncludedFormTypes;
	private String raygunAPIKey;
	private String googleAPIKey;

	private boolean configUserLoginSuccess = false;
	
	private List<ThirdPartyLibraryTaskIdentifier> thirdPartyLibTaskIdentifiers;
	private Map<String, List<ThirdPartyLibraryTaskIdentifier>> thirdPartyLibMap;

	private Boolean isGoogleMapsApiReachable = null;

	@Inject
	private PeriodicUpdateBean periodicUpdateBean;

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
		public static final String TitleLookupIncludedFormTypes = "TitleLookupIncludedFormTypes";

		//RayGun API Key
		public static final String RaygunAPIKey = "Raygun_APIKey";
	}

	@PostConstruct
	public void actionPopulateInit() {
		this.configUserLoginSuccess = false;
		try {
			this.bindConfigFluidClientDS();

			//CONFIGS...
			ConfigurationClient configClient = this.getFluidClientDSConfig().getConfigurationClient();

			ConfigurationListing configurationListing = this.periodicUpdateBean.getConfigurationListing();
			if (configurationListing == null || configurationListing.isListingEmpty()) {
				configurationListing = configClient.getAllConfigurations();
				this.periodicUpdateBean.setConfigurationListing(configurationListing);
			}

			//Populate...
			this.setPropertiesBasedOnListing(configurationListing);
			this.configUserLoginSuccess = true;
			
			//Third Party Libs...
			try {
				this.thirdPartyLibTaskIdentifiers = this.periodicUpdateBean.getThirdPartyLibTaskIdentifiers();
				if (this.thirdPartyLibTaskIdentifiers == null) {
					this.thirdPartyLibTaskIdentifiers = configClient.getAllThirdPartyTaskIdentifiers();
					this.periodicUpdateBean.setThirdPartyLibTaskIdentifiers(this.thirdPartyLibTaskIdentifiers);
				}
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
					} else if (ConfigKey.TitleLookupIncludedFormTypes.equals(configName)) {
						this.setTitleLookupIncludedFormTypes(configuration.getValue());
					} else if (ConfigKey.RaygunAPIKey.equals(configName)) {
						this.setRaygunAPIKey(configuration.getValue());
					} else if (ConfigKey.GoogleAPIKey.equals(configName)) {
						this.setGoogleAPIKey(configuration.getValue());
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
			this.getLogger().info(String.format("ConfirmingGoogleAPIReachableAt: %s", GOOGLE_MAPS_API_SERVER));
			this.isGoogleMapsApiReachable = this.performCallToSeeIfGoogleReachable();
			this.getLogger().info(String.format("Is GoogleAPI Reachable?: %s", this.isGoogleMapsApiReachable));
		}
		return this.isGoogleMapsApiReachable;
	}
	
	private boolean performCallToSeeIfGoogleReachable() {
		if (UtilGlobal.isBlank(this.googleAPIKey)) return false;

		URLConnection con = null;
		try {
			URL url = new URL(GOOGLE_MAPS_API_SERVER);
			con = url.openConnection();
			if (con instanceof HttpsURLConnection) {
				HttpsURLConnection casted = (HttpsURLConnection)con;
				casted.setRequestMethod("GET");
			} else {
				this.getLogger().error(String.format("Connection is of type [%s]%n. Please confirm supported.", con.getClass().getName()), null);
			}

			int timeout = (int) TimeUnit.SECONDS.toMillis(5);
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
			con.setRequestProperty("User-Agent","FluidWebKit Connect Tester");
			con.connect();
			return true;
		} catch (IOException except) {
			this.getLogger().error("Error Checking connection (Unavailable). Link is ["+ GOOGLE_MAPS_API_SERVER+"]. "+except.getMessage(), except);
			return false;
		} finally {
			if (con != null && con instanceof HttpsURLConnection) {
				HttpsURLConnection casted = (HttpsURLConnection)con;
				casted.disconnect();
			}
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

	public boolean getEnableSOMPostCardFeatures() {
		try {
			String label = this.getWhiteLabel();
			if (label == null || !(label.trim().toLowerCase().contains("som"))) return false;

			User loggedInUsr = this.getLoggedInUserSafe();
			if (User.ADMIN_USERNAME.equals(loggedInUsr.getUsername())) return true;

			List<Role> loggedInUsrRoles = loggedInUsr.getRoles();
			if (loggedInUsrRoles == null || loggedInUsrRoles.isEmpty()) return false;

			return true;
		} catch (WebSessionExpiredException err) {
			return false;
		}
	}

	public Currency getIssuerVelocityCurrency(SQLUtilWebSocketRESTWrapper wrapper, String issuer) {
		String ds = UtilGlobal.getProperty(new Properties(), "PostCardDSName", "postcard");
		NativeSQLQuery getSettlementRules = new NativeSQLQuery();
		getSettlementRules.setDatasourceName(ds);
		getSettlementRules.setQuery("SELECT i.velocity_currency_code FROM pc_issuers i WITH(NOLOCK) WHERE i.issuer_name = ?");

		List<SQLColumn> inputs = new ArrayList<>();
		inputs.add(new SQLColumn(null, 1, Types.VARCHAR, issuer));
		getSettlementRules.setSqlInputs(inputs);

		List<SQLResultSet> settlementRulesResultSet = wrapper.executeNativeSQL(getSettlementRules);
		for (SQLResultSet resultSet : settlementRulesResultSet) {
			if (resultSet.isListingEmpty()) continue;

			for (SQLRow row : resultSet.getListing()) {
				SQLColumn col_1 = row.getSqlColumns().get(0);
				Object objSqlVal = col_1.getSqlValue();
				if (objSqlVal == null) return null;

				final AtomicInteger code = new AtomicInteger(-1);
				String velocityCurrency = objSqlVal.toString().toUpperCase();
				try {
					code.set(Integer.parseInt(velocityCurrency.trim()));
				} catch (NumberFormatException nfe) {
					code.set(-1);
				}

				return Currency.getAvailableCurrencies().stream()
						.filter(itm -> code.get() == itm.getNumericCode() ||
								velocityCurrency.equalsIgnoreCase(itm.getCurrencyCode()))
						.findFirst()
						.orElse(null);
			}
		}
		return null;
	}

	public String getCustomCompanyLogoSmallURL() {
		return "/get_company_logo_small";
	}

	public boolean isTitleLookupIncludedFormTypesAll() {
		if (UtilGlobal.isBlank(this.titleLookupIncludedFormTypes)) return false;

		return "[all]".equals(this.titleLookupIncludedFormTypes.trim().toLowerCase());
	}

	public boolean isTitleLookupIncludedFormTypesNone() {
		if (UtilGlobal.isBlank(this.titleLookupIncludedFormTypes)) return false;

		return "[none]".equals(this.titleLookupIncludedFormTypes.trim().toLowerCase());
	}

	public boolean isRaygunEnabled() {
		return UtilGlobal.isNotBlank(this.raygunAPIKey);
	}

	public String getRaygunAPIKey() {
		return this.raygunAPIKey;
	}
}
