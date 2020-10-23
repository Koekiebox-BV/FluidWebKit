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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.uq;

import com.fluidbpm.fluidwebkit.backing.bean.workspace.ABaseWorkspaceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.WebKitViewContentModelBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.menu.WebKitMenuBean;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import com.fluidbpm.ws.client.FluidClientException;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Bean storing personal inventory related items.
 */
@SessionScoped
@Named("webKitUserQueryBean")
public class WebKitUserQueryBean extends ABaseWorkspaceBean<UserQueryItemVO, ContentViewUQ> {

	@Getter
	@Setter
	private String emptyMessage;

	@Inject
	private WebKitViewContentModelBean webKitViewContentModelBean;

	public static final String TGM_TABLE_PER_VIEW_SECTION_FORMAT = "%s - %s";
	public static final String TGM_TABLE_PER_VIEW_SECTION_DEL = " - ";
	public static final int TGM_TABLE_PER_VIEW_SECTION_DEL_LEN = TGM_TABLE_PER_VIEW_SECTION_DEL.length();

	public String actionOpenMainPageNonAjax() {
		this.actionOpenMainPage();
		return "userquery";
	}

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
	protected ContentViewUQ actionOpenMainPage(
		WebKitViewGroup webKitGroup,
		WebKitViewSub selectedSub
	) {
		try {
			if (this.getFluidClientDS() == null) {
				return null;
			}

			String userQuerySection = this.getStringRequestParam(WebKitMenuBean.ReqParam.USER_QUERY_SECTION);
			String userQueryLabel = this.getStringRequestParam(WebKitMenuBean.ReqParam.USER_QUERY_LABEL);
			Long userQueryId = this.getLongRequestParam(WebKitMenuBean.ReqParam.USER_QUERY_ID);

			ContentViewUQ contentViewUserQuery = new ContentViewUQ(this.getLoggedInUser(), this.webKitViewContentModelBean);
			contentViewUserQuery.setFluidItemsLazyModel(
					new WorkspaceUserQueryLDM(this.getFluidClientDS(), userQueryId, userQueryLabel, this));
			contentViewUserQuery.refreshData(null);
			return contentViewUserQuery;
		} catch (Exception fce) {
			if (fce instanceof FluidClientException) {
				FluidClientException casted = (FluidClientException)fce;
				if (casted.getErrorCode() == FluidClientException.ErrorCode.NO_RESULT) {
					return new ContentViewUQ(this.getLoggedInUser(), this.webKitViewContentModelBean);
				}
			}
			this.raiseError(fce);
			return new ContentViewUQ(this.getLoggedInUser(), this.webKitViewContentModelBean);
		}
	}

	@Override
	public UserQueryItemVO createABaseWebVO(
			WebKitViewGroup group,
			WebKitViewSub sub,
			WebKitWorkspaceJobView view,
			FluidItem item
	) {
		UserQueryItemVO returnVal = new UserQueryItemVO(item);
		return returnVal;
	}
}
