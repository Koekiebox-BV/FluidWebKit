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

import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.performance.PerformanceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.ABaseWorkspaceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.WebKitViewContentModelBean;
import com.fluidbpm.fluidwebkit.backing.vo.ABaseWebVO;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
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

	@Inject
	private WebKitViewContentModelBean webKitViewContentModelBean;

	@Inject
	private WebKitAccessBean accessBean;

	@Override
	@PostConstruct
	public void actionPopulateInit() {
		//Do nothing...
		//this.actionOpenMainPage();
	}

	@Override
	public void actionOpenForm(WorkspaceFluidItem workspaceFluidItem) {
		//Do nothing...
		this.openFormBean.actionFreshLoadFormAndSet(workspaceFluidItem);
	}

	@Override
	public void actionOpenMainPage() {
		this.contentView = this.actionOpenMainPage(null, null);
	}

	@Override
	protected ContentViewPI actionOpenMainPage(
		WebKitViewGroup webKitGroup,
		WebKitViewSub selectedSub
	) {
		try {
			if (this.getFluidClientDS() == null) {
				return null;
			}
			PersonalInventoryClient persInvClient = this.getFluidClientDS().getPersonalInventoryClient();
			List<FluidItem> piItems = persInvClient.getPersonalInventoryItems();
			List<WorkspaceFluidItem> wsFldItms = new ArrayList<>();
			piItems.forEach(flItm -> {
				wsFldItms.add(new WorkspaceFluidItem(this.createABaseWebVO(webKitGroup, selectedSub, null, flItm)));
			});

			Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data = new HashMap<>();
			Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>> wsFluidItmsMap = new HashMap<>();
			wsFluidItmsMap.put(new WebKitWorkspaceJobView(new JobView(ContentViewPI.PI)), wsFldItms);
			data.put(new WebKitViewSub(), wsFluidItmsMap);

			ContentViewPI contentViewPI = new ContentViewPI(this.getLoggedInUser(), this.webKitViewContentModelBean);
			contentViewPI.refreshData(data);
			return contentViewPI;
		} catch (Exception fce) {
			if (fce instanceof FluidClientException) {
				FluidClientException casted = (FluidClientException)fce;
				if (casted.getErrorCode() == FluidClientException.ErrorCode.NO_RESULT) {
					return new ContentViewPI(this.getLoggedInUser(), this.webKitViewContentModelBean);
				}
			}
			this.raiseError(fce);
			return new ContentViewPI(this.getLoggedInUser(), this.webKitViewContentModelBean);
		}
	}

	@Override
	protected PersonalInventoryItemVO createABaseWebVO(
			WebKitViewGroup group,
			WebKitViewSub sub,
			WebKitWorkspaceJobView view,
			FluidItem item
	) {
		PersonalInventoryItemVO returnVal = new PersonalInventoryItemVO(item);
		return returnVal;
	}

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

	/**
	 * Prepare to create a new instane of a form.
	 */
	public void actionPrepToCreateNewInstanceOf() {
		this.setAreaToUpdateForDialogAfterSubmit(null);
		this.currentlyHaveItemOpen = false;
		this.setDialogHeaderTitle(null);

		Long formIdType = this.getLongRequestParam(RequestParam.FORM_TYPE_ID);
		String formType = this.getStringRequestParam(RequestParam.FORM_TYPE);

		if (this.accessBean.getFormDefinitionsCanCreateInstanceOfSorted() == null) return;

		Form formDefWithNewInstanceAccess =
				this.accessBean.getFormDefinitionsCanCreateInstanceOfSorted().stream()
				.filter(itm -> itm.getFormType().equals(formType) || itm.getFormTypeId().equals(formIdType))
				.findFirst()
				.orElse(null);
		if (formDefWithNewInstanceAccess == null) {
			FacesMessage fMsg = new FacesMessage(
					FacesMessage.SEVERITY_ERROR, "Failed.", String.format(
							"You do not have access to '%s'.", formType));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
			return;
		}

		FluidItem newFluidItem = new FluidItem(new Form(formDefWithNewInstanceAccess.getFormType()));
		newFluidItem.getForm().setFormTypeId(formDefWithNewInstanceAccess.getFormTypeId());
		WorkspaceFluidItem newItem = new WorkspaceFluidItem(new PersonalInventoryItemVO(newFluidItem));
		try {
			this.openFormBean.startConversation();
			this.actionOpenForm(newItem);
			this.currentlyHaveItemOpen = true;
		} catch (Exception except) {
			this.raiseError(except);
		}
	}
}
