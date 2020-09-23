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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.jv;

import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.ABaseContentView;
import com.fluidbpm.program.api.vo.flow.JobView;
import com.fluidbpm.program.api.vo.user.User;

import java.util.List;
import java.util.Map;

public class ContentViewJV extends ABaseContentView {
	public ContentViewJV(User loggedInUserParam) {
		super(loggedInUserParam, new String[]{});
	}

	public ContentViewJV(User loggedInUserParam, List<String> subs) {
		super(loggedInUserParam, subs);
	}

	@Override
	public List<WorkspaceFluidItem> getWorkspaceFluidItemsFor(
		String sectionParam,
		Map<JobView, List<WorkspaceFluidItem>> fluidItemsForViewsParam
	) {
		if (fluidItemsForViewsParam.isEmpty()) {
			return null;
		}

		JobView firstKey = fluidItemsForViewsParam.keySet().stream().findFirst().orElse(new JobView(sectionParam));
		List<WorkspaceFluidItem> returnVal = fluidItemsForViewsParam.get(firstKey);
		return returnVal;
	}
}
