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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.jv;

import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.ABaseContentView;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

import static com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceBean.TGM_TABLE_PER_VIEW_SECTION_DEL;
import static com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceBean.TGM_TABLE_PER_VIEW_SECTION_DEL_LEN;

public class ContentViewJV extends ABaseContentView {
	@Getter
	private WebKitViewGroup wkGroup;

	@Getter
	private WebKitViewSub wkSub;

	private Map<String, WorkspaceJobViewLDM> fluidItemsLazyModel = null;

	public ContentViewJV(User loggedInUserParam) {
		super(loggedInUserParam, new String[]{});
	}

	public ContentViewJV(
		User loggedInUserParam,
		List<String> subs,
		WebKitViewGroup webKitGroup,
		WebKitViewSub selectedSub
	) {
		super(loggedInUserParam, subs);
		this.wkGroup = webKitGroup;
		this.wkSub = selectedSub;
	}

	@Override
	public List<WorkspaceFluidItem> getWorkspaceFluidItemsFor(
		String sectionParam,
		Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data
	) {
		if (data.isEmpty()) {
			return new ArrayList<>();
		}
		if (this.wkGroup.isTGMCombined()) {
			List<WorkspaceFluidItem> returnVal = new ArrayList<>();
			returnVal.addAll(data.values().stream()
					.map(itm -> itm.values())
					.flatMap(Collection::stream)
					.flatMap(Collection::stream)
					.collect(Collectors.toList()));
			return returnVal;
		} else if (this.wkGroup.isTGMTablePerSub()) {
			List<WorkspaceFluidItem> returnVal = new ArrayList<>();
			data.forEach((sub, viewMap) -> {
				if (sectionParam.equals(sub.getLabel())) {
					returnVal.addAll(viewMap.values().stream()
							.flatMap(Collection::stream)
							.collect(Collectors.toList()));
					return;
				}
			});
			return returnVal;
		} else if (this.wkGroup.isTGMTablePerView()) {
			int indexOfSeparator = sectionParam.indexOf(TGM_TABLE_PER_VIEW_SECTION_DEL);
			if (indexOfSeparator > -1) {
				String subLabel = sectionParam.substring(0,indexOfSeparator),
						viewName = sectionParam.substring(indexOfSeparator + TGM_TABLE_PER_VIEW_SECTION_DEL_LEN);
				List<WorkspaceFluidItem> returnVal = new ArrayList<>();
				data.forEach((sub, viewMap) -> {
					if (subLabel.equals(sub.getLabel())) {
						viewMap.forEach((view, items) -> {
							if (viewName.equals(view.getJobView().getViewName())) {
								returnVal.addAll(items);
								return;
							}
						});
					}
				});
				return returnVal;
			}
		}
		return new ArrayList<>();
	}

	@Override
	public Map<String, List<WorkspaceFluidItem>> getActiveMapBasedOnFilterCriteria() {
		if (this.getTextToFilterBy() == null || this.getTextToFilterBy().trim().isEmpty()) {
			return this.getFluidItemsForSection();
		}
		return this.getFluidItemsForSectionFiltered();
	}

	@Override
	public void refreshData(Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data) {
		super.refreshData(data);
		if (this.getSections() == null) {
			return;
		}

		if (this.fluidItemsLazyModel == null) {
			this.fluidItemsLazyModel = new HashMap<>();
		}

		//Load Items for each section...
		List<String> sectionsToKeep = new ArrayList<>();
		for (String section : this.getSections()) {
			List<WorkspaceFluidItem> items = this.getFluidItemsForSection().get(section);
			WorkspaceJobViewLDM workFluidItmLazy =
					this.fluidItemsLazyModel.getOrDefault(section, new WorkspaceJobViewLDM());
			workFluidItmLazy.clearInitialListing();
			if (items != null) {
				workFluidItmLazy.addToInitialListing(items);
			}

			this.fluidItemsLazyModel.put(section, workFluidItmLazy);
			if ((this.getWkGroup() == null || this.getWkGroup().isEnableRenderEmptyTable()) ||
					!workFluidItmLazy.getDataListing().isEmpty()) {
				sectionsToKeep.add(section);
			}
		}
		this.sections = sectionsToKeep.toArray(new String[]{});
	}
}
