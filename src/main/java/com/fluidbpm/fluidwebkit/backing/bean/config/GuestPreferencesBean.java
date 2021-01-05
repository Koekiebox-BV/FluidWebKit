package com.fluidbpm.fluidwebkit.backing.bean.config;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.config.Configuration;
import com.fluidbpm.program.api.vo.config.ConfigurationListing;
import com.fluidbpm.program.api.vo.webkit.global.WebKitGlobal;
import com.fluidbpm.ws.client.v1.config.ConfigurationClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Named("webKitGuestPreferencesBean")
public class GuestPreferencesBean extends ABaseManagedBean {

	@Getter
	private boolean lightLogo = true;

	@Getter
	@Setter
	private WebKitGlobal webKitGlobal;

	@Getter
	private List<ComponentTheme> componentThemes = new ArrayList<ComponentTheme>();
	{
		componentThemes.add(new ComponentTheme("Blue", "blue", "#2c84d8"));
		componentThemes.add(new ComponentTheme("Wisteria", "wisteria", "#A864AE"));
		componentThemes.add(new ComponentTheme("Cyan", "cyan", "#25A4D4"));
		componentThemes.add(new ComponentTheme("Amber", "amber", "#DB8519"));
		componentThemes.add(new ComponentTheme("Pink", "pink", "#F5487F"));
		componentThemes.add(new ComponentTheme("Orange", "orange", "#CB623A"));
		componentThemes.add(new ComponentTheme("Victoria", "victoria", "#594791"));
		componentThemes.add(new ComponentTheme("Chateau Green", "chateau-green", "#3C9462"));
		componentThemes.add(new ComponentTheme("Paradiso", "paradiso", "#3B9195"));
		componentThemes.add(new ComponentTheme("Chambray", "chambray", "#3161BA"));
		componentThemes.add(new ComponentTheme("Tapestry", "tapestry", "#A2527F"));
	}

	@Getter
	private List<LayoutPrimaryColor> layoutPrimaryColors = new ArrayList<LayoutPrimaryColor>();
	{
		layoutPrimaryColors.add(new LayoutPrimaryColor("Blue", "blue", "#2c84d8"));
		layoutPrimaryColors.add(new LayoutPrimaryColor("Wisteria", "wisteria", "#A053A7"));
		layoutPrimaryColors.add(new LayoutPrimaryColor("Cyan", "cyan", "#25A4D4"));
		layoutPrimaryColors.add(new LayoutPrimaryColor("Amber", "amber", "#DB8519"));
		layoutPrimaryColors.add(new LayoutPrimaryColor("Pink", "pink", "#F5487F"));
		layoutPrimaryColors.add(new LayoutPrimaryColor("Orange", "orange", "#CB623A"));
		layoutPrimaryColors.add(new LayoutPrimaryColor("Victoria", "victoria", "#705BB1"));
		layoutPrimaryColors.add(new LayoutPrimaryColor("Chateau Green", "chateau-green", "#3C9462"));
		layoutPrimaryColors.add(new LayoutPrimaryColor("Paradiso", "paradiso", "#3B9195"));
		layoutPrimaryColors.add(new LayoutPrimaryColor("Chambray", "chambray", "#3161BA"));
		layoutPrimaryColors.add(new LayoutPrimaryColor("Tapestry", "tapestry", "#924470"));
	}

	@PostConstruct
	public void init() {
		//CONFIGS...
		this.bindConfigFluidClientDS();
		ConfigurationListing configurationListing =
				this.getFluidClientDSConfig().getConfigurationClient().getAllConfigurations();

		//Global...
		Configuration webKitGlobal = configurationListing.getListing().stream()
				.filter(itm -> WebKitConfigBean.ConfigKey.WebKit.equals(itm.getKey()))
				.findFirst()
				.orElse(null);
		if (webKitGlobal != null) {
			String jsonVal = webKitGlobal.getValue();
			this.webKitGlobal = this.populateWebKitGlobal(jsonVal);
		} else {
			this.webKitGlobal = this.populateWebKitGlobal(null);
		}
	}

	public void actionUpdateGlobalWebKit() {
		try {
			final ConfigurationClient configurationClient = this.getFluidClientDSConfig().getConfigurationClient();
			configurationClient.upsertConfiguration(
					WebKitConfigBean.ConfigKey.WebKit, this.webKitGlobal.toString());
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", "Look and Feel Updated.");
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public WebKitGlobal populateWebKitGlobal(String json) {
		JSONObject jsonObj = (json == null || json.trim().isEmpty()) ?
				new JSONObject() : new JSONObject(json);

		WebKitGlobal returnVal = (jsonObj.isEmpty()) ?
				new WebKitGlobal(new JSONObject()) : new WebKitGlobal(new JSONObject(json));

		if (UtilGlobal.isBlank(returnVal.getLayoutMode())) returnVal.setLayoutMode("light");
		if (UtilGlobal.isBlank(returnVal.getFormType())) returnVal.setFormType("outlined");
		if (UtilGlobal.isBlank(returnVal.getLayoutColors())) returnVal.setLayoutColors("amber");
		if (UtilGlobal.isBlank(returnVal.getTopbarTheme())) returnVal.setTopbarTheme("dark");
		if (UtilGlobal.isBlank(returnVal.getMenuTheme())) returnVal.setMenuTheme("dark");
		if (returnVal.getMenuTypeDefault() == null) returnVal.setMenuTypeDefault(true);
		if (UtilGlobal.isBlank(returnVal.getMenuModeDefault())) returnVal.setMenuModeDefault("layout-static layout-static-active");
		if (UtilGlobal.isBlank(returnVal.getProfileModeDefault())) returnVal.setProfileModeDefault("popup");
		if (UtilGlobal.isBlank(returnVal.getComponentColors())) returnVal.setComponentColors("amber");

		return returnVal;
	}

	public String getDarkMode() {
		return this.webKitGlobal.getLayoutMode();
	}

	public boolean isLightLogo() {
		return lightLogo;
	}

	public void setDarkMode(String darkMode) {
		this.webKitGlobal.setLayoutMode(darkMode);
		this.webKitGlobal.setMenuTheme(darkMode);
		this.webKitGlobal.setTopbarTheme(darkMode);
		this.lightLogo = !this.webKitGlobal.getTopbarTheme().equals("light");
	}

	public String getLayout() {
		return String.format("layout-%s-%s", this.webKitGlobal.getLayoutColors(), this.getDarkMode());
	}

	public String getTheme() {
		return this.webKitGlobal.getComponentColors() + '-' + this.getDarkMode();
	}

	public String getLayoutPrimaryColor() {
		return this.webKitGlobal.getLayoutColors();
	}

	public void setLayoutPrimaryColor(String layoutPrimaryColor) {
		this.webKitGlobal.setLayoutColors(layoutPrimaryColor);
		this.webKitGlobal.setComponentColors(layoutPrimaryColor);
	}

	public String getComponentTheme() {
		return this.webKitGlobal.getComponentColors();
	}

	public void setComponentTheme(String componentTheme) {
		this.webKitGlobal.setComponentColors(componentTheme);
	}

	public String getMenuTheme() {
		return this.webKitGlobal.getMenuTheme();
	}

	public void setMenuTheme(String menuTheme) {
		this.webKitGlobal.setMenuTheme(menuTheme);
	}

	public String getTopbarTheme() {
		return this.webKitGlobal.getTopbarTheme();
	}

	public void setTopbarTheme(String topbarTheme) {
		this.webKitGlobal.setTopbarTheme(topbarTheme);
		this.lightLogo = !this.webKitGlobal.getTopbarTheme().equals("light");
	}

	public String getMenuMode() {
		return this.webKitGlobal.getMenuModeDefault();
	}

	public void setMenuMode(String menuMode) {
		this.webKitGlobal.setMenuModeDefault(menuMode);
	}

	public boolean isGroupedMenu() {
		return this.webKitGlobal.getMenuTypeDefault();
	}

	public void setGroupedMenu(boolean value) {
		this.webKitGlobal.setMenuTypeDefault(value);
	}

	public String getProfileMode() {
		return this.webKitGlobal.getProfileModeDefault();
	}

	public void setProfileMode(String profileMode) {
		this.webKitGlobal.setProfileModeDefault(profileMode);
	}

	public String getInputStyle() {
		return this.webKitGlobal.getFormType();
	}

	public void setInputStyle(String inputStyle) {
		this.webKitGlobal.setFormType(inputStyle);
	}

	public String getInputStyleClass() {
		return this.getInputStyle().equals("filled") ? "ui-input-filled" : "";
	}

	public String getInputStyleAddition() {
		return this.webKitGlobal.getInputStyleAddition();
	}

	public void setInputStyleAddition(String inputStyleAddition) {
		this.webKitGlobal.setInputStyleAddition(inputStyleAddition);
	}

	public String getFluidLogoFilename() {
		String topbarTheme = this.getTopbarTheme();
		if (topbarTheme == null) {
			return null;
		}
		switch (topbarTheme) {
			case "light" :
				return "fluid-black.svg";
			case "dark" :
			case "dim" :
				return "fluid-white.svg";
			default:
				return "fluid-white.svg";
		}
	}

	@AllArgsConstructor
	@Getter
	public class ComponentTheme {
		String name;
		String file;
		String color;
	}

	@AllArgsConstructor
	@Getter
	public class LayoutPrimaryColor {
		String name;
		String file;
		String color;
	}

}
