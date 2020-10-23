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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.ABaseWorkspaceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.jv.ContentViewJV;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.jv.JobViewItemVO;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceRouteField;
import com.fluidbpm.ws.client.FluidClientException;
import lombok.Getter;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.util.*;

/**
 *
 */
@SessionScoped
@Named("webKitViewContentModelBean")
public class WebKitViewContentModelBean extends ABaseManagedBean {
	@Getter
	private Map<String, Map<String, List<ABaseManagedBean.ColumnModel>>> columnModels;

	public WebKitViewContentModelBean() {
		this.columnModels = /*Collections.synchronizedMap(*/new HashMap<>()/*)*/;
	}

	public void storeModelFor(String category, String sectionAlias, List<ABaseManagedBean.ColumnModel> model) {
		if ((category == null || sectionAlias == null) || (model == null || model.isEmpty())) {
			return;
		}
		if (this.columnModels.containsKey(category) &&
				this.columnModels.get(category).containsKey(sectionAlias)) {
			return;
		}

		synchronized (this.columnModels) {
			Map<String, List<ABaseManagedBean.ColumnModel>> sectionMapping =
					this.columnModels.getOrDefault(category, new HashMap<>());
			sectionMapping.put(sectionAlias, model);
			this.columnModels.put(category, sectionMapping);
		}
	}

	public List<ABaseManagedBean.ColumnModel> getModelFor(String category, String sectionAlias) {
		return this.columnModels.getOrDefault(category, new HashMap<>()).getOrDefault(sectionAlias, new ArrayList<>());
	}
}
