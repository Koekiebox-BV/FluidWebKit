/*
 * Koekiebox CONFIDENTIAL
 *
 * [2012] - [2017] Koekiebox (Pty) Ltd
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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.menu;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.lf.WebKitWorkspaceLookAndFeelBean;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.component.menuitem.UIMenuItem;
import org.primefaces.component.submenu.UISubmenu;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponentBase;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SessionScoped
@Named("webKitMenuBean")
public class WebKitMenuBean extends ABaseManagedBean {

	@Inject
	private WebKitAccessBean webKitAccessBean;

	@Inject
	private WebKitWorkspaceLookAndFeelBean lookAndFeelBean;

	@Getter
	@Setter
	private UISubmenu submenuWorkspace;

	@Getter
	@Setter
	private List<UISubmenu> submenusUserQuery;

	public static final String ICON_DEFAULT_GROUP = "pi pi-list";
	public static final String ICON_DEFAULT_SUB = "pi pi-table";
	public static final String ICON_DEFAULT_WORKSPACE = "pi pi-compass";

	public static final class ReqParam {
		public static final String WORKSPACE_AIM = "workspaceAim";
		public static final String CLICKED_GROUP = "webKitItmClickedGroup";
		public static final String CLICKED_GROUP_ALIAS = "webKitItmClickedGroupAlias";
		public static final String CLICKED_SUB_ORDER = "webKitItmClickedSubOrder";
		public static final String CLICKED_SUB_ALIAS = "webKitItmClickedSubAlias";
		public static final String CLICKED_VIEWS = "webKitItmClickedViews";
		public static final String MISSING_CONFIG_MSG = "missingConfigMessage";
	}

	@PostConstruct
	public void actionPopulateInit() {
		if (this.getFluidClientDS() == null) {
			return;
		}

		this.submenusUserQuery = new ArrayList<>();

		//TODO need to fetch these from listing / config...
		this.submenuWorkspace = new UISubmenu();
		this.submenuWorkspace.setId("om_workspace");
		this.submenuWorkspace.setLabel("Workspace");
		this.submenuWorkspace.setIcon(ICON_DEFAULT_WORKSPACE);

		this.lookAndFeelBean.actionPrepareWorkspaceLookAndFeelUpdate();
		this.buildWorkspaceMenu();
	}

	public String actionOpenMissingConfig() {
		return "noconfig";
	}

	private void buildWorkspaceMenu() {
		if (this.getJobViewGroups() == null || this.getJobViewGroups().isEmpty()) {
			this.noGroups();
			return;
		}

		List<JobView> jobViewsWithAccess = this.webKitAccessBean.getJobViewsCanAccess();
		if (jobViewsWithAccess == null || jobViewsWithAccess.isEmpty()) {
			this.noAccess();
			return;
		}

		AtomicInteger groupCounter = new AtomicInteger(1);
		this.getJobViewGroups().stream().forEach(webKitGroupItm -> {
			String groupLabel = webKitGroupItm.getJobViewGroupName();
			Long groupId = webKitGroupItm.getJobViewGroupId();
			String groupIcon = webKitGroupItm.getJobViewGroupIcon();

			final UIComponentBase groupBaseToAdd;
			if (!webKitGroupItm.isTGMCombined()) {
				UISubmenu menuItemGroup = new UISubmenu();
				menuItemGroup.setLabel(groupLabel);
				menuItemGroup.setIcon((groupIcon == null || groupIcon.trim().isEmpty()) ? ICON_DEFAULT_GROUP : groupIcon);

				List<WebKitViewSub> subsForGroup = webKitGroupItm.getWebKitViewSubs();
				if (subsForGroup != null && !subsForGroup.isEmpty()) {
					subsForGroup.sort(Comparator.comparing(WebKitViewSub::getSubOrder));
					subsForGroup.stream().forEach(sub -> {
						String subLabel = sub.getLabel();
						String subIcon = sub.getIcon();
						UIMenuItem menuItemSub = new UIMenuItem();
						menuItemSub.setId(String.format("menSubId%d", sub.getSubOrder()));
						menuItemSub.setIcon((subIcon == null || subIcon.trim().isEmpty()) ? ICON_DEFAULT_SUB : subIcon);
						menuItemSub.setValue(subLabel);
						menuItemSub.setAjax(true);
						List<Long> jobViews = this.getViewIdsForSub(sub);
						final String onCompleteJS;
						if (jobViews == null || jobViews.isEmpty()) {
							onCompleteJS = String.format("javascript:rcOpenNoConfig([{name:'%s', value:\"%s\"}]);",
									ReqParam.MISSING_CONFIG_MSG,
									String.format("No Views configured to render for Group '%s' and Sub '%s'.",
											groupLabel, sub.getLabel()));
						} else {
							String combinedViewsAsList = jobViews.stream()
									.map(itm -> String.valueOf(itm))
									.collect(Collectors.joining(","));
							onCompleteJS = String.format("javascript:rcOpenWorkspaceItem" +
											"([{name:'%s', value:'%d'}, {name:'%s', value:'%s'}, {name:'%s', value:\"%s\"}, {name:'%s', value:'%d'}, {name:'%s', value:\"%s\"}]);",
									ReqParam.CLICKED_GROUP,
									groupId,
									ReqParam.CLICKED_VIEWS,
									combinedViewsAsList,
									ReqParam.CLICKED_GROUP_ALIAS,
									groupLabel,
									ReqParam.CLICKED_SUB_ORDER,
									sub.getSubOrder(),
									ReqParam.CLICKED_SUB_ALIAS,
									subLabel);
						}
						menuItemSub.setOncomplete(onCompleteJS);
						menuItemGroup.getElements().add(menuItemSub);
					});
				}
				groupBaseToAdd = menuItemGroup;
			} else {
				UIMenuItem menuItemGroup = new UIMenuItem();
				menuItemGroup.setIcon((groupIcon == null || groupIcon.trim().isEmpty()) ? ICON_DEFAULT_GROUP : groupIcon);
				menuItemGroup.setValue(groupLabel);
				menuItemGroup.setAjax(true);
				List<Long> jobViews = this.getViewIdsForGroup(webKitGroupItm);
				final String onCompleteJS;
				if (jobViews == null || jobViews.isEmpty()) {
					onCompleteJS = String.format("javascript:rcOpenNoConfig([{name:'%s', value:\"%s\"}]);",
							ReqParam.MISSING_CONFIG_MSG,
							String.format("No Views configured to render for Group '%s'.", groupLabel));
				} else {
					String combinedViewsAsList = jobViews.stream()
							.map(itm -> String.valueOf(itm))
							.collect(Collectors.joining(","));
					onCompleteJS = String.format("javascript:rcOpenWorkspaceItem([{name:'%s', value:'%d'}, {name:'%s', value:'%s'}, {name:'%s', value:\"%s\"}]);",
							ReqParam.CLICKED_GROUP,
							groupId,
							ReqParam.CLICKED_VIEWS,
							combinedViewsAsList,
							ReqParam.CLICKED_GROUP_ALIAS,
							groupLabel);
				}
				menuItemGroup.setOncomplete(onCompleteJS);
				groupBaseToAdd = menuItemGroup;
			}
			groupBaseToAdd.setId(String.format("menGroupId%d", groupCounter.getAndIncrement()));
			this.submenuWorkspace.getElements().add(groupBaseToAdd);
		});
	}

	private void noAccess() {
		UIMenuItem menuItem = new UIMenuItem();
		menuItem.setValue("No Access");
		menuItem.setIcon("pi pi-exclamation-triangle");
		menuItem.setOutcome("access");
		this.submenuWorkspace.getElements().add(this.submenuWorkspace.getElementsCount(), menuItem);
	}

	private void noGroups() {
		UIMenuItem menuItem = new UIMenuItem();
		menuItem.setValue("No Group Config");
		menuItem.setIcon("pi pi-exclamation-triangle");
		menuItem.setOutcome("noconfig");
		this.submenuWorkspace.getElements().add(this.submenuWorkspace.getElementsCount(), menuItem);
	}

	private boolean doesUserHaveAccessToItemInGroup() {
		return false;
	}

	public List<Long> getViewIdsForGroup(WebKitViewGroup viewGroup) {
		if (viewGroup == null || viewGroup.getWebKitViewSubs() == null) {
			return null;
		}

		List<Long> returnVal = new ArrayList<>();
		List<WebKitViewSub> subs = viewGroup.getWebKitViewSubs();
		subs.sort(Comparator.comparing(WebKitViewSub::getSubOrder));
		subs.forEach(viewSub -> {
			List<Long> viewIdsForSub = this.getViewIdsForSub(viewSub);
			if (viewIdsForSub == null || viewIdsForSub.isEmpty()) {
				return;
			}
			viewIdsForSub.stream()
					.filter(itm -> !returnVal.contains(itm))
					.forEach(viewItm -> {
						returnVal.add(viewItm);
					});
		});
		return returnVal;
	}

	public List<Long> getViewIdsForSub(WebKitViewSub viewSub) {
		if (viewSub == null || viewSub.getJobViews() == null) {
			return null;
		}
		List<Long> returnVal = new ArrayList<>();
		List<WebKitWorkspaceJobView> views = viewSub.getJobViews();
		views.sort(Comparator.comparing(WebKitWorkspaceJobView::getViewFlowStepViewOrder));
		views.forEach(view -> {
			List<JobView> remapped = views.stream().map(itm -> itm.getJobView()).collect(Collectors.toList());
			remapped.stream()
					.filter(itm -> !returnVal.contains(itm.getId()))
					.forEach(viewItm -> {
						returnVal.add(viewItm.getId());
					});
		});
		return returnVal;
	}

	public List<WebKitWorkspaceJobView> getUniqueViewsForGroup(WebKitViewGroup viewGroup) {
		if (viewGroup == null || viewGroup.getWebKitViewSubs() == null) {
			return null;
		}

		List<WebKitWorkspaceJobView> returnVal = new ArrayList<>();
		List<Long> addedViewIds = new ArrayList<>();
		viewGroup.getWebKitViewSubs().stream()
				.sorted(Comparator.comparing(WebKitViewSub::getSubOrder))
				.forEach(viewSub -> {
					List<WebKitWorkspaceJobView> viewsForSub = this.getViewsForSub(viewSub);
					if (viewsForSub == null || viewsForSub.isEmpty()) return;
					viewsForSub.stream()
							.filter(itm -> !addedViewIds.contains(itm.getId()))
							.forEach(viewItm -> {
								returnVal.add(viewItm);
								addedViewIds.add(viewItm.getId());
							});
		});
		return returnVal;
	}

	public List<WebKitWorkspaceJobView> getViewsForSub(WebKitViewSub viewSub) {
		if (viewSub == null || viewSub.getJobViews() == null) {
			return null;
		}
		List<WebKitWorkspaceJobView> returnVal = viewSub.getJobViews();
		returnVal.sort(Comparator.comparing(WebKitWorkspaceJobView::getViewFlowStepViewOrder));
		return returnVal;
	}

	public WebKitViewGroup getGroupWithName(String groupName) {
		if (groupName == null) {
			return null;
		}
		return this.getJobViewGroups().stream()
				.filter(itm -> groupName.equals(itm.getJobViewGroupName()))
				.findFirst()
				.orElse(null);
	}

	public List<WebKitViewGroup> getJobViewGroups() {
		return this.lookAndFeelBean.getWebKitViewGroups();
	}

}
