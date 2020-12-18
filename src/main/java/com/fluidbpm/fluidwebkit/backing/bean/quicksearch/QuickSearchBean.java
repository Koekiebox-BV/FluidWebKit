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

package com.fluidbpm.fluidwebkit.backing.bean.quicksearch;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.login.ProfileBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.form.WebKitOpenFormConversationBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.pi.PersonalInventoryItemVO;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.form.FormListing;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.primefaces.event.SelectEvent;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@SessionScoped
@Named("webKitQuickSearchBean")
public class QuickSearchBean extends ABaseManagedBean {
	private QuickSearchResultVO selectedSearchResult;

	@Inject
	private ProfileBean profileBean;

	@Inject
	protected WebKitOpenFormConversationBean openFormBean;

	@NoArgsConstructor
	@Getter
	@Setter
	public static final class QuickSearchResultVO implements Serializable {
		private Long id;
		private String title;

		private Date dateCreated;
		private Date dateLastUpdated;
		private String activeUser;
		private String formContainerFlowState;
		private String formContainerState;
		private String formContainerType;

		private SimpleDateFormat dateFormat;

		public QuickSearchResultVO(
			Form formContainerParam,
			SimpleDateFormat dateFormat
		) {
			this.dateFormat = dateFormat;
			this.convertToSearchResult(formContainerParam);
		}

		public List<QuickSearchResultNameAndValueVO> getValues() {
			List<QuickSearchResultNameAndValueVO> returnVal = new ArrayList();
			returnVal.add(new QuickSearchResultNameAndValueVO("Active User",this.activeUser));
			returnVal.add(new QuickSearchResultNameAndValueVO("State", this.formContainerState));
			returnVal.add(new QuickSearchResultNameAndValueVO("Flow State",
					this.formContainerFlowState == null ? "[No Flow]" : this.formContainerFlowState));
			returnVal.add(new QuickSearchResultNameAndValueVO("Created", dateFormat.format(this.dateCreated)));
			returnVal.add(new QuickSearchResultNameAndValueVO("Type", this.formContainerType));
			return returnVal;
		}

		public QuickSearchResultVO convertToSearchResult(Form formContainerParam) {
			if (formContainerParam == null) {
				return null;
			}

			this.setId(formContainerParam.getId());
			this.setTitle(formContainerParam.getTitle());

			User currentUser = formContainerParam.getCurrentUser();
			this.setActiveUser(((currentUser == null || currentUser.getUsername().trim().isEmpty())
							? UtilGlobal.NONE: currentUser.getUsername()));

			this.setDateCreated(formContainerParam.getDateCreated());
			this.setDateLastUpdated(formContainerParam.getDateLastUpdated());
			this.setFormContainerFlowState(formContainerParam.getFlowState());
			this.setFormContainerState(formContainerParam.getState());
			this.setFormContainerType(formContainerParam.getFormType());
			return this;
		}
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	@Setter
	public static final class QuickSearchResultNameAndValueVO implements Serializable {
		private String name;
		private String value;
	}

	public QuickSearchBean() {
		super();
	}

	public List<QuickSearchResultVO> actionCompletePossibleSearchResults(String queryParam) {
		List<QuickSearchResultVO> returnValues = new ArrayList();
		//No need to auto-complete...
		if (queryParam == null || queryParam.trim().isEmpty()) return returnValues;

		if (this.getFluidClientDS() == null) return returnValues;

		FormContainerClient ffClient = this.getFluidClientDS().getFormContainerClient();
		FormListing listing = null;
		try {
			listing = ffClient.getFormContainersByTitleContains(queryParam, 20, 0);
		} catch (Exception except) {
			if (except instanceof FluidClientException) {
				FluidClientException casted = (FluidClientException)except;
				if (FluidClientException.ErrorCode.NO_RESULT == casted.getErrorCode()) {
					return returnValues;
				}
			}
			this.raiseError(except);
			return returnValues;
		}

		SimpleDateFormat sdf = new SimpleDateFormat(this.getDateAndTimeFormat());
		for (Form formAtIndex : listing.getListing()) returnValues.add(new QuickSearchResultVO(formAtIndex, sdf));

		return returnValues;
	}

	public void actionOnItemSelect(SelectEvent eventParam) {
		QuickSearchResultVO selected = this.getSelectedSearchResult();
		if (selected == null) {
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_WARN,
				"No item selected on search result.","");
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
			return;
		}

		Form dataToSend = new Form();

		dataToSend.setId(selected.getId());
		dataToSend.setFormType(selected.getFormContainerType());
		dataToSend.setCurrentUser(this.getLoggedInUser());
		this.setDialogHeaderTitle(null);

		WorkspaceFluidItem newItem = new WorkspaceFluidItem(
				new PersonalInventoryItemVO(new FluidItem(dataToSend)));
		try {
			this.openFormBean.startConversation();
			this.openFormBean.actionFreshLoadFormAndSet(newItem);
			this.executeJavaScript("PF('varFormDialog').show();");
		} catch (Exception except) {
			this.raiseError(except);
		}

		//4. Clear the selected result...
		this.setSelectedSearchResult(null);
	}

	public QuickSearchResultVO getSelectedSearchResult() {
		return this.selectedSearchResult;
	}

	public void setSelectedSearchResult(QuickSearchResultVO selectedSearchResult) {
		this.selectedSearchResult = selectedSearchResult;
	}
}
