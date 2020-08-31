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
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.report.userstats.ViewOpenedAndSentOnEntry;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BubbleChartModel;
import org.primefaces.model.chart.BubbleChartSeries;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 *
 */
@RequestScoped
@Named("webKitUserViewVsItemOpenedBean")
@Getter
@Setter
public class UserViewVsItemOpenedBean extends ABasePerformanceBean {

	//CHARTS...
	//Login vs Logout
	private BubbleChartModel bubbleChartModel;

	private int workItemsOpened;
	private int workItemsOpenedAndSendOn;
	private int workItemsOpenedButNotSendOn;
	private String mostPopularView;
	private String mostProductiveView;

	private int openAndCompletePercentage;

	@Inject
	private PerformanceBean performanceBean;

	@PostConstruct
	public void createUserViewVsItemOpenedModel() {
		List<ViewOpenedAndSentOnEntry> viewOpenedAndSentOnEntries =
				this.performanceBean.getUserStatsReport().getViewOpenedAndSentOnEntries();

		this.workItemsOpenedAndSendOn = 0;
		this.workItemsOpenedButNotSendOn = 0;
		this.workItemsOpened = 0;
		this.openAndCompletePercentage = 0;
		this.mostPopularView = UtilGlobal.EMPTY;
		this.mostProductiveView = UtilGlobal.EMPTY;
		this.bubbleChartModel = new BubbleChartModel();

		if (viewOpenedAndSentOnEntries == null || viewOpenedAndSentOnEntries.isEmpty()) {
			return;
		}

		this.setChartBasics(this.bubbleChartModel);

		this.bubbleChartModel.setTitle("The size of the balloon represents effectiveness.");
		this.bubbleChartModel.getAxis(AxisType.X).setLabel("Number of Items Opened from View");
		this.bubbleChartModel.getAxis(AxisType.X).setMin(0);
		this.bubbleChartModel.setAnimate(true);

		Axis yAxis = this.bubbleChartModel.getAxis(AxisType.Y);
		yAxis.setLabel("Number of View 'Clicks'");
		yAxis.setMin(0);

		String mostPopView = null, mostProdView = null;
		int latestMostPopViewClicks = 0, latestProductive = 0;
		for (ViewOpenedAndSentOnEntry entry : viewOpenedAndSentOnEntries) {
			this.workItemsOpened += entry.getOpenedFromViewCounts();
			this.workItemsOpenedAndSendOn += entry.getSentOn();

			int percentageComplete = entry.getPercentageOfComplete();
			if (entry.getViewClicks() > latestMostPopViewClicks) {
				latestMostPopViewClicks = entry.getViewClicks();
				mostPopView = entry.getViewName();
			}

			if (percentageComplete > latestProductive) {
				latestProductive = percentageComplete;
				mostProdView = entry.getViewName();
			}

			this.bubbleChartModel.add(new BubbleChartSeries(
					entry.getViewName(),
					entry.getOpenedFromViewCounts(),
					entry.getViewClicks(),
					(int)((float)percentageComplete * 0.60)));
		}

		this.workItemsOpenedButNotSendOn = (this.workItemsOpened - this.workItemsOpenedAndSendOn);

		this.openAndCompletePercentage = this.getFloatAsPercentage(
						((float)this.workItemsOpenedAndSendOn / (float)this.workItemsOpened));
		this.mostPopularView = mostPopView == null || mostPopView.isEmpty() ? UtilGlobal.EMPTY : mostPopView;
		this.mostProductiveView = mostPopView == null || mostPopView.isEmpty() ? UtilGlobal.EMPTY : mostProdView;
	}

	/**
	 *
	 * @return
	 */
	public int getOpenAndCompletePercentage() {
		if(this.openAndCompletePercentage < 0) {
			return 0;
		}
		return this.openAndCompletePercentage;
	}

	/**
	 *
	 * @return
	 */
	public String getMostProductiveView() {
		if (this.mostProductiveView == null || this.mostProductiveView.trim().isEmpty()) {
			return UtilGlobal.EMPTY;
		}

		return this.mostProductiveView;
	}
}
