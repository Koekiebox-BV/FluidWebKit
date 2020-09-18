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
import com.fluidbpm.fluidwebkit.backing.bean.workspace.pi.ContentViewPI;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.flow.JobViewListing;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.program.api.vo.userquery.UserQueryListing;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroupListing;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.userquery.UserQueryClient;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.component.menuitem.UIMenuItem;
import org.primefaces.component.submenu.UISubmenu;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.Submenu;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIParameter;
import javax.faces.component.UIViewParameter;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SessionScoped
@Named("webKitMenuBean")
public class MenuBean extends ABaseManagedBean {

	@Inject
	private WebKitAccessBean webKitAccessBean;

	private List<WebKitViewGroup> webKitViewGroups;

	@Getter
	@Setter
	private UISubmenu submenuWorkspace;

	public static final class ReqParam {
		public static final String CLICKED_GROUP = "webKitItmClickedGroup";
		public static final String CLICKED_GROUP_ALIAS = "webKitItmClickedGroupAlias";
		public static final String CLICKED_VIEWS = "webKitItmClickedViews";
		public static final String MISSING_CONFIG_MSG = "missingConfigMessage";
	}

	@PostConstruct
	public void actionPopulateInit() {
		if (this.getFluidClientDS() == null) {
			return;
		}

		//TODO need to fetch these from listing / config...
		this.submenuWorkspace = new UISubmenu();
		this.submenuWorkspace.setId("om_workspace");
		this.submenuWorkspace.setLabel("Workspace");
		this.submenuWorkspace.setIcon("pi pi-compass");

		try {
			WebKitViewGroupListing listing = this.getFluidClientDSConfig().getFlowClient().getViewGroupWebKit();
			this.webKitViewGroups = listing.getListing();
			this.buildWorkspaceMenu();
		} catch (Exception fce) {
			if (fce instanceof FluidClientException) {
				FluidClientException casted = (FluidClientException)fce;
				if (casted.getErrorCode() == FluidClientException.ErrorCode.NO_RESULT) {
					this.noGroups();
					return;
				}
			}
			this.raiseError(fce);
		}
	}

	public String actionOpenMissingConfig() {
		return "noconfig";
	}

	private void buildWorkspaceMenu() {
		if (this.webKitViewGroups == null || webKitViewGroups.isEmpty()) {
			this.noGroups();
			return;
		}

		List<JobView> jobViewsWithAccess = this.webKitAccessBean.getJobViewsCanAccess();
		if (jobViewsWithAccess == null || jobViewsWithAccess.isEmpty()) {
			this.noAccess();
			return;
		}

		this.webKitViewGroups.sort(Comparator.comparing(WebKitViewGroup::getGroupOrder));

		AtomicInteger groupCounter = new AtomicInteger(1);
		this.webKitViewGroups.forEach(webKitGroupItm -> {
			String groupLabel = webKitGroupItm.getJobViewGroupName();
			Long groupId = webKitGroupItm.getJobViewGroupId();
			String groupIcon = webKitGroupItm.getJobViewGroupIcon();

			if (webKitGroupItm.isEnableGroupSubsInMenu()) {
				//TODO need to sub this here...
			} else {
				UIMenuItem menuItemGroup = new UIMenuItem();
				menuItemGroup.setId(String.format("menGroupId%d", groupCounter.getAndIncrement()));
				menuItemGroup.setIcon((groupIcon == null || groupIcon.trim().isEmpty()) ? "pi pi-list" : groupIcon);
				menuItemGroup.setValue(groupLabel);
				menuItemGroup.setAjax(true);
				List<Long> jobViews = this.getViewIdsForGroup(webKitGroupItm);
				final String onCompleteJS;
				if (jobViews == null || jobViews.isEmpty()) {
					onCompleteJS = String.format("javascript:rcOpenNoConfig([{name:'%s', value:\"%s\"}]);",
							ReqParam.MISSING_CONFIG_MSG,
							String.format("No Views configured to render for '%s'.", groupLabel));
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
				this.submenuWorkspace.getElements().add(menuItemGroup);
			}
		});

//		DefaultMenuItem menuItem = new DefaultMenuItem();
//		menuItem.setId(ID_PREFIX_VIEWMENUITEMNOTIFICATIONS);
//		menuItem.setValue(
//				"Notifications (" + this.getNotificationsCount() + ")");
//		menuItem.setIcon(GlobalVariable.Icon.CIRCLE);
//		menuItem.setOnclick(ClickJavaScript);
//		menuItem.setParam(WebParam.TypeOfInbox, CurrentDataViewType.Notifications.getAlias());
//		menuItem.setCommand("#{jobViewItemListingBean.actionViewFilterSelectedPersonalInbox}");
//		menuItem.setUpdate(ClickUpdate);
//		groupMenuModel.getElements().add(menuItem);
//
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

	private List<Long> getViewIdsForGroup(WebKitViewGroup viewGroup) {
		if (viewGroup == null || viewGroup.getWebKitViewSubs() == null) {
			return null;
		}

		List<Long> returnVal = new ArrayList<>();
		List<WebKitViewSub> subs = viewGroup.getWebKitViewSubs();
		subs.forEach(viewSub -> {
			List<WebKitWorkspaceJobView> views = viewSub.getJobViews();
			if (views == null || views.isEmpty()) {
				return;
			}

			List<JobView> remapped = views.stream().map(itm -> itm.getJobView()).collect(Collectors.toList());
			remapped.sort(Comparator.comparing(JobView::getViewPriority));
			remapped.stream()
					.filter(itm -> !returnVal.contains(itm.getId()))
					.forEach(viewItm -> {
						returnVal.add(viewItm.getId());
					});
		});
		return returnVal;
	}

}
