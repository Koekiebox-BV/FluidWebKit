package com.fluidbpm.fluidwebkit.backing.bean.workspace.jv;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseLDM;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceRouteField;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class WorkspaceJobViewLDM extends ABaseLDM<WorkspaceFluidItem> {
	public WorkspaceJobViewLDM() {
		super();
	}

	public WorkspaceJobViewLDM(List<WorkspaceFluidItem> allItems) {
		super(allItems);
	}
}
