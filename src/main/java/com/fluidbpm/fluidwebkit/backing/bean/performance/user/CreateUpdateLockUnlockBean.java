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

package com.fluidbpm.fluidwebkit.backing.bean.performance.user;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.performance.ABasePerformanceBean;
import com.fluidbpm.fluidwebkit.backing.bean.performance.PerformanceBean;
import com.fluidbpm.program.api.vo.report.userstats.CreateUpdateLockUnlockEntry;
import com.fluidbpm.program.api.vo.report.userstats.FormContainerTypeStats;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.chart.*;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SessionScoped
@Named("webKitCreateUpdateLockUnlockBean")
@Getter
@Setter
public class CreateUpdateLockUnlockBean extends ABasePerformanceBean {

	//The Counts...
	private int createdCount;
	private int updatedCount;
	private int lockedCount;
	private int unLockedCount;

	//Duplicates...
	private int duplicatedLocksCount;
	private int duplicatedUnLocksCount;
	private int duplicatedUpdatesCount;

	@Inject
	private PerformanceBean performanceBean;

	@PostConstruct
	public void createCreateUpdateLockUnlockModel() {
		List<CreateUpdateLockUnlockEntry> createUpdateLockUnlockEntries =
				this.performanceBean.getUserStatsReport().getCreateUpdateLockUnlockEntries();

		this.createdCount = 0;
		this.updatedCount = 0;
		this.lockedCount = 0;
		this.unLockedCount = 0;
		this.duplicatedLocksCount = 0;
		this.duplicatedUnLocksCount = 0;
		this.duplicatedUpdatesCount = 0;

		if (createUpdateLockUnlockEntries == null || createUpdateLockUnlockEntries.isEmpty()) {
			return;
		}

		AtomicInteger
				aiCreatedCount = new AtomicInteger(0),
				aiUpdatedCount = new AtomicInteger(0),
				aiLockedCount = new AtomicInteger(0),
				aiUnLockedCount = new AtomicInteger(0),
				aiDuplicatedLocksCount = new AtomicInteger(0),
				aiDuplicatedUnLocksCount = new AtomicInteger(0),
				aiDuplicatedUpdatesCount = new AtomicInteger(0)
		;

		createUpdateLockUnlockEntries.stream()
				.filter(itm -> itm.getFormContainerTypeStats() != null && !itm.getFormContainerTypeStats().isEmpty())
				.map(itm -> itm.getFormContainerTypeStats())
				.flatMap(List::stream)
				.forEach(itm -> {
					aiCreatedCount.getAndAdd(itm.getCountCreate());
					aiUpdatedCount.getAndAdd(itm.getCountUpdate());
					aiLockedCount.getAndAdd(itm.getCountLock());
					aiUnLockedCount.getAndAdd(itm.getCountUnlock());
					aiDuplicatedLocksCount.getAndAdd(itm.getDuplicateLocks());
					aiDuplicatedUnLocksCount.getAndAdd(itm.getDuplicateUnLocks());
					aiDuplicatedUpdatesCount.getAndAdd(itm.getDuplicateUpdates());
				});
		this.createdCount = aiCreatedCount.get();
		this.updatedCount = aiUpdatedCount.get();
		this.lockedCount = aiLockedCount.get();
		this.unLockedCount = aiUnLockedCount.get();
		this.duplicatedLocksCount = aiDuplicatedLocksCount.get();
		this.duplicatedUnLocksCount = aiDuplicatedUnLocksCount.get();
		this.duplicatedUpdatesCount = aiDuplicatedUpdatesCount.get();
	}
}
