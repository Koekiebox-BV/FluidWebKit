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

package com.fluidbpm.fluidwebkit.backing.bean.som;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.field.MultiChoice;
import com.fluidbpm.program.api.vo.user.User;
import lombok.Getter;
import lombok.Setter;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.List;


/**
 * Bean dealing with global issuer.
 */
@SessionScoped
@Named("globalIssuerBean")
public class GlobalIssuerBean extends ABaseManagedBean {

	@Inject
	private WebKitAccessBean accessBean;

	@Getter
	@Setter
	private List<SelectItem> issuers;

	@Getter
	@Setter
	private String activeIssuer;

	private static final String ISSUER = "Issuer";
	private static final String USER_MAKER = "User Maker";
	private static final String USER_CHECKER = "User Checker";
	private static final String FEEDBACK = "Feedback";
	private static final String APPROVAL_STATE = "Approval State";
	private static final String REASON_RE_ISSUE = "Reason For ReIssue";

	private static final String EXP_DATE_RE_ISSUE = "Expiry Date ReIssue";
	public static final String CARD_RENEWAL_MODE = "Card Renewal Mode";// Generate PAN | Same PAN
	public static final String CARD_STATUS_OVERRIDE = "Card Status Override";
	public static final String CARD_PROGRAM_NEW_CARD = "Card Program New Card";

	private static final List<String> IGNORED_FIELDS = new ArrayList<>();
	static {
		IGNORED_FIELDS.add(ISSUER);
		IGNORED_FIELDS.add(USER_MAKER);
		IGNORED_FIELDS.add(USER_CHECKER);
		IGNORED_FIELDS.add(APPROVAL_STATE);
		IGNORED_FIELDS.add(FEEDBACK);
		IGNORED_FIELDS.add(REASON_RE_ISSUE);
		IGNORED_FIELDS.add(EXP_DATE_RE_ISSUE);
		IGNORED_FIELDS.add(CARD_RENEWAL_MODE);
		IGNORED_FIELDS.add(CARD_STATUS_OVERRIDE);
		IGNORED_FIELDS.add(CARD_PROGRAM_NEW_CARD);
	}

	@PostConstruct
	public void actionPopulate() {
		this.issuers = new ArrayList<>();

		User loggedIn = this.getLoggedInUserSafe();
		if (loggedIn == null || loggedIn.getUserFields() == null) return;

		Field multiChoiceUserIssuers = loggedIn.getUserFields().stream()
				.filter(itm -> ISSUER.equals(itm.getFieldName()))
				.findFirst()
				.orElse(null);
		if (multiChoiceUserIssuers == null || multiChoiceUserIssuers.getFieldValueAsMultiChoice() == null) {
			getLogger().info("No Issuer field from User Fields for issuer ("+loggedIn.getUsername()+").");
			return;
		}

		List<String> userSelected = multiChoiceUserIssuers.getFieldValueAsMultiChoice().getSelectedMultiChoices();
		if (userSelected == null || userSelected.isEmpty()) {
			getLogger().error("No Issuer field Multi-Choices from User Fields for issuer.", new IllegalStateException());
			return;
		}

		Field issuerFormField = this.accessBean.getFieldBy(ISSUER);
		MultiChoice formIssuers = null;
		if (issuerFormField == null || (formIssuers = issuerFormField.getFieldValueAsMultiChoice()) == null) {
			getLogger().error("No Issuer field access for Form and user '"+
					this.getLoggedInUserSafe().getUsername()+"'.", new IllegalStateException());
			return;
		}

		List<String> availableOnForm = formIssuers.getAvailableMultiChoices();
		if (availableOnForm == null || availableOnForm.isEmpty()) return;

		availableOnForm.stream().forEach(formItem -> {
			userSelected.stream()
					.filter(itm -> itm.equalsIgnoreCase(formItem))
					.findFirst()
					.ifPresent(itm -> {
						this.issuers.add(new SelectItem(formItem, formItem));
					});
		});

		if (!this.issuers.isEmpty()) this.activeIssuer = this.issuers.get(0).getLabel();
	}

	public boolean getUserHasAccessToMultipleIssuers() {
		return (this.issuers.size() > 0);
	}

	public boolean doesFieldPassSOMCriteria(Field field) {
		if (field == null) return false;

		switch (field.getFieldType()) {
			case "Label":
			case "Table": return false;
		}

		String fieldName = field.getFieldName();
		if (fieldName.contains("Lookup")) return false;
		if (IGNORED_FIELDS.contains(fieldName)) return false;

		return true;
	}
}
