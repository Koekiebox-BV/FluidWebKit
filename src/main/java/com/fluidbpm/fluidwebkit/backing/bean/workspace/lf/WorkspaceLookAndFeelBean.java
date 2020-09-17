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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.lf;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.fluidwebkit.qualifier.cache.FormAttachmentsCache;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceRouteField;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.attachment.AttachmentClient;
import com.fluidbpm.ws.client.v1.flow.RouteFieldClient;
import com.google.common.cache.Cache;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SessionScoped
@Named("webKitWorkspaceLookAndFeelBean")
public class WorkspaceLookAndFeelBean extends ABaseManagedBean {

	@Getter
	private List<JobView> allJobViews;

	@Getter
	private Map<String, List<JobView>> groupToViewMapping;

	@Getter
	private Map<String, List<WebKitWorkspaceRouteField>> groupToRouteFieldMapping;

	@Getter
	@Setter
	private List<WebKitViewGroup> webKitViewGroups;

	//Inputs....
	@Getter
	@Setter
	private String inputNewGroupSubName;

	/**
	 * Prepare to change the look and feel for workspace.
	 */
	public void actionPrepareWorkspaceLookAndFeelUpdate() {
		this.groupToViewMapping = new HashMap<>();
		this.groupToRouteFieldMapping = new HashMap<>();
		this.setDialogHeaderTitle("Workspace - Look & Feel");
		try {
			try {
				this.webKitViewGroups = this.getFluidClientDSConfig().getFlowClient().getViewGroupWebKit().getListing();
			} catch (FluidClientException fce) {
				if (fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT) {
					throw fce;
				}
				return;
			}
			this.allJobViews = this.getFluidClientDSConfig().getFlowStepClient().getJobViewsByLoggedInUser().getListing();
			RouteFieldClient routeFieldClient = this.getFluidClientDSConfig().getRouteFieldClient();

			//Set the Views available for the Groups...
			this.allJobViews.stream()
					.filter(itm -> itm.getViewGroupName() != null && !itm.getViewGroupName().isEmpty())
					.forEach(itm -> {
						String viewGroupName = itm.getViewGroupName();
						//Add the Job Views for the Group...
						List<JobView> viewsForGroup = this.groupToViewMapping.getOrDefault(viewGroupName, new ArrayList<>());
						viewsForGroup.add(itm);
						viewsForGroup.sort(Comparator.comparing(JobView::getViewStepName).thenComparing(JobView::getViewPriority));
						this.groupToViewMapping.put(viewGroupName, viewsForGroup);
					});

			//Set the Route Fields for the Groups...
			this.groupToViewMapping.forEach((key, value) -> {
				List<Field> routeFieldsForGroup = new ArrayList<>();
				try {
					routeFieldsForGroup = routeFieldClient.getFieldsByViewGroup(new WebKitViewGroup(key)).getListing();
				} catch (FluidClientException fce) {
					if (fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT) {
						throw fce;
					}
				}

				List<WebKitWorkspaceRouteField> wsRouteFields = routeFieldsForGroup.stream()
						.map(toMap -> {
							WebKitWorkspaceRouteField wkRteField = new WebKitWorkspaceRouteField(toMap);
							return wkRteField;
						})
						.collect(Collectors.toList());
				this.groupToRouteFieldMapping.put(key, wsRouteFields);
			});

			//Merged with existing configuration set...



		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionAddNewViewSub(WebKitViewGroup groupToAddFor) {
		try {
			if (this.getInputNewGroupSubName() == null || this.getInputNewGroupSubName().trim().isEmpty()) {
				throw new ClientDashboardException(
						"'View Sub Name' cannot be empty.", ClientDashboardException.ErrorCode.VALIDATION);
			}

			WebKitViewSub wkViewSubToAdd = new WebKitViewSub();
			wkViewSubToAdd.setLabel(this.getInputNewGroupSubName().trim());

			if (groupToAddFor.getWebKitViewSubs() == null) {
				groupToAddFor.setWebKitViewSubs(new ArrayList<>());
			}

			Integer maxWebKitMaxOrder = groupToAddFor.getWebKitViewSubs().stream()
					.max(Comparator.comparing(WebKitViewSub::getSubOrder))
					.map(itm -> itm.getSubOrder())
					.orElse(Integer.valueOf(0));
			int order = (maxWebKitMaxOrder + 1);
			wkViewSubToAdd.setSubOrder(order);

			groupToAddFor.getWebKitViewSubs().add(wkViewSubToAdd);

			this.setInputNewGroupSubName(null);
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("View Sub '%s' added for '%s'.",
							wkViewSubToAdd.getLabel(), groupToAddFor.getJobViewGroupName()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionRemoveViewSub(WebKitViewGroup groupToRemoveFrom, WebKitViewSub subToRemove) {
		try {
			groupToRemoveFrom.getWebKitViewSubs().remove(subToRemove);
			AtomicInteger orderCounter = new AtomicInteger(1);
			groupToRemoveFrom.getWebKitViewSubs().forEach(itm -> {
				itm.setSubOrder(orderCounter.getAndIncrement());
			});
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("Removed View Sub '%s' from '%s'.",
							subToRemove.getLabel(), groupToRemoveFrom.getJobViewGroupName()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionMoveViewSubUp(WebKitViewGroup groupToRemoveFrom, WebKitViewSub subToMove) {
		try {
			int fromPos = subToMove.getSubOrder();
			int newPos = (subToMove.getSubOrder() - 1);
			int objPosInArray = groupToRemoveFrom.getWebKitViewSubs().indexOf(subToMove);

			WebKitViewSub fromObj = groupToRemoveFrom.getWebKitViewSubs().get(objPosInArray);
			WebKitViewSub toObj = groupToRemoveFrom.getWebKitViewSubs().get(objPosInArray-1);
			fromObj.setSubOrder(newPos);
			toObj.setSubOrder(fromPos);

			groupToRemoveFrom.getWebKitViewSubs().set(objPosInArray, toObj);
			groupToRemoveFrom.getWebKitViewSubs().set(objPosInArray - 1, fromObj);

			groupToRemoveFrom.getWebKitViewSubs().sort(Comparator.comparing(WebKitViewSub::getSubOrder));

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("Moved View Sub '%s' from '%d' to position '%d'.",
							subToMove.getLabel(), fromPos, newPos));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionMoveViewSubDown(WebKitViewGroup groupToRemoveFrom, WebKitViewSub subToMove) {
		try {
			int fromPos = subToMove.getSubOrder();
			int newPos = (subToMove.getSubOrder() + 1);
			int objPosInArray = groupToRemoveFrom.getWebKitViewSubs().indexOf(subToMove);

			WebKitViewSub fromObj = groupToRemoveFrom.getWebKitViewSubs().get(objPosInArray);
			WebKitViewSub toObj = groupToRemoveFrom.getWebKitViewSubs().get(objPosInArray+1);
			fromObj.setSubOrder(newPos);
			toObj.setSubOrder(fromPos);

			groupToRemoveFrom.getWebKitViewSubs().set(objPosInArray, toObj);
			groupToRemoveFrom.getWebKitViewSubs().set(objPosInArray + 1, fromObj);

			groupToRemoveFrom.getWebKitViewSubs().sort(Comparator.comparing(WebKitViewSub::getSubOrder));

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("Moved View Sub '%s' from '%d' to position '%d'.",
							subToMove.getLabel(), fromPos, newPos));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionSaveWebKitViewGroups(String dialogToHideAfterSuccess) {
		try {

			this.executeJavaScript(String.format("PF('%s').hide()", dialogToHideAfterSuccess));
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", "Workspace Look and Feel Updated.");
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}





}
