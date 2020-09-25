package com.fluidbpm.fluidwebkit.backing.bean.workspace.lf;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseLDM;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WebKitWorkspaceJobViewLDM extends ABaseLDM<WebKitWorkspaceJobView> {

	@Inject
	private WebKitAccessBean accessBean;

	public WebKitWorkspaceJobViewLDM() {
		super();
		this.setComparator(Comparator.comparing(WebKitWorkspaceJobView::getViewFlowStepViewOrder));
	}

	public WebKitWorkspaceJobViewLDM(List<WebKitWorkspaceJobView> allJobViews) {
		super(allJobViews);
	}

	public void populateFromJobViews(List<JobView> allJobViews) {
		this.addToInitialListing(allJobViews.stream().map(itm -> new WebKitWorkspaceJobView(itm)).collect(Collectors.toList()));
	}
}
