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

import com.fluidbpm.program.api.vo.report.userstats.PunchCardEntry;

import java.util.Comparator;

/**
 * Created by jasonbruwer on 2015/11/24.
 */
public class PunchCardEntryComp implements Comparator<PunchCardEntry> {

    /**
     *
     * @param objectOneParam
     * @param objectTwoParam
     * @return
     */
    @Override
    public int compare(PunchCardEntry objectOneParam,
                       PunchCardEntry objectTwoParam) {

        if(objectOneParam == null || objectTwoParam == null)
        {
            return 0;
        }

        if(objectOneParam.getPunchCardDay() == null || objectTwoParam.getPunchCardDay() == null)
        {
            return 0;
        }

        return objectOneParam.getPunchCardDay().compareTo(objectTwoParam.getPunchCardDay());
    }
}
