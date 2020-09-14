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

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.ABaseContentView;
import com.fluidbpm.fluidwebkit.backing.utility.Globals;
import com.fluidbpm.fluidwebkit.backing.vo.ABaseWebVO;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.flow.JobViewListing;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.item.FluidItemListing;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.userquery.UserQueryListing;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.flow.FlowStepClient;
import com.fluidbpm.ws.client.v1.flowitem.FlowItemClient;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import com.fluidbpm.ws.client.v1.sqlutil.sqlnative.SQLUtilWebSocketExecuteNativeSQLClient;
import com.fluidbpm.ws.client.v1.sqlutil.wrapper.SQLUtilWebSocketRESTWrapper;
import com.fluidbpm.ws.client.v1.userquery.UserQueryClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Bean to handle a workspace.
 * The base class for the custom Dashboard workspace.
 * 
 * @author jasonbruwer on 2017-12-30
 * @since 1.0
 */
public abstract class ABaseWorkspaceBean<T extends ABaseWebVO, J extends ABaseContentView> extends ABaseManagedBean {
	private JobViewListing jobViewListing = new JobViewListing();
	protected UserQueryListing userQueryListing = new UserQueryListing();
	//Views...
	protected List<JobView> viewsAll;
	protected Map<String, List<JobView>> viewGroupPlacings;

	//Selected Content View...
	@Getter
	protected J contentView;

	//Config...
	protected boolean currentlyHaveItemOpen;
	protected OpenPageLastCache openPageLastCache;

	private String areaToUpdateForDialogAfterSubmit;

	/**
	 * Cached items in case of a page refresh.
	 */
	@AllArgsConstructor
	@NoArgsConstructor
	private static class OpenPageLastCache implements Serializable {
		public String workspaceItemAim;
		public String selectedJobViews;

		/**
		 * Verify if cache should be used in the event of {@code workspaceItemAimParam} and {@code selectedJobViewsParam}
		 * not being set.
		 *
		 * @param workspaceItemAimParam The aim of the action fired.
		 * @param selectedJobViewsParam The comma separated list of selected views.
		 * 
		 * @return {@code true} if {@code workspaceItemAimParam} and {@code selectedJobViewsParam} is not {@code null}.
		 */
		public static boolean shouldUseCache(String workspaceItemAimParam, String selectedJobViewsParam) {
			return (workspaceItemAimParam == null && selectedJobViewsParam == null);
		}
	}

	/**
	 * Request parameter names got HTTP request mappings.
	 */
	public static class RequestParam {
		public static final String SELECTED_JOB_VIEWS = "selectedJobViews";
		public static final String WORKSPACE_AIM = "workspaceItemAim";
		public static final String OPENED_FROM_VIEW = "openedFromView";
		public static final String TO_UPDATE = "toUpdate";
	}

	/**
	 * Initial event when JSF Bean is initialised.
	 * Each of the View Groups will be mapped to {@code this.viewGroupPlacings}.
	 * Further customised grouping may be done by overriding the {@code this#getViewGroupForUI(String,String)} method.
	 * 
	 * The following will be loaded;
	 * -> The {@code JobView}'s the logged in user has access to.
	 * -> The list of {@code UserQuery}'s the logged in user has access to.
	 *
	 * @see com.fluidbpm.program.api.vo.userquery.UserQuery
	 * @see JobView
	 */
	@PostConstruct
	public void actionPopulateInit() {
		if (this.getFluidClientDS() == null) {
			return;
		}

		FlowStepClient flowStepClient = this.getFluidClientDS().getFlowStepClient();
		UserQueryClient userQueryClient = this.getFluidClientDS().getUserQueryClient();
		try {
			this.jobViewListing = flowStepClient.getJobViewsByLoggedInUser();
			this.userQueryListing = userQueryClient.getAllUserQueriesByLoggedInUser();

			//When not empty...
			if (this.jobViewListing != null && !this.jobViewListing.isListingEmpty()) {
				this.viewsAll = new ArrayList<>();
				this.viewGroupPlacings = new HashMap<>();

				for (JobView jobViewIter : this.jobViewListing.getListing()) {
					this.viewsAll.add(jobViewIter);

					String viewName = jobViewIter.getViewName();
					String viewGroup = jobViewIter.getViewGroupName();
					String uiViewGroup = this.getViewGroupForUI(viewGroup, viewName);
					if (uiViewGroup == null || uiViewGroup.isEmpty()) {
						continue;
					}

					List<JobView> jobViews = this.viewGroupPlacings.get(uiViewGroup);
					if (jobViews == null) {
						jobViews = new ArrayList<>();
					}
					jobViews.add(jobViewIter);
					this.viewGroupPlacings.put(uiViewGroup, jobViews);
				}

				//Sort by 'View Priority'...
				this.viewGroupPlacings.values().forEach(listingItm -> {
					Collections.sort(listingItm, Comparator.comparing(JobView::getViewPriority));
				});
			}
		} catch (Exception fce) {
			this.raiseError(fce);
		}
	}

	/**
	 * Over-ride this method to change the default behaviour when assigning views to groups.
	 *
	 * @param viewGroupParam The name of the group as configured in Fluid.
	 * @param viewParam The name of the group as configured in Fluid.
	 *
	 * @return {@code String} - Will always return {@code viewGroupParam}.
	 */
	protected String getViewGroupForUI(String viewGroupParam, String viewParam) {
		return viewGroupParam;
	}

	/**
	 * Get the list of {@code JobView} Ids as String where groups {@code uiGroupsParam}.
	 *
	 * @param uiGroupsParam The array of groups to fetch {@code JobView} listing for.
	 * @return The comma separated list of {@code JobView} ids which belongs to {@code uiGroupsParam}.
	 */
	public String getQueryStringForUIGroups(String ... uiGroupsParam) {
		if (uiGroupsParam == null || uiGroupsParam.length == 0) {
			return null;
		}
		if (this.viewGroupPlacings == null) {
			return null;
		}

		List<JobView> forAll = new ArrayList<>();
		for (String uiGroupParam : uiGroupsParam) {
			List<JobView> localGroup = null;
			if ((localGroup = this.viewGroupPlacings.get(uiGroupParam)) != null) {
				forAll.addAll(localGroup);
			}
		}

		return this.getViewsFor(forAll);
	}

	/**
	 * Get the view query string from {@code jobViewsParam}.
	 * 
	 * @param jobViewsParam The {@code JobView}'s to create query text from.
	 * @return String - Comma separated list of {@code JobView} Ids for {@code jobViewsParam}.
	 */
	public String getViewsFor(List<JobView> jobViewsParam) {
		StringBuffer returnVal = new StringBuffer();

		//Maker Views...
		if (jobViewsParam != null && !jobViewsParam.isEmpty()) {
			jobViewsParam.forEach(reviewItm -> {
				returnVal.append(reviewItm.getId().toString());
				returnVal.append(",");
			});
		}

		String returnValStr = returnVal.toString();
		if (returnValStr.trim().isEmpty()) {
			return returnValStr.trim();
		}
		returnValStr = returnValStr.substring(0, returnValStr.length() -1);

		try {
			return URLEncoder.encode(returnValStr, Globals.CHARSET_UTF8);
		} catch (UnsupportedEncodingException eParam) {
			return returnValStr;
		}
	}

	/**
	 * JSF action to fire when a form close action is fired.
	 * When the main page for the workspace is opened or refreshed.
	 *
	 * oncomplete="PF('panelToClose').close();"
	 */
	public void actionCloseOpenForm() {
		this.actionOpenMainPage();
	}

	/**
	 * Open an 'Form' for editing or viewing.
	 * Custom functionality needs to be placed in {@code this#actionOpenFormForEditingFromWorkspace}.
	 *
	 * @see this#actionOpenFormForEditingFromWorkspace(JobView, Long)
	 */
	public void actionOpenFormForEditingFromWorkspace() {
		this.setAreaToUpdateForDialogAfterSubmit(null);
		this.currentlyHaveItemOpen = false;

		Long formIdToUpdate = this.getLongRequestParam(RequestParam.TO_UPDATE);
		Long openedFromViewId = this.getLongRequestParam(RequestParam.OPENED_FROM_VIEW);
		JobView fromView = this.getJobViewFromListingWithId(this.viewsAll, openedFromViewId);

		try {
			this.actionOpenFormForEditingFromWorkspace(fromView, formIdToUpdate);
			this.currentlyHaveItemOpen = true;
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	/**
	 * Custom functionality for when a Form is clicked on to be opened.
	 * Open an 'Form' for editing or viewing.
	 *
	 * @param fromView Opened from view.
	 * @param formIdToUpdateParam Form ID to update.
	 */
	public abstract void actionOpenFormForEditingFromWorkspace(
		JobView fromView,
		Long formIdToUpdateParam
	);

	public String actionOpenMainPageNonAjax() {
		this.actionOpenMainPage();
		return "workspace";
	}

	/**
	 * When the main page for the workspace is opened or refreshed.
	 * Custom sub {@code ABaseWebVO} object may be created via {@code createABaseWebVO}.
	 *
	 * The {@code actionOpenMainPage} will create the 'Content View' while the
	 * {@code createABaseWebVO} method will map each one of the custom objects.
	 *
	 * @see this#actionOpenMainPage(String)
	 * @see this#createABaseWebVO(FluidItem)
	 */
	public void actionOpenMainPage() {
		this.areaToUpdateForDialogAfterSubmit = null;

		String selectedJobViews = this.getStringRequestParam(RequestParam.SELECTED_JOB_VIEWS);
		String workspaceAim = this.getStringRequestParam(RequestParam.WORKSPACE_AIM);

		//Clear the content view...
		if (this.contentView != null) {
			this.contentView.refreshData(null);
		}

		//Make use of cache...
		if (OpenPageLastCache.shouldUseCache(workspaceAim, selectedJobViews) && this.openPageLastCache != null) {
			selectedJobViews = this.openPageLastCache.selectedJobViews;
			workspaceAim = this.openPageLastCache.workspaceItemAim;
		}
		this.openPageLastCache = new OpenPageLastCache(workspaceAim, selectedJobViews);

		try {
			//Selected Job Views...
			if (selectedJobViews != null && !selectedJobViews.trim().isEmpty()) {
				selectedJobViews = URLDecoder.decode(selectedJobViews, Globals.CHARSET_UTF8);

				String[] jobViewsAsTxtArr = selectedJobViews.split("\\,");
				if (jobViewsAsTxtArr == null || jobViewsAsTxtArr.length < 1) {
					return;
				}

				this.contentView = this.actionOpenMainPage(workspaceAim);
				//Set the correct content view...
				if (this.contentView == null) {
					throw new ClientDashboardException(
							"Unable to set Content View for "+ workspaceAim+"'. Not supported.",
							ClientDashboardException.ErrorCode.VALIDATION);
				}

				List<CompletableFuture> allAsyncs = new ArrayList<>();
				final Map<JobView, List<WorkspaceFluidItem>> mappingToSet = new Hashtable<>();
				for (String jobViewIdTxt : jobViewsAsTxtArr) {
					if (jobViewIdTxt.trim().isEmpty()) {
						continue;
					}

					JobView viewToSetForm = this.getJobViewFromListingWithId(this.viewsAll, Long.getLong(jobViewIdTxt));
					if (viewToSetForm == null) {
						continue;
					}

					//Run the following asynchronous...
					CompletableFuture toAdd = CompletableFuture.runAsync(
							() -> {
								FluidItemListing listOfItems = null;
								FlowItemClient flowItemClient = this.getFluidClientDS().getFlowItemClient();
								
								final SQLUtilWebSocketRESTWrapper sqlUtilWSRESTWrapper = this.getFluidClientDSConfig().getSQLUtilWrapper();
								List<WorkspaceFluidItem> itemsForTheView = new ArrayList<>();
								try {
									listOfItems = flowItemClient.getFluidItemsForView(
											viewToSetForm,
											1000,
											0);

									if (listOfItems == null || (listOfItems.getListing() == null ||
											listOfItems.getListing().isEmpty())) {
										return;
									}

									//Add each of the items...
									listOfItems.getListing().forEach(itm -> {
										T baseWebToAdd = this.createABaseWebVO(itm);
										itemsForTheView.add(new WorkspaceFluidItem(baseWebToAdd));
									});
								} catch (FluidClientException fluidClientExcept) {
									if(fluidClientExcept.getErrorCode() == FluidClientException.ErrorCode.NO_RESULT) {
										return;
									}
									throw fluidClientExcept;
								} finally {
									//Set the items for the view...
									mappingToSet.put(viewToSetForm, itemsForTheView);

									flowItemClient.closeAndClean();
									sqlUtilWSRESTWrapper.closeAndClean();
								}
							});
					allAsyncs.add(toAdd);
				}

				//Run them all...
				CompletableFuture.allOf(allAsyncs.toArray(new CompletableFuture[]{})).join();

				//Refresh the data...
				this.contentView.refreshData(mappingToSet);
			}
		} catch (Exception except) {
			//log it...
			this.getLogger().error(except.getMessage(),except);
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Failed to fetch items for View. ", except.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} finally { /*Do nothing for now...*/ }
	}

	/**
	 * Custom functionality for when a workspace is loaded.
	 * The menu-option clicked by the user will be passed through as {@code workspaceAimParam}.
	 * 
	 * @param workspaceAimParam The specific workspace that was requested.
	 * @return ABaseContentView to use based on {@code workspaceAimParam}.
	 *
	 * @see ABaseContentView
	 */
	protected abstract J actionOpenMainPage(String workspaceAimParam);

	/**
	 * Create a custom value object based on {@code wrapperParam} and {@code fluidItemParam}.
	 * 
	 * @param fluidItemParam The Fluid {@code FluidItem} to map from.
	 *
	 * @return ABaseWebVO subclass based on {@code fluidItemParam}.
	 *
	 * @see ABaseWebVO
	 * @see SQLUtilWebSocketRESTWrapper
	 * @see FluidItem
	 */
	protected abstract T createABaseWebVO(FluidItem fluidItemParam);

	/**
	 * Find the {@code JobView} with id {@code idToGetParam} from {@code jobViewsParam}.
	 * 
	 * @param jobViewsParam The views to search through.
	 * @param idToGetParam The {@code JobView} Id of the {@code JobView} to find.
	 * @return {@code JobView} The {@code JobView} with Id {@code idToGetParam} or {@code null}.
	 *
	 * @see JobView
	 */
	protected JobView getJobViewFromListingWithId(List<JobView> jobViewsParam, Long idToGetParam) {
		if (idToGetParam == null) {
			return null;
		}

		if (jobViewsParam == null || jobViewsParam.isEmpty()) {
			return null;
		}

		//Set the Active Job View...
		if (jobViewsParam != null && !jobViewsParam.isEmpty()) {
			for(JobView view : jobViewsParam) {
				if(idToGetParam.equals(view.getId())) {
					return view;
				}
			}
		}

		return null;
	}

	/**
	 * Creates a {@code |} delimited {@code String} from {@code firstValueParam} and {@code nextValueParam}.
	 *
	 * @param firstValueParam The value left from the |.
	 * @param nextValueParam The value that will follow the |.
	 *    
	 * @return The combined value of {@code firstValueParam} and {@code nextValueParam}.
	 */
	public String addPipeIfApplicable(String firstValueParam, String nextValueParam) {
		String returnVal = firstValueParam;
		if (nextValueParam == null || nextValueParam.trim().isEmpty()) {
			return returnVal;
		}

		if (returnVal == null) {
			returnVal = "";
		}

		returnVal += " | ";
		return returnVal;
	}

	/**
	 * Get if a item is currently open from the main workspace screen.
	 * @return {@code true} if a item is open, otherwise {@code false}.
	 */
	public boolean isCurrentlyHaveItemOpen() {
		return this.currentlyHaveItemOpen;
	}

	/**
	 * Set if a item is currently open from the main workspace screen.
	 * The {@code currentlyHaveItemOpen} will automatically be set to {@code true}
	 * in the event of {@code this#actionOpenFormForEditingFromWorkspace}.
	 * 
	 * @param currentlyHaveItemOpenParam Set whether an item is currently open.
	 *
	 * @see this#actionOpenFormForEditingFromWorkspace()
	 * @see this#actionOpenFormForEditingFromWorkspace(JobView, Long)
	 */
	public void setCurrentlyHaveItemOpen(boolean currentlyHaveItemOpenParam) {
		this.currentlyHaveItemOpen = currentlyHaveItemOpenParam;
	}

	/**
	 * Get area to update after a submission has taken place.
	 *
	 * @return {@code String} areaToUpdateForDialogAfterSubmit.
	 */
	public String getAreaToUpdateForDialogAfterSubmit() {
		return this.areaToUpdateForDialogAfterSubmit;
	}

	/**
	 * Set area to update after a submission has taken place.
	 *
	 * @param areaToUpdateForDialogAfterSubmitParam areaToUpdateForDialogAfterSubmit.
	 */
	public void setAreaToUpdateForDialogAfterSubmit(String areaToUpdateForDialogAfterSubmitParam) {
		this.areaToUpdateForDialogAfterSubmit = areaToUpdateForDialogAfterSubmitParam;
	}
}
