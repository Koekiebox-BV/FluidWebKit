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

package com.fluidbpm.fluidwebkit.backing.bean.workspace.expand;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.jv.JobViewItemVO;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.ws.client.FluidClientException;
import com.fluidbpm.ws.client.v1.form.FormContainerClient;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;


@SessionScoped
@Named("webKitExpandBean")
public class WebKitExpandBean extends ABaseManagedBean {

	@Inject
	private WorkspaceBean workspaceBean;

	public WorkspaceFluidItem actionObtainAncestorFrom(WorkspaceFluidItem parentForm) {
		try {
			FormContainerClient fcClient = this.getFluidClientDS().getFormContainerClient();
			List<Form> ancestors = fcClient.getAncestorFor(parentForm.getFluidItemForm());

			WorkspaceFluidItem wfi = new WorkspaceFluidItem(
					new JobViewItemVO(new FluidItem(ancestors.get(0))));

			this.workspaceBean.actionOpenFormForEditingFromWorkspace(wfi);
			return wfi;
		} catch (FluidClientException fce) {
			if (FluidClientException.ErrorCode.NO_RESULT == fce.getErrorCode()) {
				FacesMessage fMsg = new FacesMessage(
						FacesMessage.SEVERITY_WARN, "No Ancestor.",
						String.format("Form '%s'.", parentForm.getFluidItemTitle()));
				FacesContext.getCurrentInstance().addMessage(null, fMsg);
				return parentForm;
			}

			this.raiseError(fce);
		} catch (Exception except) {
			this.raiseError(except);
		}
		return parentForm;
	}
}
