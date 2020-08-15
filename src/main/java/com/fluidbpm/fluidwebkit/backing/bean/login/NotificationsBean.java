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
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.user.UserNotification;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.user.UserNotificationClient;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Bean to handle Fluid notifications.
 * @see UserNotification
 */
@SessionScoped
@Named("webKitNotificationsBean")
public class NotificationsBean extends ABaseManagedBean {
	protected static final int MAX_COUNT_UNREAD = 10;
	protected static final int MAX_COUNT_READ = 200;

	private List<UserNotification> unreadUserNotifications;
	private List<UserNotification> readUserNotifications;

	private SimpleDateFormat dateSDF;

	/**
	 * When the bean is initialized, the pending notifications will be fetched.
	 */
	@PostConstruct
	public void initBean() {
		this.actionUpdateNotifications();
		this.dateSDF = new SimpleDateFormat(this.getDateFormat());
	}

	public String getDescriptionForType(int notiType) {
		switch (notiType) {
			case UserNotification.UserNotificationType.GLOBAL: return "Global";
			case UserNotification.UserNotificationType.EMAIL_PROCESSED: return "Email Processed";
			case UserNotification.UserNotificationType.COLLEAGUE_COLLABORATION: return "Collaboration";
			case UserNotification.UserNotificationType.VERSION_RELEASE: return "Version Release";
			case UserNotification.UserNotificationType.SYSTEM_ADMINISTRATIVE_CHANGE: return "Admin Change";
			case UserNotification.UserNotificationType.CRITICAL: return "Critical";
			default: return UtilGlobal.EMPTY;
		}
	}

	public String getPFIconForType(int notiType) {
		switch (notiType) {
			case UserNotification.UserNotificationType.GLOBAL: return "pi-globe";
			case UserNotification.UserNotificationType.EMAIL_PROCESSED: return "pi-cog";
			case UserNotification.UserNotificationType.COLLEAGUE_COLLABORATION: return "pi-users";
			case UserNotification.UserNotificationType.VERSION_RELEASE: return "pi-github";
			case UserNotification.UserNotificationType.SYSTEM_ADMINISTRATIVE_CHANGE: return "pi-id-card";
			case UserNotification.UserNotificationType.CRITICAL: return "pi-exclamation-circle";
			default: return UtilGlobal.EMPTY;
		}
	}

	public String getUnreadDisplayDescriptionForType(UserNotification userNotification) {
		return String.format("<strong>%s</strong> - %s",
				this.getDescriptionForType(userNotification.getUserNotificationType()),
				this.dateSDF.format(userNotification.getDateCreated()));
	}

	/**
	 * Action Event to update notifications.
	 * 
	 * When notifications are to be retrieved.
	 */
	public void actionUpdateNotifications() {
		if (this.getFluidClientDS() == null) {
			return;
		}

		//Fluid Clients...
		UserNotificationClient userNotificationsClient =
				this.getFluidClientDS().getUserNotificationClient();

		this.unreadUserNotifications = null;
		this.readUserNotifications = null;

		try {
			//Read...
			try {
				this.unreadUserNotifications =
						userNotificationsClient.getAllUnReadByLoggedInUser(MAX_COUNT_READ,0);
			} catch (FluidClientException fce) {
				if (fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT) {
					throw fce;
				}
			}

			//Unread...
			try {
				this.readUserNotifications =
						userNotificationsClient.getAllReadByLoggedInUser(MAX_COUNT_UNREAD,0);
			} catch (FluidClientException fce) {
				if (fce.getErrorCode() != FluidClientException.ErrorCode.NO_RESULT) {
					throw fce;
				}
			}
		} catch (Exception except) {
			this.getLogger().error("Unable to fetch Notifications. "+except.getMessage(),except);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Unable to fetch Notifications.",
							except.getMessage()));
		}
	}

	/**
	 * Action to mark the unread notifications as read.
	 */
	public void actionMarkUnreadNotificationsAsRead() {
		if (this.getFluidClientDS() == null) {
			return;
		}

		//Fluid Clients...
		final UserNotificationClient userNotificationsClient = this.getFluidClientDS().getUserNotificationClient();
		try {
			if (this.getUnreadUserNotifications() == null || this.getUnreadUserNotifications().isEmpty()) {
				return;
			}

			//Mark the notifications as READ...
			this.getUnreadUserNotifications().forEach(unreadMsg -> {
				userNotificationsClient.markUserNotificationAsRead(unreadMsg);
			});
		} catch (Exception except) {
			this.getLogger().error("Unable to mark Notifications as READ. "+except.getMessage(),except);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Unable to mark Notifications as READ. ",
							except.getMessage()));
		}
	}

	/**
	 * Action to prepare to view messages.
	 */
	public void actionPrepareToViewReadMessages() {
		//Do nothing...
	}

	/**
	 * Getter for number of unread notifications.
	 *
	 * @return int count for unread notifications.
	 */
	public int getNumberOfUnreadNotifications() {
		return (this.unreadUserNotifications == null ? 0:
						this.unreadUserNotifications.size());
	}

	/**
	 * Getter for number of read notifications.
	 *
	 * @return int count for read notifications.
	 */
	public int getNumberOfReadNotifications() {
		return (this.readUserNotifications == null ? 0:
				this.readUserNotifications.size());
	}

	/**
	 *
	 * @return
	 */
	public boolean isNoUserNotifications() {
		return this.getNumberOfReadNotifications() < 1 &&
				this.getNumberOfUnreadNotifications() < 1;
	}

	/**
	 * Getter for unread notifications.
	 *
	 * @return Unread user notifications.
	 *
	 * @see UserNotification
	 */
	public List<UserNotification> getUnreadUserNotifications() {
		return this.unreadUserNotifications;
	}

	/**
	 * Setter for unread notifications.
	 *
	 * @param unreadUserNotificationsParam Unread user notifications.
	 *
	 * @see UserNotification
	 */
	public void setUnreadUserNotifications(List<UserNotification> unreadUserNotificationsParam) {
		this.unreadUserNotifications = unreadUserNotificationsParam;
	}

	/**
	 * Getter for read notifications.
	 *
	 * @return Read user notifications.
	 */
	public List<UserNotification> getReadUserNotifications() {
		return this.readUserNotifications;
	}

	/**
	 * Setter for read notifications.
	 *
	 * @param readUserNotificationsParam Read user notifications.
	 */
	public void setReadUserNotifications(List<UserNotification> readUserNotificationsParam) {
		this.readUserNotifications = readUserNotificationsParam;
	}
}
