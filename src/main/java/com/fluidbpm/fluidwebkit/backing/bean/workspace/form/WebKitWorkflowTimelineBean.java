package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.fluidwebkit.backing.bean.ABaseManagedBean;
import com.fluidbpm.fluidwebkit.backing.bean.workspace.WorkspaceFluidItem;
import com.fluidbpm.program.api.vo.form.Form;
import com.fluidbpm.program.api.vo.historic.FormFlowHistoricDataListing;
import com.fluidbpm.program.api.vo.role.AdminPermission;
import com.fluidbpm.ws.client.FluidClientException;
import lombok.Getter;
import org.primefaces.event.timeline.TimelineSelectEvent;
import org.primefaces.model.timeline.TimelineEvent;
import org.primefaces.model.timeline.TimelineModel;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Named("webKitWorkflowTimelineBean")
@ViewScoped
public class WebKitWorkflowTimelineBean extends ABaseManagedBean {
    private static final long serialVersionUID = 1L;
    @Getter private TimelineModel<WorkflowTimeline.Visit, Object> model = new TimelineModel<>();
    @Getter private WorkflowTimeline.Visit selected;
    @Getter private String title;
    @Getter private String message;
    private ZoneOffset offset = ZoneOffset.UTC;
    private String datePattern = "yyyy-MM-dd HH:mm:ss.SSS";

    /** Only the Show Timeline action calls the history service. Rendering and selection are local. */
    public void actionLoad(WorkspaceFluidItem item) {
        model = new TimelineModel<>();
        selected = null;
        title = null;
        message = null;
        if (!doesUserHavePermission(AdminPermission.view_audit_log)) {
            message = "You do not have permission to view workflow history.";
            return;
        }
        if (item == null || item.getFluidItemFormId() == null || item.getFluidItemFormId() <= 0) {
            message = "Workflow history is available for saved items.";
            return;
        }
        try {
            title = item.getFluidItemTitle();
            offset = WorkflowTimeline.offset(getLoggedInUser().getTimezone());
            // Always include seconds: automated steps can complete within a single minute.
            String date = getLoggedInUser().getDateFormat();
            datePattern = (date == null ? "yyyy-MM-dd" : date) + " HH:mm:ss.SSS";
            formatter(); // Validate the configured format before rendering.
            FormFlowHistoricDataListing history = loadHistory(item.getFluidItemFormId());
            Instant snapshot = history.getSnapshotTime() == null ? Instant.now() : history.getSnapshotTime().toInstant();
            for (WorkflowTimeline.Visit visit : WorkflowTimeline.visits(history.getListing(),
                    history.getCurrentStep(), history.getCurrentStepEntered(), snapshot)) {
                TimelineEvent.Builder<WorkflowTimeline.Visit> event = TimelineEvent.<WorkflowTimeline.Visit>builder()
                        .id(visit.getId()).data(visit).editable(false)
                        .startDate(LocalDateTime.ofInstant(visit.getStart(), ZoneOffset.UTC))
                        .styleClass(visit.isOngoing() ? "workflow-timeline-ongoing" : "workflow-timeline-visit");
                if (visit.isEntryKnown() && visit.getDisplayEnd() != null
                        && visit.getDisplayEnd().isAfter(visit.getStart())) {
                    event.endDate(LocalDateTime.ofInstant(visit.getDisplayEnd(), ZoneOffset.UTC));
                }
                model.add(event.build());
            }
            if (model.getEvents().isEmpty()) message = "No workflow history is available for this item.";
        } catch (Exception error) {
            model = new TimelineModel<>();
            selected = null;
            message = "Unable to load workflow history. Close this window and try Show Timeline again.";
            raiseError(error);
        }
    }

    protected FormFlowHistoricDataListing loadHistory(Long formId) {
        try {
            return getFluidClientDS().getFormContainerClient().getFormFlowTimelineData(new Form(formId));
        } catch (FluidClientException error) {
            if (FluidClientException.isNoResultError(error)) return new FormFlowHistoricDataListing();
            throw error;
        }
    }

    public void onSelect(TimelineSelectEvent<WorkflowTimeline.Visit> event) {
        selected = doesUserHavePermission(AdminPermission.view_audit_log) && event.getTimelineEvent() != null
                ? event.getTimelineEvent().getData() : null;
    }

    public boolean isHasEvents() { return !model.getEvents().isEmpty(); }
    public String getTimeZoneId() { return offset.getId(); }
    public String getTimeZoneLabel() { return ZoneOffset.UTC.equals(offset) ? "UTC" : "UTC" + offset.getId(); }
    public String entryTime(WorkflowTimeline.Visit visit) { return visit.entryText(offset, formatter()); }
    public String exitTime(WorkflowTimeline.Visit visit) { return visit.exitText(offset, formatter()); }
    private DateTimeFormatter formatter() { return DateTimeFormatter.ofPattern(datePattern); }
}
