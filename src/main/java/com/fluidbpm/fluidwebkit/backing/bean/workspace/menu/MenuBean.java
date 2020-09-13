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
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.flow.JobViewListing;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.program.api.vo.userquery.UserQueryListing;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.userquery.UserQueryClient;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.Submenu;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SessionScoped
@Named("webKitMenuBean")
public class MenuBean extends ABaseManagedBean {

	@Inject
	private WebKitAccessBean webKitAccessBean;

	private List<WebKitViewGroup> webKitViewGroups;

	@Getter
	@Setter
	private Submenu submenuWorkspace;

	@PostConstruct
	public void actionPopulateInit() {
		if (this.getFluidClientDS() == null) {
			return;
		}

		try {
			this.webKitViewGroups = this.getFluidClientDSConfig().getFlowClient().getViewGroupWebKit().getListing();
			this.buildWorkspaceMenu();
		} catch (Exception fce) {
			if (fce instanceof FluidClientException) {
				FluidClientException casted = (FluidClientException)fce;
				if (casted.getErrorCode() == FluidClientException.ErrorCode.NO_RESULT) {
					return;
				}
			}
			this.raiseError(fce);
		}
	}

	private void buildWorkspaceMenu() {
		if (this.webKitViewGroups == null || webKitViewGroups.isEmpty()) {
			return;
		}

		List<JobView> jobViewsWithAccess = this.webKitAccessBean.getJobViewsCanAccess();
		if (jobViewsWithAccess == null || jobViewsWithAccess.isEmpty()) {
			DefaultMenuItem menuItem = new DefaultMenuItem();
			menuItem.setValue("No Access");
			menuItem.setIcon("fa ");
			menuItem.setOutcome("/access");
			this.submenuWorkspace.getElements().add(0, menuItem);
			return;
		}

		this.webKitViewGroups.sort(Comparator.comparing(WebKitViewGroup::getGroupOrder));

		AtomicInteger groupId = new AtomicInteger(1);
		this.webKitViewGroups.forEach(webKitGroupItm -> {
			String groupLabel = webKitGroupItm.getJobViewGroupName();
			String groupIcon = webKitGroupItm.getJobViewGroupIcon();

			if (webKitGroupItm.isEnableGroupSubsInMenu()) {

			} else {
				DefaultMenuItem menuItemGroup = new DefaultMenuItem();
				menuItemGroup.setId(String.format("menGroupId%d", groupId.getAndIncrement()));
				menuItemGroup.setIcon(groupIcon);
				menuItemGroup.setValue(groupLabel);
				menuItemGroup.setOutcome("/workspace");


				menuItemGroup.setParam("workspaceViews", "1,2,3,4");

			}

			webKitGroupItm.getWebKitViewSubs()

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

	private boolean doesUserHaveAccessToItemInGroup() {
		return false;
	}

	private List<Long> getViewIdsForGroup(WebKitViewGroup viewGroup) {
		if (viewGroup == null) {
			return null;
		}

		List<Long> returnVal = new ArrayList<>();

		List<WebKitViewSub> subs = viewGroup.getWebKitViewSubs();
		subs.forEach(viewSub -> {
			List<JobView> views = viewSub.getJobViews();
			if (views == null || views.isEmpty()) {
				return;
			}

			views.sort(Comparator.comparing(JobView::getViewPriority));
			views.forEach(viewItm -> {

			});
		});


		subs.sort(Comparator.comparing(WebKitViewSub::getV));


	}




}
