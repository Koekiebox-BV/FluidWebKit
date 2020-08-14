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

package com.fluidbpm.fluidwebkit.infrastructure;

import com.fluidbpm.fluidwebkit.ds.FluidClientDS;
import com.fluidbpm.fluidwebkit.infrastructure.cache.ExitEventForFluidAPI;
import com.fluidbpm.fluidwebkit.qualifier.WebKitResource;
import com.fluidbpm.program.api.util.sql.ABaseSQLUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import javax.enterprise.inject.Produces;
import java.util.concurrent.TimeUnit;

/**
 * CDI for resources.
 */
public class Resources {

	private static final Cache<String, FluidClientDS> cacheFluidDS = CacheBuilder.newBuilder()
			.expireAfterAccess(24, TimeUnit.HOURS)
			.removalListener(new ExitEventForFluidAPI())
			.build();

	@Produces
	@WebKitResource
	public Cache<String, FluidClientDS> getFluidClientDSCache() {
		return Resources.cacheFluidDS;
	}




}
