package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.historic.FormHistoricData;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import lombok.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.Visibility;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SessionScoped
@Named("webKitOpenFormFieldHistoryConversationBean")
public class WebKitOpenFormFieldHistoryConversationBean extends ABaseManagedBean {
	@Inject
	private WebKitAccessBean accessBean;

	@Getter
	@Setter
	private Form historyForm;

	@Getter
	@Setter
	private List<FormHistoricData> formHistoricData;

	@Getter
	@Setter
	private List<WebKitFormFieldHistory> formFieldHistories;

	@Getter
	@Setter
	private List<FlatFieldHistory> formFieldHistoriesFlat;

	private static final String EMPTY_FLAG = "-";
	private static final boolean IGNORE_FIELDS_WHERE_VAL_SAME = true;
	private static final boolean IGNORE_TABLE_LABEL = true;

	@EqualsAndHashCode
	@Data
	@RequiredArgsConstructor
	public static class WebKitFormFieldHistory {
		private final String username;
		private final Date timestamp;
		private final List<Field> fields;

		private List<PriorField> priorFields;
		private boolean isCurrent;

		public PriorField getPriorFieldForField(Field field) {
			final String fieldName;
			if (field == null || (fieldName = field.getFieldName()) == null) return null;

			if (this.priorFields == null || this.priorFields.isEmpty()) return null;

			return this.priorFields.stream()
					.filter(itm -> fieldName.equals(itm.getFieldName()))
					.findFirst()
					.orElse(null);
		}

		public Date getFirstValidPriorDate() {
			if (this.priorFields == null || this.priorFields.isEmpty()) return null;

			return this.priorFields.stream()
					.filter(itm -> itm.getModifiedDate() != null && itm.getModifiedDate().getTime() > 1L)
					.findFirst()
					.map(itm -> itm.getModifiedDate())
					.orElse(null);
		}
	}
	
	@Data
	@NoArgsConstructor
	public static class PriorField extends Field {
		private Date modifiedDate;
		private String modifiedUser;
		
		public PriorField(Field field, Date modifiedDate, String modifiedUser) {
			super(field);
			this.setModifiedDate(modifiedDate);
			this.setModifiedUser(modifiedUser);
		}
	}

	@EqualsAndHashCode
	@Data
	@RequiredArgsConstructor
	public static class FlatFieldHistory implements Serializable {
		private final String username;
		private final Long timestampTime;
		private final Date timestamp;
		private final Field field;

		private final PriorField priorField;
		private final Date priorFieldModifiedDate;
		private final boolean isCurrent;
		private final int numberOfChanges;

		public String getDisplayValueForTimeDiff() {
			if (this.priorFieldModifiedDate == null || this.priorFieldModifiedDate.getTime() < 1) return "N/A";

			long takenInMillis = (this.timestampTime - this.priorFieldModifiedDate.getTime());

			long takenInDays = TimeUnit.MILLISECONDS.toDays(takenInMillis),
					takenInHours = (TimeUnit.MILLISECONDS.toHours(takenInMillis) % 24),
					takenInMinutes = (TimeUnit.MILLISECONDS.toMinutes(takenInMillis) % 60),
					takenInSeconds = (TimeUnit.MILLISECONDS.toSeconds(takenInMillis) % 60);
			StringBuilder returnVal = new StringBuilder();

			if (takenInDays > 0) returnVal.append(String.format("%s day%s, ", takenInDays, takenInDays == 1 ? UtilGlobal.EMPTY: "s"));
			if (takenInHours > 0) returnVal.append(String.format("%s hour%s, ", takenInHours, takenInHours == 1 ? UtilGlobal.EMPTY: "s"));
			if (takenInMinutes > 0) returnVal.append(String.format("%s minute%s, ", takenInMinutes, takenInMinutes == 1 ? UtilGlobal.EMPTY: "s"));
			if (takenInSeconds > 0) returnVal.append(String.format("%s second%s, ", takenInSeconds, takenInSeconds == 1 ? UtilGlobal.EMPTY: "s"));

			String toString = returnVal.toString();
			if (toString.isEmpty()) return String.format("%s millis", takenInMillis);

			return toString.substring(0, toString.length() - 2);
		}

		public boolean isValueOfTypeSignature() {
			if (this.field == null) return false;
			if (UtilGlobal.isBlank(this.field.getFieldValueAsString())) return false;

			switch (this.field.getTypeAsEnum()) {
				case Text:
				case ParagraphText:
					String val = this.field.getFieldValueAsString();
					if (val == null) return false;

					try {
						JSONObject sigJSON = new JSONObject(val);
						return sigJSON.has("lines");
					} catch (JSONException jsonErr) {
						return false;
					}
			}

			return false;
		}

		public boolean isValueOfTypeTextDisplay() {
			if (this.field == null) return false;
			if (UtilGlobal.isBlank(this.field.getFieldValueAsString())) return false;

			switch (this.field.getTypeAsEnum()) {
				case Text:
				case ParagraphText:
				case TextEncrypted:
				case Decimal:
				case Label:
				case MultipleChoice:
				case Table:
				case TrueFalse:
					return true;
				default:
					return false;
			}
		}

		public boolean isValueOfTypeTextDateTime() {
			if (this.field == null) return false;
			if (UtilGlobal.isBlank(this.field.getFieldValueAsString())) return false;

			return (this.field.getTypeAsEnum() == Field.Type.DateTime);
		}

		public String getUsernameNoWhitespace() {
			return UtilGlobal.removeWhitespace(this.getUsername());
		}
	}

	public void actionLoadFormHistoricData() {
		if (this.getFluidClientDS() == null) return;

		final Map<Long, List<Field>> dateAndFieldValuesMapping = new HashMap<>();
		final Map<Long, String> dateAndUserMapping = new HashMap<>();

		FormContainerClient fcc = this.getFluidClientDS().getFormContainerClient();

		try {
			this.setFormHistoricData(fcc.getFormAndFieldHistoricData(this.historyForm, false, true));

			AtomicLong al = new AtomicLong();
			this.getFormHistoricData().forEach(historyItem -> {
				String historyType = historyItem.getHistoricEntryType();

				if (historyType.equals(FormHistoricData.HistoricEntryType.FORM_CONTAINER_LOG_ENTRY) ||
						historyType.equals(FormHistoricData.HistoricEntryType.FORM_CONTAINER)) {
					Long timeOfChange = historyItem.getDate() == null ? System.currentTimeMillis() : historyItem.getDate().getTime();
					String user = historyItem.getUser() == null ? EMPTY_FLAG : historyItem.getUser().getUsername();
					al.set(timeOfChange);
					dateAndUserMapping.put(timeOfChange, user);
				}

				Long key = al.get();
				if (historyType.equals(FormHistoricData.HistoricEntryType.FIELD_AND_VALUE)) {
					List<Field> fieldsForDateChange = dateAndFieldValuesMapping.getOrDefault(key, new ArrayList<>());
					fieldsForDateChange.add(historyItem.getField());
					Collections.sort(fieldsForDateChange, Comparator.comparing(Field::getFieldName));
					dateAndFieldValuesMapping.put(key, fieldsForDateChange);
				}
			});

			this.formFieldHistories = new ArrayList<>();
			this.formFieldHistoriesFlat = new ArrayList<>();

			//Map the data in order to display...
			List<Long> timestampDesc = new ArrayList<>(dateAndUserMapping.keySet());
			Collections.sort(timestampDesc);
			Collections.reverse(timestampDesc);

			for (int index = 0; index < timestampDesc.size(); index++) {
				Long timestamp = timestampDesc.get(index);

				String user = dateAndUserMapping.get(timestamp);
				List<Field> fields = dateAndFieldValuesMapping.get(timestamp);
				this.removeFieldsWhereUserHasAccess(fields);

				WebKitFormFieldHistory toAdd = new WebKitFormFieldHistory(user, new Date(timestamp), fields);
				toAdd.setPriorFields(this.obtainPriorFields(
					timestamp,
					timestampDesc,
					fields,
					dateAndFieldValuesMapping,
					dateAndUserMapping)
				);

				if (IGNORE_FIELDS_WHERE_VAL_SAME) this.removeFieldsWhereValuesMatch(toAdd);
				if (IGNORE_TABLE_LABEL) this.removeUnwantedFieldTypes(toAdd);

				this.formFieldHistories.add(toAdd);
			}

			//Set the current history...
			if (!this.formFieldHistories.isEmpty()) {
				this.formFieldHistories.get(0).setCurrent(true);
				this.formFieldHistoriesFlat = this.toFlat(this.formFieldHistories);
			}
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	private void removeFieldsWhereUserHasAccess(List<Field> fields) {
		if (fields == null || fields.isEmpty()) return;

		List<Field> fieldsUserCanView = this.accessBean.getFieldsViewableForFormDef(this.historyForm.getFormType());
		if (fieldsUserCanView == null || fieldsUserCanView.isEmpty()) {
			fields.clear();
			return;
		}

		new ArrayList<>(fields).stream().forEach(param -> {
			Field fieldWithName = fieldsUserCanView.stream()
					.filter(itm -> itm.getFieldName().equals(param.getFieldName()))
					.findFirst()
					.orElse(null);
			if (fieldWithName == null) fields.remove(param);
		});
	}

	private void removeFieldsWhereValuesMatch(WebKitFormFieldHistory history) {
		if (history.getFields() == null || history.getPriorFields() == null) return;

		List<Field> fieldListingNew = new ArrayList<>(history.getFields());
		fieldListingNew.forEach(field -> {
			PriorField priorField = history.getPriorFieldForField(field);
			if (priorField == null) return;

			if (field.valueEquals(priorField)) history.getFields().remove(field);
		});
	}

	private void removeUnwantedFieldTypes(WebKitFormFieldHistory history) {
		if (history.getFields() == null || history.getPriorFields() == null) return;

		List<Field> fieldListingNew = new ArrayList<>(history.getFields());
		fieldListingNew.forEach(field -> {
			if (field.getTypeAsEnum() != null) {
				switch (field.getTypeAsEnum()) {
					case Table:
					case Label:
						history.getFields().remove(field);
				}
			}
		});
	}

	private List<PriorField> obtainPriorFields(
			Long currentRecordTimestamp,
			List<Long> timestampDesc,
			List<Field> fieldsToObtainFor,
			Map<Long, List<Field>> fieldValuesAtDate,
			Map<Long, String> usersAtDate
	) {
		List<PriorField> returnVal = new ArrayList<>();
		if (fieldsToObtainFor == null || fieldsToObtainFor.isEmpty()) return returnVal;

		if (fieldValuesAtDate.isEmpty() || usersAtDate.isEmpty()) return returnVal;

		fieldsToObtainFor.stream().forEach(field -> {
			AtomicReference<PriorField> priorFieldFound = new AtomicReference<>(null);
			timestampDesc.stream()
					.filter(itm -> (itm < currentRecordTimestamp))
					.forEach(timestampItm -> {
						if (priorFieldFound.get() != null) return;

						List<Field> fieldsAtDate = fieldValuesAtDate.get(timestampItm);
						if (fieldsAtDate == null) return;
						
						Field priorField = fieldsAtDate.stream()
								.filter(fieldItm -> field.getFieldName().equals(fieldItm.getFieldName()))
								.findFirst()
								.orElse(null);
						if (priorField != null) {
							priorFieldFound.set(new PriorField(priorField, new Date(timestampItm), usersAtDate.get(timestampItm)));
						}
					});
			if (priorFieldFound.get() != null) returnVal.add(priorFieldFound.get());
		});

		return returnVal;
	}

	private List<FlatFieldHistory> toFlat(List<WebKitFormFieldHistory> history) {
		List<FlatFieldHistory> returnVal = new ArrayList<>();
		if (history == null || history.isEmpty()) return returnVal;

		history.stream()
				.filter(itm -> (itm.getFields() != null && !itm.getFields().isEmpty()))
				.forEach(historyItm -> {
					Date priorFieldDate = (historyItm.getPriorFields() == null) ? null :
							historyItm.getPriorFields().stream()
									.filter(itm -> itm.getModifiedDate() != null)
									.findFirst()
									.map(itm -> itm.getModifiedDate())
									.orElse(null);

					historyItm.getFields().forEach(fieldItm -> {
						FlatFieldHistory toAdd = new FlatFieldHistory(
							historyItm.getUsername(),
							historyItm.getTimestamp().getTime(),
							historyItm.getTimestamp(),
							fieldItm,
							historyItm.getPriorFieldForField(fieldItm),
							priorFieldDate,
							historyItm.isCurrent,
							historyItm.getFields() == null ? 0 : historyItm.getFields().size()
						);

						returnVal.add(toAdd);
					});
				});

		return returnVal;
	}

	public void actionOnRowToggle(ToggleEvent event) {
		if (event.getVisibility() == Visibility.VISIBLE) {
			Object data = event.getData();
		}
	}

	public int getFormFieldHistoriesFlatSize() {
		if (this.getFormFieldHistoriesFlat() == null) return 0;

		return this.getFormFieldHistoriesFlat().size();
	}

	public List<FlatFieldHistory> getFormFieldHistoriesFlatModifyOnly() {
		List<FlatFieldHistory> org = this.getFormFieldHistoriesFlat();
		if (org == null || org.isEmpty()) return org;

		return org.stream()
				.filter(itm -> {
					PriorField priorField = itm.getPriorField();
					if (priorField == null || priorField.getFieldValue() == null) return false;
					if (itm.getField() == null || itm.getField().getFieldValue() == null) return false;
					
					return !itm.getField().getFieldValue().equals(priorField.getFieldValue());
				})
				.collect(Collectors.toList());
	}
}
