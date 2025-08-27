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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.lf;

import com.fluidbpm.program.api.util.UtilGlobal;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.google.gson.JsonParser;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Named;


@Named(value = "webKitViewPickListConverter")
public class ViewPickListConverter implements Converter {
    @Override
    public Object getAsObject(FacesContext facesContextParam, UIComponent uiComponentParam, String stringValueParam) {
        if (stringValueParam == null || stringValueParam.isEmpty()) {
            return null;
        }
        return new JobView(
                JsonParser.parseString(stringValueParam).getAsJsonObject()
        );
    }

    @Override
    public String getAsString(FacesContext facesContextParam, UIComponent uiComponentParam, Object valueParam) {
        if (!(valueParam instanceof JobView)) {
            return UtilGlobal.EMPTY;
        }
        return ((JobView) valueParam).toJsonObject().toString();
    }
}
