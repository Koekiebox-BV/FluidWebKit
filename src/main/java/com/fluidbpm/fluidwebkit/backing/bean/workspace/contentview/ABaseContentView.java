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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.user.User;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.*;

/**
 * Super class for all content view classes.
 *
 * @author jasonbruwer on 3/21/18.
 * @since 1.0
 */
public abstract class ABaseContentView implements Serializable{

	private Map<JobView, List<WorkspaceFluidItem>> fluidItemsForViews;

	//Header and Column Names
	private String[] sections;
	private Map<String, List<ABaseManagedBean.ColumnModel>> columnModels;

	//Initial Data...
	private Map<String, List<WorkspaceFluidItem>> fluidItemsForSection;

	//Filter...
	private Map<String,List<WorkspaceFluidItem>> fluidItemsForSectionFiltered;

	//Selected...
	private List<WorkspaceFluidItem> fluidItemsSelectedList;

	protected User loggedInUser;

	private String textToFilterBy;

	/**
	 *
	 * @param loggedInUserParam
	 * @param sectionsParam
	 */
	public ABaseContentView(User loggedInUserParam, Collection<String> sectionsParam) {
		this(loggedInUserParam,
				sectionsParam == null ? null : sectionsParam.toArray(new String[]{}),
				null);
	}

	/**
	 *
	 * @param loggedInUserParam
	 * @param sectionsParam
	 */
	public ABaseContentView(
			User loggedInUserParam,
			String[] sectionsParam) {

		this(loggedInUserParam, sectionsParam, null);
	}

	/**
	 *
	 * @param loggedInUserParam
	 * @param sectionsParam
	 * @param fluidItemsForViewsParam
	 */
	public ABaseContentView(
			User loggedInUserParam,
			Collection<String> sectionsParam,
			Map<JobView, List<WorkspaceFluidItem>> fluidItemsForViewsParam) {
		this(loggedInUserParam,
				sectionsParam == null ? null : sectionsParam.toArray(new String[]{}),
				fluidItemsForViewsParam);
	}

	/**
	 *
	 * @param loggedInUserParam
	 * @param sectionsParam
	 * @param fluidItemsForViewsParam
	 */
	public ABaseContentView(
			User loggedInUserParam,
			String[] sectionsParam,
			Map<JobView, List<WorkspaceFluidItem>> fluidItemsForViewsParam) {

		this.loggedInUser = loggedInUserParam;
		this.sections = sectionsParam;

		//Headers for Columns...
		this.columnModels = new HashMap<>();
		this.fluidItemsForSection = new HashMap<>();
		this.fluidItemsForSectionFiltered = new HashMap<>();
		this.fluidItemsSelectedList = new ArrayList<>();

		if (this.getSections() != null) {
			for (String section : this.getSections()) {
				//Sections Headers...
				List<ABaseManagedBean.ColumnModel> columnModForSection = this.getColumnHeadersForSection(section);
				if (columnModForSection != null) {
					this.columnModels.put(section, columnModForSection);
				}

				//Placeholder for Filter...
				this.fluidItemsForSection.put(section, new ArrayList<>());
			}
		}

		this.refreshData(fluidItemsForViewsParam);
	}

	/**
	 *
	 * @param sectionAliasParam
	 * @return
	 */
	public List<ABaseManagedBean.ColumnModel> getColumnHeadersForSection(String sectionAliasParam) {
		List<ABaseManagedBean.ColumnModel> returnVal = new ArrayList<>();
		if (this.getActiveMapBasedOnFilterCriteria() == null) {
			return returnVal;
		}

		List<WorkspaceFluidItem> workspaceItemsFromList =
				this.getActiveMapBasedOnFilterCriteria().get(sectionAliasParam);
		if (workspaceItemsFromList == null || workspaceItemsFromList.isEmpty()) {
			return returnVal;
		}

		workspaceItemsFromList.forEach(itm -> {
			List<Field> routeFields = itm.getRouteFields();
			if (routeFields == null || routeFields.isEmpty()) {
				return;
			}

			//Iterate the route fields ...
			routeFields.forEach(routeField -> {
				if (routeField.getFieldName() == null || routeField.getFieldName().trim().isEmpty()) {
					return;
				}
				String routeFieldNameLowerTrim = routeField.getFieldName().trim().toLowerCase();
				boolean containsVal = false;
				for (ABaseManagedBean.ColumnModel returnItem: returnVal) {
					if (returnItem.getFluidFieldName().trim().toLowerCase().equals(routeFieldNameLowerTrim)) {
						containsVal = true;
						break;
					}
				}

				if (!containsVal) {
					returnVal.add(
							new ABaseManagedBean.ColumnModel(
									routeField.getFieldName(),
									routeField.getFieldName(),
									routeField.getTypeAsEnum()));
				}
			});
		});

		return returnVal;
	}


	/**
	 *
	 * Method executed during data refresh to map the objects correctly.
	 *
	 * @param sectionParam
	 * @param fluidItemsForViewsParam
	 * @return
	 */
	public abstract List<WorkspaceFluidItem> getWorkspaceFluidItemsFor(
			String sectionParam,
			Map<JobView, List<WorkspaceFluidItem>> fluidItemsForViewsParam);


	/**
	 * Set the filtered results.
	 */
	public void actionSetFilteredList() {
		this.fluidItemsForSectionFiltered.clear();

		if(this.getTextToFilterBy() == null ||
				this.getTextToFilterBy().trim().isEmpty()) {
			this.fluidItemsForSectionFiltered.putAll(this.fluidItemsForSection);
		}

		String filterByTextLower = this.getTextToFilterBy().trim().toLowerCase();

		for (String section : this.getSections()) {
			List<WorkspaceFluidItem> itemsInSection = null;
			if(!this.fluidItemsForSection.containsKey(section) ||
					((itemsInSection = this.fluidItemsForSection.get(section)) == null))
			{
				continue;
			}

			List<WorkspaceFluidItem> itemsInSectionFiltered = new ArrayList<>();

			for (WorkspaceFluidItem item : itemsInSection) {
				List<Field> formFields = item.getFormFields();
				List<Field> routeFields = item.getRouteFields();

				List<Field> combinedFields = new ArrayList<>();

				//Form Fields...
				if(formFields != null){
					combinedFields.addAll(formFields);
				}

				//Route Fields...
				if(routeFields != null){
					combinedFields.addAll(routeFields);
				}

				//Form Type...
				if(item.getFluidItem().getForm().getFormType() != null &&
						item.getFluidItem().getForm().getFormType().trim().toLowerCase().contains(filterByTextLower))
				{
					itemsInSectionFiltered.add(item);
				}
				//Then Form and Route Fields...
				else if(!combinedFields.isEmpty()) {

					combinedFields.forEach(field ->
					{
						if(field.getFieldValue() != null)
						{
							String fieldValueTextLower =
									(field.getFieldValue().toString() == null ? null :
											field.getFieldValue().toString().trim().toLowerCase());

							if(fieldValueTextLower.contains(filterByTextLower))
							{
								itemsInSectionFiltered.add(item);
								return;
							}
						}
					});
				}
			}

			this.fluidItemsForSectionFiltered.put(section, itemsInSectionFiltered);
		}
	}

	/**
	 *
	 * #{empty workspaceBean.contentView.textToFilterBy ?
	 workspaceBean.contentView.fluidItemsForSection[sectionRptItem]:
	 workspaceBean.contentView.fluidItemsForSectionFiltered[sectionRptItem]

	 * @return
	 */
	public Map<String, List<WorkspaceFluidItem>> getActiveMapBasedOnFilterCriteria() {
		if (this.getTextToFilterBy() == null || this.getTextToFilterBy().trim().isEmpty()) {
			return this.getFluidItemsForSection();
		}

		return this.getFluidItemsForSectionFiltered();
	}

	/**
	 *
	 * [Section][0].fieldNameAndValMap['']
	 * Section->FieldName->RowIndex
	 *
	 * @param sectionItemParam
	 * @param fluidFieldNameParam
	 * @param rowIndexParam
	 * @return
	 */
	public Object getColumnValueFor(String sectionItemParam, String fluidFieldNameParam, int rowIndexParam) {
		List<WorkspaceFluidItem> workspaceItems =
				this.getWorkspaceFluidItemsForSection(sectionItemParam);

		if (workspaceItems == null || (rowIndexParam >= workspaceItems.size())) {
			return null;
		}

		WorkspaceFluidItem itemAtIndex = workspaceItems.get(rowIndexParam);
		String fieldValue = itemAtIndex.getFluidItem().getForm().getFieldValueAsString(fluidFieldNameParam);
		return fieldValue;
	}

	/**
	 *
	 * @param sectionNameParam
	 * @param comboFieldNameParam
	 * @return
	 */
	public List<SelectItem> getPossibleOptionsForSectionAndComboFieldName(
			String sectionNameParam,
			String comboFieldNameParam
	) {
		List<WorkspaceFluidItem> allItemsForSection =
				this.getWorkspaceFluidItemsForSection(sectionNameParam);

		if (allItemsForSection == null || allItemsForSection.isEmpty()) {
			return null;
		}

		Set<String> comboItem = new HashSet<>();
		for (WorkspaceFluidItem itm : allItemsForSection) {
			if (itm.getFluidItem() != null && itm.getFluidItem().getForm() != null) {
				String valAsTxt =
						itm.getFluidItem().getForm().getFieldValueAsString(comboFieldNameParam);
				if (valAsTxt == null) {
					continue;
				}

				comboItem.add(valAsTxt);
			}
		}

		List<SelectItem> returnVal = new ArrayList<>();

		comboItem.forEach(cmbItm -> {
			returnVal.add(new SelectItem(cmbItm,cmbItm));
		});

		return returnVal;
	}

	/**
	 *
	 * @return
	 */
	public Map<String,List<WorkspaceFluidItem>> getFluidItemsForSectionFiltered() {
		return this.fluidItemsForSectionFiltered;
	}

	/**
	 *
	 * @param fluidItemsForSectionFilteredParam
	 */
	public void setFluidItemsForSectionFiltered(Map<String,List<WorkspaceFluidItem>> fluidItemsForSectionFilteredParam) {
		this.fluidItemsForSectionFiltered = fluidItemsForSectionFilteredParam;
	}

	/**
	 *
	 * @return
	 */
	public List<WorkspaceFluidItem> getFluidItemsSelectedList() {
		return this.fluidItemsSelectedList;
	}

	/**
	 *
	 * @param fluidItemsSelectedListParam
	 */
	public void setFluidItemsSelectedList(List<WorkspaceFluidItem> fluidItemsSelectedListParam) {
		this.fluidItemsSelectedList = fluidItemsSelectedListParam;
	}

	/**
	 *
	 * @param sectionParam
	 * @return
	 */
	public List<WorkspaceFluidItem> getWorkspaceFluidItemsForSection(String sectionParam) {
		if (this.fluidItemsForSection == null) {
			return null;
		}

		//Key not found, just return new arraylist...
		if (!this.fluidItemsForSection.containsKey(sectionParam)) {
			return new ArrayList<>();
		}

		return this.fluidItemsForSection.get(sectionParam);
	}

	/**
	 *
	 * @param sectionParam
	 * @param newListParam
	 */
	public void putWorkspaceFluidItemsForSection(
			String sectionParam, List<WorkspaceFluidItem> newListParam
	) {
		if (this.fluidItemsForSection == null) {
			return;
		}

		this.fluidItemsForSection.put(sectionParam, newListParam);
	}

	/**
	 *
	 * @param fluidItemsForViewsParam
	 */
	public void refreshData(Map<JobView, List<WorkspaceFluidItem>> fluidItemsForViewsParam) {
		this.fluidItemsForViews = fluidItemsForViewsParam;
		if (this.fluidItemsForSection == null) {
			this.fluidItemsForSection = new HashMap<>();
		}

		//Clear all data...
		this.fluidItemsForSection.values().clear();
		this.fluidItemsForSection.clear();

		if (this.getSections() == null) {
			return;
		}

		if (fluidItemsForViewsParam == null || fluidItemsForViewsParam.isEmpty()) {
			return;
		}

		//Load Items for each section...
		for (String section : this.getSections()) {
			if (section == null || section.trim().isEmpty()) {
				continue;
			}

			List<WorkspaceFluidItem> itemsForSection =
					this.getWorkspaceFluidItemsFor(section, fluidItemsForViewsParam);

			if (itemsForSection == null || itemsForSection.isEmpty()) {
				continue;
			}

			this.fluidItemsForSection.put(section, itemsForSection);
		}
	}

	/**
	 *
	 * @param booleansToCheckParam
	 * @return
	 */
	protected final boolean isAnyTrue(boolean ... booleansToCheckParam) {
		if (booleansToCheckParam == null || booleansToCheckParam.length == 0) {
			return false;
		}

		for (boolean toCheck : booleansToCheckParam) {
			if (toCheck) {
				return true;
			}
		}

		return false;
	}

	/**
	 *
	 * @return
	 */
	public String[] getSections() {
		return this.sections;
	}

	/**
	 *
	 * @param sectionParam
	 * @return
	 */
	public boolean areItemsForSectionEmpty(String sectionParam) {
		if ((sectionParam == null || sectionParam.trim().isEmpty())) {
			return true;
		}

		if(this.getWorkspaceFluidItemsForSection(sectionParam) == null ||
				this.getWorkspaceFluidItemsForSection(sectionParam).isEmpty()) {
			return true;
		}

		return false;
	}

	/**
	 *
	 * @return
	 */
	public Map<String, List<WorkspaceFluidItem>> getFluidItemsForSection() {
		return this.fluidItemsForSection;
	}

	/**
	 *
	 * @param columnValueParam
	 * @param filterValParam
	 * @param localeParam
	 * @return
	 */
	@Deprecated
	public boolean filterByProvided(
			Object columnValueParam,
			Object filterValParam,
			Locale localeParam
	) {
		//return true or false

		String filterText = (filterValParam == null) ? null : filterValParam.toString().trim();
		if (filterText == null || filterText.trim().isEmpty()) {
			return true;
		}

		if (columnValueParam == null) {
			return false;
		}

		return true;
	}

	/**
	 *
	 * @return
	 */
	public WorkspaceFluidItem getFirstSelectedWorkspaceFluidItem() {
		List<WorkspaceFluidItem> selectedItems = this.getFluidItemsSelectedList();
		if (selectedItems == null || selectedItems.isEmpty()) {
			return null;
		}

		return selectedItems.get(0);
	}

	/**
	 *
	 * @param formIdParam
	 * @return
	 */
	public WorkspaceFluidItem getWSFIItemByIdFromCache(Long formIdParam) {
		return this.getWSFIItemByIdFromCache(formIdParam, null);
	}

	/**
	 *
	 * @param formIdParam
	 * @param typeParam
	 * @return
	 */
	public WorkspaceFluidItem getWSFIItemByIdFromCache(
			Long formIdParam,
			Class typeParam
    ) {
		if (this.fluidItemsForSection == null || this.fluidItemsForSection.isEmpty()) {
			return null;
		}

		Set<String> keys = this.fluidItemsForSection.keySet();

		String classTypeParam = (typeParam == null) ? null: typeParam.getName();

		for (String key : keys) {
			List<WorkspaceFluidItem> itemsInSection = this.fluidItemsForSection.get(key);
			if (itemsInSection == null || itemsInSection.isEmpty()) {
				continue;
			}

			for (WorkspaceFluidItem itemToCheck : itemsInSection) {
				if ((classTypeParam == null || itemToCheck.getBaseWeb().getClass().getName().equals(classTypeParam))
						&& itemToCheck.getFluidItem().getForm().getId().equals(formIdParam)) {
					return itemToCheck;
				}
			}
		}

		return null;
	}

	/**
	 *
	 * @param jobViewIdParam
	 * @return
	 */
	public JobView retrieveJobViewFromJobViewId(Long jobViewIdParam) {
		if (jobViewIdParam == null) {
			return null;
		}

		if (this.fluidItemsForViews == null) {
			return null;
		}

		Set<JobView> resJobViewKeys = this.fluidItemsForViews.keySet();
		for (JobView resJobView : resJobViewKeys) {
			if (jobViewIdParam.equals(resJobView.getId())) {
				return resJobView;
			}
		}

		return null;
	}

	/**
	 *
	 * @return
	 */
	public String getTextToFilterBy() {
		return this.textToFilterBy;
	}

	/**
	 *
	 * @param textToFilterByParam
	 */
	public void setTextToFilterBy(String textToFilterByParam) {
		this.textToFilterBy = textToFilterByParam;
	}

	/**
	 *
	 * @param sectionNameParam
	 * @return
	 */
	public boolean isRenderActionColumn(String sectionNameParam){
		return true;
	}
}
