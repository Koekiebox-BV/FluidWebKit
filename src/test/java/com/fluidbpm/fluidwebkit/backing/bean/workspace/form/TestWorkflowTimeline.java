package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.program.api.vo.flow.FlowStep;
import com.fluidbpm.program.api.vo.historic.FormFlowHistoricData;
import org.junit.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;

public class TestWorkflowTimeline {
    private static final Instant BASE = Instant.parse("2026-11-13T10:00:00Z");

    @Test public void transitionsCloseTheSourceAndRepeatedVisitsRemainSeparate() {
        List<FormFlowHistoricData> history = Arrays.asList(
                log(1, 0, "New Route Item", 1, "Submit"),
                log(2, 1, "To New Step", 1, "Submit"),
                log(3, 2, "Entry Rule", 2, "Review"),
                log(4, 3, "Moved to User View", 2, "Review"),
                log(5, 20, "To New Step", 2, "Review"),
                log(6, 21, "Entry Rule", 3, "Rework"),
                log(7, 30, "To New Step", 3, "Rework"),
                log(8, 31, "Entry Rule", 2, "Review"));
        List<FormFlowHistoricData> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        List<WorkflowTimeline.Visit> visits = WorkflowTimeline.visits(reversed, current("Review"), null, BASE.plusSeconds(60));
        assertEquals(4, visits.size());
        assertEquals(BASE.plusSeconds(1), visits.get(1).getStart());
        assertEquals(BASE.plusSeconds(20), visits.get(1).getExit());
        assertEquals("19s", visits.get(1).getDurationText());
        assertEquals(visits.get(1).getStepId(), visits.get(3).getStepId());
        assertNotEquals(visits.get(1).getId(), visits.get(3).getId());
        assertTrue(visits.get(3).isOngoing());
        assertEquals("30s (ongoing)", visits.get(3).getDurationText());
        assertEquals("assignment_logo.png", visits.get(1).getLogo());
        assertNull("Do not mutate callers' audit records", history.get(1).getFlowStep().getDescription());
    }

    @Test public void sameStepLoopAndSimultaneousTransitionsUseAuditIdOrder() {
        List<WorkflowTimeline.Visit> visits = WorkflowTimeline.visits(Arrays.asList(
                log(3, 0, "Entry Rule", 1, "Loop"),
                log(2, 0, "To New Step", 1, "Loop"),
                log(1, 0, "New Route Item", 1, "Loop")), current("Loop"), null, BASE.plusSeconds(1));
        assertEquals(2, visits.size());
        assertEquals("0 ms", visits.get(0).getDurationText());
        assertTrue(visits.get(1).isOngoing());
    }

    @Test public void forcedMoveNamesTheDestinationAndClosesThePriorVisit() {
        List<WorkflowTimeline.Visit> visits = WorkflowTimeline.visits(Arrays.asList(
                log(1, 0, "New Route Item", 1, "Review"),
                log(2, 60, "Forcefully Moved to Different Step", 2, "Repair"),
                log(3, 90, "Forcefully Removed from Flow", 2, "Repair")), null, null, BASE.plusSeconds(120));
        assertEquals(2, visits.size());
        assertEquals("1m 0s", visits.get(0).getDurationText());
        assertEquals(BASE.plusSeconds(60), visits.get(1).getStart());
        assertEquals("30s", visits.get(1).getDurationText());
        assertFalse(visits.get(1).isOngoing());
    }

    @Test public void completedRunDoesNotExtendThroughGapBeforeReintroduction() {
        List<WorkflowTimeline.Visit> visits = WorkflowTimeline.visits(Arrays.asList(
                log(1, 0, "New Route Item", 1, "Submit"),
                log(2, 5, "Flow End", 1, "Submit"),
                log(3, 3600, "New Route Item", 1, "Submit")), current("Submit"), null, BASE.plusSeconds(3602));
        assertEquals(2, visits.size());
        assertEquals("5s", visits.get(0).getDurationText());
        assertEquals("2s (ongoing)", visits.get(1).getDurationText());
    }

    @Test public void partialHistoryDoesNotInventEntryExitOrDuration() {
        FormFlowHistoricData first = log(1, 10, "Moved to User View", 1, "Review");
        List<WorkflowTimeline.Visit> visits = WorkflowTimeline.visits(Arrays.asList(first, first,
                log(2, 20, "Entry Rule", 2, "Repair")), null, null, BASE.plusSeconds(30));
        assertEquals(2, visits.size());
        assertFalse(visits.get(0).isEntryKnown());
        assertNull(visits.get(0).getExit());
        assertEquals("Unknown", visits.get(0).getDurationText());
        assertFalse(visits.get(1).isOngoing());
    }

    @Test public void midnightAndFractionalOffsetsAreAppliedExactlyOnce() {
        FormFlowHistoricData start = log(1, 0, "New Route Item", 1, "Review");
        start.setDateCreated(Date.from(Instant.parse("2026-11-13T00:00:00Z")));
        WorkflowTimeline.Visit visit = WorkflowTimeline.visits(Collections.singletonList(start), null, null, BASE).get(0);
        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertEquals("2026-11-13 05:45:00", visit.entryText(WorkflowTimeline.offset(5.75f), pattern));
        assertEquals("2026-11-12 20:30:00", visit.entryText(WorkflowTimeline.offset(-3.5f), pattern));
        assertEquals("2026-11-13 00:00:00", visit.entryText(WorkflowTimeline.offset(null), pattern));
    }

    @Test public void emptyAndMalformedHistoryRemainEmpty() {
        assertTrue(WorkflowTimeline.visits(null, null, null, BASE).isEmpty());
        assertTrue(WorkflowTimeline.visits(Arrays.asList(null, new FormFlowHistoricData()), null, null, BASE).isEmpty());
    }

    @Test public void waitingStepWithoutAnAuditEventUsesCurrentSnapshotIncludingItsLogo() {
        FlowStep polling = current("Wait for response");
        polling.setId(9L); polling.setFlowStepType("Polling");
        List<WorkflowTimeline.Visit> visits = WorkflowTimeline.visits(Arrays.asList(
                log(1, 0, "New Route Item", 1, "Submit"),
                log(2, 5, "To New Step", 1, "Submit")), polling,
                Date.from(BASE.plusSeconds(6)), BASE.plusSeconds(66));
        assertEquals(2, visits.size());
        assertEquals("polling_logo.png", visits.get(1).getLogo());
        assertEquals("1m 0s (ongoing)", visits.get(1).getDurationText());
    }

    @Test public void activeStepIsVisibleEvenBeforeTheFirstAuditRecordArrives() {
        List<WorkflowTimeline.Visit> visits = WorkflowTimeline.visits(Collections.emptyList(),
                current("Review"), Date.from(BASE), BASE.plusSeconds(5));
        assertEquals(1, visits.size());
        assertTrue(visits.get(0).isOngoing());
        assertEquals("5s (ongoing)", visits.get(0).getDurationText());
    }

    static FlowStep current(String name) {
        FlowStep step = new FlowStep();
        step.setId("Review".equals(name) ? 2L : 1L);
        step.setName(name);
        step.setFlowStepType("Assignment");
        return step;
    }

    static FormFlowHistoricData log(long id, long seconds, String kind, long stepId, String name) {
        FormFlowHistoricData event = new FormFlowHistoricData();
        event.setId(id);
        event.setDateCreated(Date.from(BASE.plusSeconds(seconds)));
        event.setLogEntryType(kind);
        FlowStep step = new FlowStep();
        step.setId(stepId);
        step.setName(name);
        step.setFlowStepType("Assignment");
        event.setFlowStep(step);
        return event;
    }
}
