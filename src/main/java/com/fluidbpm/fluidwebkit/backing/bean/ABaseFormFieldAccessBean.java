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
import com.fluidbpm.program.api.vo.field.MultiChoice;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.form.FormFieldListing;
import com.fluidbpm.ws.client.v1.form.FormDefinitionClient;
import com.fluidbpm.ws.client.v1.form.FormFieldClient;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Bean to handle the field permissions.
 * 
 * @author jasonbruwer on 2018-01-04
 * @since 1.0
 */
public abstract class ABaseFormFieldAccessBean extends ABaseManagedBean {

	//Fields for Edit and View...
	private Map<String, List<Field>> fieldsForViewing;
	private Map<String, List<Field>> fieldsForEditing;

	private List<Form> formDefinitionsCanCreateInstanceOf;

	private boolean fieldCachingDone = false;

	static long realtimeCounter = 0;

	/**
	 *
	 */
	public ABaseFormFieldAccessBean() {
		this.fieldCachingDone = false;
	}

	/**
	 * Log a user into the Design Dashboard.
	 *
	 * @return navigation {@code dashboard}
	 */
	@PostConstruct
	public void actionPopulateInit() {
		if (this.fieldCachingDone) {
			this.getLogger().info("FFC-Bean: Fields ALREADY CACHED.");
			return;
		}

		if (this.getFluidClientDS() == null) {
			return;
		}

		realtimeCounter = 0;
		FormDefinitionClient formDefinitionClient = this.getFluidClientDS().getFormDefinitionClient();

		this.fieldsForViewing = new Hashtable<>();
		this.fieldsForEditing = new Hashtable<>();
		this.formDefinitionsCanCreateInstanceOf = new ArrayList<>();

		try {
			this.formDefinitionsCanCreateInstanceOf = formDefinitionClient.getAllByLoggedInUserWhereCanCreateInstanceOf(true);
			if (this.formDefinitionsCanCreateInstanceOf != null) {
				if (this.getFormDefsToIgnore() != null) {
					this.formDefinitionsCanCreateInstanceOf = this.formDefinitionsCanCreateInstanceOf.stream()
							.filter(itm -> !this.getFormDefsToIgnore().contains(itm.getFormType()))
							.collect(Collectors.toList());
				}
				Collections.sort(this.formDefinitionsCanCreateInstanceOf, Comparator.comparing(Form::getTitle));
			}

			this.getLogger().info("FFC-Bean: PART-1-COMPLETE.");
			List<Form> allFormDefinitions = formDefinitionClient.getAllByLoggedInUser(true);
			if (allFormDefinitions == null) {
				return;
			}

			this.getLogger().info("FFC-Bean: PART-2-COMPLETE.");

			long now = System.currentTimeMillis();

			//Set all the field definitions...
			List<CompletableFuture> allAsyncs = new ArrayList<>();
			final FormFieldClient formFieldClient = this.getFluidClientDS().getFormFieldClient();

			for (Form formDef : allFormDefinitions) {
				//Ignore any configuration [Form Definitions]...
				String formDefTitle = formDef.getFormType();
				if (this.getFormDefsToIgnore() != null && this.getFormDefsToIgnore().contains(formDefTitle)) {
					continue;
				}

				//Run the following asynchronous...
				CompletableFuture toAdd = CompletableFuture.runAsync(
				() -> {
					long starting = System.currentTimeMillis();
					List<Field> formFieldsForView = new ArrayList<>();
					List<Field> formFieldsForEdit = new ArrayList<>();

					this.setFieldsForEditAndViewing(formFieldClient, formDefTitle, formFieldsForView, formFieldsForEdit);

					this.fieldsForViewing.put(formDefTitle, formFieldsForView);
					this.fieldsForEditing.put(formDefTitle, formFieldsForEdit);

					this.getLogger().info(String.format("FFC-Bean: PART-2-[%s]-COMPLETE TK(%d)",
							formDefTitle, (System.currentTimeMillis() - starting)));
					realtimeCounter += (System.currentTimeMillis() - starting);
				});
				allAsyncs.add(toAdd);
			}

			//We are waiting for all of them to complete...
			CompletableFuture.allOf(allAsyncs.toArray(new CompletableFuture[]{})).join();
			this.getLogger().info(String.format("FFC-Bean: PART-3-COMPLETE: %d millis. Total of %d items. Should have taken %d.",
					(System.currentTimeMillis() - now),
					this.fieldsForViewing.size(),
					realtimeCounter));

			this.fieldCachingDone = true;
		} catch (Exception except) {
			this.raiseError(except);
		} finally {
			this.getLogger().info("FFC-Bean: DONE");
		}
	}

	/**
	 *
	 * @return
	 */
	protected abstract List<String> getFormDefsToIgnore();

	/**
	 *
	 * @param formDefinitionParam
	 * @return
	 */
	public List<Field> getFieldsViewableForFormDef(String formDefinitionParam) {
		if (formDefinitionParam == null || formDefinitionParam.trim().isEmpty()) {
			return null;
		}

		if (this.fieldsForViewing == null || this.fieldsForViewing.isEmpty()) {
			return null;
		}

		return this.fieldsForViewing.get(formDefinitionParam);
	}

	/**
	 * 
	 * @param formDefinitionParam
	 * @param formFieldParam
	 * @return
	 */
	public Field getFieldBy(
		String formDefinitionParam,
		String formFieldParam
	) {
		if (formDefinitionParam == null || formDefinitionParam.trim().isEmpty()) {
			return null;
		}

		if (formFieldParam == null || formFieldParam.trim().isEmpty()) {
			return null;
		}

		if (this.fieldsForViewing == null || this.fieldsForViewing.isEmpty()) {
			return null;
		}

		List<Field> fieldsForFormDef = this.fieldsForViewing.get(formDefinitionParam);
		if (fieldsForFormDef == null) {
			return null;
		}

		String paramLowerTrim = formFieldParam.trim().toLowerCase();
		return fieldsForFormDef.stream()
				.filter(itm -> itm.getFieldName() != null &&
						paramLowerTrim.equals(itm.getFieldName().trim().toLowerCase()))
				.findFirst()
				.orElse(null);
	}

	/**
	 * 
	 * @param formDefinitionParam
	 * @return
	 */
	public List<Field> getFieldsEditableForFormDef(String formDefinitionParam) {
		if (formDefinitionParam == null || formDefinitionParam.trim().isEmpty()) {
			return null;
		}

		if (this.fieldsForEditing == null || this.fieldsForEditing.isEmpty()) {
			return null;
		}

		return this.fieldsForEditing.get(formDefinitionParam);
	}

	/**
	 *
	 * @return
	 */
	public List<Form> getFormDefinitionsCanCreateInstanceOfSorted() {
		return this.formDefinitionsCanCreateInstanceOf;
	}

	/**
	 *
	 * @return
	 */
	public int getNumberOfFormDefinitionsCanCreateInstanceOf() {
		return (this.formDefinitionsCanCreateInstanceOf == null) ? 0 :
				this.formDefinitionsCanCreateInstanceOf.size();
	}

	/**
	 *
	 * @param formFieldClientParam
	 * @param formNameParam
	 * @param fieldsForViewingParam
	 * @param fieldsForEditingParam
	 */
	private void setFieldsForEditAndViewing(
		FormFieldClient formFieldClientParam,
		String formNameParam,
		List<Field> fieldsForViewingParam,
		List<Field> fieldsForEditingParam
	) {
		//View fields...
		FormFieldListing formFieldViewFields =
				formFieldClientParam.getFieldsByFormNameAndLoggedInUser(
						formNameParam, false);
		List<Field> viewListing = formFieldViewFields.getListing();
		if (fieldsForViewingParam != null && viewListing != null) {
			fieldsForViewingParam.addAll(viewListing);
		}

		//Populate Multi Choice Fields...
		this.populateAvailableMultiChoicesForField(
				formFieldClientParam,
				fieldsForViewingParam);

		//Edit fields...
		FormFieldListing formFieldEditFields =
				formFieldClientParam.getFieldsByFormNameAndLoggedInUser(
						formNameParam,
						true);

		List<Field> editListing = formFieldEditFields.getListing();
		if(fieldsForEditingParam != null && editListing != null) {
			fieldsForEditingParam.addAll(editListing);
		}
	}

	/**
	 *
	 * @param formFieldClientParam
	 * @param fieldsForViewingParam
	 */
	private void populateAvailableMultiChoicesForField(FormFieldClient formFieldClientParam, List<Field> fieldsForViewingParam) {
		if (fieldsForViewingParam == null || fieldsForViewingParam.isEmpty()) {
			return;
		}

		for (Field fieldToPopulate : fieldsForViewingParam) {
			if (fieldToPopulate.getTypeAsEnum() != Field.Type.MultipleChoice) {
				continue;
			}

			//Get from the id fetch...
			Field fieldById = formFieldClientParam.getFieldById(fieldToPopulate.getId());
			MultiChoice fieldMultiChoiceToExtractFrom = (MultiChoice)fieldById.getFieldValue();

			MultiChoice multiChoiceToPopulate = (MultiChoice)fieldToPopulate.getFieldValue();
			if (multiChoiceToPopulate == null) {
				multiChoiceToPopulate = new MultiChoice();
			}

			multiChoiceToPopulate.setAvailableMultiChoices(
					fieldMultiChoiceToExtractFrom.getAvailableMultiChoices());
			multiChoiceToPopulate.setAvailableMultiChoicesCombined(
					fieldMultiChoiceToExtractFrom.getAvailableMultiChoicesCombined());

			fieldToPopulate.setFieldValue(multiChoiceToPopulate);
		}
	}

	/**
	 * 
	 * @param formToCheckForParam
	 * @return
	 */
	public boolean isCanUserCreate(String formToCheckForParam) {
		if (formToCheckForParam == null || formToCheckForParam.trim().isEmpty()){
			return false;
		}

		List<Form> formDefs = this.getFormDefinitionsCanCreateInstanceOfSorted();
		if (formDefs == null || formDefs.isEmpty()) {
			return false;
		}

		List<String> formDefTitles = formDefs.stream()
				.map(itm -> itm.getTitle())
				.collect(Collectors.toList());
		return formDefTitles.contains(formToCheckForParam);
	}
}
