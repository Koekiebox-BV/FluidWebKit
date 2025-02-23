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

	@Override
	public String getDestinationNavigationForCancel() {
		return "userquery";
	}

	@Override
	public void actionPreRenderProcess() {
		String freshLookup = this.getStringRequestParam(RequestParam.FRESH_LOOKUP);
		if (UtilGlobal.isNotBlank(freshLookup)) {
			this.currentlyHaveItemOpen = false;
			this.currentOpenFormTitle = null;
			this.dialogDisplay = false;
		}
		//this.actionOpenMainPage();
	}

	@Override
	@PostConstruct
	public void actionPopulateInit() {
		//Do nothing...
		//this.actionOpenMainPage();
	}

	@Override
	public WorkspaceFluidItem actionOpenForm(WorkspaceFluidItem workspaceFluidItem) {
		return this.openFormBean.actionFreshLoadFormAndSet(workspaceFluidItem);
	}

	@Override
	public void actionOpenMainPage() {
		this.contentView = this.actionOpenMainPage(null, null);
	}

	/**
	 * Open an 'Form' for editing or viewing.
	 * Custom functionality needs to be placed in {@code this#actionOpenFormForEditingFromWorkspace}.
	 *
	 * @see #actionOpenForm(WorkspaceFluidItem)
	 */
	@Override
	public WorkspaceFluidItem actionOpenFormForEditingFromWorkspace(WorkspaceFluidItem wfItem) {
		this.setAreaToUpdateForDialogAfterSubmit(null);
		this.currentOpenFormTitle = null;
		this.currentlyHaveItemOpen = false;
		// Dialog or Workspace for form layout;
		this.dialogDisplay = !this.isOpenItemInWorkspace(wfItem);

		try {
			this.openFormBean.startConversation();
			this.openFormBean.setAreaToUpdateAfterSave(this.getAreaToUpdateAfterSave());
			this.openFormBean.setConversationCallback(this);
			WorkspaceFluidItem returnVal = this.actionOpenForm(wfItem);

			if (this.dialogDisplay) {
				// Now open:
				this.executeJavaScript("PF('varFormDialog').show();");
			} else {
				// Only set the title if not dialog display:
				this.currentOpenFormTitle = wfItem.getFluidItemTitle();
				this.openFormBean.setAreaToUpdateAfterSave(":panelBreadcrumb: :panelWorkspace");
			}
			this.currentlyHaveItemOpen = true;
			return returnVal;
		} catch (Exception except) {
			this.raiseError(except);
			return null;
		}
	}

	protected String getAreaToUpdateAfterSave() {
		return ":formUserQueryResults";
	}

	@Override
	public void afterSaveProcessing(WorkspaceFluidItem workspaceItemSaved) {
		this.dialogDisplay = false;
		this.currentlyHaveItemOpen = false;
		this.currentOpenFormTitle = null;
	}

	@Override
	public void closeFormDialog() {
		this.actionCloseOpenForm();
	}

	@Override
	public void afterSendOnProcessing(WorkspaceFluidItem workspaceItemSaved) {
		
	}

	@Override
	protected ContentViewUQ actionOpenMainPage(WebKitViewGroup webKitGroup, WebKitViewSub selectedSub) {
		String userQueryLabel = this.getUserQueryLabel();
		String sectionName = String.format("User Query - %s", userQueryLabel);
		
		try {
			if (this.getFluidClientDS() == null) return null;

			String userQuerySec = this.getUserQuerySection();
			Long userQueryId = this.getUserQueryId();
			if (UtilGlobal.isNotBlank(userQuerySec, userQueryLabel) && userQueryId > 0L) {
				this.openPageLastCache = new OpenPageLastCache(
						userQuerySec,
						userQuerySec,
						"",
						userQueryLabel,
						userQueryId
				);
			}

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
					if (UtilGlobal.isBlank(fluidFieldName)) {
						this.getLogger().warn(String.format("Section [%s] has an empty field name! ", sectionName));
						return;
					}

					if (this.getContentView().getFilterByTextValueMap().get(sectionName) != null &&
							this.getContentView().getFilterByTextValueMap().get(sectionName).containsKey(fluidFieldName)) {
						String value = this.getContentView().getFilterByTextValueMap().get(sectionName).get(fluidFieldName);
						if (!UtilGlobal.isBlank(value)) {
							inputFields.add(new Field(fluidFieldName, value, Field.Type.Text));
						}
					} else if (this.getContentView().getFilterByTextEncryptedValueMap().get(sectionName) != null &&
							this.getContentView().getFilterByTextEncryptedValueMap().get(sectionName).containsKey(fluidFieldName)) {
						String value = this.getContentView().getFilterByTextEncryptedValueMap().get(sectionName).get(fluidFieldName);
						if (!UtilGlobal.isBlank(value)) {
							inputFields.add(new Field(fluidFieldName, value, Field.Type.TextEncrypted));
						}
					} else if (this.getContentView().getFilterByDecimalValueMap().containsKey(fluidFieldName)) {
						Double val =  this.getContentView().getFilterByDecimalValueMap().get(sectionName).get(fluidFieldName);
						if (val != null && val.doubleValue() > 0) {
							inputFields.add(new Field(fluidFieldName, val, Field.Type.Decimal));
						}
					} else if (this.getContentView().getFilterBySelectItemMap().get(sectionName) != null &&
							this.getContentView().getFilterBySelectItemMap().get(sectionName).containsKey(fluidFieldName)) {
						Object valueForSelItm = this.getContentView().getFilterBySelectItemMap().get(sectionName).get(fluidFieldName);
						if (valueForSelItm instanceof String) {
							inputFields.add(
									new Field(fluidFieldName, new MultiChoice((String)valueForSelItm), Field.Type.MultipleChoice));
						} else {
							String[] value = (String[])valueForSelItm;
							if (value != null && value.length > 0) {
								inputFields.add(new Field(fluidFieldName, new MultiChoice(value), Field.Type.MultipleChoice));
							}
						}
					} else {
						this.getLogger().warn("Input with name [%s] is not mapped for capture on section [%s].",
								fluidFieldName, sectionName);
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

	public String getUserQuerySection() {
		String returnVal = this.getStringRequestParam(WebKitMenuBean.ReqParam.USER_QUERY_SECTION);
		if (UtilGlobal.isBlank(returnVal) && this.openPageLastCache != null) {
			return this.openPageLastCache.getClickedGroup();
		}
		return returnVal;
	}

	public String getUserQueryLabel() {
		String returnVal = this.getStringRequestParam(WebKitMenuBean.ReqParam.USER_QUERY_LABEL);
		if (UtilGlobal.isBlank(returnVal) && this.openPageLastCache != null) {
			return this.openPageLastCache.getClickedSubAlias();
		}
		return returnVal;
	}

	public Long getUserQueryId() {
		Long uqId = this.getLongRequestParam(WebKitMenuBean.ReqParam.USER_QUERY_ID);
		if (uqId < 1L &&
				(this.openPageLastCache != null && this.openPageLastCache.getUserQueryId() != null)) {
			return this.openPageLastCache.getUserQueryId();
		}
		return uqId;
	}

}
