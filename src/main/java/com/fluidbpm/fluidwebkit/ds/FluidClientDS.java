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

import com.fluidbpm.program.api.util.cache.exception.FluidCacheException;
import com.fluidbpm.ws.client.v1.ABaseClientWS;
import com.fluidbpm.ws.client.v1.config.ConfigurationClient;
import com.fluidbpm.ws.client.v1.flowitem.FlowItemClient;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;
import com.fluidbpm.ws.client.v1.form.FormDefinitionClient;
import com.fluidbpm.ws.client.v1.sqlutil.wrapper.SQLUtilWebSocketRESTWrapper;
import com.fluidbpm.ws.client.v1.user.PersonalInventoryClient;
import com.fluidbpm.ws.client.v1.user.UserClient;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Source for all the Fluid API's.
 */
public class FluidClientDS implements Closeable {
	private String serviceTicket;
	private String endpoint;
	private Map<String, ABaseClientWS> clientMap = new ConcurrentHashMap<>();

	public FluidClientDS(String serviceTicket, String endpoint) {
		this.serviceTicket = serviceTicket;
		this.endpoint = endpoint;
	}

	public UserClient getUserClient() {
		return this.getClientFor(UserClient.class);
	}

	public FlowItemClient getFlowItemClient() {
		return this.getClientFor(FlowItemClient.class);
	}

	public FormDefinitionClient getFormDefinitionClient() {
		return this.getClientFor(FormDefinitionClient.class);
	}

	public FormContainerClient getFormContainerClient() {
		return this.getClientFor(FormContainerClient.class);
	}

	public ConfigurationClient getConfigurationClient() {
		return this.getClientFor(ConfigurationClient.class);
	}

	public PersonalInventoryClient getPersonalInventoryClient() {
		return this.getClientFor(PersonalInventoryClient.class);
	}

	public SQLUtilWebSocketRESTWrapper getSQLUtilWrapper() {
		return this.getClientFor(SQLUtilWebSocketRESTWrapper.class);
	}

	private <T extends AutoCloseable> T getClientFor(Class<T> clazz) {
		if (clazz == null) {
			return null;
		}
		String keyToUse = clazz.getName();
		ABaseClientWS returnVal = this.clientMap.getOrDefault(keyToUse, null);
		if (returnVal == null) {
			ABaseClientWS fromKey = this.defaultForClass(clazz);
			if (fromKey == null) {
				throw new FluidCacheException(String.format("Unable to create client from '%s'.", keyToUse));
			}
			this.clientMap.put(keyToUse, fromKey);
			returnVal = fromKey;
		}
		return (T)returnVal;
	}

	private ABaseClientWS defaultForClass(Class<? extends AutoCloseable> clazz) {
		if (clazz.isAssignableFrom(ConfigurationClient.class)) {
			return new ConfigurationClient(this.endpoint, serviceTicket);
		} else if (clazz.isAssignableFrom(UserClient.class)) {
			return new UserClient(this.endpoint, serviceTicket);
		}

		return null;
	}

	/**
	 * Releases all the API clients.
	 */
	@Override
	public void close() {
		if (this.clientMap.isEmpty()) {
			return;
		}

		this.clientMap.values().forEach(client -> {
			client.close();
		});
	}
}
