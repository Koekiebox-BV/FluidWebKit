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

import com.fluidbpm.fluidwebkit.backing.bean.workspace.jv.ContentViewJV;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.jv.JobViewItemVO;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceRouteField;
import com.fluidbpm.ws.client.FluidClientException;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Bean storing personal inventory related items.
 */
@SessionScoped
@Named("webKitWorkspaceBean")
public class WorkspaceBean extends ABaseWorkspaceBean<JobViewItemVO, ContentViewJV> {

	public static final String TGM_TABLE_PER_VIEW_SECTION_FORMAT = "%s - %s";
	public static final String TGM_TABLE_PER_VIEW_SECTION_DEL = " - ";
	public static final int TGM_TABLE_PER_VIEW_SECTION_DEL_LEN = TGM_TABLE_PER_VIEW_SECTION_DEL.length();

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
				if (wkSub == null) {
					subs.forEach(subItm -> {
						sections.add(subItm.getLabel());
					});
				} else {
					sections.add(wkSub.getLabel());
				}
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
			contentViewJV.mapColumnModel();
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
	protected JobViewItemVO createABaseWebVO(
		WebKitViewGroup group,
		WebKitViewSub sub,
		WebKitWorkspaceJobView view,
		FluidItem item
	) {
		List<Field> mergedFields = new ArrayList<>();
		final List<WebKitWorkspaceRouteField> fieldsForSub = sub.getRouteFields();
		if (fieldsForSub != null) {
			fieldsForSub.forEach(rteItm -> {
				Field fieldToAdd = new Field(rteItm.getRouteField().getFieldName());
				Object fieldValue = null;
				if (item.getRouteFields() != null) {
					fieldValue = item.getRouteFields().stream()
							.filter(itm -> fieldToAdd.getFieldName().equals(itm.getFieldName()))
							.map(itm -> itm.getFieldValue())
							.findFirst()
							.orElse(null);
				}
				fieldToAdd.setFieldValue(fieldValue);
				mergedFields.add(fieldToAdd);
			});
		}

		item.setRouteFields(mergedFields);
		return new JobViewItemVO(item);
	}

}
