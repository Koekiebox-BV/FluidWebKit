/*----------------------------------------------------------------------------*
 *
 * Copyright (c) 2018 by Montant Limited. All rights reserved.
 *
 * This software is the confidential and proprietary property of
 * Montant Limited and may not be disclosed, copied or distributed
 * in any form without the express written permission of Montant Limited.
 *----------------------------------------------------------------------------*/

package com.fluidbpm.fluidwebkit.backing.bean.workspace;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseFormFieldAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.performance.PerformanceBean;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Bean storing personal inventory related items.
 */
@SessionScoped
@Named("webKitPersonalInvBean")
public class PersonalInventoryBean extends ABaseManagedBean {

	@Inject
	private PerformanceBean performanceBean;

	public int getNumberOfPersonalInventoryItems() {
		return performanceBean.getUserStatsReport().getPiCount();
	}

	public int getNumberOfPersonalInventoryLockedItems() {
		return performanceBean.getUserStatsReport().getPiLockedCount();
	}

}
