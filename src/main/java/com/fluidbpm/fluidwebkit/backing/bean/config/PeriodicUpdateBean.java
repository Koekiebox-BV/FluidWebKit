package com.fluidbpm.fluidwebkit.backing.bean.config;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.program.api.vo.config.ConfigurationListing;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.thirdpartylib.ThirdPartyLibraryTaskIdentifier;
import com.fluidbpm.program.api.vo.webkit.form.WebKitForm;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQuery;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.ws.client.FluidClientException;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Named("webKitPeriodicUpdateBean")
public class PeriodicUpdateBean extends ABaseManagedBean {
	private static final int UPDATE_INTERVAL_HOURS = 48;

	private long lastUpdate;

	private List<WebKitUserQuery> configWebKitUserQueries;

	@Setter
	private ConfigurationListing configurationListing;

	@Setter
	private List<ThirdPartyLibraryTaskIdentifier> thirdPartyLibTaskIdentifiers;

	@Setter
	private List<WebKitViewGroup> webKitViewGroups;

	@Setter
	private List<WebKitUserQuery> webKitUserQueries;

	@Setter
	private List<WebKitForm> webKitForms;

	@Setter
	private List<JobView> allJobViews;

	@PostConstruct
	public void actionPopulateInit() {
		this.populateWebKitUserQueries();
		this.configurationListing = null;
		this.thirdPartyLibTaskIdentifiers = null;
		this.webKitViewGroups = null;
		this.webKitUserQueries = null;
		this.webKitForms = null;
	}

	public void actionClearAllAppCaches() {
		this.actionPopulateInit();
		FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
				"Success", "App cache has been cleared.");
		FacesContext.getCurrentInstance().addMessage(null, fMsg);
	}

	public List<WebKitUserQuery> getConfigWebKitUserQueries() {
		this.updateIfNeedBe();
		return this.configWebKitUserQueries;
	}

	public ConfigurationListing getConfigurationListing() {
		this.updateIfNeedBe();
		return this.configurationListing;
	}

	public List<ThirdPartyLibraryTaskIdentifier> getThirdPartyLibTaskIdentifiers() {
		this.updateIfNeedBe();
		return this.thirdPartyLibTaskIdentifiers;
	}

	public List<WebKitUserQuery> getWebKitUserQueries() {
		this.updateIfNeedBe();
		return this.webKitUserQueries;
	}

	public List<WebKitViewGroup> getWebKitViewGroups() {
		this.updateIfNeedBe();
		return this.webKitViewGroups;
	}

	public List<WebKitForm> getWebKitForms() {
		this.updateIfNeedBe();
		return this.webKitForms;
	}

	public List<JobView> getAllJobViews() {
		this.updateIfNeedBe();
		return this.allJobViews;
	}

	private void updateIfNeedBe() {
		long now = System.currentTimeMillis();
		if ((this.lastUpdate + TimeUnit.HOURS.toMillis(UPDATE_INTERVAL_HOURS)) < now) {
			this.actionPopulateInit();
			this.lastUpdate = now;
		}
	}

	private void populateWebKitUserQueries() {
		this.configWebKitUserQueries = new ArrayList<>();
		try {
			this.configWebKitUserQueries = this.getFluidClientDSConfig().getUserQueryClient().getUserQueryWebKit().getListing();
		} catch (FluidClientException fce) {
			if (fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT) throw fce;
		}
	}
}
