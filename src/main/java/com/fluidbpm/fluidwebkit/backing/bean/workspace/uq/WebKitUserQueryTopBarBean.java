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

import com.fluidbpm.fluidwebkit.backing.bean.workspace.form.IConversationCallback;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQuery;
import lombok.Getter;
import lombok.Setter;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

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
		this.setDialogHeaderTitle(String.format("Search - %s", userQuery));

		try {
			UserQuery byId = this.accessBean.getUserQueryCanExecuteById(userQueryId);
			if (byId == null) throw new ClientDashboardException(
					String.format("Not allowed to execute '%d'.", byId), ClientDashboardException.ErrorCode.VALIDATION);
			WebKitUserQuery wkUserQueryWithName = this.webKitWorkspaceLookAndFeelBean.getUserQueryWithName(userQuery);
			if (wkUserQueryWithName == null) wkUserQueryWithName = new WebKitUserQuery();

			this.setExecutingUserQueryWebKit(wkUserQueryWithName);
			this.setExecutingUserQuery(byId);

			//Set the Content View...
			this.actionOpenMainPage();
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	public void actionExecuteUserQueryFromTopBar(String toHideIfAnyResults, String toDisplayIfAnyResults) {
		long now = System.currentTimeMillis();
		try {
			List<Field> inputFields = this.getInputFieldValues();
			int resultCount = 1;

			this.executeJavaScript(String.format("PF('%s').hide();PF('%s').show();",
					toHideIfAnyResults, toDisplayIfAnyResults));
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success.", String.format("Total of %d results in %d millis.",
					resultCount, System.currentTimeMillis() - now));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

}
