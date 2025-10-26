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

import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.form.IConversationCallback;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQuery;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import lombok.Getter;
import lombok.Setter;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Bean storing user query related items for top bar.
 */
@SessionScoped
@Named("webKitUserQueryTopBarBean")
public class WebKitUserQueryTopBarBean extends WebKitUserQueryBean implements IConversationCallback {

	@Getter
	@Setter
	private UserQuery executingUserQuery;
	
	@Getter
	@Setter
	private WebKitUserQuery executingUserQueryWebKit;

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

	public void actionPrepToSearchByTopBar() {
		this.setExecutingUserQueryWebKit(null);
		this.setExecutingUserQuery(null);

		Long userQueryId = this.getLongRequestParam(RequestParam.USER_QUERY_ID);
		String userQuery = this.getStringRequestParam(RequestParam.USER_QUERY);
		boolean retainCurrentUQ = this.getBooleanRequestParam(RequestParam.RETAIN_USER_QUERY);
		this.setDialogHeaderTitle(String.format("Search - %s", userQuery));

		try {
			UserQuery byId = this.accessBean.getUserQueryCanExecuteById(userQueryId);
			if (byId == null) throw new ClientDashboardException(
					String.format("Not allowed to execute '%d'.", byId), ClientDashboardException.ErrorCode.VALIDATION);
			WebKitUserQuery wkUserQueryWithName = this.webKitWorkspaceLookAndFeelBean.getUserQueryWithName(userQuery);
			if (wkUserQueryWithName == null) wkUserQueryWithName = new WebKitUserQuery();

			this.setExecutingUserQueryWebKit(wkUserQueryWithName);
			this.setExecutingUserQuery(byId);

			//Load the complete config...
			this.actionOpenMainPage();

			//Populate the model for input...
			this.getInputFieldValues();
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public void actionReCaptureInput() {
		//Refresh the existing content view...
		//this.getContentView().mapColumnModel();
		//this.getContentView().refreshData(null);
	}

	public void actionExecuteUserQueryFromTopBar(String toHideIfAnyResults, String toDisplayIfAnyResults) {
		long now = System.currentTimeMillis();
		try {
			Map<String, List<ColumnModel>> completeColMap = this.getContentView() == null ?
					null : this.getContentView().getSectionFilterableColumns();

			Map<String, Map<String, Date>> filterMapDate = this.getContentView() == null ?
					null : this.getContentView().getFilterByDateValueMap();
			Map<String, Map<String, Double>> filterMapDouble = this.getContentView() == null ?
					null : this.getContentView().getFilterByDecimalValueMap();
			Map<String, Map<String, String>> filterMapText = this.getContentView() == null ?
					null : this.getContentView().getFilterByTextValueMap();
			Map<String, Map<String, String>> filterMapTextEncrypted = this.getContentView() == null ?
					null : this.getContentView().getFilterByTextEncryptedValueMap();
			Map<String, Map<String,String[]>> filterMapSelectItm = this.getContentView() == null ?
					null : this.getContentView().getFilterBySelectItemMap();

			String userQueryLabel = this.executingUserQuery.getName();
			//String sectionName = String.format("User Query - %s", userQueryLabel);

			ContentViewUQ contentViewUserQuery = new ContentViewUQ(
				this.getLoggedInUser(),
				this.getSectionName(),
				this.getExecutingUserQueryWebKit(),
				this.getWebKitViewContentModelBean(),
				this.accessBean
			);
			//View set to a new Content View for User Query...
			this.contentView = contentViewUserQuery;

			contentViewUserQuery.setSectionFilterableColumns(completeColMap);

			contentViewUserQuery.setFilterByDateValueMap(filterMapDate);
			contentViewUserQuery.setFilterByDecimalValueMap(filterMapDouble);
			contentViewUserQuery.setFilterByTextValueMap(filterMapText);
			contentViewUserQuery.setFilterByTextEncryptedValueMap(filterMapTextEncrypted);
			contentViewUserQuery.setFilterBySelectItemMap(filterMapSelectItm);
			
			//TODO contentViewUserQuery.mapColumnModel();
			contentViewUserQuery.refreshData(null);

			contentViewUserQuery.setFluidItemsLazyModel(new WorkspaceUserQueryLDM(
					this.getFluidClientDS(),
					this.getExecutingUserQuery().getId(),
					userQueryLabel,
					this)
			);
			
			//Map the input criteria...
			List<Field> inputFields = this.getInputFieldValues();

			if (UtilGlobal.isNotBlank(toHideIfAnyResults, toDisplayIfAnyResults)) {
				this.executeJavaScript(
						String.format("PF('%s').hide();PF('%s').show();",
								toHideIfAnyResults, toDisplayIfAnyResults));
			}
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	@Override
	public void actionDeleteForm(WorkspaceFluidItem workspaceFluidItem) {
		try {
			if (this.getFluidClientDS() == null) return;
			if (workspaceFluidItem.getFluidItemForm() == null) return;

			FormContainerClient fcc = this.getFluidClientDS().getFormContainerClient();
			fcc.deleteFormContainer(workspaceFluidItem.getFluidItemForm());

			//Refresh the data... No need top update the whole configuration...
			this.actionExecuteUserQueryFromTopBar(UtilGlobal.EMPTY, UtilGlobal.EMPTY);

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success.", String.format("Deleted '%s'.", workspaceFluidItem.getFluidItemTitle()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	@Override
	protected String getAreaToUpdateAfterSave() {
		return ":frmExecuteTopUserQueryResults:dataTableUserQueryTopBar";
	}
}
