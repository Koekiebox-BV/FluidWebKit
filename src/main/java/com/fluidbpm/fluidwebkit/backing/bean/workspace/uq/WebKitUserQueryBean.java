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

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.ABaseWorkspaceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.WebKitViewContentModelBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.form.IConversationCallback;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.lf.WebKitWorkspaceLookAndFeelBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.menu.WebKitMenuBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.field.MultiChoice;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bean storing user query related items.
 */
@SessionScoped
@Named("webKitUserQueryBean")
public class WebKitUserQueryBean extends ABaseWorkspaceBean<UserQueryItemVO, ContentViewUQ> implements
		IConversationCallback {

	@Getter
	@Setter
	private String emptyMessage;

	@Inject
	@Getter
	private WebKitViewContentModelBean webKitViewContentModelBean;

	@Inject
	protected WebKitAccessBean accessBean;

	@Inject
	private WebKitMenuBean webKitMenuBean;

	@Inject
	protected WebKitWorkspaceLookAndFeelBean webKitWorkspaceLookAndFeelBean;

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
		this.openFormBean.actionFreshLoadFormAndSet(workspaceFluidItem);
	}

	@Override
	public void actionOpenMainPage() {
		this.contentView = this.actionOpenMainPage(null, null);
	}

	/**
	 * Open an 'Form' for editing or viewing.
	 * Custom functionality needs to be placed in {@code this#actionOpenFormForEditingFromWorkspace}.
	 *
	 * @see this#actionOpenForm(WorkspaceFluidItem)
	 */
	public void actionOpenFormForEditingFromWorkspace(WorkspaceFluidItem workspaceFluidItem) {
		this.setAreaToUpdateForDialogAfterSubmit(null);
		try {
			this.openFormBean.startConversation();
			this.openFormBean.setAreaToUpdateAfterSave(this.getAreaToUpdateAfterSave());
			this.openFormBean.setConversationCallback(this);
			this.actionOpenForm(workspaceFluidItem);
			this.currentlyHaveItemOpen = true;
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	protected String getAreaToUpdateAfterSave() {
		return ":formUserQueryResults";
	}

	@Override
	public void afterSaveProcessing(WorkspaceFluidItem workspaceItemSaved) {
		//No Need to do anything after a save...
		//this.getContentView().getFluidItemsLazyModel().load()
	}

	@Override
	protected ContentViewUQ actionOpenMainPage(
		WebKitViewGroup webKitGroup,
		WebKitViewSub selectedSub
	) {
		String userQueryLabel = this.getStringRequestParam(WebKitMenuBean.ReqParam.USER_QUERY_LABEL);
		String sectionName = String.format("User Query - %s", userQueryLabel);
		
		try {
			if (this.getFluidClientDS() == null) return null;

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
		if (this.contentView == null) return null;
		if (this.contentView.getSections() == null || this.contentView.getSections().length < 1) return null;
		return this.contentView.getSections()[0];
	}

	public List<Field> getInputFieldValues() {
		String sectionName = this.getSectionName();
		List<Field> inputFields = new ArrayList<>();
		if (this.getContentView() == null) return inputFields;

		Map<String, List<ColumnModel>> completeColMap = this.getContentView().getColumnModelsFilterable();
		if (sectionName != null && (completeColMap != null && completeColMap.containsKey(sectionName))) {
			List<ABaseManagedBean.ColumnModel> columnModels = completeColMap.get(sectionName);
			if (columnModels != null) {
				columnModels.forEach(clmItm -> {
					String fluidFieldName = clmItm.getFluidFieldName();
					if (this.getContentView().getFilterByTextValueMap().get(sectionName).containsKey(fluidFieldName)) {
						String value = this.getContentView().getFilterByTextValueMap().get(sectionName).get(fluidFieldName);
						if (!UtilGlobal.isBlank(value)) inputFields.add(new Field(fluidFieldName, value, Field.Type.Text));
					} else if (this.getContentView().getFilterByDecimalValueMap().containsKey(fluidFieldName)) {
						Double val =  this.getContentView().getFilterByDecimalValueMap().get(sectionName).get(fluidFieldName);
						if (val != null && val.doubleValue() > 0) inputFields.add(new Field(fluidFieldName, val, Field.Type.Decimal));
					} else if (this.getContentView().getFilterBySelectItemMap().get(sectionName).containsKey(fluidFieldName)) {
						String[] value = this.getContentView().getFilterBySelectItemMap().get(sectionName).get(fluidFieldName);
						if (value != null && value.length > 0) inputFields.add(new Field(fluidFieldName, new MultiChoice(value), Field.Type.MultipleChoice));
					}
				});
			}
		}
		return inputFields;
	}

	public int getEnabledNumberOfUserQueriesCanExecute() {
		return this.getEnabledUserQueriesCanExecuteSorted().size();
	}

	public List<UserQuery> getEnabledUserQueriesCanExecuteSorted() {
		if (this.accessBean.getNumberOfUserQueriesCanExecute() < 1) return new ArrayList<>();

		List<UserQuery> uqEnabled = new ArrayList<>();
		this.accessBean.getUserQueriesCanExecuteSorted().stream().forEach(uqCanExec -> {
			WebKitUserQuery wkUserQueryWithName = this.webKitWorkspaceLookAndFeelBean.getUserQueryWithName(uqCanExec.getName());
			if (wkUserQueryWithName == null) return;

			if (wkUserQueryWithName.isEnableForTopBar()) uqEnabled.add(uqCanExec);
		});
		return uqEnabled;
	}

}
