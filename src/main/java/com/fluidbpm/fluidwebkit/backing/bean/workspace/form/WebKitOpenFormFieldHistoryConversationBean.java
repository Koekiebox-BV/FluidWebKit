package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitConfigBean;
import com.fluidbpm.fluidwebkit.backing.bean.som.GlobalIssuerBean;
import com.fluidbpm.fluidwebkit.backing.utility.Globals;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.field.DecimalMetaFormat;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.historic.FormHistoricData;
import com.fluidbpm.program.api.vo.user.User;
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
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.math.MathContext.DECIMAL64;

@SessionScoped
@Named("webKitOpenFormFieldHistoryConversationBean")
public class WebKitOpenFormFieldHistoryConversationBean extends ABaseManagedBean {
	@Inject
	private WebKitAccessBean accessBean;

	@Inject
	private WebKitConfigBean webKitConfigBean;

	@Inject
	private GlobalIssuerBean globalIssuerBean;

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
	public static final String SYSTEM_ROUTE = "_SYSTEM_ROUTE_";
	private static final boolean IGNORE_FIELDS_WHERE_VAL_SAME = true;
	private static final boolean IGNORE_SYSTEM_ROUTE = true;
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
	@EqualsAndHashCode(callSuper = false)
	@NoArgsConstructor
	@ToString
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
	@AllArgsConstructor
	@ToString
	public static class FlatFieldHistory implements Serializable {
		private final String username;
		private final Long timestampTime;
		private final Date timestamp;
		private final Field field;

		private PriorField priorField;
		private Date priorFieldModifiedDate;
		private boolean isCurrent;
		private final int numberOfChanges;

		public String getDisplayValueForTimeDiff() {
			if (this.priorFieldModifiedDate == null || this.priorFieldModifiedDate.getTime() < 1) return "N/A";

			long takenInMillis = (this.timestampTime > this.priorFieldModifiedDate.getTime()) ?
					(this.timestampTime - this.priorFieldModifiedDate.getTime()) :
					(this.priorFieldModifiedDate.getTime() - this.timestampTime);

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

		public boolean isValueOfTypeDecimal() {
			if (this.field == null) return false;
			if (this.field.getFieldValueAsNumber() == null) return false;

			return (this.field.getTypeAsEnum() == Field.Type.Decimal);
		}

		public String getUsernameNoWhitespace() {
			return UtilGlobal.removeWhitespace(this.getUsername());
		}
	}

	public void actionLoadFormHistoricData(boolean useAfsMechanism) {
		if (this.getFluidClientDS() == null) return;

		final Map<Long, List<Field>> dateAndFieldValuesMapping = new HashMap<>();
		final Map<Long, String> dateAndUserMapping = new HashMap<>();

		FormContainerClient fcc = this.getFluidClientDS().getFormContainerClient();

		if (!this.webKitConfigBean.getEnableSOMPostCardFeatures()) return;

		Currency currencyOverride = this.webKitConfigBean.getIssuerVelocityCurrency(
				this.getFluidClientDSConfig().getSQLUtilWrapper(),
				this.globalIssuerBean.getActiveIssuer()
		);

		try {
			this.setFormHistoricData(fcc.getFormAndFieldHistoricData(this.historyForm, false, true));

			AtomicLong al = new AtomicLong();
			AtomicReference<String> arUser = new AtomicReference<>();
			this.getFormHistoricData().forEach(historyItem -> {
				String historyType = historyItem.getHistoricEntryType();
				String user = historyItem.getUser() == null ? EMPTY_FLAG : historyItem.getUser().getUsername();
				Long timeOfChange = historyItem.getDate() == null ? System.currentTimeMillis() : historyItem.getDate().getTime();

				if (historyType.equals(FormHistoricData.HistoricEntryType.FORM_CONTAINER_LOG_ENTRY) ||
						historyType.equals(FormHistoricData.HistoricEntryType.FORM_CONTAINER)) {
					al.set(timeOfChange);
					arUser.set(user);
					dateAndUserMapping.put(timeOfChange, user);
				}

				Long key = al.get();
				if (historyType.equals(FormHistoricData.HistoricEntryType.FIELD_AND_VALUE)) {
					List<Field> fieldsForDateChange = dateAndFieldValuesMapping.getOrDefault(key, new ArrayList<>());

					// Remap Decimal Values for All Fields to be the same Format:
					if (currencyOverride != null) {
						fieldsForDateChange.stream()
								.filter(field -> field.isAmountMinorWithCurrency())
								.forEach(field -> {
									String currForOverride = DecimalMetaFormat.format(field.getDecimalMetaFormat(), currencyOverride);
									field.setTypeMetaData(currForOverride);
								});
					}
					this.sanitiseDecimalFieldsAllDoubleFormattedCurrency(fieldsForDateChange);

					fieldsForDateChange.add(historyItem.getField());
					Collections.sort(fieldsForDateChange, Comparator.comparing(Field::getFieldName));
					dateAndFieldValuesMapping.put(key, fieldsForDateChange);
				}
			});

			this.formFieldHistories = new ArrayList<>();
			this.formFieldHistoriesFlat = new ArrayList<>();

			if (useAfsMechanism) {
				this.processWithAFSMechanism(dateAndFieldValuesMapping, dateAndUserMapping);
				return;
			}

			//Map the data in order to display...
			List<Long> timestampDesc = new ArrayList<>(dateAndUserMapping.keySet());
			Collections.sort(timestampDesc);
			Collections.reverse(timestampDesc);

			for (int index = 0; index < timestampDesc.size(); index++) {
				Long timestamp = timestampDesc.get(index);

				String user = dateAndUserMapping.get(timestamp);
				List<Field> fields = dateAndFieldValuesMapping.get(timestamp);

				// Remove Fields where user has no Access:
				this.removeFieldsWhereUserHasNoAccess(fields);

				WebKitFormFieldHistory toAdd = new WebKitFormFieldHistory(user, new Date(timestamp), fields);
				toAdd.setPriorFields(this.obtainPriorFields(
					timestamp,
					timestampDesc,
					fields,
					dateAndFieldValuesMapping,
					dateAndUserMapping)
				);

				// Remove AFS Fields no wanted at all:
				this.removeUnwantedFields(toAdd);
				if (IGNORE_FIELDS_WHERE_VAL_SAME) this.removeFieldsWhereValuesMatch(toAdd);
				if (IGNORE_TABLE_LABEL) this.removeUnwantedFieldTypes(toAdd);
				if (IGNORE_SYSTEM_ROUTE && this.isSystemRoute(toAdd)) continue;

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

	private void processWithAFSMechanism(
		Map<Long, List<Field>> dateAndFieldValuesMapping,
		Map<Long, String> dateAndUserMapping
	) {
		if (dateAndFieldValuesMapping.isEmpty()) return;

		List<Long> timestampDesc = new ArrayList<>(dateAndFieldValuesMapping.keySet());
		Collections.sort(timestampDesc);
		Collections.reverse(timestampDesc);

		// The most recent values:
		Long mostRecent = timestampDesc.isEmpty() ? 0l : timestampDesc.get(0);
		String mostRecentUser = dateAndUserMapping.get(mostRecent);

		// Earliest:
		Long oldest = timestampDesc.isEmpty() ? 0l : timestampDesc.get(timestampDesc.size() - 1);
		String oldestUser = dateAndUserMapping.get(oldest);

		if (mostRecent == null || oldest == null ||
				(!dateAndFieldValuesMapping.containsKey(mostRecent) ||
						!dateAndFieldValuesMapping.containsKey(oldest))) {
			this.getLogger().warn(String.format("We do not have the MostRecent(%s:%s) or Oldest(%s:%s) on map [%s] field values!",
					mostRecent == null ? Globals.NOT_SET : new Date(mostRecent),
					oldest == null ? Globals.NOT_SET : new Date(oldest),
					oldest,
					mostRecent,
					dateAndFieldValuesMapping)
			);
			return;
		}

		this.getLogger().debug(
				String.format("Audit: Comparing oldest [%s:%s] with latest [%s:%s].",
						new Date(oldest), oldestUser, new Date(mostRecent), mostRecentUser));

		this.mapForAuditDisplay(oldest, dateAndUserMapping, dateAndFieldValuesMapping, timestampDesc);
		this.mapForAuditDisplay(mostRecent, dateAndUserMapping, dateAndFieldValuesMapping, timestampDesc);
	}

	private void mapForAuditDisplay(
		long timestamp,
		Map<Long, String> dateAndUserMapping,
		Map<Long, List<Field>> dateAndFieldValuesMapping,
		List<Long> timestampDesc
	) {
		this.getLogger().debug(String.format("Audit: Processing [%s].", timestamp));
		List<Field> fields = dateAndFieldValuesMapping.get(timestamp);
		final String maker = dateAndUserMapping.values().stream()
				.filter(itm -> {
					if (UtilGlobal.isBlank(itm)) return false;
					switch (itm) {
						case EMPTY_FLAG:
						case User.ADMIN_USERNAME:
						case SYSTEM_ROUTE:
							return false;
						default: return true;
					}
				}).findFirst()
				.orElse("-");

		// Remove Fields where user has no Access:
		this.removeFieldsWhereUserHasNoAccess(fields);

		// Remove AFS Fields no wanted at all:
		this.removeUnwantedFields(fields);

		Date timestampDate = new Date(timestamp);
		// Add Each Of the Fields:
		fields.stream()
				.filter(fieldItm -> fieldItm.getFieldValue() != null)
				.forEach(fieldItm -> {
					FlatFieldHistory toAddMod = this.formFieldHistoriesFlat.stream()
							.filter(itm -> itm.getField().getFieldName().equals(fieldItm.getFieldName()))
							.findFirst()
							.orElse(null);
					if (toAddMod == null) {
						toAddMod = new FlatFieldHistory(maker, timestamp, timestampDate, fieldItm, 0);
						this.formFieldHistoriesFlat.add(toAddMod);
					} else this.modifyExisting(
							timestampDesc,
							dateAndFieldValuesMapping,
							maker,
							toAddMod
					);
				});
	}

	private void modifyExisting(
		List<Long> timestampDesc,
		Map<Long, List<Field>> dateAndFieldValuesMapping,
		String maker,
		FlatFieldHistory toModify
	) {
		String fieldName = toModify.getField().getFieldName();

		for (Long atIndex : timestampDesc) {
			List<Field> listOfFields = dateAndFieldValuesMapping.get(atIndex);
			if (listOfFields == null) continue;

			Field matchingField = listOfFields.stream()
					.filter(itm -> itm.getFieldName().equals(fieldName))
					.findFirst()
					.orElse(null);
			if (matchingField == null || matchingField.getFieldValue() == null) continue;

			// Field Matches, now let's ensure the values are not matching.
			if (!toModify.getField().valueEquals(matchingField)) {
				Date modifiedAt = new Date(atIndex);

				toModify.priorFieldModifiedDate = modifiedAt;
				toModify.priorField = new PriorField(matchingField, modifiedAt, maker);
			}
		}
	}

	private void sanitiseDecimalFieldsAllDoubleFormattedCurrency(List<Field> fields) {
		fields.stream()
				.filter(field ->
						(field.getTypeAsEnum() != null && field.getTypeAsEnum() == Field.Type.Decimal) &&
								field.getFieldValueAsNumber() != null)
				.forEach(field -> {
					int currencyFraction = 0;
					if (field.getDecimalMetaFormat() != null &&
							field.getDecimalMetaFormat().getAmountCurrency() != null) {
						currencyFraction = field.getDecimalMetaFormat().getAmountCurrency().getDefaultFractionDigits();
					}

					// Only Reformat Currency Fields:
					if (currencyFraction > 0) {
						//TODO field.setFieldValueAsDouble(this.reformatFieldAsDblCurrency(field.getFieldName(), field.getFieldValue(), currencyFraction));
					}
				});
	}

	private static MathContext MC = DECIMAL64;
	private double reformatFieldAsDblCurrency(
		String fieldName,
		Object fieldVal,
		int currencyFraction
	) {
		if (fieldVal == null) return 0.0;

		final Double fieldDblVal;
		if (fieldVal instanceof Number) {
			fieldDblVal = Double.valueOf(fieldVal.toString());
		} else return 0.0;

		BigDecimal bd = new BigDecimal(fieldDblVal, MC);
		if (bd.doubleValue() <= 0.0d) return 0.0;// We treat negatives as not set.

		if (currencyFraction > 0) {
			//TODO testing for history view:
			double returnVal = new BigDecimal(fieldDblVal)
					.round(MathContext.UNLIMITED)
					.movePointLeft(currencyFraction)
					.doubleValue();
			System.out.println("JasonTest: CurFraction("+fieldName+")[" + fieldVal + " : "+ fieldDblVal + " : "+returnVal+"]");
			return returnVal;
		}

		long returnVal = 0;
		if (bd.scale() > 0) {
			String dblSting = Double.toString(fieldDblVal);
			if (dblSting.toUpperCase().contains("E")) {
				dblSting = Double.toString(new BigDecimal(returnVal, MC)
						.round(MathContext.DECIMAL32)
						.setScale(currencyFraction > 0 ? currencyFraction : 2)
						.doubleValue());
			}
			if (currencyFraction > 0) {
				int indexOfScale = dblSting.indexOf(".");
				int fractions = dblSting.substring(indexOfScale + 1).length();
				for (; fractions < currencyFraction; fractions++) dblSting += "0";
				String txtValToParse = dblSting.replace(".", "");
				try {
					returnVal = new Long(txtValToParse);
				} catch (NumberFormatException nde) {
					nde.printStackTrace();
					return fieldDblVal;
				}
			} else returnVal = new Long(dblSting.replace(".", ""));
		} else if (currencyFraction > 0) {
			String lngSting = Long.toString(bd.longValue());
			for (int index = 0;index < currencyFraction; index++) lngSting += "0";
			returnVal = new Long(lngSting);
		} else returnVal = bd.longValue();

		if (currencyFraction > 0) {
			String returnValTxt = Long.toString(returnVal);
			System.out.println("JasonTest: Converting[" + returnVal + ":"+ returnValTxt + "]");

			int splitIndex = (returnValTxt.length() - currencyFraction);
			if (splitIndex > 0) {
				String leading = returnValTxt.substring(0, splitIndex);
				String trailing = returnValTxt.substring(splitIndex);
				returnValTxt = String.format("%s.%s", leading, trailing);
			}

			double returnValDbl = Double.valueOf(returnValTxt);
			System.out.println("JasonTest: Result[" + returnValDbl + ":"+ returnValTxt + "]");
			return returnValDbl;
		}

		return returnVal;
	}

	private void removeFieldsWhereUserHasNoAccess(List<Field> fields) {
		if (fields == null || fields.isEmpty()) return;

		List<Field> fieldsUserCanView = this.accessBean.getFieldsViewableForFormDef(this.historyForm.getFormType());
		if (fieldsUserCanView == null || fieldsUserCanView.isEmpty()) {
			fields.clear();
			return;
		}

		new ArrayList<>(fields).stream().forEach(param -> {
			Field fieldWithName = fieldsUserCanView.stream()
					.filter(itm -> {
						boolean canView = itm.getFieldName().equals(param.getFieldName());
						String displayVal = null;
						if (!canView && UtilGlobal.isNotBlank(displayVal = itm.getFieldNameDisplayValue())) {
							return displayVal.equals(param.getFieldName());
						}
						return canView;
					})
					.findFirst()
					.orElse(null);
			if (fieldWithName == null) fields.remove(param);
		});
	}

	private void removeUnwantedFields(List<Field> fields) {
		if (fields == null || fields.isEmpty()) return;

		new ArrayList<>(fields).stream().forEach(param -> {
			switch (param.getFieldName()) {
				case "Date Activated":
				case "Date Issued":
				case "Card Number":
				case "Primary Account Number":
					fields.remove(param);
			}
		});
	}

	private void removeUnwantedFields(WebKitFormFieldHistory history) {
		if (history.getFields() == null || history.getPriorFields() == null) return;

		this.removeUnwantedFields(history.getFields());
	}

	private void removeFieldsWhereValuesMatch(WebKitFormFieldHistory history) {
		if (history.getFields() == null || history.getPriorFields() == null) return;

		List<Field> fieldListingNew = new ArrayList<>(history.getFields());
		fieldListingNew.forEach(field -> {
			PriorField priorField = history.getPriorFieldForField(field);
			if (priorField == null) return;

			if (field.valueEquals(priorField) ||
					(field.isFieldValueEmpty() && priorField.isFieldValueEmpty())) history.getFields().remove(field);
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

	private boolean isSystemRoute(WebKitFormFieldHistory history) {
		return SYSTEM_ROUTE.equalsIgnoreCase(history.getUsername());
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

						Field priorFieldDiffVal = fieldsAtDate.stream()
								.filter(fieldItm -> {
									boolean fieldMatch = field.getFieldName().equals(fieldItm.getFieldName());
									if (fieldMatch && field.getFieldValueAsString() != null &&
											fieldItm.getFieldValue() != null) {
										return !field.getFieldValueAsString().equalsIgnoreCase(fieldItm.getFieldValueAsString());
									}
									return fieldMatch;
								})
								.findFirst()
								.orElse(null);
						if (priorFieldDiffVal != null) {
							priorFieldFound.set(new PriorField(priorFieldDiffVal, new Date(timestampItm), usersAtDate.get(timestampItm)));
							priorFieldFound.get().setTypeMetaData(field.getTypeMetaData());
							return;
						}
						
						Field priorField = fieldsAtDate.stream()
								.filter(fieldItm -> field.getFieldName().equals(fieldItm.getFieldName()))
								.findFirst()
								.orElse(null);
						if (priorField != null) {
							priorFieldFound.set(new PriorField(priorField, new Date(timestampItm), usersAtDate.get(timestampItm)));
							priorFieldFound.get().setTypeMetaData(field.getTypeMetaData());
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

		Collections.sort(org, Comparator.comparing(FlatFieldHistory::getTimestampTime));

		return org.stream()
				.filter(itm -> {
					PriorField priorField = itm.getPriorField();
					if (priorField == null || priorField.getFieldValue() == null) return false;
					if (itm.getField() == null || itm.getField().getFieldValue() == null) return false;
					
					return !itm.getField().getFieldValue().equals(priorField.getFieldValue());
				})
				.collect(Collectors.toList());
	}

	private static String[] WHOLE_NR_FIELDS = {
			"Goods Number of Transactions Limit",
			"Cash Number of Transactions Limit",
			"Payment Number Transactions Limit",
			// Monthly:
			"Monthly Cash Number of Transactions Limit",
			"Monthly Goods Number of Transactions Limit",
			"Monthly Payment Number of Transactions Limit",
			// Weekly:
			"Weekly Cash Number of Transactions Limit",
			"Weekly Goods Number of Transactions Limit",
			"Weekly Payment Number of Transactions Limit",
	};

	private static String RETURN_FORMAT_000 = "###,###,###,##0.000";
	private static String RETURN_FORMAT_00 = "###,###,###,##0.00";

	public String formatFieldOfTypeDecimalFormat(Field field) {
		if (field == null || field.getFieldValue() == null) return "-";

		if (field.isAmountMinorWithCurrency()) {
			DecimalFormat df = new DecimalFormat(
					field.getDecimalMetaFormat().getCurrencyDecimalPlaces() > 2 ?
							RETURN_FORMAT_000 : RETURN_FORMAT_00);
			Double dbl = field.getFieldValueAsDouble();
			if (dbl == null) return "-";
			String formatted = df.format(dbl);
			return formatted;
		}
		DecimalFormat df = new DecimalFormat("0");
		Long valLong = field.getFieldValueAsLong();
		if (valLong == null) return "-";

		return df.format(valLong);
	}
}
