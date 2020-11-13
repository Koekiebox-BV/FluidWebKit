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

import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.ABaseWorkspaceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.WebKitViewContentModelBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.lf.WebKitWorkspaceLookAndFeelBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.menu.WebKitMenuBean;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQuery;
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

	@Inject
	private WebKitAccessBean accessBean;

	@Inject
	private WebKitMenuBean webKitMenuBean;

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
	public void actionOpenForm(WorkspaceFluidItem workspaceFluidItem) {
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
		String userQueryLabel = this.getStringRequestParam(WebKitMenuBean.ReqParam.USER_QUERY_LABEL);
		String sectionName = String.format("User Query - %s", userQueryLabel);
		String userQuerySection = this.getStringRequestParam(WebKitMenuBean.ReqParam.USER_QUERY_SECTION);
		try {
			if (this.getFluidClientDS() == null) {
				return null;
			}
			Long userQueryId = this.getLongRequestParam(WebKitMenuBean.ReqParam.USER_QUERY_ID);
			WebKitUserQuery wkUserQuery = this.webKitMenuBean.getWebKitUserQueryWithId(userQueryId);

			ContentViewUQ contentViewUserQuery = new ContentViewUQ(
					this.getLoggedInUser(),
					sectionName,
					wkUserQuery,
					this.webKitViewContentModelBean,
					this.accessBean);
			contentViewUserQuery.setFluidItemsLazyModel(
					new WorkspaceUserQueryLDM(this.getFluidClientDS(), userQueryId, userQueryLabel, this));
			contentViewUserQuery.mapColumnModel();
			contentViewUserQuery.refreshData(null);
			return contentViewUserQuery;
		} catch (Exception fce) {
			if (fce instanceof FluidClientException) {
				FluidClientException casted = (FluidClientException)fce;
				if (casted.getErrorCode() == FluidClientException.ErrorCode.NO_RESULT) {
					return new ContentViewUQ(
							this.getLoggedInUser(),
							sectionName,
							null,
							this.webKitViewContentModelBean,
							this.accessBean);
				}
			}
			this.raiseError(fce);
			return new ContentViewUQ(
					this.getLoggedInUser(),
					sectionName,
					null,
					this.webKitViewContentModelBean,
					this.accessBean);
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

	public String getSectionName() {
		if (this.contentView.getSections() == null || this.contentView.getSections().length < 1) return null;
		return this.contentView.getSections()[0];
	}
}
