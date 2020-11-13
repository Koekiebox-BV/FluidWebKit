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

package com.fluidbpm.fluidwebkit.backing.vo;

import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.user.User;
import lombok.Getter;
import lombok.Setter;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all web value objects.
 *
 * @author jasonbruwer on 2018-06-02
 * @since 1.0
 */
public abstract class ABaseWebVO implements Serializable {
	private FluidItem fluidItem;

	@Getter
	@Setter
	private boolean justReviewed;

	@Getter
	@Setter
	protected List<Field> fieldsViewable;

	@Getter
	@Setter
	protected List<Field> fieldsEditable;

	private String openModeMessage = null;

	private Long assignedUserId;

	@Getter
	@Setter
	private List<User> allUsers;

	public ABaseWebVO() {
		super();
		this.justReviewed = false;
	}

	public ABaseWebVO(FluidItem fluidItmParam) {
		super();
		this.fluidItem = fluidItmParam;
	}

	public ABaseWebVO(List<Field> fieldsViewableParam, List<Field> fieldsEditableParam) {
		this();
		this.fieldsViewable = fieldsViewableParam;
		this.fieldsEditable = fieldsEditableParam;
	}

	public Form getForm() {
		return (this.fluidItem == null) ? null : this.fluidItem.getForm();
	}

	public FluidItem getFluidItem() {
		return this.fluidItem;
	}

	public boolean isFieldsViewableEmpty() {
		return (this.fieldsViewable == null ||
				this.fieldsViewable.isEmpty());
	}

	public boolean isFieldsEditableEmpty() {
		return (this.fieldsEditable == null || this.fieldsEditable.isEmpty());
	}

	public Field getFieldViewableWithName(String fieldNameParam) {
		if (fieldNameParam == null || fieldNameParam.trim().isEmpty()) return null;

		if (this.getFieldsViewable() == null || this.getFieldsViewable().isEmpty()) return null;

		for (Field toCheck : this.getFieldsViewable()) {
			if (toCheck.getFieldName() == null || toCheck.getFieldName().trim().isEmpty()) continue;

			if (fieldNameParam.equals(toCheck.getFieldName())) return toCheck;
		}
		return null;
	}

	public boolean isFieldViewable(String fieldNameParam) {
		Field fieldWithName = this.getFieldWithName(
				this.fieldsViewable,
				fieldNameParam);
		return (fieldWithName == null) ? false:true;
	}

	public boolean isFieldEditable(String fieldNameParam) {
		Field fieldWithName = this.getFieldWithName(
				this.fieldsEditable,
				fieldNameParam);

		return (fieldWithName == null) ? false:true;
	}

	protected Field getFieldWithName(List<Field> listToCheckParam, String fieldNameParam) {
		if (fieldNameParam == null || fieldNameParam.isEmpty()) return null;

		if (listToCheckParam == null || listToCheckParam.isEmpty()) return null;

		for (Field viewableField : listToCheckParam) {
			if (fieldNameParam.equals(viewableField.getFieldName())) return viewableField;
		}

		return null;
	}

	protected List<SelectItem> convertToSelectItems(List<String> selectableItemParam) {
		if (selectableItemParam == null) return null;

		List<SelectItem> returnVal = new ArrayList<>();
		for (String availChoice : selectableItemParam) returnVal.add(new SelectItem(availChoice, availChoice));

		return returnVal;
	}

	public String getOpenModeMessage() {
		return this.openModeMessage;
	}

	public void setOpenModeMessage(String openModeMessageParam) {
		this.openModeMessage = openModeMessageParam;
	}

	public Long getAssignedUserId() {
		return this.assignedUserId;
	}

	public void setAssignedUserId(Long assignedUserIdParam) {
		this.assignedUserId = assignedUserIdParam;
	}

	public abstract ABaseWebVO cloneVO(
		FluidItem fluidItem,
		List<Field> fieldsViewable,
		List<Field> fieldsEditable,
		List<User> allUsers
	);
}
