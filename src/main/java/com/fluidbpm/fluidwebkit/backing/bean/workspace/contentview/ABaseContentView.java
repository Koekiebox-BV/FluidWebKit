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
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.Visibility;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.*;

/**
 * Super class for all content view classes.
 *
 * The content view classes are used by the workspace bean to
 * indicate which sections should be displayed.
 *
 * @author jasonbruwer on 3/21/18.
 * @since 1.0
 *
 * @see com.fluidbpm.fluidwebkit.backing.bean.workspace.ABaseWorkspaceBean
 */
public abstract class ABaseContentView implements Serializable {

	private Map<JobView, List<WorkspaceFluidItem>> fluidItemsForViews;

	//Header and Column Names
	private String[] sections;
	private Map<String, List<ABaseManagedBean.ColumnModel>> columnModels;

	//Initial Data...
	private Map<String, List<WorkspaceFluidItem>> fluidItemsForSection;

	//Filter...
	private Map<String,List<WorkspaceFluidItem>> fluidItemsForSectionFiltered;

	//Selected...
	@Getter
	@Setter
	private List<WorkspaceFluidItem> fluidItemsSelectedList;

	protected User loggedInUser;
	private String textToFilterBy;

	/**
	 * Base to set logged in user and applicable sections.
	 * 
	 * @param loggedInUserParam The currently logged in user.
	 * @param sectionsParam The list of sections applicable to the view.
	 */
	public ABaseContentView(User loggedInUserParam, Collection<String> sectionsParam) {
		this(loggedInUserParam,
				sectionsParam == null ? null : sectionsParam.toArray(new String[]{}),
				null);
	}

	/**
	 * Base to set logged in user and applicable sections.
	 * 
	 * @param loggedInUserParam The currently logged in user.
	 * @param sectionsParam The list of sections applicable to the view.
	 */
	public ABaseContentView(
		User loggedInUserParam,
		String[] sectionsParam
	) {
		this(loggedInUserParam, sectionsParam, null);
	}

	/**
	 * Base to set logged in user and applicable sections.
	 *
	 * @param loggedInUserParam The currently logged in user.
	 * @param sectionsParam The list of sections applicable to the view.
	 * @param fluidItemsForViewsParam The mapped fluid items per view.
	 *
	 * @see JobView
	 * @see WorkspaceFluidItem
	 */
	public ABaseContentView(
		User loggedInUserParam,
		Collection<String> sectionsParam,
		Map<JobView, List<WorkspaceFluidItem>> fluidItemsForViewsParam
	) {
		this(loggedInUserParam,
				sectionsParam == null ? null : sectionsParam.toArray(new String[]{}),
				fluidItemsForViewsParam);
	}

	/**
	 * Base to set logged in user and applicable sections.
	 *
	 * @param loggedInUserParam The currently logged in user.
	 * @param sectionsParam The list of sections applicable to the view.
	 * @param fluidItemsForViewsParam The mapped fluid items per view.
	 *
	 * @see JobView
	 * @see WorkspaceFluidItem
	 */
	public ABaseContentView(
		User loggedInUserParam,
		String[] sectionsParam,
		Map<JobView, List<WorkspaceFluidItem>> fluidItemsForViewsParam
	) {
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
	 * Retrieve all the column models for section {@code sectionAliasParam}.
	 *
	 * @param sectionAliasParam The section to retrieve column model headers for.
	 * @return ABaseManagedBean.ColumnModel list.
	 *
	 * @see ABaseManagedBean.ColumnModel
	 * @see List
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
	 * Method executed during data refresh to map the objects correctly.
	 *
	 * @param sectionParam The section to retrieve data for.
	 * @param fluidItemsForViewsParam All items and their views.
	 *
	 * @return List of {@code WorkspaceFluidItem} items.
	 *
	 * @see WorkspaceFluidItem
	 * @see JobView
	 */
	public abstract List<WorkspaceFluidItem> getWorkspaceFluidItemsFor(
			String sectionParam,
			Map<JobView, List<WorkspaceFluidItem>> fluidItemsForViewsParam);

	/**
	 *
	 * @param event
	 */
	public void actionOnRowToggle(ToggleEvent event) {
		if (event.getVisibility() == Visibility.VISIBLE) {
			Object data = event.getData();

		}
	}


	public String getDeleteButtonMessage() {
		if (this.hasSelectedItems()) {
			int size = this.fluidItemsSelectedList.size();
			return size > 1 ? size + " items selected" : "1 item selected";
		}

		return "Remove";
	}

	public boolean hasSelectedItems() {
		return this.fluidItemsSelectedList != null && !this.fluidItemsSelectedList.isEmpty();
	}

	/**
	 * The JSF event to be fired when the list of filter items need to be updated.
	 * The {@code textToFilterBy} value needs to be set.
	 *
	 * The filtered listing value {@code this.fluidItemsForSectionFiltered} will be updated with the
	 * filtered data.
	 * 
	 * @see #getTextToFilterBy
	 */
	public void actionSetFilteredList() {
		this.fluidItemsForSectionFiltered.clear();
		if (this.getTextToFilterBy() == null || this.getTextToFilterBy().trim().isEmpty()) {
			this.fluidItemsForSectionFiltered.putAll(this.fluidItemsForSection);
		}

		String filterByTextLower = this.getTextToFilterBy().trim().toLowerCase();

		for (String section : this.getSections()) {
			List<WorkspaceFluidItem> itemsInSection = null;
			if (!this.fluidItemsForSection.containsKey(section) ||
					((itemsInSection = this.fluidItemsForSection.get(section)) == null)
			) {
				continue;
			}

			List<WorkspaceFluidItem> itemsInSectionFiltered = new ArrayList<>();

			for (WorkspaceFluidItem item : itemsInSection) {
				List<Field> formFields = item.getFormFields();
				List<Field> routeFields = item.getRouteFields();

				List<Field> combinedFields = new ArrayList<>();

				//Form Fields...
				if (formFields != null) {
					combinedFields.addAll(formFields);
				}

				//Route Fields...
				if (routeFields != null) {
					combinedFields.addAll(routeFields);
				}

				//Form Type...
				if (item.getFluidItem().getForm().getFormType() != null &&
						item.getFluidItem().getForm().getFormType().trim().toLowerCase().contains(filterByTextLower)) {
					itemsInSectionFiltered.add(item);
				} else if (!combinedFields.isEmpty()) {
					//Then Form and Route Fields...
					combinedFields.forEach(field -> {
						if (field.getFieldValue() != null) {
							String fieldValueTextLower =
									(field.getFieldValue().toString() == null ? null :
											field.getFieldValue().toString().trim().toLowerCase());

							if (fieldValueTextLower.contains(filterByTextLower)) {
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
	 * The method to tie to the PrimeFaces {@code <p:dataTable/>} in order to map each section
	 * to its PrimeFaces datatable.
	 *
	 * #{empty workspaceBean.contentView.textToFilterBy ?
	 * workspaceBean.contentView.fluidItemsForSection[sectionRptItem]:
	 * workspaceBean.contentView.fluidItemsForSectionFiltered[sectionRptItem]
	 *
	 * @return Map of sections and their {@code WorkspaceFluidItem}'s.
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
	 * @param sectionItemParam The section/datatable to retrieve data from.
	 * @param fluidFieldNameParam The name of the Fluid field.
	 * @param rowIndexParam The row the column should be fetched from.
	 *    
	 * @return Value as a java {@code Object}
	 */
	public Object getColumnValueFor(String sectionItemParam, String fluidFieldNameParam, int rowIndexParam) {
		List<WorkspaceFluidItem> workspaceItems = this.getWorkspaceFluidItemsForSection(sectionItemParam);
		if (workspaceItems == null || (rowIndexParam >= workspaceItems.size())) {
			return null;
		}

		WorkspaceFluidItem itemAtIndex = workspaceItems.get(rowIndexParam);
		String fieldValue = itemAtIndex.getFluidItem().getForm().getFieldValueAsString(fluidFieldNameParam);
		return fieldValue;
	}

	/**
	 * Retrieve the list of possible select items for a field in a datatable.
	 * 
	 * @param sectionNameParam The section to retrieve the combo-box values from.
	 * @param comboFieldNameParam The name of the field.
	 * @return List of {@code SelectItem}'s.
	 *
	 * @see SelectItem
	 */
	public List<SelectItem> getPossibleOptionsForSectionAndComboFieldName(
		String sectionNameParam, String comboFieldNameParam
	) {
		List<WorkspaceFluidItem> allItemsForSection = this.getWorkspaceFluidItemsForSection(sectionNameParam);
		if (allItemsForSection == null || allItemsForSection.isEmpty()) {
			return null;
		}

		Set<String> comboItem = new HashSet<>();
		for (WorkspaceFluidItem itm : allItemsForSection) {
			if (itm.getFluidItem() != null && itm.getFluidItem().getForm() != null) {
				String valAsTxt = itm.getFluidItem().getForm().getFieldValueAsString(comboFieldNameParam);
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
	 * Retrieve the filtered list of items if a filter is active.
	 * The text lookup is a global filter. While each {@code p:dataTable} also has a filter.
	 *
	 * @return {@code Map<String,List<WorkspaceFluidItem>>} mapping of data table items filtered.
	 */
	public Map<String,List<WorkspaceFluidItem>> getFluidItemsForSectionFiltered() {
		return this.fluidItemsForSectionFiltered;
	}

	/**
	 * Retrieve the filtered list of items if a filter is active.
	 * The text lookup is a global filter. While each {@code p:dataTable} also has a filter.
	 *
	 * @param fluidItemsForSectionFilteredParam = {@code Map<String,List<WorkspaceFluidItem>>} mapping of data table items filtered.
	 */
	public void setFluidItemsForSectionFiltered(Map<String,List<WorkspaceFluidItem>> fluidItemsForSectionFilteredParam) {
		this.fluidItemsForSectionFiltered = fluidItemsForSectionFilteredParam;
	}

	/**
	 * Get the list of workitems for section {@code sectionsParam}.
	 * @param sectionParam The section to retrieve the {@code List} of {@code WorkspaceFluidItem} for.
	 *
	 * @return List of {@code WorkspaceFluidItem}
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
	 * Update the {@code WorkspaceFluidItem}'s list for {@code Map} with key {@code sectionParam}.
	 * 
	 * @param sectionParam The section to update.
	 * @param newListParam New list of items.
	 */
	public void putWorkspaceFluidItemsForSection(
		String sectionParam,
		List<WorkspaceFluidItem> newListParam
	) {
		if (this.fluidItemsForSection == null) {
			return;
		}

		this.fluidItemsForSection.put(sectionParam, newListParam);
	}

	/**
	 * Refresh the fluid items ({@code List<WorkspaceFluidItem>}) for {@code this} content view.
	 * 
	 * @param fluidItemsForViewsParam The updated list of {@code WorkspaceFluidItem}'s for each section.
	 *
	 * @see JobView
	 * @see WorkspaceFluidItem
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
	 * Check if any one of the {@code booleansToCheckParam} values are {@code true}.
	 *
	 * @param booleansToCheckParam The boolean values to check.
	 * @return {@code true} If any one of the elements for {@code booleansToCheckParam} is {@code true}.
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
	 * Get all the sections.
	 * Each section is usually a {@code p:dataTable}.
	 * 
	 * @return All of the available sections.
	 */
	public String[] getSections() {
		return this.sections;
	}

	/**
	 * Verify if a section has any workitems.
	 * 
	 * @param sectionParam Confirm whether the {@code sectionParam} has any workitems.
	 * @return {@code true} if the section {@code sectionParam} is empty, otherwise {@code false}.
	 */
	public boolean areItemsForSectionEmpty(String sectionParam) {
		if ((sectionParam == null || sectionParam.trim().isEmpty())) {
			return true;
		}

		if (this.getWorkspaceFluidItemsForSection(sectionParam) == null ||
				this.getWorkspaceFluidItemsForSection(sectionParam).isEmpty()) {
			return true;
		}

		return false;
	}

	/**
	 * Get all the items for a section.
	 *
	 * @return {@code Map} of sections and their workitems.
	 * @see WorkspaceFluidItem
	 */
	public Map<String, List<WorkspaceFluidItem>> getFluidItemsForSection() {
		return this.fluidItemsForSection;
	}

	/**
	 * Get the first item from the list of selected items.
	 *
	 * @return {@code WorkspaceFluidItem} First entry from the selected Fluid items.
	 *
	 * @see this#getFluidItemsSelectedList
	 */
	public WorkspaceFluidItem getFirstSelectedWorkspaceFluidItem() {
		List<WorkspaceFluidItem> selectedItems = this.getFluidItemsSelectedList();
		if (selectedItems == null || selectedItems.isEmpty()) {
			return null;
		}
		
		return selectedItems.get(0);
	}

	/**
	 * Get {@code WorkspaceFluidItem} where the Form id is {@code formIdParam}.
	 * 
	 * @param formIdParam The Form id to retrieve from view cache.
	 * @return {@code WorkspaceFluidItem} with Form id {@code formIdParam}.
	 *
	 * @see com.fluidbpm.program.api.vo.form.Form
	 * @see WorkspaceFluidItem
	 */
	public WorkspaceFluidItem getWSFIItemByIdFromCache(Long formIdParam) {
		return this.getWSFIItemByIdFromCache(formIdParam, null);
	}

	/**
	 * Locates the {@code WorkspaceFluidItem} with Form ID {@code formIdParam} in all of the sections.
	 * The {@code typeParam} may be {@code null}.
	 *
	 * @param formIdParam The ID of the form to retrieve.
	 * @param typeParam The class type to filter by. May be {@code null}.
	 * 
	 * @return First {@code WorkspaceFluidItem} with Form Id {@code formIdParam} and of class type {@code typeParam}.
	 */
	public WorkspaceFluidItem getWSFIItemByIdFromCache(Long formIdParam, Class typeParam) {
		if (this.fluidItemsForSection == null || this.fluidItemsForSection.isEmpty()) {
			return null;
		}
		String classTypeParam = (typeParam == null) ? null : typeParam.getName();
		
		return this.fluidItemsForSection.keySet().stream()
				.map(itm -> this.fluidItemsForSection.get(itm))
				.filter(itm -> itm != null && !itm.isEmpty())
				.flatMap(toFlat -> toFlat.stream())
				.filter(itm -> ((classTypeParam == null || itm.getBaseWeb().getClass().getName().equals(classTypeParam))
						&& itm.getFluidItem().getForm().getId().equals(formIdParam)))
				.findFirst()
				.orElse(null);
	}

	/**
	 * Retrieve the {@code JobView} with Id {@code jobViewIdParam}.
	 *
	 * @param jobViewIdParam The ID of the {@code JobView} to retrieve.
	 * @return Retrieve the {@code JobView} with Id {@code jobViewIdParam}.
	 *
	 * @see JobView
	 */
	public JobView retrieveJobViewFromJobViewId(Long jobViewIdParam) {
		if (jobViewIdParam == null) {
			return null;
		}
		if (this.fluidItemsForViews == null) {
			return null;
		}

		return this.fluidItemsForViews.keySet().stream()
				.filter(itm -> jobViewIdParam.equals(itm.getId()))
				.findFirst()
				.orElse(null);
	}

	/**
	 * Get the entered text to filter all items by.
	 * 
	 * @return String - Entered text as {@code String}.
	 */
	public String getTextToFilterBy() {
		return this.textToFilterBy;
	}

	/**
	 * Get the entered text to filter all items by.
	 * 
	 * @param textToFilterByParam String - Entered text as {@code String}.
	 */
	public void setTextToFilterBy(String textToFilterByParam) {
		this.textToFilterBy = textToFilterByParam;
	}

	/**
	 * Should the Action column be rendered for the section {@code sectionNameParam}.
	 * 
	 * @param sectionNameParam The name of the section to confirm if it should be rendered.
	 * @return {@code true}
	 */
	public boolean isRenderActionColumn(String sectionNameParam){
		return true;
	}
}
