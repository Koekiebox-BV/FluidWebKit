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

package com.fluidbpm.fluidwebkit.backing.bean.quicksearch;

import com.fluidbpm.fluidwebkit.backing.bean.login.ProfileBean;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.form.Form;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.SimpleDateFormat;

/**
 *
 */
@Named(value = "webKitConverterQuickSearch")
public class QuickSearchConverter implements Converter {

	@Inject
	private ProfileBean profileBean;

	/**
	 *
	 * @param facesContextParam
	 * @param uiComponentParam
	 * @param stringValueParam
	 * @return
	 */
	@Override
	public Object getAsObject(
		FacesContext facesContextParam,
		UIComponent uiComponentParam,
		String stringValueParam
	) {
		if (stringValueParam == null || stringValueParam.trim().isEmpty()) {
			return null;
		}

		long fieldId = UtilGlobal.toLongSafe(stringValueParam);
		if (fieldId < 1) {
			return null;
		}

		Form form = profileBean.getFluidClientDS().getFormContainerClient().getFormContainerById(fieldId);
		return new QuickSearchBean.QuickSearchResultVO(form, new SimpleDateFormat(this.profileBean.getDateAndTimeFormat()));
	}

	/**
	 *
	 * @param facesContextParam
	 * @param uiComponentParam
	 * @param valueParam
	 * @return
	 */
	@Override
	public String getAsString(FacesContext facesContextParam, UIComponent uiComponentParam, Object valueParam) {
		if (valueParam == null) {
			return UtilGlobal.EMPTY;
		}

		if (valueParam instanceof QuickSearchBean.QuickSearchResultVO) {
			QuickSearchBean.QuickSearchResultVO casted = (QuickSearchBean.QuickSearchResultVO) valueParam;
			if (casted != null || casted.getId() > 0L) {
				return casted.getId().toString();
			}

			return UtilGlobal.EMPTY;
		}

		return UtilGlobal.EMPTY;
	}
}
