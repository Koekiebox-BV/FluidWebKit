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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.pi;

import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.ABaseContentView;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.WebKitViewContentModelBean;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContentViewPI extends ABaseContentView {
	public static final String PI = "Personal Inventory";
	public static final String TO_ME = "Sent to Me";//TODO add the others....

	public ContentViewPI(User loggedInUserParam, WebKitViewContentModelBean webKitViewContentModelBean) {
		super(loggedInUserParam, new String[]{PI}, webKitViewContentModelBean);
	}

	@Override
	public List<WorkspaceFluidItem> getWorkspaceFluidItemsFor(
		String sectionParam,
		Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data
	) {
		if (data.isEmpty()) {
			return null;
		}

		//FIXME need to break out for each of the personal inbox types...

		return data.values().stream()
				.map(itm -> itm.values())
				.flatMap(Collection::stream)
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}
}
