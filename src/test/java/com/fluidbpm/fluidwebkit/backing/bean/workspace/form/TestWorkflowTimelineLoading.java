package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.fluidwebkit.backing.vo.ABaseWebVO;
import com.fluidbpm.program.api.vo.field.Field;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.historic.FormFlowHistoricDataListing;
import com.fluidbpm.program.api.vo.item.FluidItem;
import com.fluidbpm.program.api.vo.role.AdminPermission;
import com.fluidbpm.program.api.vo.user.User;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TestWorkflowTimelineLoading {
    private static class Bean extends WebKitWorkflowTimelineBean {
        int requests;
        boolean permitted = true;
        boolean fail;
        @Override public boolean doesUserHavePermission(AdminPermission... permissions) { return permitted; }
        @Override public User getLoggedInUser() {
            User user = new User(1L); user.setTimezone(5.5f); user.setDateFormat("yyyy-MM-dd"); return user;
        }
        @Override protected FormFlowHistoricDataListing loadHistory(Long id) {
            requests++;
            if (fail) throw new IllegalStateException("Service unavailable");
            FormFlowHistoricDataListing history = new FormFlowHistoricDataListing();
            history.setListing(Collections.singletonList(TestWorkflowTimeline.log(1, 0, "New Route Item", 1, "Review")));
            history.setCurrentStep(history.getListing().get(0).getFlowStep());
            return history;
        }
        @Override public void raiseError(Exception error) { }
    }

    @Test public void historyIsOnlyFetchedOnExplicitLoadAndNotByRenderingGetters() {
        Bean bean = new Bean();
        assertFalse(bean.isHasEvents());
        bean.getModel(); bean.getSelected(); bean.getTitle(); bean.getTimeZoneLabel();
        assertEquals(0, bean.requests);
        bean.actionLoad(item(10L));
        assertTrue(bean.isHasEvents());
        assertEquals(1, bean.requests);
        bean.getModel(); bean.getTimeZoneId(); bean.getMessage();
        assertEquals(1, bean.requests);
        assertEquals("UTC+05:30", bean.getTimeZoneLabel());
        bean.actionLoad(item(11L));
        assertEquals(2, bean.requests);
        assertEquals("Form 11", bean.getTitle());
        assertEquals(1, bean.getModel().getEvents().size());
    }

    @Test public void permissionIsRecheckedBeforeLoadingAndPreviouslyLoadedDataIsCleared() {
        Bean bean = new Bean();
        bean.actionLoad(item(10L));
        bean.permitted = false;
        bean.actionLoad(item(11L));
        assertEquals(1, bean.requests);
        assertFalse(bean.isHasEvents());
        assertNull(bean.getSelected());
        assertNull(bean.getTitle());
        assertTrue(bean.getMessage().contains("permission"));
    }

    @Test public void failureDoesNotDisplayThePreviousItemsHistory() {
        Bean bean = new Bean();
        bean.actionLoad(item(10L));
        bean.fail = true;
        bean.actionLoad(item(11L));
        assertFalse(bean.isHasEvents());
        assertTrue(bean.getMessage().contains("Unable"));
    }

    private WorkspaceFluidItem item(Long id) {
        FluidItem fluid = new FluidItem();
        Form form = new Form(id); form.setTitle("Form " + id); fluid.setForm(form);
        return new WorkspaceFluidItem(new ABaseWebVO(fluid) {
            @Override public ABaseWebVO cloneVO(FluidItem copy, List<Field> viewable, List<Field> editable, List<User> users) { return this; }
        });
    }
}
