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
import com.fluidbpm.fluidwebkit.exception.WebSessionExpiredException;
import com.fluidbpm.program.api.vo.user.UserNotification;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.user.UserNotificationClient;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.List;

/**
 * Bean to handle notifications.
 * 
 * @author jasonbruwer on 2018-09-06
 * @since 1.0
 */
public abstract class ABaseNotificationsBean extends ABaseManagedBean {
	private static final int MAX_COUNT_UNREAD = 10;
	private static final int MAX_COUNT_READ = 200;

	private List<UserNotification> unreadUserNotifications;
	private List<UserNotification> readUserNotifications;

	/**
	 *
	 */
	@PostConstruct
	public void initBean() {
		this.actionUpdateNotifications();
	}

	/**
	 * When notifications are to be retrieved.
	 */
	public void actionUpdateNotifications() {
		//Fluid Clients...
		UserNotificationClient userNotificationsClient = null;

		try {
			userNotificationsClient = new UserNotificationClient(
					this.getConfigURLFromSystemProperty(),
					this.getLoggedInUser().getServiceTicket());

		} catch (WebSessionExpiredException wse){
			return;
		}

		this.unreadUserNotifications = null;
		this.readUserNotifications = null;

		try {
			//Read...
			try {
				this.unreadUserNotifications =
						userNotificationsClient.getAllUnReadByLoggedInUser(
								MAX_COUNT_READ,0);
			} catch (FluidClientException fce) {
				if(fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT)
				{
					throw fce;
				}
			}

			//Unread...
			try {
				this.readUserNotifications =
						userNotificationsClient.getAllReadByLoggedInUser(
								MAX_COUNT_UNREAD,0);
			} catch (FluidClientException fce) {
				if(fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT)
				{
					throw fce;
				}
			}
		} catch (Exception except) {
			this.logger.error("Unable to fetch Notifications. "+except.getMessage(),except);

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Unable to fetch Notifications.",
							except.getMessage()));
		} finally {
			userNotificationsClient.closeAndClean();
		}
	}

	/**
	 * Mark the unread notifications as read.
	 */
	public void actionMarkUnreadNotificationsAsRead() {
		//Fluid Clients...
		final UserNotificationClient userNotificationsClient =
				new UserNotificationClient(
						this.getConfigURLFromSystemProperty(),
						this.getLoggedInUser().getServiceTicket());

		try {
			if(this.getUnreadUserNotifications() == null ||
					this.getUnreadUserNotifications().isEmpty()) {
				return;
			}

			//Mark the notifications as READ...
			this.getUnreadUserNotifications().forEach(unreadMsg -> {
				userNotificationsClient.markUserNotificationAsRead(unreadMsg);
			});
		} catch (Exception except) {
			this.logger.error("Unable to mark Notifications as READ. "+except.getMessage(),except);

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Unable to mark Notifications as READ. ",
							except.getMessage()));
		} finally {
			userNotificationsClient.closeAndClean();
		}
	}

	/**
	 *
	 */
	public void actionPrepareToViewReadMesssages(){

	}

	/**
	 *
	 * @return
	 */
	public int getNumberOfUnreadNotifications() {
		return (this.unreadUserNotifications == null ? 0:
						this.unreadUserNotifications.size());
	}

	/**
	 *
	 * @return
	 */
	public int getNumberOfReadNotifications() {

		return (this.readUserNotifications == null ? 0:
				this.readUserNotifications.size());
	}

	/**
	 *
	 * @return
	 */
	public List<UserNotification> getUnreadUserNotifications() {
		return this.unreadUserNotifications;
	}

	/**
	 *
	 * @param unreadUserNotificationsParam
	 */
	public void setUnreadUserNotifications(List<UserNotification> unreadUserNotificationsParam) {
		this.unreadUserNotifications = unreadUserNotificationsParam;
	}

	/**
	 *
	 * @return
	 */
	public List<UserNotification> getReadUserNotifications() {
		return this.readUserNotifications;
	}

	/**
	 *
	 * @param readUserNotificationsParam
	 */
	public void setReadUserNotifications(List<UserNotification> readUserNotificationsParam) {
		this.readUserNotifications = readUserNotificationsParam;
	}
}
