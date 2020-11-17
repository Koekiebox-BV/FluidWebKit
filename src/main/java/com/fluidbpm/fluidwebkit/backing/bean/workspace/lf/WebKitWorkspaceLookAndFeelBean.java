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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.lf;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitConfigBean;
import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.program.api.vo.config.Configuration;
import com.fluidbpm.program.api.vo.config.ConfigurationListing;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.program.api.vo.webkit.WebKitForm;
import com.fluidbpm.program.api.vo.webkit.WebKitGlobal;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitMenuItem;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQuery;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQueryListing;
import com.fluidbpm.program.api.vo.webkit.viewgroup.*;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.config.ConfigurationClient;
import com.fluidbpm.ws.client.v1.flow.RouteFieldClient;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import org.primefaces.event.CellEditEvent;
import org.primefaces.event.data.PageEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SessionScoped
@Named("webKitWorkspaceLookAndFeelBean")
public class WebKitWorkspaceLookAndFeelBean extends ABaseManagedBean {
	private Map<String, List<JobView>> groupToViewMapping;
	private Map<String, List<Field>> groupToRouteFieldMapping;
	private List<JobView> allJobViews;

	@Getter
	@Setter
	private Map<String, WebKitWorkspaceJobViewLDM> inputSubToViewMapping;

	@Getter
	@Setter
	private Map<String, WebKitWorkspaceRouteFieldLDM> inputSubToRouteFieldMapping;

	@Getter
	@Setter
	private List<WebKitViewGroup> webKitViewGroups;

	//Inputs....
	@Getter
	@Setter
	private String inputNewGroupSubName;

	@Getter
	@Setter
	private Map<String, List<String>> inputVisibleColumns;

	@Getter
	@Setter
	private Map<String, List<String>> inputVisibleButtons;

	private WebKitGlobal webKitGlobal;

	//User Query related objects...
	@Getter
	@Setter
	private TreeNode treeNodeUserQueryMenuRoot;
	private List<WebKitUserQuery> webKitUserQueries;

	@Getter
	@Setter
	private WebKitWorkspaceUserQueryLDM userQueryLDM;

	//Form Definitions...
	@Getter
	@Setter
	private List<WebKitForm> webKitForms;

	@Getter
	@Setter
	private WebKitWorkspaceFormDefinitionLDM formDefinitionLDM;

	//Inputs....
	@Getter
	@Setter
	private String inputNewRootMenuLabel;

	public enum VisibleColumnItems {
		showColumnID,
		showColumnFormType,
		showColumnTitle,
		showColumnStepEntryTime,
		showColumnCurrentFlow,
		showColumnCurrentStep,
		showColumnCurrentView,
		showColumnProgressPercentage,
		showColumnAttachment;

		public static List<String> asListFrom(WebKitViewGroup group) {
			List<String> returnVal = new ArrayList<>();
			if (group == null) {
				return returnVal;
			}
			if (group.isShowColumnID()) {
				returnVal.add(VisibleColumnItems.showColumnID.name());
			}
			if (group.isShowColumnFormType()) {
				returnVal.add(VisibleColumnItems.showColumnFormType.name());
			}
			if (group.isShowColumnTitle()) {
				returnVal.add(VisibleColumnItems.showColumnTitle.name());
			}
			if (group.isShowColumnStepEntryTime()) {
				returnVal.add(VisibleColumnItems.showColumnStepEntryTime.name());
			}
			if (group.isShowColumnCurrentFlow()) {
				returnVal.add(VisibleColumnItems.showColumnCurrentFlow.name());
			}
			if (group.isShowColumnCurrentStep()) {
				returnVal.add(VisibleColumnItems.showColumnCurrentStep.name());
			}
			if (group.isShowColumnCurrentView()) {
				returnVal.add(VisibleColumnItems.showColumnCurrentView.name());
			}
			if (group.isShowColumnProgressPercentage()) {
				returnVal.add(VisibleColumnItems.showColumnProgressPercentage.name());
			}
			if (group.isShowColumnAttachment()) {
				returnVal.add(VisibleColumnItems.showColumnAttachment.name());
			}
			return returnVal;
		}
	}

	public enum VisibleButtonItems {
		showButtonBulkUpdate,
		showButtonExport,
		showButtonSendOn,
		showButtonDelete,
		showButtonLock,
		showButtonAddToPI,
		;

		public static List<String> asListFrom(WebKitViewGroup group) {
			List<String> returnVal = new ArrayList<>();
			if (group == null) {
				return returnVal;
			}
			if (group.isShowButtonBulkUpdate()) {
				returnVal.add(VisibleButtonItems.showButtonBulkUpdate.name());
			}
			if (group.isShowButtonExport()) {
				returnVal.add(VisibleButtonItems.showButtonExport.name());
			}
			if (group.isShowButtonSendOn()) {
				returnVal.add(VisibleButtonItems.showButtonSendOn.name());
			}
			if (group.isShowButtonDelete()) {
				returnVal.add(VisibleButtonItems.showButtonDelete.name());
			}
			if (group.isShowButtonLock()) {
				returnVal.add(VisibleButtonItems.showButtonLock.name());
			}
			if (group.isShowButtonAddToPI()) {
				returnVal.add(VisibleButtonItems.showButtonAddToPI.name());
			}
			return returnVal;
		}
	}

	/**
	 * Prepare to change the look and feel for workspace.
	 */
	public void actionPrepareWorkspaceLookAndFeelUpdate() {
		this.inputSubToViewMapping = new HashMap<>();
		this.inputSubToRouteFieldMapping = new HashMap<>();
		this.groupToViewMapping = new HashMap<>();
		this.groupToRouteFieldMapping = new HashMap<>();
		this.inputVisibleColumns = new HashMap<>();
		this.inputVisibleButtons = new HashMap<>();
		this.webKitUserQueries = new ArrayList<>();
		this.webKitForms = new ArrayList<>();
		this.userQueryLDM = new WebKitWorkspaceUserQueryLDM();
		this.formDefinitionLDM = new WebKitWorkspaceFormDefinitionLDM();
		this.setDialogHeaderTitle("Workspace - Look & Feel");
		try {
			try {
				this.webKitViewGroups = this.getFluidClientDSConfig().getFlowClient().getViewGroupWebKit().getListing();
			} catch (FluidClientException fce) {
				if (fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT) throw fce;
				return;
			}

			//Populate the menu items...
			this.populateUserQueryMenu();

			try {
				this.webKitUserQueries = this.getFluidClientDSConfig().getUserQueryClient().getUserQueryWebKit().getListing();
				this.userQueryLDM.addToInitialListing(this.webKitUserQueries);
			} catch (FluidClientException fce) {
				if (fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT) throw fce;
			}

			try {
				this.webKitForms = this.getFluidClientDSConfig().getFormDefinitionClient().getAllFormWebKits();
				this.formDefinitionLDM.addToInitialListing(this.webKitForms);
			} catch (FluidClientException fce) {
				if (fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT) throw fce;
			}

			this.allJobViews = this.getFluidClientDSConfig().getFlowStepClient().getJobViewsByLoggedInUser().getListing();
			RouteFieldClient routeFieldClient = this.getFluidClientDSConfig().getRouteFieldClient();

			//Set the Views available for the Groups...
			this.allJobViews.stream()
					.filter(itm -> itm.getViewGroupName() != null && !itm.getViewGroupName().isEmpty())
					.forEach(itm -> {
						String viewGroupName = itm.getViewGroupName();
						//Add the Job Views for the Group...
						List<JobView> jobViewList = this.groupToViewMapping.getOrDefault(
								viewGroupName, new ArrayList<>());
						jobViewList.add(itm);
						this.groupToViewMapping.put(viewGroupName, jobViewList);
					});

			//Set the Route Fields for the Groups...
			this.groupToViewMapping.forEach((key, value) -> {
				this.inputVisibleColumns.put(key, new ArrayList<>());
				this.inputVisibleButtons.put(key, new ArrayList<>());

				List<Field> routeFieldsForGroup = new ArrayList<>();
				try {
					routeFieldsForGroup = routeFieldClient.getFieldsByViewGroup(new WebKitViewGroup(key)).getListing();
				} catch (FluidClientException fce) {
					if (fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT) throw fce;
				}
				this.groupToRouteFieldMapping.put(key, routeFieldsForGroup);
			});

			//Merged with existing configuration set...
			if (this.webKitViewGroups == null || this.webKitViewGroups.isEmpty()) {
				return;
			}

			//Merge the properties with what is stored...
			this.webKitViewGroups.forEach(groupItm -> {
				String groupName = groupItm.getJobViewGroupName();
				this.inputVisibleColumns.put(groupName, VisibleColumnItems.asListFrom(groupItm));
				this.inputVisibleButtons.put(groupName, VisibleButtonItems.asListFrom(groupItm));

				List<WebKitViewSub> subsForGroup = groupItm.getWebKitViewSubs();
				if (subsForGroup == null || subsForGroup.isEmpty()) {
					return;
				}

				subsForGroup.forEach(subItm -> {
					String keyForStorage = this.generateGroupSubKey(groupItm.getJobViewGroupName(), subItm.getLabel());
					List<WebKitWorkspaceJobView> jobViewsToSet = new ArrayList<>();
					List<WebKitWorkspaceRouteField> fieldsToSet = new ArrayList<>();

					List<JobView> viewsInCore = this.groupToViewMapping.get(groupName);
					if (viewsInCore != null && !viewsInCore.isEmpty()) {
						List<WebKitWorkspaceJobView> subViews = subItm.getJobViews() == null ?
								new ArrayList<>() : subItm.getJobViews();
						viewsInCore.forEach(coreItem -> {
							WebKitWorkspaceJobView viewFromSub = subViews.stream()
									.filter(itm -> itm.getJobView().getId().equals(coreItem.getId()))
									.findFirst()
									.orElse(null);
							if (viewFromSub == null) {
								viewFromSub = new WebKitWorkspaceJobView(coreItem);
							} else {
								viewFromSub.setSelected(true);
								viewFromSub.setJobView(coreItem);
							}
							jobViewsToSet.add(viewFromSub);
						});
					}
					List<Field> fieldsInCore = this.groupToRouteFieldMapping.get(groupName);
					if (fieldsInCore != null && !fieldsInCore.isEmpty()) {
						List<WebKitWorkspaceRouteField> subFields = subItm.getRouteFields() == null ?
								new ArrayList<>() : subItm.getRouteFields();
						AtomicInteger order = new AtomicInteger(1);
						fieldsInCore.forEach(coreItem -> {
							WebKitWorkspaceRouteField fieldFromSub = subFields.stream()
									.filter(itm -> itm.getRouteField().getId().equals(coreItem.getId()))
									.findFirst()
									.orElse(null);
							if (fieldFromSub == null) {
								fieldFromSub = new WebKitWorkspaceRouteField(coreItem);
								fieldFromSub.setFieldOrder(order.getAndIncrement());
							} else {
								fieldFromSub.setSelected(true);
								fieldFromSub.setRouteField(coreItem);
							}
							fieldsToSet.add(fieldFromSub);
						});
					}

					this.inputSubToViewMapping.put(keyForStorage, new WebKitWorkspaceJobViewLDM(jobViewsToSet));
					this.inputSubToRouteFieldMapping.put(keyForStorage, new WebKitWorkspaceRouteFieldLDM(fieldsToSet));
				});
			});
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	private void populateUserQueryMenu() {
		ConfigurationListing configurationListing =
				this.getFluidClientDSConfig().getConfigurationClient().getAllConfigurations();
		Configuration webKitGlobal = configurationListing.getListing().stream()
				.filter(itm -> WebKitConfigBean.ConfigKey.WebKit.equals(itm.getKey()))
				.findFirst()
				.orElse(null);
		String jsonVal = (webKitGlobal == null ||
				(webKitGlobal.getValue() == null || webKitGlobal.getValue().trim().isEmpty())) ? "{}" :
				webKitGlobal.getValue();
		this.webKitGlobal = new WebKitGlobal(new JSONObject(jsonVal));
		if (this.webKitGlobal.getWebKitMenuItems() == null) {
			this.webKitGlobal.setWebKitMenuItems(new ArrayList<>());
		}
		this.constructTreeNode();
	}

	private void constructTreeNode() {
		WebKitMenuItem menuItemRoot = new WebKitMenuItem();
		menuItemRoot.setMenuLabel("ROOT");
		this.treeNodeUserQueryMenuRoot = new DefaultTreeNode(menuItemRoot, null);
		this.treeNodeUserQueryMenuRoot.setExpanded(true);
		List<WebKitMenuItem> menuItems = this.webKitGlobal.getWebKitMenuItems();
		menuItems.stream()
				.filter(itm -> itm.getParentMenuId() == null || itm.getParentMenuId().trim().isEmpty())
				.forEach(itm -> {
					TreeNode toAdd = new DefaultTreeNode(itm, this.treeNodeUserQueryMenuRoot);
					toAdd.setExpanded(true);
				});
		if (this.treeNodeUserQueryMenuRoot.getChildCount() > 0) {
			this.treeNodeUserQueryMenuRoot.getChildren().forEach(itm -> this.placeMenuForParent(itm));
		}
	}

	private void placeMenuForParent(TreeNode node) {
		if (node == null) return;

		WebKitMenuItem data = (WebKitMenuItem)node.getData();
		//Find all where parent is...
		List<WebKitMenuItem> menusWithNodeAsParent = this.webKitGlobal.getWebKitMenuItems().stream()
				.filter(itm -> itm.getParentMenuId() != null && itm.getParentMenuId().equals(data.getMenuId()))
				.collect(Collectors.toList());
		menusWithNodeAsParent.forEach(menuItmWhereParent -> {
			TreeNode addedNode = new DefaultTreeNode(menuItmWhereParent, node);
			addedNode.setExpanded(true);
		});

		if (node.getChildCount() > 0) {
			node.getChildren().forEach(itm -> this.placeMenuForParent(itm));
		}
	}

	public boolean doesMenuHaveChildren(WebKitMenuItem toCheck) {
		return this.doesMenuHaveChildren(toCheck, this.webKitGlobal.getWebKitMenuItems());
	}

	public boolean doesMenuHaveChildren(WebKitMenuItem toCheck, List<WebKitMenuItem> webKitMenuItems) {
		if (toCheck == null || webKitMenuItems == null) return false;
		String idToCheck = toCheck.getMenuId();
		if (idToCheck == null || idToCheck.isEmpty()) return false;

		WebKitMenuItem menuItm = webKitMenuItems.stream()
				.filter(itm -> itm.getParentMenuId() != null && idToCheck.equals(itm.getParentMenuId()))
				.findFirst()
				.orElse(null);
		return (menuItm == null) ? false : true;
	}

	public void actionAddNewViewSub(WebKitViewGroup groupToAddFor) {
		try {
			if (this.getInputNewGroupSubName() == null || this.getInputNewGroupSubName().trim().isEmpty()) {
				throw new ClientDashboardException(
						"'View Sub Name' cannot be empty.", ClientDashboardException.ErrorCode.VALIDATION);
			}

			WebKitViewSub wkViewSubToAdd = new WebKitViewSub();
			wkViewSubToAdd.setLabel(this.getInputNewGroupSubName().trim());

			if (groupToAddFor.getWebKitViewSubs() == null) {
				groupToAddFor.setWebKitViewSubs(new ArrayList<>());
			}

			Integer maxWebKitMaxOrder = groupToAddFor.getWebKitViewSubs().stream()
					.max(Comparator.comparing(WebKitViewSub::getSubOrder))
					.map(itm -> itm.getSubOrder())
					.orElse(Integer.valueOf(0));
			int order = (maxWebKitMaxOrder + 1);
			wkViewSubToAdd.setSubOrder(order);

			groupToAddFor.getWebKitViewSubs().add(wkViewSubToAdd);

			String groupName = groupToAddFor.getJobViewGroupName();
			String subName = wkViewSubToAdd.getLabel();

			WebKitWorkspaceJobViewLDM ldmJobViews = new WebKitWorkspaceJobViewLDM();
			ldmJobViews.populateFromJobViews(this.groupToViewMapping.get(groupName));

			WebKitWorkspaceRouteFieldLDM ldmRouteFields = new WebKitWorkspaceRouteFieldLDM();
			ldmRouteFields.populateFromFields(this.groupToRouteFieldMapping.get(groupName));

			String storageKey = this.generateGroupSubKey(groupName, subName);
			this.inputSubToViewMapping.put(storageKey, ldmJobViews);
			this.inputSubToRouteFieldMapping.put(storageKey, ldmRouteFields);

			this.setInputNewGroupSubName(null);
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("View Sub '%s' added for '%s'.",
							wkViewSubToAdd.getLabel(), groupToAddFor.getJobViewGroupName()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionRemoveViewSub(WebKitViewGroup groupToRemoveFrom, WebKitViewSub subToRemove) {
		try {
			String keyToRemove = this.generateGroupSubKey(
					groupToRemoveFrom.getJobViewGroupName(), subToRemove.getLabel());
			this.inputSubToViewMapping.remove(keyToRemove);
			this.inputSubToRouteFieldMapping.remove(keyToRemove);

			groupToRemoveFrom.getWebKitViewSubs().remove(subToRemove);
			AtomicInteger orderCounter = new AtomicInteger(1);
			groupToRemoveFrom.getWebKitViewSubs().forEach(itm -> {
				itm.setSubOrder(orderCounter.getAndIncrement());
			});
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("Removed View Sub '%s' from '%s'.",
							subToRemove.getLabel(), groupToRemoveFrom.getJobViewGroupName()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionClearMaxResultsTbl(WebKitViewGroup groupToRemoveFrom) {
		groupToRemoveFrom.setTableMaxCountPerPage(null);
	}

	public void actionMoveViewSubUp(WebKitViewGroup groupToRemoveFrom, WebKitViewSub subToMove) {
		try {
			int fromPos = subToMove.getSubOrder();
			int newPos = (subToMove.getSubOrder() - 1);
			int objPosInArray = groupToRemoveFrom.getWebKitViewSubs().indexOf(subToMove);

			WebKitViewSub fromObj = groupToRemoveFrom.getWebKitViewSubs().get(objPosInArray);
			WebKitViewSub toObj = groupToRemoveFrom.getWebKitViewSubs().get(objPosInArray-1);
			fromObj.setSubOrder(newPos);
			toObj.setSubOrder(fromPos);

			groupToRemoveFrom.getWebKitViewSubs().set(objPosInArray, toObj);
			groupToRemoveFrom.getWebKitViewSubs().set(objPosInArray - 1, fromObj);

			groupToRemoveFrom.getWebKitViewSubs().sort(Comparator.comparing(WebKitViewSub::getSubOrder));

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("Moved View Sub '%s' from '%d' to position '%d'.",
							subToMove.getLabel(), fromPos, newPos));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionMoveRouteFieldDown(WebKitViewGroup groupToRemoveFrom, WebKitWorkspaceRouteField fieldToMove) {

	}

	public void actionMoveRouteFieldUp(WebKitViewGroup groupToRemoveFrom, WebKitWorkspaceRouteField fieldToMove) {

	}

	public void actionMoveViewSubDown(WebKitViewGroup groupToRemoveFrom, WebKitViewSub subToMove) {
		try {
			int fromPos = subToMove.getSubOrder();
			int newPos = (subToMove.getSubOrder() + 1);
			int objPosInArray = groupToRemoveFrom.getWebKitViewSubs().indexOf(subToMove);

			WebKitViewSub fromObj = groupToRemoveFrom.getWebKitViewSubs().get(objPosInArray);
			WebKitViewSub toObj = groupToRemoveFrom.getWebKitViewSubs().get(objPosInArray+1);
			fromObj.setSubOrder(newPos);
			toObj.setSubOrder(fromPos);

			groupToRemoveFrom.getWebKitViewSubs().set(objPosInArray, toObj);
			groupToRemoveFrom.getWebKitViewSubs().set(objPosInArray + 1, fromObj);

			groupToRemoveFrom.getWebKitViewSubs().sort(Comparator.comparing(WebKitViewSub::getSubOrder));

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("Moved View Sub '%s' from '%d' to position '%d'.",
							subToMove.getLabel(), fromPos, newPos));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionSaveWebKitViewGroups(String dialogToHideAfterSuccess) {
		try {
			//View Groups...
			this.webKitViewGroups.forEach(groupItm -> {
				String groupName = groupItm.getJobViewGroupName();
				List<String> visibleColumns = new ArrayList<>();
				List<String> visibleButtons = new ArrayList<>();
				Object objVisibleColumns = this.inputVisibleColumns.get(groupName);
				if (objVisibleColumns instanceof String[]) {
					for (String selected : (String[])objVisibleColumns) {
						visibleColumns.add(selected);
					}
				}
				Object objVisibleButtons = this.inputVisibleButtons.get(groupName);
				if (objVisibleButtons instanceof String[]) {
					for (String selected : (String[])objVisibleButtons) {
						visibleButtons.add(selected);
					}
				}
				this.updateGroupPropertyBasedOnSelected(visibleColumns, visibleButtons, groupItm);
			});

			//Global WebKit Config
			FluidClientDS configDs = this.getFluidClientDSConfig();
			final ConfigurationClient configurationClient = configDs.getConfigurationClient();
			configurationClient.upsertConfiguration(WebKitConfigBean.ConfigKey.WebKit, this.webKitGlobal.toString());

			WebKitViewGroupListing listingVg = new WebKitViewGroupListing();
			listingVg.setListing(this.webKitViewGroups);
			listingVg.setListingCount(this.webKitViewGroups.size());
			configDs.getFlowClient().upsertViewGroupWebKit(listingVg);

			//User Query...
			WebKitUserQueryListing listingUq = new WebKitUserQueryListing();
			listingUq.setListing(this.userQueryLDM.getDataListing());
			listingUq.setListingCount(this.userQueryLDM.getDataListing().size());
			configDs.getUserQueryClient().upsertUserQueryWebKit(listingUq);

			this.executeJavaScript(String.format("PF('%s').hide();", dialogToHideAfterSuccess));
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", "Workspace Look and Feel Updated.");
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionAddNewRootMenuLabel() {
		try {
			if (this.getInputNewRootMenuLabel() == null || this.getInputNewRootMenuLabel().trim().isEmpty()) {
				throw new ClientDashboardException(
						"'Menu Label' cannot be empty.", ClientDashboardException.ErrorCode.VALIDATION);
			}

			WebKitMenuItem webKitMenuItem = new WebKitMenuItem();
			webKitMenuItem.setMenuId(UUID.randomUUID().toString());
			webKitMenuItem.setMenuLabel(this.getInputNewRootMenuLabel().trim());
			webKitMenuItem.setMenuIcon("pi pi-ban");
			webKitMenuItem.setParentMenuId(null);

			this.webKitGlobal.getWebKitMenuItems().add(webKitMenuItem);
			this.constructTreeNode();

			this.setInputNewRootMenuLabel(null);
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("Root Menu '%s' added.", webKitMenuItem.getMenuLabel()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionOnMenuCellEdit(CellEditEvent event) {
		Object oldValue = event.getOldValue();
		Object newValue = event.getNewValue();
		if (newValue != null && !newValue.equals(oldValue)) {
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Menu Updated", "Old: " + oldValue + ", New:" + newValue);
			FacesContext.getCurrentInstance().addMessage(null, msg);
		}
		this.constructTreeNode();
	}

	public void actionRemoveMenuItem(WebKitMenuItem webKitMenuItem) {
		try {
			this.webKitGlobal.getWebKitMenuItems().remove(webKitMenuItem);
			this.constructTreeNode();
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("Removed Menu Item '%s'.", webKitMenuItem.getMenuLabel()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionAddSubmenuFor(WebKitMenuItem webKitMenuItem) {
		try {
			WebKitMenuItem toAdd = new WebKitMenuItem();
			toAdd.setMenuLabel("New...");
			toAdd.setMenuId(UUID.randomUUID().toString());
			toAdd.setParentMenuId(webKitMenuItem.getMenuId());
			toAdd.setMenuIcon("pi pi-question");

			this.webKitGlobal.getWebKitMenuItems().add(toAdd);
			this.constructTreeNode();
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("Added Sub Menu Item '%s' for '%s'.",
							toAdd.getMenuLabel(),
							webKitMenuItem.getMenuLabel()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	public void actionSelectDeselectAllViews(WebKitViewSub viewSub) {
		if (viewSub.getJobViews() == null) {
			return;
		}
		AtomicBoolean toSetAllTo = new AtomicBoolean(false);
		viewSub.getJobViews().stream().forEach(itm -> {
			if (!itm.isSelected()) {
				toSetAllTo.set(true);
				return;
			}
		});

		viewSub.getJobViews().stream().forEach(itm -> itm.setSelected(toSetAllTo.get()));
	}

	public void actionOnRouteFieldCellEdit(CellEditEvent event) {
		Object oldValue = event.getOldValue();
		Object newValue = event.getNewValue();

		if (newValue != null && !newValue.equals(oldValue)) {
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Cell Changed", "Old: " + oldValue + ", New:" + newValue);
			FacesContext.getCurrentInstance().addMessage(null, msg);
		}
	}

	public void actionOnPage(PageEvent pageEvent) {

	}

	public void actionOnBoolChange(AjaxBehaviorEvent boolEvent) {

	}

	private void updateGroupPropertyBasedOnSelected(
		List<String> visibleColumns,
		List<String> visibleButtons,
		WebKitViewGroup groupToUpdate
	) {
		//Columns...
		if (visibleColumns.contains(VisibleColumnItems.showColumnID.name())) {
			groupToUpdate.setShowColumnID(true);
		} else {
			groupToUpdate.setShowColumnID(false);
		}
		if (visibleColumns.contains(VisibleColumnItems.showColumnFormType.name())) {
			groupToUpdate.setShowColumnFormType(true);
		} else {
			groupToUpdate.setShowColumnFormType(false);
		}
		if (visibleColumns.contains(VisibleColumnItems.showColumnTitle.name())) {
			groupToUpdate.setShowColumnTitle(true);
		} else {
			groupToUpdate.setShowColumnTitle(false);
		}
		if (visibleColumns.contains(VisibleColumnItems.showColumnStepEntryTime.name())) {
			groupToUpdate.setShowColumnStepEntryTime(true);
		} else {
			groupToUpdate.setShowColumnStepEntryTime(false);
		}
		if (visibleColumns.contains(VisibleColumnItems.showColumnCurrentFlow.name())) {
			groupToUpdate.setShowColumnCurrentFlow(true);
		} else {
			groupToUpdate.setShowColumnCurrentFlow(false);
		}
		if (visibleColumns.contains(VisibleColumnItems.showColumnCurrentStep.name())) {
			groupToUpdate.setShowColumnCurrentStep(true);
		} else {
			groupToUpdate.setShowColumnCurrentStep(false);
		}
		if (visibleColumns.contains(VisibleColumnItems.showColumnCurrentView.name())) {
			groupToUpdate.setShowColumnCurrentView(true);
		} else {
			groupToUpdate.setShowColumnCurrentView(false);
		}
		if (visibleColumns.contains(VisibleColumnItems.showColumnProgressPercentage.name())) {
			groupToUpdate.setShowColumnProgressPercentage(true);
		} else {
			groupToUpdate.setShowColumnProgressPercentage(false);
		}
		if (visibleColumns.contains(VisibleColumnItems.showColumnAttachment.name())) {
			groupToUpdate.setShowColumnAttachment(true);
		} else {
			groupToUpdate.setShowColumnAttachment(false);
		}

		//Buttons...
		if (visibleButtons.contains(VisibleButtonItems.showButtonBulkUpdate.name())) {
			groupToUpdate.setShowButtonBulkUpdate(true);
		} else {
			groupToUpdate.setShowButtonBulkUpdate(false);
		}
		if (visibleButtons.contains(VisibleButtonItems.showButtonExport.name())) {
			groupToUpdate.setShowButtonExport(true);
		} else {
			groupToUpdate.setShowButtonExport(false);
		}
		if (visibleButtons.contains(VisibleButtonItems.showButtonSendOn.name())) {
			groupToUpdate.setShowButtonSendOn(true);
		} else {
			groupToUpdate.setShowButtonSendOn(false);
		}
		if (visibleButtons.contains(VisibleButtonItems.showButtonDelete.name())) {
			groupToUpdate.setShowButtonDelete(true);
		} else {
			groupToUpdate.setShowButtonDelete(false);
		}
		if (visibleButtons.contains(VisibleButtonItems.showButtonLock.name())) {
			groupToUpdate.setShowButtonLock(true);
		} else {
			groupToUpdate.setShowButtonLock(false);
		}
		if (visibleButtons.contains(VisibleButtonItems.showButtonAddToPI.name())) {
			groupToUpdate.setShowButtonAddToPI(true);
		} else {
			groupToUpdate.setShowButtonAddToPI(false);
		}

		if (groupToUpdate.getWebKitViewSubs() != null) {
			groupToUpdate.getWebKitViewSubs().forEach(subItm -> {
				String key = this.generateGroupSubKey(groupToUpdate.getJobViewGroupName(), subItm.getLabel());

				subItm.setRouteFields(
						this.inputSubToRouteFieldMapping.get(key).getDataListing().stream()
						.filter(itm -> itm.isSelected())
						.collect(Collectors.toList()));
				subItm.setJobViews(
						this.inputSubToViewMapping.get(key).getDataListing().stream()
								.filter(itm -> itm.isSelected())
								.collect(Collectors.toList()));
			});
		}
	}

	public String generateGroupSubKey(String groupName, String subname) {
		return String.format("%s_%s", groupName, subname).toLowerCase();
	}

	public List<SelectItem> getUserQueriesAsSelectItems() {
		return this.webKitUserQueries.stream()
				.map(itm -> {
					UserQuery userQuery = itm.getUserQuery();
					return new SelectItem(userQuery.getId(), userQuery.getName());
				})
				.collect(Collectors.toList());
	}

	public WebKitUserQuery getUserQueryWithName(String userQueryName) {
		if (userQueryName == null) return null;
		return this.webKitUserQueries.stream()
				.filter(itm -> userQueryName.equals(itm.getUserQuery().getName()))
				.findFirst()
				.orElse(null);
	}

	public WebKitUserQuery getUserQueryWithUserQueryId(Long userQueryId) {
		if (userQueryId == null) return null;
		return this.webKitUserQueries.stream()
				.filter(itm -> userQueryId.equals(itm.getUserQuery().getId()))
				.findFirst()
				.orElse(null);
	}

	public String getLabelForUserQueryId(Long userQueryId) {
		if (userQueryId == null || userQueryId.longValue() < 1) {
			return "[None]";
		}
		return this.webKitUserQueries.stream()
				.filter(itm -> userQueryId.equals(itm.getUserQuery().getId()))
				.map(itm -> itm.getUserQuery().getName())
				.findFirst()
				.orElse("[Missing]");
	}

	public WebKitForm getWebKitFormWithFormDef(String formDefType) {
		if (formDefType == null) return null;
		return this.webKitForms.stream()
				.filter(itm -> formDefType.equals(itm.getForm().getFormType()))
				.findFirst()
				.orElse(null);
	}

	public List<SelectItem> extractTableFieldsFrom(String formDef) {
		List<Field> fieldsForFormDef = this.getFluidClientDSConfig().getFormFieldClient().getFieldsByFormNameAndLoggedInUser(
						formDef, false).getListing();
		List<SelectItem> returnVal = new ArrayList<>();
		if (fieldsForFormDef == null) return null;

		fieldsForFormDef.stream()
				.filter(itm -> Field.Type.Table == itm.getTypeAsEnum())
				.map(itm -> itm.getFieldName())
				.forEach(itm -> returnVal.add(new SelectItem(itm, itm)));
		return returnVal;
	}
}
