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

package com.fluidbpm.fluidwebkit.infrastructure;

import com.fluidbpm.fluidwebkit.qualifier.ClientDashboardWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI for resources.
 *
 * @author jasonbruwer on 2018-05-31
 * @since v1.0
 */
public class Resources {

	/**
	 * Produce log.
	 *
	 * @param injectionPointParam the injection point
	 *
	 * @return the logger
	 */
	@Produces
	@ClientDashboardWebResource
	public Logger getLogger(final InjectionPoint injectionPointParam) {
	    return LoggerFactory.getLogger(injectionPointParam.getMember().getDeclaringClass().getName());
	}

}
