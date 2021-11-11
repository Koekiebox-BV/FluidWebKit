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
import com.fluidbpm.fluidwebkit.backing.bean.workspace.form.WebKitOpenFormConversationBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.menu.WebKitMenuBean;
import com.fluidbpm.fluidwebkit.backing.utility.Globals;
import com.fluidbpm.fluidwebkit.backing.vo.ABaseWebVO;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.flow.JobViewListing;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.item.FluidItemListing;
import com.fluidbpm.program.api.vo.userquery.UserQueryListing;
import com.fluidbpm.program.api.vo.webkit.RowExpansion;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.flow.FlowStepClient;
import com.fluidbpm.ws.client.v1.flowitem.FlowItemClient;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import com.fluidbpm.ws.client.v1.sqlutil.wrapper.SQLUtilWebSocketRESTWrapper;
import com.fluidbpm.ws.client.v1.userquery.UserQueryClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
	protected Map<String, List<JobView>> loggedInUsrViewGroupPlacings;

	//Selected Content View...
	@Getter
	protected J contentView;

	//Config...
	protected boolean currentlyHaveItemOpen;
	protected OpenPageLastCache openPageLastCache;

	private String areaToUpdateForDialogAfterSubmit;

	@Inject
	protected WebKitOpenFormConversationBean openFormBean;

	@Inject
	protected WebKitMenuBean webKitMenuBean;

	/**
	 * Cached items in case of a page refresh.
	 */
	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	public static class OpenPageLastCache implements Serializable {
		public String clickedGroup;
		public String clickedGroupAlias;
		public String selectedJobViews;
		public String clickedSubAlias;
	}

	/**
	 * Request parameter names got HTTP request mappings.
	 */
	public static class RequestParam {
		public static final String SELECTED_JOB_VIEWS = "selectedJobViews";
		public static final String WORKSPACE_AIM = "workspaceItemAim";
		public static final String OPENED_FROM_VIEW = "openedFromView";
		public static final String TO_UPDATE = "toUpdate";

		//Create new Form instance...
		public static final String FORM_TYPE = "formType";
		public static final String FORM_TYPE_ID = "formTypeId";

		//User Query Search...
		public static final String USER_QUERY = "userQuery";
		public static final String RETAIN_USER_QUERY = "retainUserQuery";
		public static final String USER_QUERY_ID = "userQueryId";
	}

	/**
	 * Initial event when JSF Bean is initialised.
	 * Each of the View Groups will be mapped to {@code this.viewGroupPlacings}.
	 * Further customised grouping may be done by overriding the {@code this#getViewGroupForUI(String,String)} method.
	 * 
	 * The following will be loaded;
	 * - The {@code JobView}'s the logged in user has access to.
	 * - The list of {@code UserQuery}'s the logged in user has access to.
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
				this.loggedInUsrViewGroupPlacings = new HashMap<>();

				for (JobView jobViewIter : this.jobViewListing.getListing()) {
					this.viewsAll.add(jobViewIter);

					String viewName = jobViewIter.getViewName();
					String viewGroup = jobViewIter.getViewGroupName();
					String uiViewGroup = this.getViewGroupForUI(viewGroup, viewName);
					if (uiViewGroup == null || uiViewGroup.isEmpty()) continue;

					List<JobView> jobViews = this.loggedInUsrViewGroupPlacings.get(uiViewGroup);
					if (jobViews == null) jobViews = new ArrayList<>();
					jobViews.add(jobViewIter);
					this.loggedInUsrViewGroupPlacings.put(uiViewGroup, jobViews);
				}

				//Sort by 'View Priority'...
				this.loggedInUsrViewGroupPlacings.values().forEach(listingItm -> {
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
		if (this.loggedInUsrViewGroupPlacings == null) {
			return null;
		}

		List<JobView> forAll = new ArrayList<>();
		for (String uiGroupParam : uiGroupsParam) {
			List<JobView> localGroup = null;
			if ((localGroup = this.loggedInUsrViewGroupPlacings.get(uiGroupParam)) != null) {
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
		if (this.openFormBean != null) {
			this.openFormBean.endConversation();
		}

		this.currentlyHaveItemOpen = false;
		this.actionOpenMainPage();
	}

	/**
	 * Open an 'Form' for editing or viewing.
	 * Custom functionality needs to be placed in {@code this#actionOpenFormForEditingFromWorkspace}.
	 *
	 * @param workspaceFluidItem The workspace item.
	 *
	 * @see #actionOpenForm(WorkspaceFluidItem)
	 */
	public abstract void actionOpenFormForEditingFromWorkspace(WorkspaceFluidItem workspaceFluidItem);

	/**
	 * Custom functionality for when a Form is clicked on to be opened.
	 * Open an 'Form' for editing or viewing.
	 *
	 * @param workspaceFluidItem The workitem to open.
	 */
	public abstract void actionOpenForm(WorkspaceFluidItem workspaceFluidItem);

	public String actionOpenMainPageNonAjax() {
		this.actionOpenMainPage();
		return "workspace";
	}

	/**
	 * When the main page for the workspace is opened or refreshed.
	 * Custom sub {@code ABaseWebVO} object may be created via {@code createABaseWebVO}.
	 *
	 * The {@code actionOpenMainPage} will create the 'Content View' while the {@code createABaseWebVO}
	 * method will map each one of the custom objects.
	 *
	 * @see #actionOpenMainPage(WebKitViewGroup, WebKitViewSub)
	 * @see #createABaseWebVO(WebKitViewGroup,WebKitViewSub,WebKitWorkspaceJobView,FluidItem)
	 */
	public void actionOpenMainPage() {
		this.areaToUpdateForDialogAfterSubmit = null;
		if (this.getFluidClientDS() == null) return;

		String workspaceAim = this.getStringRequestParam(WebKitMenuBean.ReqParam.WORKSPACE_AIM);
		String clickedGroup = this.getStringRequestParam(WebKitMenuBean.ReqParam.CLICKED_GROUP);
		String clickedGroupAlias = this.getStringRequestParam(WebKitMenuBean.ReqParam.CLICKED_GROUP_ALIAS);
		String clickedSubAlias = this.getStringRequestParam(WebKitMenuBean.ReqParam.CLICKED_SUB_ALIAS);
		String clickedWorkspaceViews = this.getStringRequestParam(WebKitMenuBean.ReqParam.CLICKED_VIEWS);
		if ((clickedWorkspaceViews == null || clickedWorkspaceViews.trim().isEmpty())
				&& this.openPageLastCache != null) {
			clickedGroup = this.openPageLastCache.clickedGroup;
			clickedWorkspaceViews = this.openPageLastCache.selectedJobViews;
			clickedGroupAlias = this.openPageLastCache.clickedGroupAlias;
			clickedSubAlias = this.openPageLastCache.clickedSubAlias;
		}
		this.openPageLastCache = new OpenPageLastCache(
			clickedGroup,
			clickedGroupAlias,
			clickedWorkspaceViews,
			clickedSubAlias
		);

		//Clear the content view...
		if (this.contentView != null) this.contentView.refreshData(null);

		try {
			WebKitViewGroup groupWithName = this.webKitMenuBean.getGroupWithName(clickedGroupAlias);
			WebKitViewSub subFilter = (groupWithName == null) ? null :
					groupWithName.getViewSubWithName(clickedSubAlias);
			List<WebKitWorkspaceJobView> viewToFetchFor = null;
			if (groupWithName.isTGMCombined()) {
				viewToFetchFor = this.webKitMenuBean.getUniqueViewsForGroup(groupWithName);
				subFilter = this.computeCombinedSubFromGroup(groupWithName);//force no sub selection to pull all items...
			} else {
				viewToFetchFor = this.webKitMenuBean.getViewsForSub(subFilter);
			}

			List<WebKitWorkspaceJobView> viewsWithAccess = this.filterViewsForUserAccess(viewToFetchFor, clickedGroupAlias);
			if (viewsWithAccess == null || viewsWithAccess.isEmpty()) return;

			List<CompletableFuture> allAsyncs = new ArrayList<>();
			final Map<WebKitWorkspaceJobView, List<FluidItem>> mappingRawViewFetch = new Hashtable<>();

			//Fetch all the view data...
			for (WebKitWorkspaceJobView jobViewWK : viewsWithAccess) {
				//Run the following asynchronous...
				FlowItemClient flowItemClient = this.getFluidClientDS().getFlowItemClient();
				CompletableFuture toAdd = CompletableFuture.runAsync(() -> {
					FluidItemListing listOfItems = null;
					try {
						int fetchLimit = (jobViewWK.getFetchLimit() == null) ? 300 : jobViewWK.getFetchLimit();

						listOfItems = flowItemClient.getFluidItemsForView(
								jobViewWK.getJobView(),
								fetchLimit,
								0);
						if (listOfItems == null || (listOfItems.getListing() == null || listOfItems.getListing().isEmpty())) return;
					} catch (FluidClientException fluidClientExcept) {
						if (fluidClientExcept.getErrorCode() == FluidClientException.ErrorCode.NO_RESULT) return;
						throw fluidClientExcept;
					} finally {
						//Set the items for the view...
						if (listOfItems != null && !listOfItems.isListingEmpty()) {
							mappingRawViewFetch.put(jobViewWK, listOfItems.getListing());
						}
					}
				});
				allAsyncs.add(toAdd);
			}
			//Run them all...
			CompletableFuture.allOf(allAsyncs.toArray(new CompletableFuture[]{})).join();

			//Selected Job Views...
			//Group -> Sub -> View -> Items...
			Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data = new HashMap<>();
			List<WebKitViewSub> subs = groupWithName.getWebKitViewSubs();
			if (subFilter != null) {
				subs = new ArrayList<>();
				subs.add(subFilter);
			}

			subs.sort(Comparator.comparing(WebKitViewSub::getSubOrder));
			subs.stream()
					.filter(itm -> itm.getJobViews() != null)
					.forEach(sub -> {
						Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>> viewMapForSub = new HashMap<>();
						List<WebKitWorkspaceJobView> views = sub.getJobViews();
						List<Long> fluidItemIdsAdded = new ArrayList<>();

						//FIXME 001 List<CompletableFuture> subProcessorSyncs = new ArrayList<>();
						//Assign a processor for each of the views
						//FIXME 001 CompletableFuture subAsync = CompletableFuture.runAsync(() -> {
						views.stream()
								.filter(itm -> mappingRawViewFetch.get(itm) != null)
								.sorted(Comparator.comparing(WebKitWorkspaceJobView::getViewOrder))
								.forEach(viewWithResults -> {
									List<FluidItem> viewResults = mappingRawViewFetch.get(viewWithResults);
									List<WorkspaceFluidItem> wfiList = new ArrayList<>();
									viewResults.forEach(fldItem -> {
										if (groupWithName.isTGMNoDuplicates()) {
											Long toCheck = fldItem.getId();
											if (fluidItemIdsAdded.contains(toCheck)) return;
											else fluidItemIdsAdded.add(toCheck);
										}
										T baseWebToAdd = this.createABaseWebVO(groupWithName, sub, viewWithResults, fldItem);
										WorkspaceFluidItem item = new WorkspaceFluidItem(baseWebToAdd);
										item.setJobView(viewWithResults.getJobView());
										wfiList.add(item);
									});
									viewMapForSub.put(viewWithResults, wfiList);
								});
						//FIXME 001 });
						//FIXME 001 subProcessorSyncs.add(subAsync);
						//FIXME 001 CompletableFuture.allOf(subProcessorSyncs.toArray(new CompletableFuture[]{})).join();
						data.put(sub, viewMapForSub);
					});

			//Map the layout...
			this.contentView = this.actionOpenMainPage(groupWithName, subFilter);
			//Set the correct content view...
			if (this.contentView == null) {
				throw new ClientDashboardException(
						"Unable to set Content View for "+ workspaceAim+"'. Not supported.",
						ClientDashboardException.ErrorCode.VALIDATION);
			}

			//Refresh the data...
			this.contentView.refreshData(data);
		} catch (Exception except) {
			//log it...
			this.getLogger().error(except.getMessage(),except);
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Failed to fetch items for View. ", except.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} finally { /*Do nothing for now...*/ }
	}

	private WebKitViewSub computeCombinedSubFromGroup(WebKitViewGroup groupWithName) {
		if (groupWithName.getWebKitViewSubs() == null || groupWithName.getWebKitViewSubs().isEmpty()) return null;

		WebKitViewSub returnVal = new WebKitViewSub();
		returnVal.setIcon(groupWithName.getJobViewGroupIcon());
		returnVal.setLabel(groupWithName.getJobViewGroupName());
		returnVal.setJobViews(new ArrayList<>());
		returnVal.setRouteFields(new ArrayList<>());
		returnVal.setRowExpansion(new RowExpansion());
		
		for (WebKitViewSub sub : groupWithName.getWebKitViewSubs()) {
			if (sub.getJobViews() != null) {
				returnVal.getJobViews().addAll(sub.getJobViews().stream().filter(itm -> {
					return returnVal.getJobViews().stream()
							.filter(returnValViewItm -> returnValViewItm.getJobView().getId().equals(itm.getJobView().getId()))
							.count() < 1;
				}).collect(Collectors.toList()));
			};

			if (sub.getRouteFields() != null) {
				returnVal.getRouteFields().addAll(sub.getRouteFields().stream().filter(itm -> {
					return returnVal.getRouteFields().stream()
							.filter(returnValFldItm -> returnValFldItm.getRouteField().getFieldName().equals(
									itm.getRouteField().getFieldName()))
							.count() < 1;
				}).collect(Collectors.toList()));
			}

			returnVal.setShowColumnAttachment(returnVal.isShowColumnAttachment() | sub.isShowColumnAttachment());
			returnVal.setShowColumnCurrentFlow(returnVal.isShowColumnCurrentFlow() | sub.isShowColumnCurrentFlow());
			returnVal.setShowColumnCurrentStep(returnVal.isShowColumnCurrentStep() | sub.isShowColumnCurrentStep());
			returnVal.setShowColumnCurrentView(returnVal.isShowColumnCurrentView() | sub.isShowColumnCurrentView());
			returnVal.setShowColumnID(returnVal.isShowColumnID() | sub.isShowColumnID());
			returnVal.setShowColumnDateCreated(returnVal.isShowColumnDateCreated() | sub.isShowColumnDateCreated());
			returnVal.setShowColumnDateLastUpdated(returnVal.isShowColumnDateLastUpdated() | sub.isShowColumnDateLastUpdated());
			returnVal.setShowColumnFormType(returnVal.isShowColumnFormType() | sub.isShowColumnFormType());
			returnVal.setShowColumnProgressPercentage(returnVal.isShowColumnProgressPercentage() | sub.isShowColumnProgressPercentage());
			returnVal.setShowColumnTitle(returnVal.isShowColumnTitle() | sub.isShowColumnTitle());

			if (sub.getRowExpansion() != null) {
				RowExpansion re = sub.getRowExpansion();
				RowExpansion reReturnVal = returnVal.getRowExpansion();

				reReturnVal.setTableExpansionDisplayAncestor(
						reReturnVal.isTableExpansionDisplayAncestor() | re.isTableExpansionDisplayAncestor());
				reReturnVal.setTableExpansionDisplayAttachments(
						reReturnVal.isTableExpansionDisplayAttachments() | re.isTableExpansionDisplayAttachments());
				reReturnVal.setTableExpansionDisplayDescendant(
						reReturnVal.isTableExpansionDisplayDescendant() | re.isTableExpansionDisplayDescendant());
				reReturnVal.setTableExpansionDisplayFlowHistory(
						reReturnVal.isTableExpansionDisplayFlowHistory() | re.isTableExpansionDisplayFlowHistory());
				reReturnVal.setTableExpansionDisplayFormHistory(
						reReturnVal.isTableExpansionDisplayFormHistory() | re.isTableExpansionDisplayFormHistory());
				reReturnVal.setTableExpansionDisplayRecords(
						reReturnVal.isTableExpansionDisplayRecords() | re.isTableExpansionDisplayRecords());
				reReturnVal.setTableExpansionDisplayRecordsInlineEdit(
						reReturnVal.isTableExpansionDisplayRecordsInlineEdit() | re.isTableExpansionDisplayRecordsInlineEdit());
			}
			
		}
		return returnVal;
	}

	private List<WebKitWorkspaceJobView> filterViewsForUserAccess(
		List<WebKitWorkspaceJobView> webKitConfigViews,
		String group
	) {
		if (webKitConfigViews == null || webKitConfigViews.isEmpty()) {
			return new ArrayList<>();
		}

		if (this.loggedInUsrViewGroupPlacings == null) {
			return null;
		}

		List<JobView> allowed =
				this.loggedInUsrViewGroupPlacings.getOrDefault(group, new ArrayList<>());
		List<Long> allowedIds = allowed.stream().map(toMap -> toMap.getId()).collect(Collectors.toList());
		List<WebKitWorkspaceJobView> returnList = new ArrayList<>();
		for (WebKitWorkspaceJobView fromConfig : webKitConfigViews) {
			if (allowedIds.contains(fromConfig.getId())) {
				returnList.add(fromConfig);
			}
		}
		return returnList;
	}

	/**
	 * Custom functionality for when a workspace is loaded.
	 * The menu-option clicked by the user will be passed through as {@code workspaceAimParam}.
	 *
	 * @param webKitGroup The group.
	 * @param selectedSub The sub.
	 *
	 * @return ABaseContentView to use based on {@code workspaceAimParam}.
	 *
	 * @see ABaseContentView
	 */
	protected abstract J actionOpenMainPage(WebKitViewGroup webKitGroup, WebKitViewSub selectedSub);

	/**
	 * Create a custom value object based on {@code wrapperParam} and {@code fluidItemParam}.
	 *
	 * @param group The view group.
	 * @param sub The view sub.
	 * @param view The view.
	 * @param item The Fluid {@code FluidItem} to map from.
	 *
	 * @return ABaseWebVO subclass based on {@code fluidItemParam}.
	 *
	 * @see ABaseWebVO
	 * @see SQLUtilWebSocketRESTWrapper
	 * @see FluidItem
	 */
	protected abstract T createABaseWebVO(
			WebKitViewGroup group,
			WebKitViewSub sub,
			WebKitWorkspaceJobView view,
			FluidItem item);

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
		if (idToGetParam == null) return null;

		if (jobViewsParam == null || jobViewsParam.isEmpty()) return null;

		return jobViewsParam.stream()
				.filter(itm -> idToGetParam.equals(itm.getId()))
				.findFirst()
				.orElse(null);
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
	 * @see #actionOpenFormForEditingFromWorkspace(WorkspaceFluidItem)
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

	public String getCategory() {
		return this.contentView == null ? null : this.contentView.getCategory();
	}

	public void actionDeleteForm(WorkspaceFluidItem workspaceFluidItem) {
		try {
			if (this.getFluidClientDS() == null) return;
			if (workspaceFluidItem.getFluidItemForm() == null) return;

			FormContainerClient fcc = this.getFluidClientDS().getFormContainerClient();
			fcc.deleteFormContainer(workspaceFluidItem.getFluidItemForm());

			//Refresh the data...
			this.actionOpenMainPage();

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success.", String.format("Deleted '%s'.", workspaceFluidItem.getFluidItemTitle()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}
}
