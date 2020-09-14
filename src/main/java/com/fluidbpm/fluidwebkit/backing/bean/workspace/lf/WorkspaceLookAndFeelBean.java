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
import com.fluidbpm.fluidwebkit.qualifier.cache.FormAttachmentsCache;
import com.fluidbpm.program.api.vo.attachment.Attachment;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.attachment.AttachmentClient;
import com.fluidbpm.ws.client.v1.flow.RouteFieldClient;
import com.google.common.cache.Cache;
import lombok.Getter;
import lombok.Setter;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.stream.Collectors;

@SessionScoped
@Named("webKitWorkspaceLookAndFeelBean")
public class WorkspaceLookAndFeelBean extends ABaseManagedBean {

	@Getter
	@Setter
	private List<WebKitViewGroup> webKitViewGroups;

	@Getter
	@Setter
	private List<JobView> allJobViews;

	@Getter
	private Map<String, List<JobView>> groupToViewMapping;

	@Getter
	private Map<String, List<Field>> groupToRouteFieldMapping;

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

			this.allJobViews.stream()
					.filter(itm -> itm.getViewGroupName() != null && !itm.getViewGroupName().isEmpty())
					.forEach(itm -> {
						String key = itm.getViewGroupName();
						List<Field> routeFieldsForGroup = new ArrayList<>();
						try {
							routeFieldsForGroup = routeFieldClient.getFieldsByViewGroup(new WebKitViewGroup(key)).getListing();
						} catch (FluidClientException fce) {
							if (fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT) {
								throw fce;
							}
						}

						//Route Fields...
						this.groupToRouteFieldMapping.put(key, routeFieldsForGroup);

						//Add the Job Views for the Group...
						List<JobView> viewsForGroup = this.groupToViewMapping.getOrDefault(key, new ArrayList<>());
						viewsForGroup.add(itm);
						viewsForGroup.sort(Comparator.comparing(JobView::getViewStepName).thenComparing(JobView::getViewPriority));
						this.groupToViewMapping.put(key, viewsForGroup);
					});


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
