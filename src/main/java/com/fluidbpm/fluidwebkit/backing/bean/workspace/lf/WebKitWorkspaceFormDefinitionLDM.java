package com.fluidbpm.fluidwebkit.backing.bean.workspace.lf;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseLDM;
import com.fluidbpm.program.api.vo.webkit.WebKitForm;

import java.util.List;

public class WebKitWorkspaceFormDefinitionLDM extends ABaseLDM<WebKitForm> {

	public WebKitWorkspaceFormDefinitionLDM() {
		super();
	}

	public WebKitWorkspaceFormDefinitionLDM(List<WebKitForm> allJobViews) {
		super(allJobViews);
	}
}
