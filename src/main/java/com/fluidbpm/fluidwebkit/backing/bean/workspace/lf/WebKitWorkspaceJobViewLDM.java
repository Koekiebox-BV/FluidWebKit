package com.fluidbpm.fluidwebkit.backing.bean.workspace.lf;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseLDM;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;

import javax.inject.Inject;
import java.util.List;

public class WebKitWorkspaceJobViewLDM extends ABaseLDM<WebKitWorkspaceJobView> {

	@Inject
	private WebKitAccessBean accessBean;

	public WebKitWorkspaceJobViewLDM() {
		super();
	}

	public WebKitWorkspaceJobViewLDM(List<WebKitWorkspaceJobView> allJobViews) {
		super(allJobViews);
	}
}
