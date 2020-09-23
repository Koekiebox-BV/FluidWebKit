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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.pi;

import com.fluidbpm.fluidwebkit.backing.bean.performance.PerformanceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.ABaseWorkspaceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
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
@Named("webKitPersonalInvBean")
public class PersonalInventoryBean extends ABaseWorkspaceBean<PersonalInventoryItemVO, ContentViewPI> {

	@Inject
	private PerformanceBean performanceBean;

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
		this.contentView = this.actionOpenMainPage(null, null);
	}

	@Override
	protected ContentViewPI actionOpenMainPage(String workspaceAimParam, WebKitViewGroup webKitGroup) {
		try {
			if (this.getFluidClientDS() == null) {
				return null;
			}
			PersonalInventoryClient persInvClient = this.getFluidClientDS().getPersonalInventoryClient();
			List<FluidItem> piItems = persInvClient.getPersonalInventoryItems();
			List<WorkspaceFluidItem> wsFldItms = new ArrayList<>();
			piItems.forEach(flItm -> {
				wsFldItms.add(new WorkspaceFluidItem(createABaseWebVO(flItm)));
			});

			Map<JobView, List<WorkspaceFluidItem>> wsFluidItmsMap = new HashMap<>();
			wsFluidItmsMap.put(new JobView(ContentViewPI.PI), wsFldItms);

			ContentViewPI contentViewPI = new ContentViewPI(this.getLoggedInUser());
			contentViewPI.refreshData(wsFluidItmsMap);
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

	/**
	 *
	 */
	public void actionRemoveSelectedItemsFromPI() {
		try {
			this.getContentView().getFluidItemsSelectedList().clear();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Products Removed"));
			PrimeFaces.current().ajax().update("form:messages", "form:dt-products");

		} catch (Exception fce) {
			this.raiseError(fce);
		}
	}



	public int getNumberOfPersonalInventoryItems() {
		return performanceBean.getUserStatsReport().getPiCount();
	}

	public int getNumberOfPersonalInventoryLockedItems() {
		return performanceBean.getUserStatsReport().getPiLockedCount();
	}
}
