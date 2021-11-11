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

package com.fluidbpm.fluidwebkit.infrastructure.cache;

import com.fluidbpm.program.api.util.cache.exception.FluidCacheException;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * SQL util event when removing an entry from the cache util.
 *
 * @since 1.1
 */
public class ExitEventForFluidAPI implements RemovalListener {
	
	@Override
	public void onRemoval(RemovalNotification notificationParam) {
		Object oldVal = notificationParam.getValue();
		if (oldVal instanceof AutoCloseable) {
			AutoCloseable casted = (AutoCloseable)oldVal;
			try {
				casted.close();
			} catch (Exception io) {
				throw new FluidCacheException("Unable to close and cleanup '"+oldVal.getClass()+"'. "+io.getMessage(), io);
			}
		} else {
			throw new FluidCacheException("Unable to process '"+oldVal.getClass()+"'.");
		}
	}
}
