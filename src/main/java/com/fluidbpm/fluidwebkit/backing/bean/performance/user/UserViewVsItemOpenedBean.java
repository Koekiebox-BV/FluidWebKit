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

import com.fluidbpm.fluidwebkit.backing.bean.performance.ABasePerformanceBean;
import com.fluidbpm.fluidwebkit.backing.bean.performance.PerformanceBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.report.userstats.ViewOpenedAndSentOnEntry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import software.xdev.chartjs.model.charts.BubbleChart;
import software.xdev.chartjs.model.datapoint.BubbleDataPoint;
import software.xdev.chartjs.model.dataset.BubbleDataset;

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
	// Keep property name for XHTML compatibility; type migrated to XDEV BubbleChart
	private BubbleChart bubbleChartModel;

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
		this.bubbleChartModel = new BubbleChart();

		if (viewOpenedAndSentOnEntries == null || viewOpenedAndSentOnEntries.isEmpty()) {
			return;
		}

		// No-op in new charts; keep call for backward compatibility
		this.setChartBasics(this.bubbleChartModel);

		// Title
		this.bubbleChartModel.getOptions().getPlugins().getTitle().setDisplay(true);
		this.bubbleChartModel.getOptions().getPlugins().getTitle().setText("The size of the balloon represents effectiveness.");

		// Axes (optional). Defaults are fine with PrimeFaces 15; explicit axis API differs across versions.
		// If axis titles are required, configure them in the XHTML via ChartJS options JSON.

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

			// Create one dataset per point to keep tooltip label as the view name
			BubbleDataset dataset = new BubbleDataset();
			dataset.setLabel(entry.getViewName());
			double radius = ((double)percentageComplete) * 0.60d;
			dataset.addData(new BubbleDataPoint(
					(double)entry.getOpenedFromViewCounts(),
					(double)entry.getViewClicks(),
					radius));
			this.bubbleChartModel.getData().addDataset(dataset);
		}

		this.workItemsOpenedButNotSendOn = (this.workItemsOpened - this.workItemsOpenedAndSendOn);

		this.openAndCompletePercentage = this.getFloatAsPercentage(
					((float)this.workItemsOpenedAndSendOn / (float)this.workItemsOpened));
		this.mostPopularView = mostPopView == null || mostPopView.isEmpty() ? UtilGlobal.EMPTY : mostPopView;
		this.mostProductiveView = mostPopView == null || mostPopView.isEmpty() ? UtilGlobal.EMPTY : mostProdView;
	}

	public int getOpenAndCompletePercentage() {
		if(this.openAndCompletePercentage < 0) return 0;
		return this.openAndCompletePercentage;
	}

	public String getMostProductiveView() {
		if (this.mostProductiveView == null || this.mostProductiveView.trim().isEmpty()) return UtilGlobal.EMPTY;
		return this.mostProductiveView;
	}

	public boolean isBubbleChartModelEmpty() {
		return this.bubbleChartModel == null
				|| this.bubbleChartModel.getData() == null
				|| this.bubbleChartModel.getData().getDatasets() == null
				|| this.bubbleChartModel.getData().getDatasets().isEmpty();
	}
}
