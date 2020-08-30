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
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitConfigBean;
import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.user.UserFieldListing;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.user.UserClient;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean to handle password reset related operations.
 */
@RequestScoped
@Named("webKitResetPasswordBean")
public class ResetPasswordBean extends ABaseManagedBean {

	@Getter
	@Setter
	private String inputEmailUsername;

	@Inject
	private WebKitConfigBean webKitConfigBean;

	/**
	 * Send a password reset email.
	 */
	public String actionRequestPasswordResetEmail() {
		try {
			//Mail Transfer...
			FluidClientDS dsConfig = this.getFluidClientDSConfig();
			dsConfig.getConfigurationClient().getSystemMailTransfer();

			//Expiring Link...
			String serverUrl = this.webKitConfigBean.getFluidServerURL();
			if (serverUrl == null || serverUrl.trim().isEmpty()) {
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Unable to Reset Password.",
						"Server URL Config not set. Contact Administrator.");
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
				return null;
			}

			if (this.getInputEmailUsername() == null || this.getInputEmailUsername().trim().isEmpty()) {
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Unable to Reset Password.",
						"Username/Email is mandatory.");
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
				return null;
			}

			//All config is set, now we will attempt to send the reset notification...
			dsConfig.getUserClient().sendPasswordResetRequest(this.getInputEmailUsername());
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_INFO,
					"Success",
					String.format("See Inbox '%s' for reset Email.", this.getInputEmailUsername()));
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
			this.setInputEmailUsername(null);
		} catch (FluidClientException err) {
			this.raiseError(err);
		}
		return null;
	}
}
