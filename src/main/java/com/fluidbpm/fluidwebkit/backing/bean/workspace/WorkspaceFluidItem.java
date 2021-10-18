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

import com.fluidbpm.fluidwebkit.backing.bean.workspace.field.WebKitField;
import com.fluidbpm.fluidwebkit.backing.vo.ABaseWebVO;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.ABaseFluidVO;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.field.MultiChoice;
import com.fluidbpm.program.api.vo.field.TableField;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.webkit.form.WebKitForm;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlTransient;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Workspace object to host the {@code JobView} and {@code ABaseWebVO}.
 */
public class WorkspaceFluidItem extends ABaseFluidVO {
	@Getter
	private ABaseWebVO baseWeb;

	@Getter
	@Setter
	private Map<String, Object> fieldMap;

	@Getter
	@Setter
	private JobView jobView;

	@Getter
	@Setter
	private WebKitForm webKitForm;

	@Getter
	@Setter
	private List<WebKitField> formFieldsEdit;

	@Getter
	@Setter
	private User loggedInUser;

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
		if (this.baseWeb == null) return null;
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

	public String getFluidItemFormState() {
		if (this.getFluidItemForm() == null) return null;
		return this.getFluidItemForm().getState();
	}

	public FluidItem.FlowState getFluidItemFormFlowState() {
		if (this.getFluidItem() == null) return null;
		return this.getFluidItem().getFlowState();
	}

	public String getFluidItemTitle() {
		if (this.getFluidItemForm() == null) return null;
		return this.getFluidItemForm().getTitle();
	}

	public Date getFluidItemStepEntered() {
		if (this.getFluidItem() == null) return null;
		return this.getFluidItem().getStepEnteredTime();
	}

	public Long getFluidItemStepEnteredTimestamp() {
		if (this.getFluidItemStepEntered() == null) return new Date().getTime();
		return this.getFluidItemStepEntered().getTime();
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

	public List<Field> getRouteFields() {
		if (this.baseWeb == null || this.baseWeb.getFluidItem() == null) return null;
		return this.baseWeb.getFluidItem().getRouteFields();
	}

	public List<Field> getFormFields() {
		if (this.baseWeb == null || this.baseWeb.getForm() == null) return null;
		return this.baseWeb.getForm().getFormFields();
	}

	public void refreshFormFieldsEdit() {
		List<Field> visibleFields = this.getFormFieldsViewable();
		if (visibleFields == null) visibleFields = new ArrayList<>();
		final List<Field> formFields = (this.getFormFields() == null) ? new ArrayList<>() : this.getFormFields();

		List<WebKitField> editFieldsList = new ArrayList<>();
		visibleFields.forEach(visibleField -> {
			Object fieldValueToSet = formFields.stream()
							.filter(itm -> itm.getFieldName().equals(visibleField.getFieldName()))
							.findFirst()
							.map(itm -> itm.getFieldValue())
							.orElse(visibleField.getFieldValue());
			final List<String> userFilteredAvailableOptions;
			if (fieldValueToSet instanceof MultiChoice) {
				MultiChoice casted = (MultiChoice)fieldValueToSet;
				fieldValueToSet = casted.cloneMultiChoice();
				userFilteredAvailableOptions = this.applyUserToFormFieldFilterForLoggedInUser(
						visibleField.getFieldName(), casted.getAvailableMultiChoices());
			} else if (fieldValueToSet instanceof TableField) {
				fieldValueToSet = null;//Clear the records...
				userFilteredAvailableOptions = null;//Clear the records...
			} else userFilteredAvailableOptions = null;

			WebKitField fieldToAdd = new WebKitField(
				null,
				visibleField.getFieldName(),
				fieldValueToSet,
				visibleField.getTypeAsEnum(),
				userFilteredAvailableOptions
			);
			fieldToAdd.setTypeMetaData(visibleField.getTypeMetaData());
			fieldToAdd.setFieldDescription(visibleField.getFieldDescription());
			editFieldsList.add(fieldToAdd);
		});
		this.formFieldsEdit = editFieldsList;
	}

	private List<String> applyUserToFormFieldFilterForLoggedInUser(
		String fieldName,
		List<String> allAvailableMultiChoices
	) {
		if (this.webKitForm == null) return allAvailableMultiChoices;

		List<String> multiChoicesMarkedForFilter = this.webKitForm.getUserToFormFieldLimitOnMultiChoice();
		if (multiChoicesMarkedForFilter == null || multiChoicesMarkedForFilter.isEmpty()) return allAvailableMultiChoices;

		//Not marked for the filtering, display all...
		if (!multiChoicesMarkedForFilter.contains(fieldName)) return allAvailableMultiChoices;

		if (this.loggedInUser == null) return allAvailableMultiChoices;

		MultiChoice choiceFieldFromUser = this.loggedInUser.getFieldValueAsMultiChoice(fieldName);
		if (choiceFieldFromUser == null) return multiChoicesMarkedForFilter;

		//We need to match the form against the currently logged-in user selected choices.
		List<String> userFieldSelectChoices = choiceFieldFromUser.getSelectedMultiChoices();
		if (userFieldSelectChoices == null || userFieldSelectChoices.isEmpty()) return multiChoicesMarkedForFilter;

		return allAvailableMultiChoices.stream()
				.filter(itm -> userFieldSelectChoices.contains(itm))
				.collect(Collectors.toList());
	}

	public List<Field> getFormFieldsEditAsFields() {
		if (this.formFieldsEdit == null) return null;
		return this.formFieldsEdit.stream()
				.map(WebKitField::asField)
				.collect(Collectors.toList());
	}

	@XmlTransient
	public WebKitField getFormFieldsEditWithName(String fieldName) {
		if (fieldName == null || fieldName.trim().isEmpty()) return null;
		if (this.formFieldsEdit == null) return null;

		return this.formFieldsEdit.stream()
				.filter(itm -> fieldName.equals(itm.getFieldName()))
				.findFirst()
				.orElse(null);
	}

	@XmlTransient
	public void resetFormFieldsEditMandatoryAndEmpty() {
		if (this.formFieldsEdit == null) return;

		this.formFieldsEdit.forEach(itm -> itm.setMandatoryAndEmpty(false));
	}
	
	public List<Field> getFormFieldsViewable() {
		if (this.baseWeb == null || this.baseWeb.getFieldsViewable() == null) return null;
		return this.baseWeb.getFieldsViewable();
	}

	public List<Field> getFormFieldsEditable() {
		if (this.baseWeb == null || this.baseWeb.getFieldsEditable() == null) return null;
		return this.baseWeb.getFieldsEditable();
	}

	public String getRowId() {
		return String.format("%d-%d-%d",
				this.getFluidItemFormId(),
				this.getFluidItemId(),
				this.getFluidItemViewId());
	}

	public boolean isFormFieldEditable(String formField) {
		if (formField == null || formField.trim().isEmpty()) return false;
		List<Field> editableFields = this.getFormFieldsEditable();
		if (editableFields == null || editableFields.isEmpty()) return false;

		String fieldLower = formField.trim().toLowerCase();
		Field fieldWithName = editableFields.stream()
				.filter(itm -> itm.getFieldName() != null &&
						fieldLower.equals(itm.getFieldName().trim().toLowerCase()))
				.findFirst()
				.orElse(null);
		return fieldWithName != null;
	}
	
	public boolean isFormFieldMandatory(String formField) {
		if ((formField == null || formField.trim().isEmpty()) || this.webKitForm == null) return false;

		if (this.webKitForm.getMandatoryFields() == null || this.webKitForm.getMandatoryFields().isEmpty()) return false;

		return this.webKitForm.getMandatoryFields().contains(formField);
	}

	public boolean isFluidItemInWIPState() {
		if (this.getFluidItemId() != null && this.getFluidItemId() > 0) return true;
		if (FluidItem.FlowState.isFlowStateWIP(this.getFluidItemFormFlowState())) return true;

		return false;
	}

	public boolean isFormLocked() {
		return Form.State.LOCKED.equals(this.getFluidItemFormState());
	}

	public boolean isFormLockedByLoggedInUser() {
		return (this.getLoggedInUser() == null || this.getLoggedInUser().getUsername() == null) ? false :
				((this.getFluidItemForm() == null || this.getFluidItemForm().getCurrentUser() == null) ||
						this.getFluidItemForm().getCurrentUser().getUsername() == null) ? false :
						this.getFluidItemForm().getCurrentUser().getUsername().equals(this.getLoggedInUser().getUsername());
	}

	public boolean isFluidItemFormSet() {
		return (this.getFluidItemFormId() != null && this.getFluidItemFormId() > 0);
	}

	/**
	 * Refresh the local {@code fieldMap} based on the {@code FluidItem} route and form fields.
	 *
	 * @see FluidItem
	 * @see Field
	 */
	private void refreshFieldNameAndValueMap() {
		if (this.getFluidItem() == null) return;

		this.fieldMap = new HashMap<>();

		//Route Fields...
		if (this.getFluidItem().getRouteFields() != null) {
			this.getFluidItem().getRouteFields().stream()
					.filter(itm -> itm.getFieldValue() != null)
					.forEach(field -> {
						if (Field.Type.DateTime.toString().equals(field.getFieldType())) {
							this.fieldMap.put(field.getFieldName(), field.getFieldValue());
						} else {
							this.fieldMap.put(field.getFieldName(), field.getFieldValue().toString());
						}
					});
		}

		//Form Fields...
		if (this.getFluidItem().getForm() != null && this.getFluidItem().getForm().getFormFields() != null) {
			this.getFluidItem().getForm().getFormFields().stream()
					.filter(itm -> itm.getFieldValue() != null)
					.forEach(field -> {
						if (Field.Type.TextEncrypted.toString().equals(field.getFieldType())) {
							//String fieldValueClearText = (String)field.getFieldValue();
							//TODO @jason Need to mask it properly here...
							this.fieldMap.put(field.getFieldName(), "**********");
						} else if (Field.Type.DateTime.toString().equals(field.getFieldType())) {
							this.fieldMap.put(field.getFieldName(), field.getFieldValue());
						} else {
							this.fieldMap.put(field.getFieldName(), field.getFieldValue().toString());
						}
					});
		}
	}
}
