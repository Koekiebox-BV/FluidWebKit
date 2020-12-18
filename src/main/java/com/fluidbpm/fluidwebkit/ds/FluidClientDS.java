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

package com.fluidbpm.fluidwebkit.ds;

import com.fluidbpm.fluidwebkit.infrastructure.cache.ExitEventForFluidAPI;
import com.fluidbpm.program.api.util.cache.exception.FluidCacheException;
import com.fluidbpm.ws.client.v1.ABaseClientWS;
import com.fluidbpm.ws.client.v1.attachment.AttachmentClient;
import com.fluidbpm.ws.client.v1.config.ConfigurationClient;
import com.fluidbpm.ws.client.v1.flow.FlowClient;
import com.fluidbpm.ws.client.v1.flow.FlowStepClient;
import com.fluidbpm.ws.client.v1.flow.RouteFieldClient;
import com.fluidbpm.ws.client.v1.flowitem.FlowItemClient;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import com.fluidbpm.ws.client.v1.form.FormDefinitionClient;
import com.fluidbpm.ws.client.v1.form.FormFieldClient;
import com.fluidbpm.ws.client.v1.report.ReportSystemClient;
import com.fluidbpm.ws.client.v1.report.ReportUserClient;
import com.fluidbpm.ws.client.v1.sqlutil.wrapper.SQLUtilWebSocketRESTWrapper;
import com.fluidbpm.ws.client.v1.user.PersonalInventoryClient;
import com.fluidbpm.ws.client.v1.user.UserClient;
import com.fluidbpm.ws.client.v1.user.UserNotificationClient;
import com.fluidbpm.ws.client.v1.userquery.UserQueryClient;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 * Data Source for all the Fluid API's.
 */
public class FluidClientDS implements Closeable {
	@Getter
	private String serviceTicket;

	@Getter
	private String endpoint;

	private Cache<String, ABaseClientWS> clientCache;

	public FluidClientDS() {
		super();
		this.clientCache = CacheBuilder.newBuilder()
				.expireAfterAccess(10, TimeUnit.MINUTES)
				.removalListener(new ExitEventForFluidAPI())
				.build();
	}

	public FluidClientDS(String serviceTicket, String endpoint) {
		this();
		this.serviceTicket = serviceTicket;
		this.endpoint = endpoint;
	}

	public UserClient getUserClient() {
		return this.getClientFor(UserClient.class);
	}

	public FlowItemClient getFlowItemClient() {
		return this.getClientFor(FlowItemClient.class);
	}

	public UserQueryClient getUserQueryClient() {
		return this.getClientFor(UserQueryClient.class);
	}

	public FlowStepClient getFlowStepClient() {
		return this.getClientFor(FlowStepClient.class);
	}

	public FlowClient getFlowClient() {
		return this.getClientFor(FlowClient.class);
	}

	public RouteFieldClient getRouteFieldClient() {
		return this.getClientFor(RouteFieldClient.class);
	}

	public FormDefinitionClient getFormDefinitionClient() {
		return this.getClientFor(FormDefinitionClient.class);
	}

	public FormFieldClient getFormFieldClient() {
		return this.getClientFor(FormFieldClient.class);
	}

	public UserNotificationClient getUserNotificationClient() {
		return this.getClientFor(UserNotificationClient.class);
	}

	public ReportUserClient getReportUserClient() {
		return this.getClientFor(ReportUserClient.class);
	}

	public ReportSystemClient getReportSystemClient() {
		return this.getClientFor(ReportSystemClient.class);
	}

	public FormContainerClient getFormContainerClient() {
		return this.getClientFor(FormContainerClient.class);
	}

	public ConfigurationClient getConfigurationClient() {
		return this.getClientFor(ConfigurationClient.class);
	}

	public AttachmentClient getAttachmentClient() {
		return this.getClientFor(AttachmentClient.class);
	}

	public PersonalInventoryClient getPersonalInventoryClient() {
		return this.getClientFor(PersonalInventoryClient.class);
	}

	public SQLUtilWebSocketRESTWrapper getSQLUtilWrapper() {
		return this.getClientFor(SQLUtilWebSocketRESTWrapper.class);
	}

	private <T extends AutoCloseable> T getClientFor(Class<T> clazz) {
		if (clazz == null) return null;

		String keyToUse = clazz.getName();
		ABaseClientWS returnVal = this.clientCache.getIfPresent(keyToUse);
		if (returnVal == null) {
			ABaseClientWS fromKey = this.defaultForClass(clazz);
			if (fromKey == null) throw new FluidCacheException(
					String.format("Unable to create client from '%s'. Please MAP!", keyToUse));

			this.clientCache.put(keyToUse, fromKey);
			return (T)this.clientCache.getIfPresent(keyToUse);
		}
		return (T)returnVal;
	}

	private ABaseClientWS defaultForClass(Class<? extends AutoCloseable> clazz) {
		if (clazz.isAssignableFrom(ConfigurationClient.class)) {
			return new ConfigurationClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(UserClient.class)) {
			return new UserClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(UserNotificationClient.class)) {
			return new UserNotificationClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(FormDefinitionClient.class)) {
			return new FormDefinitionClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(FormFieldClient.class)) {
			return new FormFieldClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(ReportUserClient.class)) {
			return new ReportUserClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(ReportSystemClient.class)) {
			return new ReportSystemClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(UserQueryClient.class)) {
			return new UserQueryClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(FormContainerClient.class)) {
			return new FormContainerClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(PersonalInventoryClient.class)) {
			return new PersonalInventoryClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(AttachmentClient.class)) {
			return new AttachmentClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(FlowClient.class)) {
			return new FlowClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(RouteFieldClient.class)) {
			return new RouteFieldClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(FlowStepClient.class)) {
			return new FlowStepClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(FlowItemClient.class)) {
			return new FlowItemClient(this.endpoint, this.serviceTicket);
		} else if (clazz.isAssignableFrom(SQLUtilWebSocketRESTWrapper.class)) {
			return new SQLUtilWebSocketRESTWrapper(this.endpoint, this.serviceTicket, TimeUnit.SECONDS.toMillis(30));
		}

		return null;
	}

	/**
	 * Releases all the API clients.
	 */
	@Override
	public void close() {
		if (this.clientCache == null || this.clientCache.size() < 1) {
			return;
		}
		this.clientCache.cleanUp();
		this.clientCache.invalidateAll();
	}
}
