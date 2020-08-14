/*----------------------------------------------------------------------------*
 *
 * Copyright (c) 2018 by Montant Limited. All rights reserved.
 *
 * This software is the confidential and proprietary property of
 * Montant Limited and may not be disclosed, copied or distributed
 * in any form without the express written permission of Montant Limited.
 *----------------------------------------------------------------------------*/

package com.fluidbpm.fluidwebkit.backing.bean.config;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseFormFieldAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.program.api.vo.config.Configuration;
import com.fluidbpm.program.api.vo.config.ConfigurationListing;
import com.fluidbpm.program.api.vo.ws.auth.AppRequestToken;
import com.fluidbpm.ws.client.v1.config.ConfigurationClient;
import com.fluidbpm.ws.client.v1.user.LoginClient;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.List;

/**
 * Bean storing
 */
@Named("webKitFormAccess")
public class WebKitAccessBean extends ABaseFormFieldAccessBean {

	/**
	 * Don't ignore any forms.
	 * @return {@code null}
	 */
	@Override
	protected List<String> getFormDefsToIgnore() {
		return null;
	}
}
