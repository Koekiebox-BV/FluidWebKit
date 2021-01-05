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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import lombok.Getter;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.util.*;
import java.util.stream.Collectors;

@SessionScoped
@Named("webKitViewContentModelBean")
public class WebKitViewContentModelBean extends ABaseManagedBean {
	@Getter
	private Map<String, Map<String, List<ABaseManagedBean.ColumnModel>>> columnModels;

	public WebKitViewContentModelBean() {
		this.columnModels = /*Collections.synchronizedMap(*/new HashMap<>()/*)*/;
	}

	public void storeModelFor(String category, String sectionAlias, List<ABaseManagedBean.ColumnModel> model) {
		if ((category == null || sectionAlias == null) || (model == null || model.isEmpty())) return;

		if (this.columnModels.containsKey(category) && this.columnModels.get(category).containsKey(sectionAlias)) return;

		synchronized (this.columnModels) {
			Map<String, List<ABaseManagedBean.ColumnModel>> sectionMapping =
					this.columnModels.getOrDefault(category, new HashMap<>());
			List<ABaseManagedBean.ColumnModel> cloned = new ArrayList<>();
			model.forEach(itm -> {
				cloned.add(itm.cloneCM());
			});
			sectionMapping.put(sectionAlias, cloned);
			this.columnModels.put(category, sectionMapping);
		}
	}

	public List<ABaseManagedBean.ColumnModel> getModelFor(String category, String sectionAlias) {
		return this.columnModels.getOrDefault(category, new HashMap<>()).getOrDefault(sectionAlias, new ArrayList<>());
	}

	public Map<String, List<ABaseManagedBean.ColumnModel>> getColumnModelsFilterable(String category) {
		Map<String,List<ABaseManagedBean.ColumnModel>> modelMap = this.getColumnModels().get(category);
		Map<String, List<ABaseManagedBean.ColumnModel>> returnVal = new HashMap<>();
		if (modelMap == null) return returnVal;
		modelMap.forEach((key, val) -> {
			List<ABaseManagedBean.ColumnModel> filterList = modelMap.get(key).stream()
					.filter(itm -> itm.isEnabled() && itm.isFilterable())
					.collect(Collectors.toList());
			if (filterList == null) filterList = new ArrayList<>();

			Collections.sort(filterList, Comparator.comparing(ColumnModel::getFluidFieldName));
			returnVal.put(key, filterList);
		});
		return returnVal;
	}
}
