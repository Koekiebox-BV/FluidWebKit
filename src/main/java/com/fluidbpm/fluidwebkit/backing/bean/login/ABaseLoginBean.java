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
import com.fluidbpm.fluidwebkit.backing.bean.login.ldap.LDAP;
import com.fluidbpm.fluidwebkit.backing.utility.Globals;
import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.program.api.vo.role.Role;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.user.UserFieldListing;
import com.fluidbpm.program.api.vo.ws.auth.AppRequestToken;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.user.LoginClient;
import com.fluidbpm.ws.client.v1.user.UserClient;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * Bean to handle user authentication.
 * 
 * @author jasonbruwer on 2018-06-03
 * @since 1.0
 */
public abstract class ABaseLoginBean extends ABaseManagedBean {
    private String inputUsername = null;
	private String inputPassword = null;

	/**
	 * action="#{saibDashLoginBean.actionLogin}"
	 *
	 * Log a user into the SAIB Dashboard.
	 *
	 * @return navigation {@code dashboard}
	 */
	public String actionLogin() {

		this.getLogger().debug("Login attempt ["+this.getInputUsername()+"]");

		//Lets confirm username and password is set first...
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
		LoginClient loginClient = new LoginClient(Globals.getConfigURLFromSystemProperty());
		UserClient userClient = null;
		try {
			if (!this.isConfigUserLoginSuccess()) {
				throw new ClientDashboardException(
						"'Config User' not logged in. Please check logs!",
                        ClientDashboardException.ErrorCode.ILLEGAL_STATE);
			}

			//App Request Token ...
			AppRequestToken appReqToken =
					loginClient.login(
							this.getInputUsername(),
							this.getInputPassword());
			user.setServiceTicket(appReqToken.getServiceTicket());
			user.setRoles(Role.convertToObjects(appReqToken.getRoleString()));

			userClient = new UserClient(
					this.getConfigURLFromSystemProperty(),
					appReqToken.getServiceTicket());

			//Get logged in user info...
			User loggedInUserInfo = userClient.getLoggedInUserInformation();

			//Set the User Id...
			user.setDateFormat(loggedInUserInfo.getDateFormat());
			user.setTimeFormat(loggedInUserInfo.getTimeFormat());
			user.setId(loggedInUserInfo.getId());

			try {
				UserFieldListing fieldListing = userClient.getAllUserFieldValuesByUser(loggedInUserInfo);
				if (!fieldListing.isListingEmpty()) {
					user.setUserFields(fieldListing.getListing());
				}
			} catch (FluidClientException fle) {
				this.getLogger().error("Unable to get user fields for "+
						loggedInUserInfo.getUsername()+". "+fle.getMessage(),fle);
			}
		} catch (FluidClientException fce) {
			if (FluidClientException.ErrorCode.LOGIN_FAILURE == fce.getErrorCode()) {
				FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Failed to Login.", "");
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
				return null;
			}

			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Failed to Login. ", fce.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
			return null;
		} catch (ClientDashboardException reportExcept) {
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Failed to Login. ", reportExcept.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, fMsg);

			return null;
		} finally {//Close the login after done...
			loginClient.closeAndClean();

			if (userClient != null) {
				userClient.closeAndClean();
			}
		}

		//Only put the user if success...
		Map<String, Object> sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
		sessionMap.put(SessionVariable.USER, user);

		return Outcome.DASHBOARD;
	}

	/**
	 * action="#{saibDashLoginBean.actionLogin}"
	 *
	 * Log a user into the SAIB Dashboard.
	 *
	 * @return navigation {@code dashboard}
	 */
	public String actionLoginLDAP() {
		try {
			LDAP.authenticateLDAP(
					this.getInputUsername(),
					this.getInputPassword());
			this.getLogger().info("LDAP LOGIN IS GOOD -> !");
		} catch (Exception exception) {
			this.getLogger().error("Unable to login through LDAP. " +
					exception.getMessage(),exception);
			FacesMessage fMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Failed to Login. ", exception.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, fMsg);
		}

		return null;
	}

	/**
	 * Log a user out of the Design Dashboard.
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
     *
     * @return
     */
	public abstract boolean isConfigUserLoginSuccess();

    /**
     *
     * @return
     */
	public abstract String getLogoutOutcomePage();

	/**
	 *
	 * @return
	 */
	public String getInputPassword() {
		return this.inputPassword;
	}

	/**
	 *
	 * @param inputPasswordParam
	 */
	public void setInputPassword(String inputPasswordParam) {
		this.inputPassword = inputPasswordParam;
	}

	/**
	 *
	 * @return
	 */
	public String getInputUsername() {
		return this.inputUsername;
	}

	/**
	 *
	 * @param inputUsernameParam
	 */
	public void setInputUsername(String inputUsernameParam) {
		this.inputUsername = inputUsernameParam;
	}
}
