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
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.ws.client.v1.user.UserClient;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Bean to handle user profile related operations.
 */
@SessionScoped
@Named("webKitProfileBean")
public class ProfileBean extends ABaseManagedBean {
	/**
	 * Return the currently logged in users Gravator as {@code StreamedContent}
	 *
	 * @return StreamedContent
	 * @see StreamedContent
	 */
	public StreamedContent getUserProfileImageForLoggedInUser() {
		return this.getUserProfileImageForUser(this.getLoggedInUserSafe());
	}

	/**
	 * Return the {@code user} User Gravator as {@code StreamedContent}
	 *
	 * @param user User to retrieve profile image for.
	 * @return StreamedContent
	 * @see StreamedContent
	 * @see User
	 */
	public StreamedContent getUserProfileImageForUser(User user) {
		if (user == null || (user.getId() == null || user.getId() < 1L)) {
			return new DefaultStreamedContent();
		}
		FacesContext context = FacesContext.getCurrentInstance();
		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
			return new DefaultStreamedContent();
		} else {
			User loggedInUser = this.getLoggedInUserSafe();
			if (loggedInUser == null) {
				return new DefaultStreamedContent();
			}

			UserClient userClient = this.getFluidClientDS().getUserClient();
			//Get the bytes by user...
			try {
				final byte[] imageBytes = userClient.getGravatarForUser(user, 45);
				if (imageBytes == null || imageBytes.length < 1) {
					return this.getNoGravatarStreamContent();
				}

				return DefaultStreamedContent.builder()
						.stream(() -> new ByteArrayInputStream(imageBytes))
						.name(String.format("profile_image_%d.jpg", user.getId()))
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
		String path = "/image/no_profile_logo.png";
		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
			if (inputStream == null) {
				throw new IOException("Unable to find '"+ path+"'.");
			}

			return DefaultStreamedContent.builder()
					.stream(() -> inputStream)
					.name("no_profile_logo.png")
					.contentType("image/png")
					.build();
		} catch (IOException ioExcept) {
			FacesMessage fMsg = new FacesMessage(
					FacesMessage.SEVERITY_ERROR, "Failed. " + ioExcept.getMessage(), "");
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
			return null;
		}
	}

	/**
	 *
	 */
	public void actionPrepareLoggedInUserProfileDialog() {

	}
}
