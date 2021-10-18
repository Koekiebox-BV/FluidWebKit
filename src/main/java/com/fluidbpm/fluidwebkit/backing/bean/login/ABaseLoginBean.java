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

package com.fluidbpm.fluidwebkit.backing.bean.login;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.utility.Globals;
import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.role.Role;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.user.UserFieldListing;
import com.fluidbpm.program.api.vo.ws.auth.AppRequestToken;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.user.LoginClient;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Properties;

/**
 * Bean to handle user authentication for JSF application.
 * 
 * @author jasonbruwer on 2018-06-03
 * @since 1.0
 */
public abstract class ABaseLoginBean extends ABaseManagedBean {
	private String inputUsername = null;
	private String inputPassword = null;

	/**
	 * action="#{loginBean.actionLogin}"
	 *
	 * Log a user into the SAIB Dashboard.
	 *
	 * @return navigation {@code dashboard}.
	 */
	public String actionLogin() {
		Properties existingProps = null;

		this.getLogger().debug("Login attempt ["+ this.getInputUsername()+":"+
				UtilGlobal.getConfigURLFromSystemProperty(existingProps)+"]");

		//Let's confirm username and password is set first...
		if (this.getInputUsername() == null ||
				this.getInputUsername().trim().isEmpty() ||
				this.getInputPassword() == null || this.getInputPassword().trim().isEmpty()){

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Invalid", "Username and Password required.");
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
			return null;
		}

		User user = new User();
		user.setUsername(this.getInputUsername());
		LoginClient loginClient = new LoginClient(UtilGlobal.getConfigURLFromSystemProperty(existingProps));
		try {
			if (!this.isConfigUserLoginSuccess()) {
				throw new ClientDashboardException(
						"'Config User' not logged in. Please check logs!",
						ClientDashboardException.ErrorCode.ILLEGAL_STATE);
			}

			//App Request Token ...
			this.getLogger().debug("Performing login action.");

			final String serviceTicket;
			if (Globals.isConfigBasicAuthFromSystemProperty()) {
				this.getLogger().debug("Login using BASIC authentication.");
				User loginBasicUser = loginClient.loginBasic(
						this.getInputUsername(), this.getInputPassword());
				serviceTicket = loginBasicUser.getServiceTicket();
				user.setRoles(loginBasicUser.getRoles());
			} else {
				this.getLogger().debug("Login using TOKEN authentication.");
				AppRequestToken appReqToken = loginClient.login(
						this.getInputUsername(), this.getInputPassword());
				serviceTicket = appReqToken.getServiceTicket();
				user.setRoles(Role.convertToObjects(appReqToken.getRoleString()));
			}

			//We have a valid login here...
			user.setServiceTicket(serviceTicket);
			this.bindFluidClientDSToSession(serviceTicket);

			//Bind the config for the Fluid DS User...
			this.bindConfigFluidClientDS();

			this.getLogger().debug("User logged in. Retrieving logged in user info.");

			FluidClientDS fcDSConfig = this.getFluidClientDSConfig();

			//Get logged in user info...
			User loggedInUserInfo = fcDSConfig.getUserClient().getLoggedInUserInformation();

			//Set the User Id...
			user.setDateFormat(loggedInUserInfo.getDateFormat());
			user.setTimeFormat(loggedInUserInfo.getTimeFormat());
			user.setId(loggedInUserInfo.getId());

			this.getLogger().debug("Retrieving user field listing.");
			try {
				UserFieldListing fieldListing = fcDSConfig.getUserClient().getAllUserFieldValuesByUser(user);
				if (!fieldListing.isListingEmpty()) {
					user.setUserFields(fieldListing.getListing());
				}
			} catch (FluidClientException fle) {
				this.getLogger().error("Unable to get user fields for "+
						loggedInUserInfo.getUsername()+". "+fle.getMessage(),fle);
			}
		} catch (FluidClientException fce) {
			this.getLogger().error(fce.getMessage(), fce);
			if (FluidClientException.ErrorCode.LOGIN_FAILURE == fce.getErrorCode()) {
				this.getFluidClientDSConfig().getUserClient().incrementInvalidLoginForUser(user);
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to Login.", "");
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
				return null;
			}

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Failed to Login. ", fce.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
			return null;
		} catch (ClientDashboardException dashExcept) {
			this.getLogger().error(dashExcept.getMessage(), dashExcept);

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Failed to Login. ", dashExcept.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, fMsg);

			return null;
		} finally {//Close the login after done...
			loginClient.closeAndClean();
		}

		this.getLogger().debug("Adding user to session.");
		//Only put the user if success...
		Map<String, Object> sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
		sessionMap.put(SessionVariable.USER, user);

		return Outcome.DASHBOARD;
	}

	/**
	 * Log a user out of the Dashboard.
	 *
	 * @return navigation {@code dashboard}
	 */
	public String actionLogout() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
		if (session != null) {
			session.invalidate();
		}

		return this.getLogoutOutcomePage();
	}

	/**
	 * Confirm whether the Fluid config user was logged in successfully.
	 * @return {@code true} if logged in successfully. Otherwise {@code false}.
	 */
	public abstract boolean isConfigUserLoginSuccess();

	/**
	 * The JSF {@code outcome} for when a logout action is performed.
	 * @return JSF outcome.
	 */
	public abstract String getLogoutOutcomePage();

	/**
	 * The password provided by the user.
	 *
	 * @return User password.
	 */
	public String getInputPassword() {
		return this.inputPassword;
	}

	/**
	 * The password provided by the user.
	 *
	 * @param inputPasswordParam The JSF bean mapping for password.
	 */
	public void setInputPassword(String inputPasswordParam) {
		this.inputPassword = inputPasswordParam;
	}

	/**
	 * The username provided by the user.
	 *
	 * @return The JSF bean mapping for username.
	 */
	public String getInputUsername() {
		return this.inputUsername;
	}

	/**
	 * The username provided by the user.
	 * 
	 * @param inputUsernameParam  The JSF bean mapping for username.
	 */
	public void setInputUsername(String inputUsernameParam) {
		this.inputUsername = inputUsernameParam;
	}
}
