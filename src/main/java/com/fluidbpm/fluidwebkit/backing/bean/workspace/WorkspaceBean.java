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

package com.fluidbpm.fluidwebkit.backing.bean.workspace;

import com.fluidbpm.fluidwebkit.backing.bean.performance.PerformanceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.pi.ContentViewPI;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.pi.PersonalInventoryItemVO;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.user.PersonalInventoryClient;
import org.primefaces.PrimeFaces;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bean storing personal inventory related items.
 */
@SessionScoped
@Named("webKitWorkspaceBean")
public class WorkspaceBean extends ABaseWorkspaceBean<PersonalInventoryItemVO, ContentViewPI> {

	@Override
	@PostConstruct
	public void actionPopulateInit() {
		//Do nothing...
		//this.actionOpenMainPage();
	}

	@Override
	public void actionOpenFormForEditingFromWorkspace(
		JobView fromView,
		Long formIdToUpdateParam
	) {
		//Do nothing...
	}

	@Override
	public void actionOpenMainPage() {
		this.contentView = this.actionOpenMainPage(null);
	}

	@Override
	protected ContentViewPI actionOpenMainPage(String workspaceAimParam) {
		try {
			if (this.getFluidClientDS() == null) {
				return null;
			}

			FacesContext current = FacesContext.getCurrentInstance();
			Map<String,String> reqMapping = current.getExternalContext().getRequestParameterMap();
			String clickedGroup = reqMapping.get("webKitItmClickedGroup");
			String clickedWorkspaceViews = reqMapping.get("webKitItmClickedViews");



			ContentViewPI contentViewPI = new ContentViewPI(this.getLoggedInUser());
			return contentViewPI;
		} catch (Exception fce) {
			if (fce instanceof FluidClientException) {
				FluidClientException casted = (FluidClientException)fce;
				if (casted.getErrorCode() == FluidClientException.ErrorCode.NO_RESULT) {
					return new ContentViewPI(this.getLoggedInUser());
				}
			}
			this.raiseError(fce);
			return new ContentViewPI(this.getLoggedInUser());
		}
	}

	@Override
	protected PersonalInventoryItemVO createABaseWebVO(FluidItem fluidItemParam) {
		PersonalInventoryItemVO returnVal = new PersonalInventoryItemVO(fluidItemParam);
		return returnVal;
	}

}
