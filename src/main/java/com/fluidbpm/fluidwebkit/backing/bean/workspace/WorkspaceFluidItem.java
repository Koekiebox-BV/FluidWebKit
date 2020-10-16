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

import com.fluidbpm.fluidwebkit.backing.vo.ABaseWebVO;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.ABaseFluidVO;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Workspace object to host the {@code JobView} and {@code ABaseWebVO}.
 */
public class WorkspaceFluidItem extends ABaseFluidVO {
	private ABaseWebVO baseWeb;
	private Map<String, Object> fieldMap;
	private JobView jobView;

	/**
	 * Default constructor to create a {@code WorkspaceFluidItem} without setting the view.
	 * 
	 * @param baseWebParam Subclass instance of {@code ABaseWebVO}.
	 */
	public WorkspaceFluidItem(ABaseWebVO baseWebParam) {
		this.baseWeb = baseWebParam;
		this.refreshFieldNameAndValueMap();
	}

	/**
	 * Get the {@code FluidItem} from the {@code ABaseWebVO}.
	 *
	 * @return {@code this.baseWeb.getFluidItem()}
	 */
	public FluidItem getFluidItem() {
		if (this.baseWeb == null) {
			return null;
		}
		return this.baseWeb.getFluidItem();
	}

	/**
	 * Get the {@code Form} from the {@code FluidItem}.
	 * 
	 * @return {@code this.baseWeb.getForm()}
	 *
	 * @see Form
	 */
	public Form getFluidItemForm() {
		return this.baseWeb.getForm();
	}

	public Long getFluidItemFormId() {
		if (this.getFluidItemForm() == null) return null;
		return this.getFluidItemForm().getId();
	}

	public Long getFluidItemId() {
		if (this.getFluidItem() == null) return null;
		return this.getFluidItem().getId();
	}

	public String getFluidItemFormType() {
		if (this.getFluidItemForm() == null) return null;
		return this.getFluidItemForm().getFormType();
	}

	public String getFluidItemTitle() {
		if (this.getFluidItemForm() == null) return null;
		return this.getFluidItemForm().getTitle();
	}

	public String getFluidItemFlow() {
		if (this.getFluidItem() == null) return null;
		return this.getFluidItem().getFlow();
	}

	public String getFluidItemStep() {
		if (this.getFluidItem() == null) return null;
		return this.getFluidItem().getStep();
	}

	public String getFluidItemView() {
		if (this.getJobView() == null) return null;
		return this.getJobView().getViewName();
	}

	public Long getFluidItemViewId() {
		if (this.getJobView() == null) return null;
		return this.getJobView().getId();
	}

	//TODO Need to add the step entry time to Fluid Item...

	public String getFluidItemFormTypeURLSafe() {
		if (this.getFluidItemForm() == null) return null;
		return UtilGlobal.encodeURL(this.getFluidItemForm().getFormType());
	}

	/**
	 * 
	 * @return
	 * 
	 * @see Field
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
	 * Refresh the local {@code fieldMap} based on the {@code FluidItem} route and form fields.
	 *
	 * @see FluidItem
	 * @see Field
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
