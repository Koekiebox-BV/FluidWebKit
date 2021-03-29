package com.fluidbpm.fluidwebkit.backing.bean.workspace.field;

import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.field.MultiChoice;

import javax.faces.model.SelectItem;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WebKitField extends Field {

	public WebKitField() {
		super();
	}

	public WebKitField(
		Long fieldIdParam,
		String fieldNameParam,
		Object fieldValueParam,
		Type fieldTypeParam
	) {
		super(fieldIdParam);
		this.setFieldName(fieldNameParam);
		this.setTypeAsEnum(fieldTypeParam);
		this.setFieldValue(fieldValueParam);
	}

	@XmlTransient
	public SelectItem getSelectedMultiChoiceSelectItem() {
		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null) return null;
		
		return new SelectItem(multi.getSelectedMultiChoice(), multi.getSelectedMultiChoice());
	}

	@XmlTransient
	public void setSelectedMultiChoiceSelectItem(SelectItem selectItem) {
		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null) return;
		
		multi.setSelectedMultiChoice(selectItem.getValue() == null ? null : selectItem.getValue().toString());
	}

	@XmlTransient
	public List<SelectItem> getActionFetchAllAvailMultiChoice() {
		List<SelectItem> returnList = new ArrayList();
		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null) return returnList;
		
		List<String> availSelectItems = multi.getAvailableMultiChoices();
		if (availSelectItems == null) return returnList;
		return new ArrayList<>(availSelectItems.stream()
				.map(itm -> new SelectItem(itm, itm))
				.collect(Collectors.toList()));
	}
	
	@XmlTransient
	public List<SelectItem> autoCompleteWithQuery(String itemQueryParam) {
		List<SelectItem> returnList = new ArrayList();

		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null) return returnList;

		List<String> availSelectItems = multi.getAvailableMultiChoices();
		List<SelectItem> availSelectItemsAsSE = new ArrayList<>(availSelectItems.stream()
				.map(itm -> new SelectItem(itm, itm))
				.collect(Collectors.toList()));

		if (UtilGlobal.isBlank(itemQueryParam)) {
			returnList.addAll(availSelectItemsAsSE);
			return returnList;
		}

		String itemQueryParamLower = itemQueryParam.toLowerCase();
		for (SelectItem toCheck : availSelectItemsAsSE) {
			String label = toCheck.getLabel();

			if (UtilGlobal.isBlank(label)) continue;

			if (toCheck.getValue() == null || !(toCheck.getValue() instanceof String)) continue;

			String stringValue = (String)toCheck.getValue();
			if (UtilGlobal.isBlank(stringValue)) continue;

			String labelLower = label.toLowerCase();
			if (labelLower.indexOf(itemQueryParamLower) > -1) returnList.add(toCheck);
		}

		return returnList;
	}

	@XmlTransient
	public List<String> autoCompleteWithQueryString(String itemQueryParam) {
		List<String> returnList = new ArrayList();

		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null) return returnList;

		List<String> availSelectItems = multi.getAvailableMultiChoices();

		if (UtilGlobal.isBlank(itemQueryParam)) {
			returnList.addAll(availSelectItems);
			return returnList;
		}

		String itemQueryParamLower = itemQueryParam.toLowerCase();
		for (String toCheck : availSelectItems) {
			if (UtilGlobal.isBlank(toCheck)) continue;

			String labelLower = toCheck.toLowerCase();
			if (labelLower.indexOf(itemQueryParamLower) > -1) returnList.add(toCheck);
		}

		return returnList;
	}

	@XmlTransient
	public int getInputValueColumnCount() {
		MultiChoice multi = this.getFieldValueAsMultiChoice();
		if (multi == null || multi.getAvailableMultiChoices() == null) return 1;

		int count = multi.getAvailableMultiChoices().size();

		if (count < 1) return 1;

		int returnVal = (int)Math.sqrt(count);

		if (returnVal > 3) returnVal = 3;

		return returnVal;
	}

	@XmlTransient
	public Field asField() {
		return this;
	}
}
