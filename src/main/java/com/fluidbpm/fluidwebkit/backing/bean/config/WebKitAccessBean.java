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
package com.fluidbpm.fluidwebkit.backing.bean.config;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.field.MultiChoice;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.flow.JobViewListing;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.form.FormFieldListing;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.userquery.UserQuery;
import com.fluidbpm.program.api.vo.userquery.UserQueryListing;
import com.fluidbpm.ws.client.v1.flow.FlowStepClient;
import com.fluidbpm.ws.client.v1.form.FormDefinitionClient;
import com.fluidbpm.ws.client.v1.form.FormFieldClient;
import com.fluidbpm.ws.client.v1.userquery.UserQueryClient;
import lombok.Getter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Bean storing all access for the logged in user.
 */
@SessionScoped
@Named("webKitAccessBean")
public class WebKitAccessBean extends ABaseManagedBean {
	//Fields for Edit and View...
	private Map<String, List<Field>> fieldsForViewing;
	private Map<String, List<Field>> fieldsForEditing;

	private List<Form> formDefinitionsCanCreateInstanceOf;

	@Getter
	private List<Form> formDefinitionsAttachmentCanView;

	@Getter
	private List<Form> formDefinitionsAttachmentCanEdit;

	@Getter
	private List<Form> allFormDefinitionsForLoggedIn;

	@Getter
	private List<UserQuery> userQueriesCanExecute;

	@Getter
	private Map<Long, List<Field>> userQueryFieldMapping = new HashMap<>();

	@Getter
	private List<JobView> jobViewsCanAccess;

	private boolean cachingDone = false;
	static long realtimeCounter = 0;
	private String loggedInUsername;

	public WebKitAccessBean() {
		this.cachingDone = false;
	}

	/**
	 * Log a user into the Design Dashboard.
	 *
	 * @return navigation {@code dashboard}
	 */
	@PostConstruct
	public void actionPopulateInit() {
		if (this.cachingDone) {
			this.getLogger().info("FFC-Bean: Fields ALREADY CACHED.");
			return;
		}
		if (this.getFluidClientDS() == null) return;

		try {
			this.loggedInUsername = this.getLoggedInUserUsername();
			this.getLogger().info("FFC-Bean: Caching FormFieldsAndDef");
			this.cacheFormAndDefs();
			this.getLogger().info("FFC-Bean: Caching UserQueries");
			this.cacheUserQueries();
			this.getLogger().info("FFC-Bean: Caching JobViews");
			this.cacheJobViews();

			this.cachingDone = true;
		} catch (Exception except) {
			this.raiseError(except);
		} finally {
			this.getLogger().info("FFC-Bean: DONE");
		}
	}

	private void cacheFormAndDefs() {
		realtimeCounter = 0;
		FormDefinitionClient formDefinitionClient = this.getFluidClientDS().getFormDefinitionClient();

		this.fieldsForViewing = new Hashtable<>();
		this.fieldsForEditing = new Hashtable<>();
		this.formDefinitionsCanCreateInstanceOf = new ArrayList<>();
		this.formDefinitionsAttachmentCanView = new ArrayList<>();
		this.formDefinitionsAttachmentCanEdit = new ArrayList<>();
		this.allFormDefinitionsForLoggedIn = new ArrayList<>();

		//Retrieve Forms and their Workflows...
		this.formDefinitionsCanCreateInstanceOf = formDefinitionClient.getAllByLoggedInUserWhereCanCreateInstanceOf(
				false, true);
		if (this.formDefinitionsCanCreateInstanceOf != null) {
			if (this.getFormDefsToIgnore() != null) {
				this.formDefinitionsCanCreateInstanceOf = this.formDefinitionsCanCreateInstanceOf.stream()
						.filter(itm -> !this.getFormDefsToIgnore().contains(itm.getFormType()))
						.collect(Collectors.toList());
			}
			Collections.sort(this.formDefinitionsCanCreateInstanceOf, Comparator.comparing(Form::getFormType));
		}

		this.formDefinitionsAttachmentCanView = formDefinitionClient.getAllByLoggedInUserWhereCanViewAttachments();
		this.formDefinitionsAttachmentCanEdit = formDefinitionClient.getAllByLoggedInUserWhereCanEditAttachments();

		this.getLogger().info("FFC-Bean: PART-1-COMPLETE.");
		this.allFormDefinitionsForLoggedIn = formDefinitionClient.getAllByLoggedInUser(true);
		if (allFormDefinitionsForLoggedIn == null) return;

		this.getLogger().info("FFC-Bean: PART-2-COMPLETE.");
		long now = System.currentTimeMillis();

		//Set all the field definitions...
		List<CompletableFuture> allAsyncs = new ArrayList<>();
		for (Form formDef : this.allFormDefinitionsForLoggedIn) {
			//Ignore any configuration [Form Definitions]...
			String formDefTitle = formDef.getFormType();
			if (this.getFormDefsToIgnore() != null && this.getFormDefsToIgnore().contains(formDefTitle)) continue;

			//Run the following asynchronous...
			String endpoint = this.getFluidClientDS().getEndpoint(),
					serviceTicket = this.getFluidClientDS().getServiceTicket();
			CompletableFuture toAdd = CompletableFuture.runAsync(
					() -> {
						try (FormFieldClient formFieldClient = new FormFieldClient(endpoint, serviceTicket)) {
							long starting = System.currentTimeMillis();
							List<Field> formFieldsForView = new ArrayList<>();
							List<Field> formFieldsForEdit = new ArrayList<>();

							this.setFieldsForEditAndViewing(formFieldClient, formDefTitle, formFieldsForView, formFieldsForEdit);

							this.fieldsForViewing.put(formDefTitle, formFieldsForView);
							this.fieldsForEditing.put(formDefTitle, formFieldsForEdit);

							this.getLogger().info(String.format("FFC-Bean: PART-2-[%s]-COMPLETE TK(%d)",
									formDefTitle, (System.currentTimeMillis() - starting)));
							realtimeCounter += (System.currentTimeMillis() - starting);
						}
					});
			allAsyncs.add(toAdd);
		}

		//We are waiting for all of them to complete...
		CompletableFuture.allOf(allAsyncs.toArray(new CompletableFuture[]{})).join();
		this.getLogger().info(String.format("FFC-Bean: PART-3-COMPLETE: %d millis. Total of %d items. Should have taken %d.",
				(System.currentTimeMillis() - now),
				this.fieldsForViewing.size(),
				realtimeCounter));

	}

	private void cacheUserQueries() {
		UserQueryClient userQueryClient = this.getFluidClientDS().getUserQueryClient();
		UserQueryListing userQueries = userQueryClient.getAllUserQueriesByLoggedInUser();
		this.userQueriesCanExecute = userQueries.getListing();
		if (this.userQueriesCanExecute == null) this.userQueriesCanExecute = new ArrayList<>();

		//Run the following asynchronous...
		String endpoint = this.getFluidClientDSConfig().getEndpoint(),
				serviceTicket = this.getFluidClientDSConfig().getServiceTicket();
		List<CompletableFuture> allAsyncs = new ArrayList<>();
		this.userQueriesCanExecute.forEach(itm -> {
			CompletableFuture toAdd = CompletableFuture.runAsync(
					() -> {
						try (FormFieldClient formFieldClient = new FormFieldClient(endpoint, serviceTicket)) {
							List<Field> fieldsForUserQuery = formFieldClient.getFormFieldsByUserQuery(itm);
							synchronized (this.userQueryFieldMapping) {
								this.userQueryFieldMapping.put(itm.getId(), fieldsForUserQuery);
							}
						}
					});
			allAsyncs.add(toAdd);
		});
		//We are waiting for all of them to complete...
		CompletableFuture.allOf(allAsyncs.toArray(new CompletableFuture[]{})).join();

		Collections.sort(this.userQueriesCanExecute, Comparator.comparing(UserQuery::getName));
	}

	private void cacheJobViews() {
		FlowStepClient stepClient = this.getFluidClientDS().getFlowStepClient();
		JobViewListing jobViewsListing = stepClient.getJobViewsByLoggedInUser();
		this.jobViewsCanAccess = jobViewsListing.getListing();
		if (this.jobViewsCanAccess != null) {
			Collections.sort(this.jobViewsCanAccess,
					Comparator
							.comparing(JobView::getViewGroupName)
							.thenComparing(JobView::getViewPriority));
		}
	}

	public List<Field> getFieldsViewableForFormDef(String formDefinitionParam) {
		if (formDefinitionParam == null || formDefinitionParam.trim().isEmpty()) return null;
		if (this.fieldsForViewing == null || this.fieldsForViewing.isEmpty()) return null;

		return this.fieldsForViewing.get(formDefinitionParam);
	}

	public String getFormDefinitionFromTableField(Field tableField) {
		if (this.allFormDefinitionsForLoggedIn == null || this.allFormDefinitionsForLoggedIn.isEmpty()) return null;

		String fieldMetaData = null;
		if (tableField == null || UtilGlobal.isBlank(fieldMetaData = tableField.getTypeMetaData())) return null;

		int indexOfUnderscore = fieldMetaData.indexOf("_");
		if (indexOfUnderscore < 0) return null;
		String formDefIdTxt = fieldMetaData.substring(0, indexOfUnderscore);
		if (UtilGlobal.isBlank(formDefIdTxt)) return null;

		return this.allFormDefinitionsForLoggedIn.stream()
				.filter(itm -> itm.getFormTypeId() != null && formDefIdTxt.equals(itm.getFormTypeId().toString()))
				.findFirst()
				.map(itm -> itm.getFormType())
				.orElse(null);
	}

	public Field getFieldBy(
			String formDefinitionParam,
			String formFieldParam
	) {
		if (formDefinitionParam == null || formDefinitionParam.trim().isEmpty()) return null;
		if (formFieldParam == null || formFieldParam.trim().isEmpty()) return null;
		if (this.fieldsForViewing == null || this.fieldsForViewing.isEmpty()) return null;

		List<Field> fieldsForFormDef = this.fieldsForViewing.get(formDefinitionParam);
		if (fieldsForFormDef == null) return null;

		String paramLowerTrim = formFieldParam.trim().toLowerCase();
		return fieldsForFormDef.stream()
				.filter(itm -> itm.getFieldName() != null &&
						paramLowerTrim.equals(itm.getFieldName().trim().toLowerCase()))
				.findFirst()
				.orElse(null);
	}

	public Field getFieldBy(String formFieldParam) {
		if (formFieldParam == null || formFieldParam.trim().isEmpty()) return null;
		if (this.fieldsForViewing == null || this.fieldsForViewing.isEmpty()) return null;

		return this.fieldsForViewing.values().stream()
				.flatMap(List::stream)
				.filter(itm -> formFieldParam.equals(itm.getFieldName()))
				.findFirst()
				.orElse(null);
	}

	public List<Field> getFieldsEditableForFormDef(String formDefinitionParam) {
		if (formDefinitionParam == null || formDefinitionParam.trim().isEmpty()) return null;
		if (this.fieldsForEditing == null || this.fieldsForEditing.isEmpty()) return null;

		return this.fieldsForEditing.get(formDefinitionParam);
	}

	public List<Form> getFormDefinitionsCanCreateInstanceOfSorted() {
		return this.formDefinitionsCanCreateInstanceOf;
	}

	public int getNumberOfFormDefinitionsCanCreateInstanceOf() {
		return (this.formDefinitionsCanCreateInstanceOf == null) ? 0 :
				this.formDefinitionsCanCreateInstanceOf.size();
	}

	public List<UserQuery> getUserQueriesCanExecuteSorted() {
		return this.userQueriesCanExecute;
	}

	public int getNumberOfUserQueriesCanExecute() {
		return (this.userQueriesCanExecute == null) ? 0 : this.userQueriesCanExecute.size();
	}

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
		if (fieldsForViewingParam != null && viewListing != null) fieldsForViewingParam.addAll(viewListing);

		//Populate Multi Choice Fields...
		this.populateAvailableMultiChoicesForField(
				this.getFluidClientDSConfig().getFormFieldClient(),
				fieldsForViewingParam);

		//Edit fields...
		FormFieldListing formFieldEditFields =
				formFieldClientParam.getFieldsByFormNameAndLoggedInUser(
						formNameParam,
						true);

		List<Field> editListing = formFieldEditFields.getListing();
		if (fieldsForEditingParam != null && editListing != null) fieldsForEditingParam.addAll(editListing);
	}

	private void populateAvailableMultiChoicesForField(FormFieldClient formFieldClientParam, List<Field> fieldsForViewingParam) {
		if (fieldsForViewingParam == null || fieldsForViewingParam.isEmpty()) return;

		for (Field fieldToPopulate : fieldsForViewingParam) {
			if (fieldToPopulate.getTypeAsEnum() != Field.Type.MultipleChoice) continue;

			//Get from the id fetch...
			Field fieldById = formFieldClientParam.getFieldById(fieldToPopulate.getId());
			MultiChoice fieldMultiChoiceToExtractFrom = (MultiChoice)fieldById.getFieldValue();

			MultiChoice multiChoiceToPopulate = (MultiChoice)fieldToPopulate.getFieldValue();
			if (multiChoiceToPopulate == null) multiChoiceToPopulate = new MultiChoice();

			multiChoiceToPopulate.setAvailableMultiChoices(
					fieldMultiChoiceToExtractFrom.getAvailableMultiChoices());
			multiChoiceToPopulate.setAvailableMultiChoicesCombined(
					fieldMultiChoiceToExtractFrom.getAvailableMultiChoicesCombined());

			fieldToPopulate.setFieldValue(multiChoiceToPopulate);
		}
	}

	public boolean isCanUserCreate(String formToCheckForParam) {
		if (formToCheckForParam == null || formToCheckForParam.trim().isEmpty()) return false;

		if (User.ADMIN_USERNAME.equals(this.getLoggedInUserUsername())) return true;

		List<Form> formDefs = this.getFormDefinitionsCanCreateInstanceOfSorted();
		if (formDefs == null || formDefs.isEmpty()) return false;

		List<String> formDefTitles = formDefs.stream()
				.map(itm -> itm.getFormType())
				.collect(Collectors.toList());
		return formDefTitles.contains(formToCheckForParam);
	}

	public boolean isCanUserAttachmentsView(String formToCheckForParam) {
		if (formToCheckForParam == null || formToCheckForParam.trim().isEmpty()) return false;

		if (User.ADMIN_USERNAME.equals(this.loggedInUsername)) return true;

		List<Form> formDefs = this.getFormDefinitionsAttachmentCanView();
		if (formDefs == null || formDefs.isEmpty()) return false;

		List<String> formDefTitles = formDefs.stream()
				.map(itm -> itm.getFormType())
				.collect(Collectors.toList());
		return formDefTitles.contains(formToCheckForParam);
	}

	public boolean isCanUserAttachmentsEdit(String formToCheckForParam) {
		if (formToCheckForParam == null || formToCheckForParam.trim().isEmpty()) return false;

		if (User.ADMIN_USERNAME.equals(this.loggedInUsername)) return true;

		List<Form> formDefs = this.getFormDefinitionsAttachmentCanEdit();
		if (formDefs == null || formDefs.isEmpty()) return false;

		List<String> formDefTitles = formDefs.stream()
				.map(itm -> itm.getFormType())
				.collect(Collectors.toList());
		return formDefTitles.contains(formToCheckForParam);
	}

	/**
	 * Don't ignore any forms.
	 * @return {@code null}
	 */
	protected List<String> getFormDefsToIgnore() {
		//TODO need to fetch this from the web kit client...
		return null;
	}

	public List<Field> getFormFieldsUserHasVisibilityFor(UserQuery query) {
		if (query == null) return null;
		if (this.fieldsForViewing == null || this.fieldsForViewing.isEmpty()) return null;

		List<Field> allForUserQuery = this.userQueryFieldMapping.get(query.getId());
		if (allForUserQuery == null || allForUserQuery.isEmpty()) return null;

		return allForUserQuery.stream()
				.filter(usrQueryFieldItm -> {
					String fieldNameToFetch = usrQueryFieldItm.getFieldName();
					Field fieldWithName = this.fieldsForViewing.values().stream()
							.flatMap(Collection::stream)
							.filter(itm -> itm.getFieldName().equals(fieldNameToFetch))
							.findFirst()
							.orElse(null);
					return fieldWithName != null && fieldWithName.getId() > 0L;
				})
				.collect(Collectors.toList());
	}
}
