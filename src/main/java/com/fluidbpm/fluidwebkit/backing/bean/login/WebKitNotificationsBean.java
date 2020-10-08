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
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.user.UserNotification;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.user.UserNotificationClient;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Bean to handle Fluid notifications.
 * @see UserNotification
 */
@SessionScoped
@Named("webKitNotificationsBean")
public class WebKitNotificationsBean extends ABaseManagedBean {
	public static final int MAX_COUNT_UNREAD = 15;
	protected static final int MAX_COUNT_READ = 60;

	@Getter
	@Setter
	private List<UserNotification> unreadUserNotifications;

	@Getter
	@Setter
	private List<UserNotification> readUserNotifications;

	@Getter
	@Setter
	private NotificationState notificationState = NotificationState.Unknown;

	@Getter
	@Setter
	private long lastNotificationFetch = 0l;

	private SimpleDateFormat dateTimeSDF;

	public static enum NotificationState {
		Unknown,
		AllRead,
		UnreadNotifications,
		UnreadNotificationsFull,
		NoNotifications,
		UnreadNotificationsNowRead
	}

	/**
	 * When the bean is initialized, the pending notifications will be fetched.
	 */
	@PostConstruct
	public void initBean() {
		this.actionUpdateNotifications();
		this.dateTimeSDF = new SimpleDateFormat(this.getDateAndTimeFormat());
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
				this.dateTimeSDF.format(userNotification.getDateCreated()));
	}

	public String getReadDisplayDescriptionForType(UserNotification userNotification) {
		return String.format("<o class=\"fa fa-eye\"></o> %s", this.dateTimeSDF.format(userNotification.getDateRead()));
	}

	/**
	 * Action Event to update notifications.
	 * 
	 * When notifications are to be retrieved.
	 */
	public void actionUpdateNotifications() {
		if (this.getFluidClientDS() == null) {
			this.lastNotificationFetch = System.currentTimeMillis();
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
				if (this.unreadUserNotifications != null && this.unreadUserNotifications.size() > MAX_COUNT_UNREAD) {
					this.unreadUserNotifications = this.unreadUserNotifications.subList(0, MAX_COUNT_UNREAD);
				}
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
			this.raiseError(except);
		} finally {
			this.updateStateBasedOnNotifications();
			this.lastNotificationFetch = System.currentTimeMillis();
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
		try {
			if (this.getUnreadUserNotifications() == null || this.getUnreadUserNotifications().isEmpty()) {
				return;
			}

			final UserNotificationClient userNotificationsClient = this.getFluidClientDS().getUserNotificationClient();
			//Mark the notifications as READ...
			final Date dateRead = new Date(System.currentTimeMillis());
			this.getUnreadUserNotifications().forEach(unreadMsg -> {
				userNotificationsClient.markUserNotificationAsRead(unreadMsg);
				unreadMsg.setDateRead(dateRead);
				if (this.getReadUserNotifications() == null) {
					this.setReadUserNotifications(new ArrayList<>());
				}
				this.getReadUserNotifications().add(unreadMsg);
			});
			this.getUnreadUserNotifications().clear();
			this.getLogger().info("actionMarkUnreadNotificationsAsRead(DONE)");
			this.setNotificationState(NotificationState.UnreadNotificationsNowRead);
		} catch (Exception except) {
			this.raiseError(except);
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
	 * Getter for number of notifications.
	 * @return int count for notifications.
	 */
	public int getTotalNumberOfNotifications() {
		return (this.getNumberOfReadNotifications() + this.getNumberOfUnreadNotifications());
	}

	/**
	 *
	 * @return
	 */
	public boolean isNoUserNotifications() {
		return this.getTotalNumberOfNotifications() < 1;
	}

	/**
	 * Update the current state of notifications.
	 */
	public void updateStateBasedOnNotifications() {
		if (this.isNoUserNotifications()) {
			this.setNotificationState(NotificationState.NoNotifications);
			return;
		}

		if (this.getNumberOfUnreadNotifications() >= MAX_COUNT_UNREAD) {
			this.setNotificationState(NotificationState.UnreadNotificationsFull);
			return;
		}

		if (this.getNumberOfUnreadNotifications() > 0) {
			this.setNotificationState(NotificationState.UnreadNotifications);
			return;
		}

		this.setNotificationState(NotificationState.AllRead);
	}
}
