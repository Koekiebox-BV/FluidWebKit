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
import com.fluidbpm.fluidwebkit.backing.bean.ABaseWebVO;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.ABaseContentView;
import com.fluidbpm.fluidwebkit.backing.utility.Globals;
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
import com.fluidbpm.ws.client.v1.sqlutil.wrapper.SQLUtilWebSocketRESTWrapper;
import com.fluidbpm.ws.client.v1.userquery.UserQueryClient;

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
 * Bean to handle workspace.
 * 
 * @author jasonbruwer on 2017-12-30
 * @since 1.0
 */
public abstract class ABaseWorkspaceBean extends ABaseManagedBean {
	private JobViewListing jobViewListing = new JobViewListing();
	private UserQueryListing userQueryListing = new UserQueryListing();

	//Views...
	private List<JobView> viewsAll;

	private Map<String, List<JobView>> viewGroupPlacings;

	//Selected Content View...
	private ABaseContentView contentView;

	//Config...
	private boolean currentlyHaveItemOpen;

	private OpenPageLastCache openPageLastCache;
	private String areaToUpdateForDialogAfterSubmit;

	/**
	 * Cached items in case of a page refresh.
	 */
	private static class OpenPageLastCache implements Serializable
	{
		public String workspaceItemAim;
		public String selectedJobViews;

		/**
		 *
		 * @param workspaceItemAimParam
		 * @param selectedJobViewsParam
		 */
		public OpenPageLastCache(
				String workspaceItemAimParam,
				String selectedJobViewsParam
		) {
			this.workspaceItemAim = workspaceItemAimParam;
			this.selectedJobViews = selectedJobViewsParam;
		}

		/**
		 *
		 * @param workspaceItemAimParam
		 * @param selectedJobViewsParam
		 * @return
		 */
		public static boolean shouldUseCache(
				String workspaceItemAimParam,
				String selectedJobViewsParam) {

			return (workspaceItemAimParam == null && selectedJobViewsParam == null);
		}
	}

	/**
	 * Request parameter names.
	 */
	public static class RequestParam {
		public static final String SELECTED_JOB_VIEW = "selectedJobView";
		public static final String SELECTED_JOB_VIEWS = "selectedJobViews";
		public static final String WORKSPACE_AIM = "workspaceItemAim";
		public static final String IDEATION_ONLY_ITEMS = "ideationOnlyItems";

		public static final String OPENED_FROM_VIEW = "openedFromView";
		public static final String TO_UPDATE = "toUpdate";
	}

	/**
	 * Request attribute names.
	 */
	public static class RequestAttribute {
		public static final String ITEM_VALUE_TO_REPLACE = "itemValueToReplace";
		public static final String ITEM_VALUE_TO_REPLACE_WITH = "itemValueToReplaceWith";
	}

	/**
	 * Request attribute names.
	 */
	public static class WorkspaceItemAim {
		public static final String ForAssignmentAdjustmentsConsole = "ForAssignmentAdjustmentsConsole";
		public static final String ForAssignmentOfficeSettlement = "ForAssignmentOfficeSettlement";
		public static final String ForAssignmentReleaseFunds = "ForAssignmentReleaseFunds";
	}

	/**
	 * Log a user into the Design Dashboard.
	 *
	 * @return navigation {@code dashboard}
	 */
	@PostConstruct
	public void actionPopulateInit() {
		FlowStepClient flowStepClient = null;
		UserQueryClient userQueryClient = null;
		try {
			User loggedInUser = this.getLoggedInUser();

			flowStepClient = new FlowStepClient(
					this.getConfigURLFromSystemProperty(),
					loggedInUser.getServiceTicket());

			userQueryClient = new UserQueryClient(
					this.getConfigURLFromSystemProperty(),
					loggedInUser.getServiceTicket());

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
					if (uiViewGroup == null) {
						throw new ClientDashboardException(
								"View Group assignment returned [null] for Group["+viewGroup+"] and View["+viewName+"].",
								ClientDashboardException.ErrorCode.ILLEGAL_STATE);
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
			//We have a problem...
			this.logger.error(fce.getMessage(),fce);

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Failed to fetch User Queries and Views. ", fce.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} finally {
			//Close the login after done...
			if (flowStepClient != null) {
				flowStepClient.closeAndClean();
			}

			if (userQueryClient != null) {
				userQueryClient.closeAndClean();
			}
		}
	}

	/**
	 * 
	 * @param viewGroupParam
	 * @param viewParam
	 * @return
	 */
	protected String getViewGroupForUI(String viewGroupParam, String viewParam) {
		return viewGroupParam;
	}

	/**
	 *
	 * @param uiGroupParam
	 * @return
	 */
	public String getQueryStringForUIGroup(String uiGroupParam) {
		List<JobView> forGroup = null;
		if (this.viewGroupPlacings == null ||
				(forGroup = this.viewGroupPlacings.get(uiGroupParam)) == null) {
			return null;
		}
		
		return this.getViewsFor(forGroup);
	}

	/**
	 * @param jobViewsParam
	 * @return
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
		returnValStr = returnValStr.substring(0,returnValStr.length() -1);

		try {
			return URLEncoder.encode(returnValStr, Globals.CHARSET_UTF8);
		} catch (UnsupportedEncodingException eParam) {
			return returnValStr;
		}
	}

	/**
	 * When the main page for the workspace is opened or refreshed.
	 *
	 * oncomplete="PF('varPanelSettlementDesign').close();"
	 *
	 */
	public void actionCloseOpenForm() {
		this.actionOpenMainPage();
	}

	/**
	 * When the main page for the workspace is opened or refreshed.
	 */
	public void actionOpenMainPage() {
		this.areaToUpdateForDialogAfterSubmit = null;

		String selectedJobViews = this.getStringRequestParam(RequestParam.SELECTED_JOB_VIEWS);
		String workspaceAim = this.getStringRequestParam(RequestParam.WORKSPACE_AIM);

		User loggedInUser = this.getLoggedInUser();
		//Clear the content view...
		if (this.contentView != null) {
			this.contentView.refreshData(null);
		}

		//Make use of cache...
		if (OpenPageLastCache.shouldUseCache(
				workspaceAim, selectedJobViews) && this.openPageLastCache != null) {
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

					JobView viewToSetForm = this.getJobViewFromListingWithId(this.viewsAll, new Long(jobViewIdTxt));
					if (viewToSetForm == null) {
						continue;
					}

					//Run the following asynchronous...
					CompletableFuture toAdd = CompletableFuture.runAsync(
							() -> {
								FluidItemListing listOfItems = null;
								FlowItemClient clnt = new FlowItemClient(
										this.getConfigURLFromSystemProperty(),
										loggedInUser.getServiceTicket());

								final SQLUtilWebSocketRESTWrapper sqlUtilWSRESTWrapper =
										new SQLUtilWebSocketRESTWrapper(
												this.getConfigURLFromSystemProperty(),
												loggedInUser.getServiceTicket(),
												Globals.WEB_SOCKET_TIMEOUT_MILLIS);
								List<WorkspaceFluidItem> itemsForTheView = new ArrayList<>();
								try {
									listOfItems = clnt.getFluidItemsForView(
											viewToSetForm,
											1000,
											0,
											"",
											"");

									if (listOfItems == null || (listOfItems.getListing() == null ||
											listOfItems.getListing().isEmpty())) {
										return;
									}

									//Add each of the items...
									listOfItems.getListing().forEach(itm -> {
										ABaseWebVO baseWebToAdd = this.createABaseWebVO(sqlUtilWSRESTWrapper, itm);
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

									clnt.closeAndClean();
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
			this.logger.error(except.getMessage(),except);
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Failed to fetch items for View. ", except.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} finally {
			//Do nothing for now...
		}
	}

	/**
	 * 
	 * @param workspaceAimParam
	 * @return
	 */
	protected abstract ABaseContentView actionOpenMainPage(String workspaceAimParam);

	/**
	 * 
	 * @param wrapperParam
	 * @param fluidItemParam
	 * @return
	 */
	protected abstract ABaseWebVO createABaseWebVO(SQLUtilWebSocketRESTWrapper wrapperParam, FluidItem fluidItemParam);

	/**
	 *
	 * @param jobViewsParam
	 * @param idToGetParam
	 * @return
	 */
	private JobView getJobViewFromListingWithId(List<JobView> jobViewsParam, Long idToGetParam) {
		if(idToGetParam == null) {
			return null;
		}

		if(jobViewsParam == null || jobViewsParam.isEmpty()) {
			return null;
		}

		//Set the Active Job View...
		if(jobViewsParam != null && !jobViewsParam.isEmpty()) {
			for(JobView view : jobViewsParam) {
				if(idToGetParam.equals(view.getId())) {
					return view;
				}
			}
		}

		return null;
	}

	/**
	 * Open an 'Form' for editing or viewing.
	 */
	public abstract void actionOpenFormForEditingFromWorkspace();

	/**
	 *
	 * @param firstValueParam
	 * @param nextValueParam
	 * @return
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
	 *
	 * @return
	 */
	public boolean isCurrentlyHaveItemOpen() {
		return this.currentlyHaveItemOpen;
	}

	/**
	 *
	 * @param currentlyHaveItemOpenParam
	 */
	public void setCurrentlyHaveItemOpen(boolean currentlyHaveItemOpenParam) {
		this.currentlyHaveItemOpen = currentlyHaveItemOpenParam;
	}

	/**
	 *
	 * @return
	 */
	public ABaseContentView getContentView() {
		return this.contentView;
	}

	/**
	 *
	 * @return
	 */
	public String getAreaToUpdateForDialogAfterSubmit() {
		return this.areaToUpdateForDialogAfterSubmit;
	}

	/**
	 *
	 * @param areaToUpdateForDialogAfterSubmitParam
	 */
	public void setAreaToUpdateForDialogAfterSubmit(String areaToUpdateForDialogAfterSubmitParam) {
		this.areaToUpdateForDialogAfterSubmit = areaToUpdateForDialogAfterSubmitParam;
	}
}
