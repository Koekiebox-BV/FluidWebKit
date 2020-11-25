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

import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.WebKitViewContentModelBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.form.IConversationCallback;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.jv.ContentViewJV;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.jv.JobViewItemVO;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceRouteField;
import com.fluidbpm.ws.client.FluidClientException;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
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
public class WorkspaceBean extends ABaseWorkspaceBean<JobViewItemVO, ContentViewJV> implements
		IConversationCallback {

	public static final String TGM_TABLE_PER_VIEW_SECTION_FORMAT = "%s - %s";
	public static final String TGM_TABLE_PER_VIEW_SECTION_DEL = " - ";
	public static final int TGM_TABLE_PER_VIEW_SECTION_DEL_LEN = TGM_TABLE_PER_VIEW_SECTION_DEL.length();

	@Inject
	private WebKitViewContentModelBean webKitViewContentModelBean;

	@Override
	public void actionOpenForm(WorkspaceFluidItem workspaceFluidItem) {
		this.openFormBean.actionFreshLoadFormAndSet(workspaceFluidItem);
	}

	@Override
	protected ContentViewJV actionOpenMainPage(
		WebKitViewGroup wkGroup,
		WebKitViewSub wkSub
	) {
		try {
			if (this.getFluidClientDS() == null) return null;

			final String tgm = wkGroup.getTableGenerateMode();

			if (tgm == null) throw new FluidClientException(
						"Unable to determine outcome for TGM not being set.",
						FluidClientException.ErrorCode.FIELD_VALIDATE);

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

			ContentViewJV contentViewJV = new ContentViewJV(
					this.getLoggedInUser(), sections, wkGroup, wkSub, this.webKitViewContentModelBean);
			contentViewJV.mapColumnModel();
			return contentViewJV;
		} catch (Exception fce) {
			if (fce instanceof FluidClientException) {
				FluidClientException casted = (FluidClientException)fce;
				if (casted.getErrorCode() == FluidClientException.ErrorCode.NO_RESULT) {
					return new ContentViewJV(this.getLoggedInUser(), this.webKitViewContentModelBean);
				}
			}
			this.raiseError(fce);
			return new ContentViewJV(this.getLoggedInUser(), this.webKitViewContentModelBean);
		}
	}

	/**
	 * Open an 'Form' for editing or viewing.
	 * Custom functionality needs to be placed in {@code this#actionOpenFormForEditingFromWorkspace}.
	 *
	 * @see this#actionOpenForm(WorkspaceFluidItem)
	 */
	public void actionOpenFormForEditingFromWorkspace(WorkspaceFluidItem workspaceFluidItem) {
		this.setAreaToUpdateForDialogAfterSubmit(null);
		this.currentlyHaveItemOpen = false;

//		Long formIdToUpdate = this.getLongRequestParam(RequestParam.TO_UPDATE);
//		Long openedFromViewId = this.getLongRequestParam(RequestParam.OPENED_FROM_VIEW);
//		JobView fromView = this.getJobViewFromListingWithId(this.viewsAll, openedFromViewId);

		try {
			this.openFormBean.startConversation();
			this.openFormBean.setAreaToUpdateAfterSave(":dataListResults");
			this.openFormBean.setConversationCallback(this);
			this.actionOpenForm(workspaceFluidItem);
			this.currentlyHaveItemOpen = true;
		} catch (Exception except) {
			this.raiseError(except);
		}
	}

	@Override
	public void afterSaveProcessing(WorkspaceFluidItem workspaceItemSaved) {
		this.actionOpenMainPage();
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
							.findFirst()
							.map(itm -> itm.getFieldValue())
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
