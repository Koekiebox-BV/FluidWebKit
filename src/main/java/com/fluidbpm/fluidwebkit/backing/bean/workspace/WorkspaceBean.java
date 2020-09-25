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

package com.fluidbpm.fluidwebkit.backing.bean.workspace;

import com.fluidbpm.fluidwebkit.backing.bean.performance.PerformanceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.jv.ContentViewJV;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.jv.JobViewItemVO;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.menu.MenuBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.pi.ContentViewPI;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.pi.PersonalInventoryItemVO;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.user.PersonalInventoryClient;
import org.primefaces.PrimeFaces;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

/**
 * Bean storing personal inventory related items.
 */
@SessionScoped
@Named("webKitWorkspaceBean")
public class WorkspaceBean extends ABaseWorkspaceBean<JobViewItemVO, ContentViewJV> {

	public static final String TGM_TABLE_PER_VIEW_SECTION_FORMAT = "%s - %s";
	public static final String TGM_TABLE_PER_VIEW_SECTION_DEL = " - ";

	@Override
	public void actionOpenFormForEditingFromWorkspace(
		JobView fromView,
		Long formIdToUpdateParam
	) {
		//Do nothing...
	}

	@Override
	protected ContentViewJV actionOpenMainPage(
		WebKitViewGroup wkGroup,
		WebKitViewSub wkSub
	) {
		try {
			if (this.getFluidClientDS() == null) {
				return null;
			}

			final String tgm = wkGroup.getTableGenerateMode();
			if (tgm == null) {
				throw new FluidClientException(
						"Unable to determine outcome for TGM not being set.",
						FluidClientException.ErrorCode.FIELD_VALIDATE);
			}
			List<WebKitViewSub> subs = wkGroup.getWebKitViewSubs();
			Collections.sort(subs, Comparator.comparing(WebKitViewSub::getSubOrder));
			List<String> sections = new ArrayList<>();
			if (wkGroup.isTGMCombined()) {
				sections.add(wkGroup.getJobViewGroupName());
			} else if (wkGroup.isTGMTablePerSub()) {
				subs.forEach(subItm -> {
					sections.add(subItm.getLabel());
				});
			} else if (wkGroup.isTGMTablePerView()) {
				subs.stream().filter(itm -> itm.getJobViews() != null)
						.forEach(subItm -> {
							List<WebKitWorkspaceJobView> viewsForSub = subItm.getJobViews();
							Collections.sort(viewsForSub, Comparator.comparing(WebKitWorkspaceJobView::getViewOrder));
							viewsForSub.forEach(viewItm -> {
								sections.add(String.format(TGM_TABLE_PER_VIEW_SECTION_FORMAT,
										subItm.getLabel(), viewItm.getJobView().getViewName()));
							});
						});
			} else {
				throw new FluidClientException(
						String.format("Unable to determine outcome for TGM ''.", wkGroup.getTableGenerateMode()),
						FluidClientException.ErrorCode.FIELD_VALIDATE);
			}

			ContentViewJV contentViewJV = new ContentViewJV(this.getLoggedInUser(), sections, wkGroup, wkSub);
			return contentViewJV;
		} catch (Exception fce) {
			if (fce instanceof FluidClientException) {
				FluidClientException casted = (FluidClientException)fce;
				if (casted.getErrorCode() == FluidClientException.ErrorCode.NO_RESULT) {
					return new ContentViewJV(this.getLoggedInUser());
				}
			}
			this.raiseError(fce);
			return new ContentViewJV(this.getLoggedInUser());
		}
	}

	@Override
	protected JobViewItemVO createABaseWebVO(FluidItem item, WebKitViewSub sub, WebKitWorkspaceJobView view) {
		JobViewItemVO returnVal = new JobViewItemVO(item);

		//FIXME @jason, need map the route fields here...
		//FIXME @jason, need to map Route field webkit to normal route field...



		return returnVal;
	}

}
