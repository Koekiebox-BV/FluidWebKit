package com.fluidbpm.fluidwebkit.backing.bean.workspace;

import com.fluidbpm.fluidwebkit.backing.vo.ABaseWebVO;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.user.User;
import com.fluidbpm.program.api.vo.webkit.form.WebKitForm;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class TestWorkflowProgressVisibility {
    @Test public void settingDefaultsOffAndSurvivesJsonRoundTrip() {
        assertFalse(new WebKitForm().isShowWorkflowProgress());
        WebKitForm settings = new WebKitForm();
        settings.setShowWorkflowProgress(true);
        assertTrue(new WebKitForm(settings.toJsonObject()).isShowWorkflowProgress());
        settings.setShowWorkflowProgress(false);
        assertFalse(new WebKitForm(settings.toJsonObject()).isShowWorkflowProgress());
    }

    @Test public void visibleOnlyForOptedInActiveItemsWithProgress() {
        FluidItem item = item("25");
        WorkspaceFluidItem workspace = workspace(item);
        assertFalse(workspace.isShowWorkflowProgress());
        workspace.getWebKitForm().setShowWorkflowProgress(true);
        assertTrue(workspace.isShowWorkflowProgress());
        assertEquals(Integer.valueOf(25), workspace.getWorkflowProgressPercentage());
        item.setFlowStateString(FluidItem.FlowState.UserSendWorkInProgress.name());
        assertTrue(workspace.isShowWorkflowProgress());
        item.setFlowStateString(FluidItem.FlowState.Archive.name());
        assertFalse(workspace.isShowWorkflowProgress());
        item.setFlowStateString(FluidItem.FlowState.NotInFlow.name());
        assertFalse(workspace.isShowWorkflowProgress());
    }

    @Test public void missingOrMalformedProgressAndStandaloneFormsAreHidden() {
        FluidItem item = item("bad");
        WorkspaceFluidItem workspace = workspace(item);
        workspace.getWebKitForm().setShowWorkflowProgress(true);
        assertFalse(workspace.isShowWorkflowProgress());
        item.setCustomProperties(null);
        assertFalse(workspace.isShowWorkflowProgress());
        item.addCustomProperty(new FluidItem.FluidItemProperty(FluidItem.FluidItemProperty.Prop.FLOW_COMPLETE_PERCENTAGE, "50"));
        item.setId(-1L);
        assertFalse(workspace.isShowWorkflowProgress());
    }

    @Test public void savedFormsCanShowHistoryAfterLeavingWorkflow() {
        FluidItem item = item("100");
        item.setForm(new Form(42L));
        item.setFlowStateString(FluidItem.FlowState.Archive.name());
        WorkspaceFluidItem workspace = workspace(item);
        assertFalse(workspace.isShowWorkflowTimeline());
        workspace.getWebKitForm().setShowWorkflowProgress(true);
        assertTrue(workspace.isShowWorkflowTimeline());
        assertFalse(workspace.isShowWorkflowProgress());
        item.getForm().setId(null);
        assertFalse(workspace.isShowWorkflowTimeline());
    }

    private static FluidItem item(String percentage) {
        FluidItem item = new FluidItem();
        item.setId(1L);
        item.setFlowStateString(FluidItem.FlowState.WorkInProgress.name());
        item.addCustomProperty(new FluidItem.FluidItemProperty(FluidItem.FluidItemProperty.Prop.FLOW_COMPLETE_PERCENTAGE, percentage));
        return item;
    }
    private static WorkspaceFluidItem workspace(FluidItem item) {
        WorkspaceFluidItem workspace = new WorkspaceFluidItem(new ABaseWebVO(item) {
            @Override public ABaseWebVO cloneVO(FluidItem copy, List<Field> viewable, List<Field> editable, List<User> users) { return this; }
        });
        workspace.setWebKitForm(new WebKitForm());
        return workspace;
    }
}
