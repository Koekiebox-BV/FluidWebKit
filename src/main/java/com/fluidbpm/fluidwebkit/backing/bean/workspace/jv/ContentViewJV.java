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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.jv;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.ABaseContentView;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.WebKitViewContentModelBean;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceRouteField;
import lombok.Getter;

import javax.faces.model.SelectItem;
import java.util.*;
import java.util.stream.Collectors;

import static com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceBean.TGM_TABLE_PER_VIEW_SECTION_DEL;
import static com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceBean.TGM_TABLE_PER_VIEW_SECTION_DEL_LEN;

public class ContentViewJV extends ABaseContentView {
	@Getter
	private WebKitViewGroup wkGroup;

	@Getter
	private WebKitViewSub wkSub;

	private Map<String, WorkspaceJobViewLDM> fluidItemsLazyModel = null;

	public static final class SystemField {
		public static final String DATE_CREATED = "Date Created";
		public static final String DATE_LAST_UPDATED = "Date Last Updated";
		public static final String DATE_STEP_ENTERED = "Date Step Entered";
		public static final String FORM_TYPE = "Form Type";
		public static final String FLOW = "Flow";
		public static final String STEP = "Step";
		public static final String VIEW = "View";
		public static final String ID = "ID";
		public static final String TITLE = "Title";
	}

	public ContentViewJV(User loggedInUserParam, WebKitViewContentModelBean webKitViewContentModelBean) {
		super(loggedInUserParam, new String[]{}, webKitViewContentModelBean);
	}

	public ContentViewJV(
		User loggedInUserParam,
		List<String> subs,
		WebKitViewGroup webKitGroup,
		WebKitViewSub selectedSub,
		WebKitViewContentModelBean webKitViewContentModelBean
	) {
		super(loggedInUserParam, subs, webKitViewContentModelBean);
		this.wkGroup = webKitGroup;
		this.wkSub = selectedSub;
	}

	@Override
	public List<WorkspaceFluidItem> getWorkspaceFluidItemsFor(
		String sectionParam,
		Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data
	) {
		if (data == null || data.isEmpty()) return new ArrayList<>();

		if (this.wkGroup.isTGMCombined()) {
			List<WorkspaceFluidItem> returnVal = new ArrayList<>();
			returnVal.addAll(data.values().stream()
					.map(itm -> itm.values())
					.flatMap(Collection::stream)
					.flatMap(Collection::stream)
					.collect(Collectors.toList()));
			return returnVal;
		} else if (this.wkGroup.isTGMTablePerSub()) {
			List<WorkspaceFluidItem> returnVal = new ArrayList<>();
			data.forEach((sub, viewMap) -> {
				if (sectionParam.equals(sub.getLabel())) {
					returnVal.addAll(viewMap.values().stream()
							.flatMap(Collection::stream)
							.collect(Collectors.toList()));
					return;
				}
			});
			return returnVal;
		} else if (this.wkGroup.isTGMTablePerView()) {
			int indexOfSeparator = sectionParam.indexOf(TGM_TABLE_PER_VIEW_SECTION_DEL);
			if (indexOfSeparator > -1) {
				String subLabel = sectionParam.substring(0, indexOfSeparator),
						viewName = sectionParam.substring(indexOfSeparator + TGM_TABLE_PER_VIEW_SECTION_DEL_LEN);
				List<WorkspaceFluidItem> returnVal = new ArrayList<>();
				data.forEach((sub, viewMap) -> {
					if (subLabel.equals(sub.getLabel())) {
						viewMap.forEach((view, items) -> {
							if (viewName.equals(view.getJobView().getViewName())) {
								returnVal.addAll(items);
								return;
							}
						});
					}
				});
				return returnVal;
			}
		}
		return new ArrayList<>();
	}

	@Override
	public void refreshData(Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data) {
		super.refreshData(data);
		if (this.getSections() == null) return;

		if (this.fluidItemsLazyModel == null) this.fluidItemsLazyModel = new HashMap<>();

		//Load Items for each section...
		List<String> sectionsToKeep = new ArrayList<>();
		for (String section : this.getSections()) {
			List<WorkspaceFluidItem> items = this.getFluidItemsForSection().get(section);
			WorkspaceJobViewLDM workFluidItmLazy = this.fluidItemsLazyModel.getOrDefault(section, new WorkspaceJobViewLDM());
			workFluidItmLazy.clearInitialListing();

			if (items != null) {
				//Sort the items by when they entered...
				items.sort(Comparator.comparing(WorkspaceFluidItem::getFluidItemStepEnteredTimestamp));
				workFluidItmLazy.addToInitialListing(items);
			}

			this.fluidItemsLazyModel.put(section, workFluidItmLazy);
			if ((this.getWkGroup() == null || this.getWkGroup().isEnableRenderEmptyTable()) ||
					!workFluidItmLazy.getDataListing().isEmpty()) {
				sectionsToKeep.add(section);
			}
		}
		this.sections = sectionsToKeep.toArray(new String[]{});
	}

	/**
	 * Retrieve all the column models for section {@code sectionAliasParam}.
	 *
	 * @param sectionAlias The section to retrieve column model headers for.
	 * @return ABaseManagedBean.ColumnModel list.
	 *
	 * @see ABaseManagedBean.ColumnModel
	 * @see List
	 */
	@Override
	public List<ABaseManagedBean.ColumnModel> getColumnHeadersForSection(String sectionAlias) {
		List<ABaseManagedBean.ColumnModel> returnVal = new ArrayList<>();
		if (this.wkGroup == null) return returnVal;
		if (this.wkSub == null) return returnVal;

		//Index - 01 - Attachment
		returnVal.add(new ABaseManagedBean.ColumnModel(
				this.wkGroup.getAttachmentColumnLabel(),
				this.wkGroup.getAttachmentColumnLabel(),
				Field.Type.Label,
				META_DATA_PLAIN,
				this.wkSub.isShowColumnAttachment(),
				true,
				false));

		//Index - 02 - ID
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.ID,
				SystemField.ID,
				Field.Type.Text,
				META_DATA_PLAIN,
				this.wkSub.isShowColumnID(),
				true,
				true));

		//Index - 03 - Title
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.TITLE,
				SystemField.TITLE,
				Field.Type.Text,
				META_DATA_PLAIN,
				this.wkSub.isShowColumnTitle(),
				true,
				true));

		//Index - 04 - Form Type
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.FORM_TYPE,
				SystemField.FORM_TYPE,
				Field.Type.MultipleChoice,
				META_DATA_PLAIN,
				this.wkSub.isShowColumnFormType(),
				true,
				true));

		//Index - 05 - Flow
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.FLOW,
				SystemField.FLOW,
				Field.Type.MultipleChoice,
				META_DATA_PLAIN,
				this.wkSub.isShowColumnCurrentFlow(),
				true,
				true));

		//Index - 06 - Step
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.STEP,
				SystemField.STEP,
				Field.Type.MultipleChoice,
				META_DATA_PLAIN,
				this.wkSub.isShowColumnCurrentStep(),
				true,
				true));

		//Index - 07 - View
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.VIEW,
				SystemField.VIEW,
				Field.Type.MultipleChoice,
				META_DATA_PLAIN,
				this.wkSub.isShowColumnCurrentView(),
				true,
				true));

		//Index - 08 - Date Step Entered
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.DATE_STEP_ENTERED,
				SystemField.DATE_STEP_ENTERED,
				Field.Type.DateTime,
				META_DATA_PLAIN,
				this.wkSub.isShowColumnStepEntryTime(),
				true,
				true));

		//Index - 09 - Date Created
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.DATE_CREATED,
				SystemField.DATE_CREATED,
				Field.Type.DateTime,
				META_DATA_PLAIN,
				this.wkSub.isShowColumnDateCreated(),
				true,
				true));

		//Index - 10 - Date Last Updated
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.DATE_LAST_UPDATED,
				SystemField.DATE_LAST_UPDATED,
				Field.Type.DateTime,
				META_DATA_PLAIN,
				this.wkSub.isShowColumnDateLastUpdated(),
				true,
				true));

		if (this.wkSub == null) return returnVal;

		List<WebKitWorkspaceRouteField> routeFieldsForKit = this.wkSub.getRouteFields();
		if (routeFieldsForKit == null || routeFieldsForKit.isEmpty()) return returnVal;

		routeFieldsForKit.sort(Comparator.comparing(WebKitWorkspaceRouteField::getFieldOrder));
		List<ABaseManagedBean.ColumnModel> returnValCustomRouteFields = routeFieldsForKit.stream()
				.map(wkField -> {
					Field field = wkField.getRouteField();
					boolean canFilter = false;
					switch (field.getTypeAsEnum()) {
						case Text:
						case DateTime:
						case Decimal:
						case MultipleChoice:
							canFilter = true;
					}

					return new ABaseManagedBean.ColumnModel(
							field.getFieldName(),
							field.getFieldName(),
							field.getTypeAsEnum(),
							field.getTypeMetaData(),
							true,
							true,
							canFilter);
				})
				.collect(Collectors.toList());
		returnVal.addAll(returnValCustomRouteFields);
		return returnVal;
	}

	@Override
	public List<SelectItem> getPossibleCombinationsMapAsSelectItemsFor(String section, String fieldName) {
		if ((section == null || section.trim().isEmpty()) || (fieldName == null || fieldName.trim().isEmpty())) return new ArrayList<>();

		List<WorkspaceFluidItem> workItemsForSection = this.getWorkspaceFluidItemsForSection(section);
		if (workItemsForSection == null) return new ArrayList<>();

		Set<String> setOfPossibleOptions = new HashSet<>();
		workItemsForSection.stream()
				.forEach(itm -> {
					Map<String, Object> map = itm.getFieldMap();
					Object fieldVal = (map == null) ? null : map.get(fieldName);
					if (fieldVal == null) {
						switch (fieldName) {
							case SystemField.FORM_TYPE:
								fieldVal = itm.getFluidItemFormType();
							break;
							case SystemField.DATE_CREATED:
								fieldVal = itm.getFluidItemForm().getDateCreated();
							break;
							case SystemField.DATE_LAST_UPDATED:
								fieldVal = itm.getFluidItemForm().getDateLastUpdated();
							break;
							case SystemField.DATE_STEP_ENTERED:
								fieldVal = itm.getFluidItem().getStepEnteredTime();
							break;
							case SystemField.FLOW:
								fieldVal = itm.getFluidItemFlow();
							break;
							case SystemField.STEP:
								fieldVal = itm.getFluidItemStep();
							break;
							case SystemField.VIEW:
								fieldVal = itm.getFluidItemView();
							break;
							case SystemField.TITLE:
								fieldVal = itm.getFluidItemTitle();
							break;
						}
					}
					setOfPossibleOptions.add(fieldVal.toString());
				});
		List<SelectItem> returnVal = setOfPossibleOptions.stream()
				.map(itm -> new SelectItem(itm, itm))
				.collect(Collectors.toList());

		//Sort by Label...
		Collections.sort(returnVal, Comparator.comparing(SelectItem::getLabel));
		return returnVal;
	}
}
