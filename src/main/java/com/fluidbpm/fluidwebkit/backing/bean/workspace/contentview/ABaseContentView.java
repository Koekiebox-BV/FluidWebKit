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
import com.fluidbpm.fluidwebkit.backing.utility.RaygunUtil;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.Visibility;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

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

	private Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data;

	//Header and Column Names
	protected String[] sections;

	//Initial Data...
	private Map<String, List<WorkspaceFluidItem>> fluidItemsForSection;

	//Filter...
	private Map<String, List<WorkspaceFluidItem>> fluidItemsForSectionFiltered = new HashMap<>();

	//Selected...
	@Getter
	@Setter
	private List<WorkspaceFluidItem> fluidItemsSelectedList;

	private Map<String, Map<String,String[]>> filterBySelectItemMap;
	private Map<String,Map<String,String>> filterByTextValueMap;
	private Map<String,Map<String,Double>> filterByDecimalValueMap;

	protected User loggedInUser;
	private String textToFilterBy;

	protected WebKitViewContentModelBean webKitViewContentModelBean;

	/**
	 * Base to set logged in user and applicable sections.
	 * 
	 * @param loggedInUserParam The currently logged in user.
	 * @param sectionsParam The list of sections applicable to the view.
	 */
	public ABaseContentView(
			User loggedInUserParam,
			Collection<String> sectionsParam,
			WebKitViewContentModelBean webKitViewContentModelBean
	) {
		this(loggedInUserParam,
				sectionsParam == null ? null : sectionsParam.toArray(new String[]{}),
				null, webKitViewContentModelBean);
	}

	/**
	 * Base to set logged in user and applicable sections.
	 * 
	 * @param loggedInUserParam The currently logged in user.
	 * @param sectionsParam The list of sections applicable to the view.
	 */
	public ABaseContentView(
		User loggedInUserParam,
		String[] sectionsParam,
		WebKitViewContentModelBean webKitViewContentModelBean
	) {
		this(loggedInUserParam, sectionsParam, null, webKitViewContentModelBean);
	}

	/**
	 * Base to set logged in user and applicable sections.
	 *
	 * @param loggedInUserParam The currently logged in user.
	 * @param sectionsParam The list of sections applicable to the view.
	 * @param data The mapped fluid items per view.
	 *
	 * @see JobView
	 * @see WorkspaceFluidItem
	 */
	public ABaseContentView(
		User loggedInUserParam,
		Collection<String> sectionsParam,
		Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data,
		WebKitViewContentModelBean webKitViewContentModelBean
	) {
		this(loggedInUserParam, sectionsParam == null ? null : sectionsParam.toArray(new String[]{}), data, webKitViewContentModelBean);
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
		Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> fluidItemsForViewsParam,
		WebKitViewContentModelBean webKitViewContentModelBean
	) {
		this.loggedInUser = loggedInUserParam;
		this.sections = sectionsParam;
		this.webKitViewContentModelBean = webKitViewContentModelBean;

		//Headers for Columns...
		this.fluidItemsForSection = new HashMap<>();
		this.fluidItemsForSectionFiltered = new HashMap<>();
		this.fluidItemsSelectedList = new ArrayList<>();

		this.mapColumnModel();

		this.refreshData(fluidItemsForViewsParam);
	}

	public void mapColumnModel() {
		if (this.getSections() != null) {
			for (String section : this.getSections()) {
				//Sections Headers...
				if (this.webKitViewContentModelBean != null) {
					List<ABaseManagedBean.ColumnModel> columnModForSection = this.getColumnHeadersForSection(section);
					this.webKitViewContentModelBean.storeModelFor(this.getCategory(), section, columnModForSection);
				}
				//Placeholder for Filter...
				this.fluidItemsForSection.put(section, new ArrayList<>());
			}
		}
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
		return returnVal;
	}

	public String getCategory() {
		return this.getClass().getName();
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
			Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> fluidItemsForViewsParam);

	public void actionOnRowToggle(ToggleEvent event) {
		if (event.getVisibility() == Visibility.VISIBLE) {
			Object data = event.getData();

		}
	}

	/**
	 *
	 * @param sectionParam
	 * @param fieldNameParam
	 * @return
	 */
	public boolean checkIfFieldCurrentlyFiltered(String sectionParam, String fieldNameParam) {
		String selectedColumns = this.checkListingOfFiltersForSectionAndField(sectionParam, fieldNameParam);
		return (selectedColumns == null || selectedColumns.trim().isEmpty()) ? false:true;
	}

	/**
	 *
	 * @param sectionParam
	 * @param fieldNameParam
	 * @return
	 */
	public String checkListingOfFiltersForSectionAndField(String sectionParam, String fieldNameParam) {
		Map<String, String[]> mappingForSection = this.getSelectItemSelectedMap(sectionParam);
		if(mappingForSection == null) {
			return null;
		}

		String[] stringSelectedFilters = mappingForSection.get(fieldNameParam);
		if(stringSelectedFilters == null || stringSelectedFilters.length < 1) {
			return null;
		}

		StringBuilder builder = new StringBuilder();
		for(String selected : stringSelectedFilters) {
			builder.append(selected);
			builder.append(", ");
		}

		String toStringed = builder.toString();
		return toStringed.substring(0, toStringed.length() - 2);
	}

	public Map<String, String[]> getSelectItemSelectedMap(String sectionParam) {
		if (sectionParam == null || sectionParam.trim().isEmpty()) {
			return new HashMap<>();
		}

		if (this.filterBySelectItemMap == null) {
			return new HashMap<>();
		}

		Map<String,String[]> selectedValuesForField = this.filterBySelectItemMap.get(sectionParam);
		Set<String> fieldNames = selectedValuesForField.keySet();

		Map<String, String[]> newMapWithNoDups = new HashMap<>();
		for (String fieldName : fieldNames) {
			String[] fieldValues = selectedValuesForField.get(fieldName);;
			if (fieldValues == null) {
				continue;
			}

			Set<String> returnValUniq = new HashSet<>();
			returnValUniq.addAll(Arrays.asList(fieldValues));

			newMapWithNoDups.put(fieldName, returnValUniq.toArray(new String[]{}));
		}

		this.filterBySelectItemMap.put(sectionParam, newMapWithNoDups);
		return this.filterBySelectItemMap.get(sectionParam);
	}

	/**
	 * Update the visible column configs.
	 */
	public void actionUpdateVisibleColumn() {

	}

	/**
	 * Update the filter column configs.
	 */
	public void actionUpdateFilterColumn() {

	}

	public String getRemoveButtonMessage() {
		if (this.hasSelectedItems()) {
			int size = this.fluidItemsSelectedList.size();
			return size > 1 ? size + " Items" : "1 Item";
		}
		return "Remove";
	}

	public String getDeleteButtonMessage() {
		if (this.hasSelectedItems()) {
			int size = this.fluidItemsSelectedList.size();
			return size > 1 ? size + " Items" : "1 Item";
		}
		return "Delete";
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
	public Map<String, List<WorkspaceFluidItem>> getFluidItemsForSectionFiltered() {
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
	 * @param data The updated list of {@code WorkspaceFluidItem}'s for each section.
	 *
	 * @see JobView
	 * @see WorkspaceFluidItem
	 */
	public void refreshData(Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data) {
		this.data = data;
		if (this.fluidItemsForSection == null) {
			this.fluidItemsForSection = new HashMap<>();
		}

		//Clear all data...
		this.fluidItemsForSection.values().clear();
		this.fluidItemsForSection.clear();
		if (this.getSections() == null) {
			return;
		}

		if (data == null || data.isEmpty()) {
			return;
		}

		//Load Items for each section...
		for (String section : this.getSections()) {
			if (section == null || section.trim().isEmpty()) {
				continue;
			}
			List<WorkspaceFluidItem> itemsForSection =
					this.getWorkspaceFluidItemsFor(section, data);
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
	public WebKitWorkspaceJobView retrieveJobViewFromId(Long jobViewIdParam) {
		if (jobViewIdParam == null) {
			return null;
		}
		if (this.data == null) {
			return null;
		}

		return this.data.keySet().stream()
				.map(itm -> itm.getJobViews())
				.flatMap(List::stream)
				.filter(itm -> jobViewIdParam.equals(itm.getId()))
				.findFirst()
				.orElse(null);
	}

	public ABaseManagedBean.ColumnModel getCHForSecCM(
		String sectionAliasParam, int columnIndexParam
	) {
		List<ABaseManagedBean.ColumnModel> headers = this.getColumnHeadersForSection(sectionAliasParam);
		int headerSize = (headers == null) ? 0:headers.size();
		if ((columnIndexParam + 1) > headerSize){
			return null;
		}
		return headers.get(columnIndexParam);
	}

	public boolean shouldRenderColumnForSectionAndIndex(
		String sectionRptItem,
		int indexParam
	) {
		ABaseManagedBean.ColumnModel columnModel =
				this.getCHForSecCM(sectionRptItem, indexParam);
		if (columnModel == null) {
			return false;
		}
		return this.shouldRenderColumnForSectionAndModel(sectionRptItem, columnModel);
	}

	public boolean shouldRenderColumnForSectionAndModel(
		String sectionRptItem,
		ABaseManagedBean.ColumnModel columnModelParam
	) {
		if (sectionRptItem == null ||
				(columnModelParam == null || columnModelParam.getHeader() == null)) {
			return false;
		}

		List<ABaseManagedBean.ColumnModel> columnsForSection = null;
		if (this.webKitViewContentModelBean != null) {
			columnsForSection = this.webKitViewContentModelBean.getModelFor(this.getCategory(), sectionRptItem);
		}
		if (columnsForSection == null || columnsForSection.isEmpty()) {
			return false;
		}
		for (ABaseManagedBean.ColumnModel toCheckAgainst : columnsForSection) {
			if (toCheckAgainst.getHeader().equals(columnModelParam.getHeader())) {
				return (toCheckAgainst.isEnabled() && toCheckAgainst.isVisible());
			}
		}
		return true;
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

	public Map<String, List<ABaseManagedBean.ColumnModel>> getColumnModels() {
		Map<String, List<ABaseManagedBean.ColumnModel>> returnVal = (this.webKitViewContentModelBean == null) ?
				null : this.webKitViewContentModelBean.getColumnModels().get(this.getCategory());
		if (returnVal != null) {
			returnVal.forEach((key, val) -> {
				Collections.sort(val, Comparator.comparing(ABaseManagedBean.ColumnModel::getFluidFieldName));
			});
		}
		return returnVal;
	}

	public Map<String, List<ABaseManagedBean.ColumnModel>> getColumnModelsEnabled() {
		Map<String, List<ABaseManagedBean.ColumnModel>> returnVal = this.getColumnModels();
		Map<String, List<ABaseManagedBean.ColumnModel>> returnValNew = new HashMap<>();
		if (returnVal != null) {
			returnVal.forEach((key, val) -> {
				List<ABaseManagedBean.ColumnModel> enabledOnly = val.stream()
						.filter(itm -> itm.isEnabled())
						.collect(Collectors.toList());
				returnValNew.put(key, enabledOnly);
			});
		}
		return returnValNew;
	}

	public Map<String, List<ABaseManagedBean.ColumnModel>> getColumnModelsFilterable() {
		return (this.webKitViewContentModelBean == null) ?
				null : this.webKitViewContentModelBean.getColumnModelsFilterable(this.getCategory());
	}

	public Map<String, String> getTextMap(String sectionParam) {
		if (sectionParam == null || sectionParam.trim().isEmpty()) {
			new RaygunUtil().raiseErrorToRaygun(
					new ClientDashboardException("Text: Section name is empty. Not allowed. Returning new Hashmap. ",
							ClientDashboardException.ErrorCode.ILLEGAL_STATE));
			return new HashMap<>();
		}

		if (this.filterByTextValueMap == null) {
			this.filterByTextValueMap = new HashMap<>();
		}
		return this.filterByTextValueMap.get(sectionParam);
	}

	public Map<String, Double> getDecimalMap(String sectionParam) {
		if (sectionParam == null || sectionParam.trim().isEmpty()) {
			new RaygunUtil().raiseErrorToRaygun(
					new ClientDashboardException("Decimal: Section name is empty. Not allowed. Returning new Hashmap. ",
							ClientDashboardException.ErrorCode.ILLEGAL_STATE));
			return new HashMap<>();
		}

		if (this.filterByDecimalValueMap == null) {
			this.filterByDecimalValueMap = new HashMap<>();
		}
		return this.filterByDecimalValueMap.get(sectionParam);
	}

	public List<SelectItem> getPossibleCombinationsMapAsSelectItemsFor(String section, String fieldName) {
		if ((section == null || section.trim().isEmpty()) || (fieldName == null || fieldName.trim().isEmpty())) return new ArrayList<>();

		List<WorkspaceFluidItem> workItemsForSection = this.getWorkspaceFluidItemsForSection(section);
		if (workItemsForSection == null) return new ArrayList<>();

		Set<String> setOfPossibleOptions = new HashSet<>();
		workItemsForSection.stream()
				.filter(itm -> itm.getFieldMap() != null && !itm.getFieldMap().isEmpty())
				.map(itm -> itm.getFieldMap())
				.forEach(map -> {
					Object fieldVal = map.get(fieldName);
					if (fieldVal == null) return;
					setOfPossibleOptions.add(fieldVal.toString());
				});
		List<SelectItem> returnVal = setOfPossibleOptions.stream()
				.map(itm -> new SelectItem(itm, itm))
				.collect(Collectors.toList());

		//Sort by Label...
		Collections.sort(returnVal, Comparator.comparing(SelectItem::getLabel));
		return returnVal;
	}

	public String getSelectItemMultipleLabelForSectionAndField(String sectionParam, String fieldNameParam) {
		Map<String,String[]> selectedValuesForField = this.getSelectItemSelectedMap(sectionParam);
		String[] selectedValues = selectedValuesForField.get(fieldNameParam);
		if (selectedValues == null || selectedValues.length == 0) {
			return ("-- Not Filtered --");
		}

		StringBuilder returnVal = new StringBuilder();
		for (String val : selectedValues) {
			returnVal.append(val);
			returnVal.append(",");
		}

		String toStr = returnVal.toString();
		String returnValStr = toStr.substring(0,toStr.length() - 1);
		return returnValStr;
	}
}
