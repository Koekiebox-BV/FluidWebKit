package com.fluidbpm.fluidwebkit.backing.bean.workspace.lf;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseLDM;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceRouteField;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class WebKitWorkspaceRouteFieldLDM extends ABaseLDM<WebKitWorkspaceRouteField> {
	public WebKitWorkspaceRouteFieldLDM() {
		super();
		this.setComparator(Comparator.comparing(WebKitWorkspaceRouteField::getFieldOrder));
	}

	public WebKitWorkspaceRouteFieldLDM(List<WebKitWorkspaceRouteField> allFields) {
		super(allFields);
	}

	public void populateFromFields(List<Field> allFields) {
		AtomicInteger order = new AtomicInteger(1);
		this.addToInitialListing(allFields.stream().map(itm -> {
			WebKitWorkspaceRouteField rv = new WebKitWorkspaceRouteField(itm);
			rv.setFieldOrder(order.getAndIncrement());
			return rv;
		}).collect(Collectors.toList()));
	}
}
