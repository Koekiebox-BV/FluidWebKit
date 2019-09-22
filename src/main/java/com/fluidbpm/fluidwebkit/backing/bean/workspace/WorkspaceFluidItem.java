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

import com.fluidbpm.fluidwebkit.backing.bean.ABaseWebVO;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View item wrapped for {@code ABaseWebVO}.
 *
 * @author jasonbruwer on 2018-05-31.
 * @since 1.0
 */
public class WorkspaceFluidItem {
	private ABaseWebVO baseWeb;
	private Map<String, Object> fieldMap;
	private JobView jobView;

	/**
	 *
	 * @param baseWebParam
	 */
	public WorkspaceFluidItem(ABaseWebVO baseWebParam) {
		this.baseWeb = baseWebParam;
		this.refreshFieldNameAndValueMap();
	}

	/**
	 *
	 * @return
	 */
	public FluidItem getFluidItem() {
		if (this.baseWeb == null) {
			return null;
		}

		return this.baseWeb.getFluidItem();
	}

	/**
	 *
	 * @return
	 */
	public Form getFluidItemForm() {

		return this.baseWeb.getForm();
	}

	/**
	 * 
	 * @return
	 */
	public List<Field> getRouteFields() {
		if (this.baseWeb == null || this.baseWeb.getFluidItem() == null) {
			return null;
		}

		return this.baseWeb.getFluidItem().getRouteFields();
	}

	/**
	 * 
	 * @return
	 */
	public List<Field> getFormFields() {
		if (this.baseWeb == null || this.baseWeb.getForm() == null) {
			return null;
		}

		return this.baseWeb.getForm().getFormFields();
	}

	/**
	 *
	 * @return
	 */
	public ABaseWebVO getBaseWeb() {
		return this.baseWeb;
	}

	/**
	 *
	 * @return
	 */
	private void refreshFieldNameAndValueMap() {
		if (this.getFluidItem() == null) {
			return;
		}

		this.fieldMap = new HashMap<>();

		//Route Fields...
		if (this.getFluidItem().getRouteFields() != null) {
			for (Field field : this.getFluidItem().getRouteFields()) {
				if (Field.Type.DateTime.toString().equals(field.getFieldType())) {
					this.fieldMap.put(field.getFieldName(), field.getFieldValue());
				} else {
					this.fieldMap.put(field.getFieldName(), field.getFieldValue().toString());
				}
			}
		}

		//Form Fields...
		if (this.getFluidItem().getForm() != null && this.getFluidItem().getForm().getFormFields() != null) {
			for (Field field : this.getFluidItem().getForm().getFormFields()) {
				if (Field.Type.DateTime.toString().equals(field.getFieldType())) {
					this.fieldMap.put(field.getFieldName(), field.getFieldValue());
				} else {
					this.fieldMap.put(field.getFieldName(), field.getFieldValue().toString());
				}
			}
		}
	}

	/**
	 *
	 * @return
	 */
	public Map<String, Object> getFieldMap() {
		return this.fieldMap;
	}

	/**
	 *
	 * @param fieldMapParam
	 */
	public void setFieldMap(Map<String, Object> fieldMapParam) {
		this.fieldMap = fieldMapParam;
	}

	/**
	 *
	 * @return
	 */
	public JobView getJobView() {
		return this.jobView;
	}

	/**
	 *
	 * @param jobViewParam
	 */
	public void setJobView(JobView jobViewParam) {
		this.jobView = jobViewParam;
	}
}
