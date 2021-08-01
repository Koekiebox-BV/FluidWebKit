/*
 * Koekiebox CONFIDENTIAL
 *
 * [2012] - [2017] Koekiebox (Pty) Ltd
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

package com.fluidbpm.fluidwebkit.backing.bean.login;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.utility.DateTimeUtil;
import com.fluidbpm.fluidwebkit.backing.utility.TimeZoneUtil;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.user.UserFieldListing;
import com.fluidbpm.ws.client.v1.user.UserClient;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bean to handle user profile related operations.
 */
@SessionScoped
@Named("webKitProfileBean")
public class ProfileBean extends ABaseManagedBean {
	private static final List<String> USER_PROF_IGNORE = new ArrayList<>();
	static {
		USER_PROF_IGNORE.add("Username");
		USER_PROF_IGNORE.add("Roles");
		USER_PROF_IGNORE.add("Email Addresses");
		USER_PROF_IGNORE.add("Date Created");
		USER_PROF_IGNORE.add("Date Last Updated");
	}

	@Getter
	@Setter
	private String inputEmailToAdd;

	@Getter
	@Setter
	private String inputPasswordCurrent;

	@Getter
	@Setter
	private String inputPasswordA;

	@Getter
	@Setter
	private String inputPasswordB;

	/**
	 * Return the currently logged in users Gravator as {@code StreamedContent}
	 *
	 * @return StreamedContent
	 * @see StreamedContent
	 */
	public StreamedContent getUserProfileImageForLoggedInUser() {
		return this.getUserProfileImageForUser(this.getLoggedInUserSafe(), 45);
	}

	/**
	 * Return the currently logged in users Gravator as {@code StreamedContent}
	 *
	 * @return StreamedContent
	 * @see StreamedContent
	 */
	public StreamedContent getUserProfileImageForLoggedInUserLarger() {
		return this.getUserProfileImageForUser(this.getLoggedInUserSafe(), 250);
	}

	/**
	 * Return the {@code user} User Gravator as {@code StreamedContent}
	 *
	 * @param user User to retrieve profile image for.
	 * @param size The width.
	 * @return StreamedContent
	 * @see StreamedContent
	 * @see User
	 */
	public StreamedContent getUserProfileImageForUser(User user, int size) {
		if (user == null || (user.getId() == null || user.getId() < 1L)) return new DefaultStreamedContent();

		FacesContext context = FacesContext.getCurrentInstance();
		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
			return new DefaultStreamedContent();
		} else {
			User loggedInUser = this.getLoggedInUserSafe();
			if (loggedInUser == null) return new DefaultStreamedContent();

			UserClient userClient = this.getFluidClientDS().getUserClient();
			//Get the bytes by user...
			try {
				final byte[] imageBytes = userClient.getGravatarForUser(user, size);
				if (imageBytes == null || imageBytes.length < 1) return this.getNoGravatarStreamContent();

				return DefaultStreamedContent.builder()
						.stream(() -> new ByteArrayInputStream(imageBytes))
						.name(String.format("profile_image_%d_%d.jpg", user.getId(), size))
						.contentType("image/jpeg")
						.contentLength(imageBytes.length)
						.build();
			} catch (Exception unable) {
				this.getLogger().info("Unable to get Gravatar: "+unable.getMessage());
				return this.getNoGravatarStreamContent();
			}
		}
	}

	/**
	 * {@code DefaultStreamedContent} from local path {@code "/image/no_profile_logo.png"}.
	 *
	 * @return new instance of {@code DefaultStreamedContent}.
	 */
	protected DefaultStreamedContent getNoGravatarStreamContent() {
		String path = "/image/no_profile_logo.svg";
		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
			if (inputStream == null) throw new IOException("Unable to find '"+ path+"'.");

			return DefaultStreamedContent.builder()
					.stream(() -> inputStream)
					.name(String.format("no_profile_logo_%s.svg", UUID.randomUUID().toString()))
					.contentType("image/svg+xml")
					.build();
		} catch (IOException ioExcept) {
			this.raiseError(ioExcept);
			return null;
		}
	}

	/**
	 * Prepare to display profile for user.
	 */
	public void actionPrepareLoggedInUserProfileDialog() {
		try {
			final UserClient userClient = this.getFluidClientDS().getUserClient();

			//User Fields...
			User userFromWS = userClient.getUserById(this.getLoggedInUser().getId());
			UserFieldListing ufList = userClient.getAllUserFieldValuesByUser(userFromWS);
			userFromWS.setUserFields(ufList.isListingEmpty() ? new ArrayList(): ufList.getListing());

			//Update the logged in user fields...
			this.getLoggedInUser().setUserFields(userFromWS.getUserFields());
			this.setDialogHeaderTitle(String.format("Profile - %s", this.getLoggedInUserUsername()));
		} catch (Exception err) {
			this.raiseError(err);
		}
	}

	/**
	 * Change the password for the logged in user.
	 */
	public void actionChangePassword() {
		try {
			final UserClient userClient = this.getFluidClientDS().getUserClient();
			userClient.changePasswordForLoggedInUser(
				this.getInputPasswordCurrent(),
				this.getInputPasswordA(),
				this.getInputPasswordB()
			);
			this.executeJavaScript("PF('varDialogResetPassword').hide()");
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success", "Password Changed.");
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	/**
	 * Prepare to change password for user.
	 */
	public void actionPrepareLoggedInUserChangePasswordDialog() {
		this.setDialogHeaderTitle(String.format("Change Password - %s", this.getLoggedInUserUsername()));
		this.setInputPasswordCurrent(null);
		this.setInputPasswordA(null);
		this.setInputPasswordB(null);
	}

	public List<Field> getLoggedInUserFieldsProfile() {
		List<Field> allUserFields = this.getLoggedInUserSafe().getUserFields();
		if (allUserFields == null) return new ArrayList<>();
		List<Field> returnVal = allUserFields.stream()
				.filter(itm -> !USER_PROF_IGNORE.contains(itm.getFieldName()))
				.collect(Collectors.toList());
		Collections.sort(returnVal, Comparator.comparing(Field::getFieldName));
		return returnVal;
	}

	public List<SelectItem> getAvailableTimeZonesAsSelectItems() {
		return TimeZoneUtil.getAvailableTimeZonesAsSelectItems();
	}

	public List<SelectItem> getAvailableDateFormatsAsSelectItems() {
		return DateTimeUtil.getAvailableDateFormatsAsSelectItems();
	}

	public List<SelectItem> getAvailableTimeFormatsAsSelectItems() {
		return DateTimeUtil.getAvailableTimeFormatsAsSelectItems();
	}

	public int getLoggedInUserFieldsProfileSize() {
		return this.getLoggedInUserFieldsProfile() == null ? 0 :
				this.getLoggedInUserFieldsProfile().size();
	}
}
