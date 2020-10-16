package com.fluidbpm.fluidwebkit.backing.bean.workspace.lf;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseLDM;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQuery;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WebKitWorkspaceUserQueryLDM extends ABaseLDM<WebKitUserQuery> {

	public WebKitWorkspaceUserQueryLDM() {
		super();
		//this.setComparator(Comparator.comparing(WebKitUserQuery::getViewFlowStepViewOrder));
	}

	public WebKitWorkspaceUserQueryLDM(List<WebKitUserQuery> allJobViews) {
		super(allJobViews);
	}

	public void populateFromUserQueries(List<UserQuery> allJobViews) {
		this.addToInitialListing(allJobViews.stream().map(itm -> new WebKitUserQuery(itm)).collect(Collectors.toList()));
	}
}
