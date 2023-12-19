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
import com.fluidbpm.fluidwebkit.backing.bean.config.GuestPreferencesBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.PeriodicUpdateBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.lf.WebKitWorkspaceLookAndFeelBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.program.api.vo.webkit.global.WebKitGlobal;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitMenuItem;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQuery;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.component.menuitem.UIMenuItem;
import org.primefaces.component.submenu.UISubmenu;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SessionScoped
@Named("webKitMenuBean")
public class WebKitMenuBean extends ABaseManagedBean {

	@Inject
	private WebKitAccessBean webKitAccessBean;

	@Inject
	private WebKitWorkspaceLookAndFeelBean lookAndFeelBean;

	@Inject
	private GuestPreferencesBean guestPreferencesBean;

	@Getter
	@Setter
	private UISubmenu submenuWorkspace;

	@Inject
	private PeriodicUpdateBean periodicUpdateBean;

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

		//User Query...
		public static final String USER_QUERY_SECTION = "userQuerySection";
		public static final String USER_QUERY_LABEL = "userQueryLabel";
		public static final String USER_QUERY_ID = "userQueryId";
	}

	@PostConstruct
	public void actionPopulateInit() {
		if (this.getFluidClientDS() == null) return;

		this.buildLeftMenuUserQueries();

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

	private void buildLeftMenuUserQueries() {
		this.submenusUserQuery = new ArrayList<>();
		WebKitGlobal wkGlobal = this.guestPreferencesBean.getWebKitGlobal();
		final List<WebKitMenuItem> menuItems = wkGlobal.getWebKitMenuItems();
		if (menuItems == null || menuItems.isEmpty()) return;

		menuItems.stream()
				.filter(itm -> itm.getParentMenuId() == null || itm.getParentMenuId().trim().isEmpty())
				.forEach(itm -> {
					UISubmenu subMenToAdd = new UISubmenu();
					subMenToAdd.setId(String.format("om_%s", itm.getMenuId()));
					subMenToAdd.setLabel(itm.getMenuLabel());
					if (UtilGlobal.isNotBlank(itm.getMenuIcon())) {
						subMenToAdd.setIcon(itm.getMenuIcon());
					}
					if (this.populateSubmenuUserQuery(subMenToAdd, menuItems)) this.submenusUserQuery.add(subMenToAdd);
				});
	}

	public boolean isSubmenusUserQueryAvailForIndex(int index) {
		if (this.submenusUserQuery == null) return false;
		return (index < this.submenusUserQuery.size());
	}

	public UISubmenu retrieveSubMenuForIndex(int index) {
		if (!this.isSubmenusUserQueryAvailForIndex(index)) return new UISubmenu();
		return this.submenusUserQuery.get(index);
	}

	public void writeSubMenuForIndex(int index, UISubmenu menu) {
		if (!this.isSubmenusUserQueryAvailForIndex(index)) return;
		this.submenusUserQuery.set(index, menu);
	}

	public UISubmenu getRetrieveSubMenuForIndexOne() {
		return this.retrieveSubMenuForIndex(0);
	}

	public void setRetrieveSubMenuForIndexOne(UISubmenu menu) {
		this.writeSubMenuForIndex(0, menu);
	}

	public UISubmenu getRetrieveSubMenuForIndexTwo() {
		return this.retrieveSubMenuForIndex(1);
	}

	public void setRetrieveSubMenuForIndexTwo(UISubmenu menu) {
		this.writeSubMenuForIndex(1, menu);
	}

	public UISubmenu getRetrieveSubMenuForIndexThree() {
		return this.retrieveSubMenuForIndex(2);
	}

	public void setRetrieveSubMenuForIndexThree(UISubmenu menu) {
		this.writeSubMenuForIndex(2, menu);
	}

	public UISubmenu getRetrieveSubMenuForIndexFour() {
		return this.retrieveSubMenuForIndex(3);
	}

	public void setRetrieveSubMenuForIndexFour(UISubmenu menu) {
		this.writeSubMenuForIndex(3, menu);
	}

	public UISubmenu getRetrieveSubMenuForIndexFive() {
		return this.retrieveSubMenuForIndex(4);
	}

	public void setRetrieveSubMenuForIndexFive(UISubmenu menu) {
		this.writeSubMenuForIndex(4, menu);
	}

	public UISubmenu getRetrieveSubMenuForIndexSix() {
		return this.retrieveSubMenuForIndex(5);
	}

	public void setRetrieveSubMenuForIndexSix(UISubmenu menu) {
		this.writeSubMenuForIndex(5, menu);
	}

	public UISubmenu getRetrieveSubMenuForIndexSeven() {
		return this.retrieveSubMenuForIndex(6);
	}

	public void setRetrieveSubMenuForIndexSeven(UISubmenu menu) {
		this.writeSubMenuForIndex(6, menu);
	}

	public UISubmenu getRetrieveSubMenuForIndexEight() {
		return this.retrieveSubMenuForIndex(7);
	}

	public void setRetrieveSubMenuForIndexEight(UISubmenu menu) {
		this.writeSubMenuForIndex(7, menu);
	}

	public UISubmenu getRetrieveSubMenuForIndexNine() {
		return this.retrieveSubMenuForIndex(8);
	}

	public void setRetrieveSubMenuForIndexNine(UISubmenu menu) {
		this.writeSubMenuForIndex(8, menu);
	}

	public UISubmenu getRetrieveSubMenuForIndexTen() {
		return this.retrieveSubMenuForIndex(9);
	}

	public void setRetrieveSubMenuForIndexTen(UISubmenu menu) {
		this.writeSubMenuForIndex(9, menu);
	}

	private boolean populateSubmenuUserQuery(UISubmenu submenu, List<WebKitMenuItem> menuItems) {
		if (submenu == null) return false;

		String idNoPrefix = submenu.getId().substring(3);//[om_] removed...
		//Find all where parent is...
		List<WebKitMenuItem> menusWithNodeAsParent = menuItems.stream()
				.filter(itm -> idNoPrefix.equals(itm.getParentMenuId()))
				.collect(Collectors.toList());
		if (menusWithNodeAsParent.isEmpty()) {//No parent...
			WebKitMenuItem menuWithId = menuItems.stream()
					.filter(itm -> idNoPrefix.equals(itm.getMenuId()))
					.findFirst()
					.orElse(null);
			if (menuWithId == null) return false;

			this.createMenuItem(submenu, menuWithId);
			return true;
		}

		AtomicBoolean userHasAccess = new AtomicBoolean(false);

		menusWithNodeAsParent.forEach(menuItmWhereParent -> {
			boolean userHasAccLcl;
			if (this.lookAndFeelBean.doesMenuHaveChildren(menuItmWhereParent, menuItems)) {
				UISubmenu subMenToAdd = new UISubmenu();
				subMenToAdd.setId(String.format("om_%s", menuItmWhereParent.getMenuId()));
				subMenToAdd.setLabel(menuItmWhereParent.getMenuLabel());
				if (userHasAccLcl = this.populateSubmenuUserQuery(subMenToAdd, menuItems))
					submenu.getChildren().add(subMenToAdd);
			} else userHasAccLcl = this.createMenuItem(submenu, menuItmWhereParent);

			if (userHasAccLcl) userHasAccess.set(true);
		});
		return userHasAccess.get();
	}

	private boolean createMenuItem(UISubmenu submenu, WebKitMenuItem menuItmWhereParent) {
		WebKitUserQuery wkUserQryByMenu = this.periodicUpdateBean.getConfigWebKitUserQueries().stream()
				.filter(itm -> menuItmWhereParent.getId() != null &&
						menuItmWhereParent.getId().equals(itm.getUserQuery().getId()))
				.findFirst()
				.orElse(null);
		UserQuery userQuery = null;
		boolean foundByIdAccess = false, userHasAccLcl = false;
		if (wkUserQryByMenu != null && wkUserQryByMenu.getUserQuery() != null) {
			userQuery = wkUserQryByMenu.getUserQuery();
			foundByIdAccess = (this.webKitAccessBean.getUserQueriesCanExecute() == null) ? false :
					this.webKitAccessBean.getUserQueriesCanExecute().stream()
							.filter(itm -> menuItmWhereParent.getId().equals(itm.getId()))
							.findFirst()
							.isPresent();
		}

		UIMenuItem menuItem = new UIMenuItem();
		menuItem.setId(String.format("menUQId_%s", menuItmWhereParent.getMenuId()));
		String menuIcon = menuItmWhereParent.getMenuIcon();
		menuItem.setIcon(this.iconSafe(menuIcon, ICON_DEFAULT_SUB));
		menuItem.setValue(menuItmWhereParent.getMenuLabel());
		menuItem.setAjax(true);
		final String onCompleteJS;
		if (userQuery == null) {
			onCompleteJS = String.format("javascript:rcOpenNoConfig([{name:'%s', value:\"%s\"}]);",
					ReqParam.MISSING_CONFIG_MSG,
					String.format("No User Query configured for Menu '%s'.", menuItmWhereParent.getMenuLabel()));
		} else if (foundByIdAccess) {
			onCompleteJS = String.format("javascript:rcOpenUserQueryItem" +
							"([{name:'%s', value:\"%s\"}, {name:'%s', value:\"%s\"}, {name:'%s', value:'%d'}]);",
					ReqParam.USER_QUERY_LABEL,
					menuItmWhereParent.getMenuLabel(),
					ReqParam.USER_QUERY_SECTION,
					submenu.getLabel(),
					ReqParam.USER_QUERY_ID,
					userQuery.getId());
			userHasAccLcl = true;
		} else {
			onCompleteJS = String.format("javascript:rcOpenNoConfig([{name:'%s', value:\"%s\"}]);",
					ReqParam.MISSING_CONFIG_MSG,
					String.format("No Access. Please request Access to User Query '%s'.", userQuery.getName()));
		}
		menuItem.setOncomplete(onCompleteJS);

		if (userHasAccLcl) submenu.getChildren().add(menuItem);

		return userHasAccLcl;
	}

	private String iconSafe(String icon, String defaultVal) {
		return String.format(" %s ", UtilGlobal.isBlank(icon) ? defaultVal : icon);
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
		
		List<Long> viewIdsWhereUserHasAccess = jobViewsWithAccess.stream()
				.map(itm -> itm.getId())
				.collect(Collectors.toList());

		AtomicInteger groupCounter = new AtomicInteger(1);
		this.getJobViewGroups().stream().forEach(webKitGroupItm -> {
			String groupLabel = webKitGroupItm.getJobViewGroupName();
			Long groupId = webKitGroupItm.getJobViewGroupId();
			String groupIcon = webKitGroupItm.getJobViewGroupIcon();

			final UIComponent toAdd;
			if (webKitGroupItm.isTGMCombined()) {
				UIMenuItem menuItem = new UIMenuItem();
				menuItem.setIcon(this.iconSafe(groupIcon, ICON_DEFAULT_GROUP));
				menuItem.setValue(groupLabel);
				menuItem.setAjax(true);
				List<Long> jobViews = this.getViewIdsForGroup(webKitGroupItm);
				final String onCompleteJS;
				if (jobViews == null || jobViews.isEmpty()) {
					onCompleteJS = null;
				} else {
					List<Long> viewWhenUserHasAccess = jobViews.stream()
							.filter(itm -> viewIdsWhereUserHasAccess.contains(itm))
							.collect(Collectors.toList());
					if (viewWhenUserHasAccess.isEmpty()) {
						onCompleteJS = null;
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
				}

				if (onCompleteJS == null) toAdd = null;
				else {
					menuItem.setOncomplete(onCompleteJS);
					toAdd = menuItem;
				}
			} else {
				UISubmenu submenu = new UISubmenu();
				submenu.setLabel(groupLabel);

				List<WebKitViewSub> subsForGroup = webKitGroupItm.getWebKitViewSubs();
				if (subsForGroup != null && !subsForGroup.isEmpty()) {
					subsForGroup.sort(Comparator.comparing(WebKitViewSub::getSubOrder));
					subsForGroup.stream().forEach(sub -> {
						String subLabel = sub.getLabel();
						String subIcon = sub.getIcon();
						UIMenuItem menuItemSub = new UIMenuItem();
						menuItemSub.setId(String.format("menSubId_%d_%s",
								sub.getSubOrder(), UUID.randomUUID().toString().substring(0, 5)));
						menuItemSub.setIcon(this.iconSafe(subIcon, ICON_DEFAULT_SUB));
						menuItemSub.setValue(subLabel);
						menuItemSub.setAjax(true);

						List<Long> jobViews = this.getViewIdsForSub(sub);
						final String onCompleteJS;
						if (jobViews == null || jobViews.isEmpty()) {
							onCompleteJS = null;
						} else {
							List<Long> viewWhenUserHasAccess = jobViews.stream()
									.filter(itm -> viewIdsWhereUserHasAccess.contains(itm))
									.collect(Collectors.toList());
							if (viewWhenUserHasAccess.isEmpty()) {
								onCompleteJS = null;
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
						}

						if (onCompleteJS != null) {
							menuItemSub.setOncomplete(onCompleteJS);
							submenu.getChildren().add(menuItemSub);
						}
					});
				}
				toAdd = submenu.getChildCount() > 0 ? submenu : null;
			}

			if (toAdd != null) {
				String id = String.format("menGroupId%d", groupCounter.getAndIncrement());
				toAdd.setId(id);
				this.submenuWorkspace.getChildren().add(toAdd);
			}
		});

		if (this.submenuWorkspace.getChildCount() < 1) this.noAccess();
	}

	private void noAccess() {
		UIMenuItem menuItem = new UIMenuItem();
		menuItem.setValue("No Access");
		menuItem.setIcon("pi pi-exclamation-triangle");
		menuItem.setOutcome("access");
		this.submenuWorkspace.getChildren().add(this.submenuWorkspace.getChildCount(), menuItem);
	}

	private void noGroups() {
		UIMenuItem menuItem = new UIMenuItem();
		menuItem.setValue("No Workspace Config");
		menuItem.setIcon("pi pi-exclamation-triangle");
		menuItem.setOutcome("noconfig");
		this.submenuWorkspace.getChildren().add(this.submenuWorkspace.getChildCount(), menuItem);
	}

	private boolean doesUserHaveAccessToItemInGroup() {
		return false;
	}

	public List<Long> getViewIdsForGroup(WebKitViewGroup viewGroup) {
		if (viewGroup == null || viewGroup.getWebKitViewSubs() == null) return null;

		List<Long> returnVal = new ArrayList<>();
		List<WebKitViewSub> subs = viewGroup.getWebKitViewSubs();
		subs.sort(Comparator.comparing(WebKitViewSub::getSubOrder));
		subs.forEach(viewSub -> {
			List<Long> viewIdsForSub = this.getViewIdsForSub(viewSub);
			if (viewIdsForSub == null || viewIdsForSub.isEmpty()) return;

			viewIdsForSub.stream()
					.filter(itm -> !returnVal.contains(itm))
					.forEach(viewItm -> {
						returnVal.add(viewItm);
					});
		});
		return returnVal;
	}

	public List<Long> getViewIdsForSub(WebKitViewSub viewSub) {
		if (viewSub == null || viewSub.getJobViews() == null) return null;

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
		if (viewGroup == null || viewGroup.getWebKitViewSubs() == null) return null;

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
		if (viewSub == null || viewSub.getJobViews() == null) return null;

		List<WebKitWorkspaceJobView> returnVal = viewSub.getJobViews();
		returnVal.sort(Comparator.comparing(WebKitWorkspaceJobView::getViewFlowStepViewOrder));
		return returnVal;
	}

	public WebKitViewGroup getGroupWithName(String groupName) {
		if (groupName == null) return null;

		return this.getJobViewGroups().stream()
				.filter(itm -> groupName.equals(itm.getJobViewGroupName()))
				.findFirst()
				.orElse(null);
	}

	public List<WebKitViewGroup> getJobViewGroups() {
		return this.lookAndFeelBean.getWebKitViewGroups();
	}

	public WebKitUserQuery getWebKitUserQueryWithName(String userQueryName) {
		return this.lookAndFeelBean.getUserQueryWithName(userQueryName);
	}

	public WebKitUserQuery getWebKitUserQueryWithId(Long userQueryId) {
		return this.lookAndFeelBean.getUserQueryWithUserQueryId(userQueryId);
	}
}
