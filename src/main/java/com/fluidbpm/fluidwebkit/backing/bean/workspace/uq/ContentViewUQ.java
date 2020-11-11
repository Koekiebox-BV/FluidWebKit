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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.uq;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.config.WebKitAccessBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.ABaseContentView;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.contentview.WebKitViewContentModelBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.jv.ContentViewJV;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.jv.WorkspaceJobViewLDM;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.webkit.userquery.WebKitUserQuery;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewGroup;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitViewSub;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceJobView;
import com.fluidbpm.program.api.vo.webkit.viewgroup.WebKitWorkspaceRouteField;
import lombok.Getter;
import lombok.Setter;

import javax.faces.model.SelectItem;
import java.util.*;
import java.util.stream.Collectors;

public class ContentViewUQ extends ABaseContentView {
	@Getter
	private WebKitUserQuery wkUserQuery;

	private WebKitAccessBean accessBean;

	@Getter
	@Setter
	private WorkspaceUserQueryLDM fluidItemsLazyModel = null;

	public static final class SystemField {
		public static final String DATE_CREATED = "Date Created";
		public static final String DATE_LAST_UPDATED = "Date Last Updated";
		public static final String FORM_TYPE = "Form Type";
		public static final String ID = "ID";
		public static final String TITLE = "Title";
		public static final String USER = "User";
		public static final String STATE = "State";
		public static final String FLOW_STATE = "Flow State";
	}

	public ContentViewUQ(
		User loggedInUserParam,
		String sectionName,
		WebKitUserQuery webKitUserQuery,
		WebKitViewContentModelBean webKitViewContentModelBean,
		WebKitAccessBean accessBean
	) {
		super(loggedInUserParam, new String[]{sectionName}, webKitViewContentModelBean);
		this.wkUserQuery = webKitUserQuery;
		this.accessBean = accessBean;
	}

	@Override
	public List<WorkspaceFluidItem> getWorkspaceFluidItemsFor(
		String sectionParam,
		Map<WebKitViewSub, Map<WebKitWorkspaceJobView, List<WorkspaceFluidItem>>> data
	) {
		return null;//Not Required...
	}

	/**
	 * Retrieve all the column models for section {@code sectionAliasParam}.
	 *
	 * @param sectionAlias The section to retrieve column model headers for.
	 * @return ABaseManagedBean.ColumnModel list.
	 *
	 * @see ABaseManagedBean.ColumnModel
	 * @see List
	 */
	@Override
	public List<ABaseManagedBean.ColumnModel> getColumnHeadersForSection(String sectionAlias) {
		List<ABaseManagedBean.ColumnModel> returnVal = new ArrayList<>();
		if (this.wkUserQuery == null) {
			return returnVal;
		}

		//Index - 01 - Attachment
		returnVal.add(new ABaseManagedBean.ColumnModel(
				this.wkUserQuery.getAttachmentHeader(),
				this.wkUserQuery.getAttachmentHeader(),
				Field.Type.Label,
				this.wkUserQuery.isShowColumnAttachment(),
				true,
				false));

		//Index - 02 - Title
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.TITLE,
				SystemField.TITLE,
				Field.Type.Text,
				this.wkUserQuery.isShowColumnTitle(),
				true,
				true));

		//Index - 03 - ID
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.ID,
				SystemField.ID,
				Field.Type.Text,
				this.wkUserQuery.isShowColumnID(),
				true,
				true));

		//Index - 04 - Form Type
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.FORM_TYPE,
				SystemField.FORM_TYPE,
				Field.Type.MultipleChoice,
				this.wkUserQuery.isShowColumnFormType(),
				true,
				true));

		//Index - 05 - Date Created
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.DATE_CREATED,
				SystemField.DATE_CREATED,
				Field.Type.DateTime,
				this.wkUserQuery.isShowColumnDateCreated(),
				true,
				true));

		//Index - 06 - Date Last Updated
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.DATE_LAST_UPDATED,
				SystemField.DATE_LAST_UPDATED,
				Field.Type.DateTime,
				this.wkUserQuery.isShowColumnDateLastUpdated(),
				true,
				true));

		//Index - 07 - User
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.USER,
				SystemField.USER,
				Field.Type.MultipleChoice,
				this.wkUserQuery.isShowColumnUser(),
				true,
				true));

		//Index - 08 - State
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.STATE,
				SystemField.STATE,
				Field.Type.MultipleChoice,
				this.wkUserQuery.isShowColumnState(),
				true,
				true));

		//Index - 09 - Flow State
		returnVal.add(new ABaseManagedBean.ColumnModel(
				SystemField.FLOW_STATE,
				SystemField.FLOW_STATE,
				Field.Type.MultipleChoice,
				this.wkUserQuery.isShowColumnFlowState(),
				true,
				true));

		List<Field> fieldsLoggedInUserHasAccessTo =
				this.accessBean.getFormFieldsUserHasVisibilityFor(this.wkUserQuery.getUserQuery());
		if (fieldsLoggedInUserHasAccessTo == null || fieldsLoggedInUserHasAccessTo.isEmpty()) return returnVal;

		List<ABaseManagedBean.ColumnModel> returnValCustomRouteFields = fieldsLoggedInUserHasAccessTo.stream()
				.map(formField -> {
					boolean canFilter = false;
					switch (formField.getTypeAsEnum()) {
						case Text:
						case DateTime:
						case TrueFalse:
						case Decimal:
						case MultipleChoice:
							canFilter = true;
					}
					return new ABaseManagedBean.ColumnModel(
							formField.getFieldName(),
							formField.getFieldName(),
							formField.getTypeAsEnum(),
							true,
							true,
							canFilter);
				})
				.collect(Collectors.toList());
		returnVal.addAll(returnValCustomRouteFields);
		return returnVal;
	}
}
