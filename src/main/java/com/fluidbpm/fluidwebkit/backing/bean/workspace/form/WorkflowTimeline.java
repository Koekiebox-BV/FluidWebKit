package com.fluidbpm.fluidwebkit.backing.bean.workspace.form;

import com.fluidbpm.program.api.vo.flow.FlowStep;
import com.fluidbpm.program.api.vo.historic.FormFlowHistoricData;
import lombok.Getter;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Converts audit events into individual step visits. All internal times are UTC instants. */
public final class WorkflowTimeline {
    private WorkflowTimeline() { }

    private static final Set<String> STARTS = new HashSet<>(Arrays.asList(
            "New Route Item", "New Route Item From Email", "New Route Item From Custom Program",
            "New Route Item From Clone", "New Item From Blockchain"));
    private static final Set<String> ENDS = new HashSet<>(Arrays.asList(
            "Flow End", "Forcefully Removed from Flow"));

    public static List<Visit> visits(List<FormFlowHistoricData> history, FlowStep current, Date currentEntered, Instant now) {
        List<FormFlowHistoricData> ordered = new ArrayList<>();
        if (history != null) {
            for (FormFlowHistoricData event : history) {
                if (event != null && event.getDateCreated() != null && event.getFlowStep() != null) ordered.add(event);
            }
        }
        ordered.sort(Comparator.comparing(FormFlowHistoricData::getDateCreated)
                .thenComparing(FormFlowHistoricData::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        List<Visit> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        Visit open = null;
        Instant departed = null;
        for (FormFlowHistoricData event : ordered) {
            if (event.getId() != null && !seen.add(event.getId())) continue;
            Instant at = event.getDateCreated().toInstant();
            FlowStep step = event.getFlowStep();
            String kind = event.getLogEntryType();
            boolean starts = STARTS.contains(kind);
            boolean forcedMove = "Forcefully Moved to Different Step".equals(kind);
            if (starts || forcedMove) {
                // A new run does not extend the previous run across an unrecorded gap.
                if (forcedMove && open != null) open.exit = at;
                open = null;
                departed = null;
            }
            if (open == null || !open.sameStep(step)) {
                // Without a transition record, an observed step change gives no exact boundary.
                open = new Visit("visit-" + result.size(), step,
                        departed == null ? at : departed, starts || forcedMove || departed != null);
                result.add(open);
                departed = null;
            }
            if ("To New Step".equals(kind) || ENDS.contains(kind)) {
                // This event belongs to the SOURCE step, not the destination.
                open.exit = at;
                open = null;
                departed = ENDS.contains(kind) ? null : at;
            }
        }
        if (current != null) {
            // Waiting steps may have no audit event yet (for example polling without entry rules).
            // The live snapshot supplies their identity, type and recorded entry time.
            if (open == null || !open.sameStep(current)) {
                Instant entry = currentEntered == null ? (departed == null ? now : departed) : currentEntered.toInstant();
                open = new Visit("visit-" + result.size(), current, entry, currentEntered != null || departed != null);
                result.add(open);
            }
            open.ongoing = true;
            open.observedAt = now.isBefore(open.start) ? open.start : now;
        }
        return result;
    }

    public static ZoneOffset offset(Float hours) {
        if (hours == null || !Float.isFinite(hours) || Math.abs(hours) > 18) return ZoneOffset.UTC;
        return ZoneOffset.ofTotalSeconds(Math.round(hours * 60) * 60);
    }

    @Getter
    public static final class Visit implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private final Long stepId;
        private final String stepName;
        private final String stepType;
        private final Instant start;
        private final boolean entryKnown;
        private Instant exit;
        private boolean ongoing;
        private Instant observedAt;

        private Visit(String id, FlowStep step, Instant start, boolean entryKnown) {
            this.id = id;
            this.stepId = step.getId();
            this.stepName = step.getName() == null ? "Unknown step" : step.getName();
            this.stepType = step.getFlowStepType() == null ? "Unknown" : step.getFlowStepType();
            this.start = start;
            this.entryKnown = entryKnown;
        }

        private boolean sameStep(FlowStep step) {
            return stepId != null ? stepId.equals(step.getId()) : Objects.equals(stepName, step.getName());
        }

        public Instant getDisplayEnd() { return ongoing ? observedAt : exit; }

        public String getDurationText() {
            Instant end = getDisplayEnd();
            if (!entryKnown || end == null || end.isBefore(start)) return "Unknown";
            long millis = Duration.between(start, end).toMillis();
            long seconds = millis / 1000;
            String duration;
            if (millis < 1000) duration = millis + " ms";
            else if (seconds < 60) duration = seconds + "s";
            else if (seconds < 3600) duration = (seconds / 60) + "m " + (seconds % 60) + "s";
            else if (seconds < 86400) duration = (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
            else duration = (seconds / 86400) + "d " + ((seconds % 86400) / 3600) + "h " + ((seconds % 3600) / 60) + "m";
            return duration + (ongoing ? " (ongoing)" : "");
        }

        public String entryText(ZoneOffset offset, DateTimeFormatter format) {
            String time = format.withZone(offset).format(start);
            return entryKnown ? time : "Unknown (first recorded: " + time + ")";
        }

        public String exitText(ZoneOffset offset, DateTimeFormatter format) {
            return ongoing ? "Ongoing" : exit == null ? "Unknown" : format.withZone(offset).format(exit);
        }

        public String getLogo() {
            switch (stepType) {
                case "Introduction": return "introduction_logo.png";
                case "Re Route": return "reroute_logo.png";
                case "Exit": return "exit_logo.png";
                case "Assignment": return "assignment_logo.png";
                case "Mail Capture": return "mail_capture_logo.png";
                case "Database Capture": return "database_capture_logo.png";
                case "Send Mail": return "send_mail_logo.png";
                case "Polling": case "Polling Step": return "polling_logo.png";
                case "Java Program": return "java_logo_white.jpeg";
                case "Clone Item": return "clone_logo_orange.png";
                case "Document Template": return "document_template.png";
                case "Ripple Pull": case "Ripple Push": return "blockchain_ripple.png";
                case "Bitcoin Pull": case "Bitcoin Push": return "bitcoin.png";
                case "ILP Pull": case "ILP Push": return "interledger.png";
                default: return null;
            }
        }
    }
}
