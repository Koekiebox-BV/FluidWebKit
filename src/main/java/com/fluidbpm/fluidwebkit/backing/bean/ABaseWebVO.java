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

package com.fluidbpm.fluidwebkit.backing.bean;

import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.user.User;

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
public class ABaseWebVO implements Serializable {
	private FluidItem fluidItem;

	private boolean justReviewed;

	protected List<Field> fieldsViewable;
	protected List<Field> fieldsEditable;

	private String openModeMessage = null;

	private Long assignedUserId;
	private List<User> allUsers;

	/**
	 * Default constructor.
	 */
	public ABaseWebVO() {
		super();
		this.justReviewed = false;
	}

	/**
	 * 
	 * @param fluidItmParam
	 */
	public ABaseWebVO(FluidItem fluidItmParam) {
		super();
		this.fluidItem = fluidItmParam;
	}

	/**
	 *
	 * @param fieldsViewableParam
	 * @param fieldsEditableParam
	 */
	public ABaseWebVO(List<Field> fieldsViewableParam, List<Field> fieldsEditableParam) {
		this();
		this.fieldsViewable = fieldsViewableParam;
		this.fieldsEditable = fieldsEditableParam;
	}

	/**
	 *
	 * @return
	 */
	public Form getForm() {
		return (this.fluidItem == null) ? null : this.fluidItem.getForm();
	}

	/**
	 * @return
	 */
	public FluidItem getFluidItem() {
		return this.fluidItem;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isFieldsViewableEmpty() {
		return (this.fieldsViewable == null ||
				this.fieldsViewable.isEmpty());
	}

	/**
	 * 
	 * @return
	 */
	public boolean isFieldsEditableEmpty() {
		return (this.fieldsEditable == null || this.fieldsEditable.isEmpty());
	}

	/**
	 *
	 * @return
	 */
	public List<Field> getFieldsViewable() {
		return this.fieldsViewable;
	}

	/**
	 *
	 * @param fieldNameParam
	 * @return
	 */
	public Field getFieldViewableWithName(String fieldNameParam) {
		if (fieldNameParam == null || fieldNameParam.trim().isEmpty()) {
			return null;
		}

		if (this.getFieldsViewable() == null || this.getFieldsViewable().isEmpty()) {
			return null;
		}

		for (Field toCheck : this.getFieldsViewable()) {
			if(toCheck.getFieldName() == null || toCheck.getFieldName().trim().isEmpty()) {
				continue;
			}

			if (fieldNameParam.equals(toCheck.getFieldName())) {
				return toCheck;
			}
		}

		return null;
	}

	/**
	 *
	 * @param fieldsViewableParam
	 */
	public void setFieldsViewable(List<Field> fieldsViewableParam) {
		this.fieldsViewable = fieldsViewableParam;
	}

	/**
	 *
	 * @return
	 */
	public List<Field> getFieldsEditable() {
		return this.fieldsEditable;
	}

	/**
	 *
	 * @param fieldsEditableParam
	 */
	public void setFieldsEditable(List<Field> fieldsEditableParam) {
		this.fieldsEditable = fieldsEditableParam;
	}

	/**
	 *
	 * @param fieldNameParam
	 * @return
	 */
	public boolean isFieldViewable(String fieldNameParam) {
		Field fieldWithName = this.getFieldWithName(
				this.fieldsViewable,
				fieldNameParam);

		return (fieldWithName == null) ? false:true;
	}

	/**
	 *
	 * @param fieldNameParam
	 * @return
	 */
	public boolean isFieldEditable(String fieldNameParam)
	{
		Field fieldWithName = this.getFieldWithName(
				this.fieldsEditable,
				fieldNameParam);

		return (fieldWithName == null) ? false:true;
	}

	/**
	 *
	 * @param listToCheckParam
	 * @param fieldNameParam
	 * @return
	 */
	protected Field getFieldWithName(List<Field> listToCheckParam, String fieldNameParam) {
		if (fieldNameParam == null || fieldNameParam.isEmpty()) {
			return null;
		}

		if (listToCheckParam == null || listToCheckParam.isEmpty()) {
			return null;
		}

		for (Field viewableField : listToCheckParam) {
			if (fieldNameParam.equals(viewableField.getFieldName())) {
				return viewableField;
			}
		}

		return null;
	}

	/**
	 * 
	 * @param selectableItemParam
	 * @return
	 */
	protected List<SelectItem> convertToSelectItems(List<String> selectableItemParam) {
		if (selectableItemParam == null) {
			return null;
		}

		List<SelectItem> returnVal = new ArrayList<>();
		for (String availChoice : selectableItemParam) {
			returnVal.add(new SelectItem(availChoice, availChoice));
		}

		return returnVal;
	}

	/**
	 *
	 * @return
	 */
	public String getOpenModeMessage() {
		return this.openModeMessage;
	}

	/**
	 *
	 * @param openModeMessageParam
	 */
	public void setOpenModeMessage(String openModeMessageParam) {
		this.openModeMessage = openModeMessageParam;
	}

	/**
	 *
	 * @return
	 */
	public Long getAssignedUserId() {
		return this.assignedUserId;
	}

	/**
	 *
	 * @param assignedUserIdParam
	 */
	public void setAssignedUserId(Long assignedUserIdParam) {
		this.assignedUserId = assignedUserIdParam;
	}

	/**
	 *
	 * @return
	 */
	public List<User> getAllUsers() {
		return this.allUsers;
	}

	/**
	 *
	 * @param allUsersParam
	 */
	public void setAllUsers(List<User> allUsersParam) {
		this.allUsers = allUsersParam;
	}

	/**
	 *
	 * @return
	 */
	public boolean isJustReviewed() {
		return this.justReviewed;
	}

	/**
	 * @param justReviewedParam
	 */
	public void setJustReviewed(boolean justReviewedParam) {
		this.justReviewed = justReviewedParam;
	}
}
