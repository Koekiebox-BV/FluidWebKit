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

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.ABaseContentView;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.WebKitViewContentModelBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.field.MultiChoice;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQuery;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import lombok.Getter;
import lombok.Setter;

import javax.faces.model.SelectItem;
import java.util.*;
import java.util.stream.Collectors;

public class ContentViewUQ extends ABaseContentView {
	@Getter
	private WebKitUserQuery wkUserQuery;

	private WebKitAccessBean accessBean;

	@Getter
	@Setter
	private WorkspaceUserQueryLDM fluidItemsLazyModel = null;

	@Getter
	@Setter
	private Map<String, List<ABaseManagedBean.ColumnModel>> sectionFilterableColumns;

	private Map<String, List<String>> multiSelectMapping;

	@Getter @Setter
	private Map<String, Map<String, Date>> rangeToDates = new HashMap<>();
	@Getter @Setter
	private Map<String, Map<String, Double>> rangeToDecimals = new HashMap<>();

	public static final class SystemField {
		public static final String DATE_CREATED = "Date Created";
		public static final String DATE_LAST_UPDATED = "Date Last Updated";
		public static final String FORM_TYPE = "Form Type";
		public static final String ID = "ID";
		public static final String TITLE = "Title";
		public static final String USER = "User";
		public static final String STATE = "State";
		public static final String FLOW_STATE = "Flow State";

		// Query names follow the backend FormField.SystemField catalogue, not result-column labels.
		private static final List<String> QUERY_FIELDS = Arrays.asList(
				DATE_CREATED, DATE_LAST_UPDATED, FORM_TYPE, "Form Flow State", TITLE, "Current User",
				"Email From Address", "Email To Address", "Email Subject", "Email Sent Date",
				"Email Received Date", "Email Unique Identifier");

		public static String queryFieldName(String name) {
			if (name == null) return null;
			return QUERY_FIELDS.stream().filter(field -> field.equalsIgnoreCase(name.trim())).findFirst().orElse(null);
		}
	}

	public ContentViewUQ(
		User loggedInUserParam,
		String sectionName,
		WebKitUserQuery webKitUserQuery,
		WebKitViewContentModelBean webKitViewContentModelBean,
		WebKitAccessBean accessBean
	) {
		super(loggedInUserParam, new String[]{sectionName}, webKitViewContentModelBean);
		this.wkUserQuery = webKitUserQuery;
		this.accessBean = accessBean;
	}

	@Override
	public List<WorkspaceFluidItem> getWorkspaceFluidItemsFor(
		String sectionParam,
		Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data
	) {
		return null;//Not Required...
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
		if (this.wkUserQuery == null) return returnVal;

		//Index - 01 - Attachment
		returnVal.add(new ABaseManagedBean.ColumnModel(
				this.wkUserQuery.getAttachmentHeader(),
				this.wkUserQuery.getAttachmentHeader(),
				Field.Type.Label,
				META_DATA_PLAIN,
				this.wkUserQuery.isShowColumnAttachment(),
				true,
				false));

		//Index - 02 - Title
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.TITLE,
				SystemField.TITLE,
				Field.Type.Text,
				META_DATA_PLAIN,
				this.wkUserQuery.isShowColumnTitle(),
				true,
				false));

		//Index - 03 - ID
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.ID,
				SystemField.ID,
				Field.Type.Text,
				META_DATA_PLAIN,
				this.wkUserQuery.isShowColumnID(),
				true,
				false));

		//Index - 04 - Form Type
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.FORM_TYPE,
				SystemField.FORM_TYPE,
				Field.Type.MultipleChoice,
				META_DATA_PLAIN,
				this.wkUserQuery.isShowColumnFormType(),
				true,
				false));

		//Index - 05 - Date Created
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.DATE_CREATED,
				SystemField.DATE_CREATED,
				Field.Type.DateTime,
				META_DATA_DATE_AND_TIME,
				this.wkUserQuery.isShowColumnDateCreated(),
				true,
				false));

		//Index - 06 - Date Last Updated
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.DATE_LAST_UPDATED,
				SystemField.DATE_LAST_UPDATED,
				Field.Type.DateTime,
				META_DATA_DATE_AND_TIME,
				this.wkUserQuery.isShowColumnDateLastUpdated(),
				true,
				false));

		//Index - 07 - User
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.USER,
				SystemField.USER,
				Field.Type.MultipleChoice,
				META_DATA_PLAIN,
				this.wkUserQuery.isShowColumnUser(),
				true,
				false));

		//Index - 08 - State
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.STATE,
				SystemField.STATE,
				Field.Type.MultipleChoice,
				META_DATA_PLAIN,
				this.wkUserQuery.isShowColumnState(),
				true,
				false));

		//Index - 09 - Flow State
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.FLOW_STATE,
				SystemField.FLOW_STATE,
				Field.Type.MultipleChoice,
				META_DATA_PLAIN,
				this.wkUserQuery.isShowColumnFlowState(),
				true,
				false));

		List<Field> fieldsLoggedInUserHasAccessTo =
				this.accessBean.getFormFieldsUserHasVisibilityFor(this.wkUserQuery.getUserQuery());
		if (fieldsLoggedInUserHasAccessTo == null || fieldsLoggedInUserHasAccessTo.isEmpty()) return returnVal;

		List<ABaseManagedBean.ColumnModel> returnValCustomRouteFields = fieldsLoggedInUserHasAccessTo.stream()
				.map(formField -> {
					return new ABaseManagedBean.ColumnModel(
							formField.getFieldName(),
							formField.getFieldName(),
							formField.getTypeAsEnum(),
							formField.getTypeMetaData(),
							true,
							true,
							false);
				})
				.collect(Collectors.toList());
		returnVal.addAll(returnValCustomRouteFields);
		
		return returnVal;
	}

	@Override
	public boolean checkIfFieldCurrentlyFiltered(String sectionParam, String fieldNameParam) {
		if (this.sectionFilterableColumns == null) return false;

		List<ABaseManagedBean.ColumnModel> modelForSection =
				this.sectionFilterableColumns.get(sectionParam);
		if (modelForSection == null || modelForSection.isEmpty()) return false;

		//Txt...
		Map<String, String> fieldFilterValTxt = this.getFilterByTextValueMap().get(sectionParam);
		if (fieldFilterValTxt != null)
			for (String value : fieldFilterValTxt.values()) 
				if (UtilGlobal.isNotBlank(value)) return true;

		//Decimal...
		Map<String, Double> fieldFilterValDec = this.getFilterByDecimalValueMap().get(sectionParam);
		if (fieldFilterValDec != null)
			for (Double value : fieldFilterValDec.values())
				if (value != null && value != 0.0) return true;

		//MultiChoice...
		Map<String, String[]> fieldFilterValMulti = this.getFilterBySelectItemMap().get(sectionParam);
		if (fieldFilterValMulti != null)
			for (String[] value : fieldFilterValMulti.values())
				if (value != null && value.length > 0) return true;

		return false;
	}

	@Override
	public List<SelectItem> getPossibleCombinationsMapAsSelectItemsFor(String section, String fieldName) {
		if ((section == null || section.trim().isEmpty()) || (fieldName == null || fieldName.trim().isEmpty())) return new ArrayList<>();

		if (this.multiSelectMapping == null) this.multiSelectMapping = new HashMap<>();

		final List<String> availableOptions;
		if (this.multiSelectMapping.containsKey(fieldName)) {
			availableOptions = this.multiSelectMapping.get(fieldName);
		} else {
			Field multiField = new Field(fieldName, new MultiChoice(), Field.Type.MultipleChoice);
			this.accessBean.populateAvailableMultiChoicesForField(
					this.accessBean.getFluidClientDSConfig().getFormFieldClient(), multiField);
			availableOptions = multiField.getFieldValueAsMultiChoice().getAvailableMultiChoices();
			this.multiSelectMapping.put(fieldName, availableOptions);
		}

		List<SelectItem> returnVal = (availableOptions == null ? Collections.<String>emptyList() : availableOptions).stream()
				.map(itm -> new SelectItem(itm, itm))
				.collect(Collectors.toList());

		//Sort by Label...
		Collections.sort(returnVal, Comparator.comparing(SelectItem::getLabel));
		return returnVal;
	}

	@Override
	public Map<String, List<ABaseManagedBean.ColumnModel>> getColumnModelsFilterable() {
		String section = this.getSections()[0];

		if (this.sectionFilterableColumns == null) {
			if (this.wkUserQuery == null) return new HashMap<>();

			String userQueryName = this.wkUserQuery.getUserQuery().getName();
			UserQuery freshFetch = this.accessBean.fetchUserQueryWithNameUsingConfigUser(userQueryName);

			List<ABaseManagedBean.ColumnModel> inputModels = null;
			List<String> inputsRulesWhereInputIsReq = (freshFetch == null) ? null : freshFetch.getRules();
			if (inputsRulesWhereInputIsReq != null && !inputsRulesWhereInputIsReq.isEmpty()) {
				inputModels = inputsRulesWhereInputIsReq.stream()
						.filter(rule -> UserQueryInputColumn.inputFieldName(rule) != null)
						.map(inputRule -> {
							String formFieldName = UserQueryInputColumn.inputFieldName(inputRule);
							Field fieldByName = this.accessBean.getFieldBy(formFieldName);
							// System fields need not be attached to a visible form definition.
							String systemFieldName = SystemField.queryFieldName(formFieldName);
							if (fieldByName == null && systemFieldName != null) {
								fieldByName = this.accessBean.getFluidClientDSConfig().getFormFieldClient()
										.getFieldByName(systemFieldName);
							}
							if (fieldByName == null) return null;

							return new UserQueryInputColumn(fieldByName, inputRule);
						})
						.filter(itm -> itm != null)
						.collect(Collectors.toList());
			}
			if (inputModels == null) inputModels = new ArrayList<>();
			// Inputs may be absent from the configured result columns. Preserve already entered values.
			for (ABaseManagedBean.ColumnModel input : inputModels) {
				String name = input.getFluidFieldName();
				switch (input.getFluidFieldColumnType()) {
					case Text:
						this.getFilterByTextValueMap().computeIfAbsent(section, key -> new HashMap<>()).putIfAbsent(name, "");
						break;
					case TextEncrypted:
						this.getFilterByTextEncryptedValueMap().computeIfAbsent(section, key -> new HashMap<>()).putIfAbsent(name, "");
						break;
					case MultipleChoice:
						this.getFilterBySelectItemMap().computeIfAbsent(section, key -> new HashMap<>()).putIfAbsent(name, new String[]{});
						break;
					case DateTime:
						this.getFilterByDateValueMap().computeIfAbsent(section, key -> new HashMap<>()).putIfAbsent(name, null);
						this.rangeToDates.computeIfAbsent(section, key -> new HashMap<>()).putIfAbsent(name, null);
						break;
					case Decimal:
						this.getFilterByDecimalValueMap().computeIfAbsent(section, key -> new HashMap<>()).putIfAbsent(name, null);
						this.rangeToDecimals.computeIfAbsent(section, key -> new HashMap<>()).putIfAbsent(name, null);
						break;
					default: break;
				}
			}
			// Only cache a completed lookup so a failed system-field request can be retried.
			this.sectionFilterableColumns = new HashMap<>();
			this.sectionFilterableColumns.put(section, inputModels);
		}
		return this.sectionFilterableColumns;
	}

	@Override
	public void actionClearFilteredList(String section) {
		this.rangeToDates.remove(section);
		this.rangeToDecimals.remove(section);
		Map<String, Double> decimals = this.getFilterByDecimalValueMap().get(section);
		if (decimals != null) decimals.replaceAll((field, value) -> null);
		// Keep writable maps present for subsequent JSF input binding.
		this.rangeToDates.put(section, new HashMap<>());
		this.rangeToDecimals.put(section, new HashMap<>());
		super.actionClearFilteredList(section);
	}
}
