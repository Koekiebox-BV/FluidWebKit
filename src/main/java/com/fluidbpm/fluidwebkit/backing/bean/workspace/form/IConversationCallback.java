package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;

/**
 * Callback interface from the form conversation to the workspace bean.
 */
public interface IConversationCallback {
	void afterSaveProcessing(WorkspaceFluidItem workspaceItemSaved);
	void afterSendOnProcessing(WorkspaceFluidItem workspaceItemSaved);

	void closeFormDialog();

	String getDestinationNavigationForCancel();
}
