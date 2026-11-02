package com.loganalyzer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a prioritized, actionable operational incident formed from detected anomalies.
 */
public class Incident {
    private final String id;
    private final String title;
    private final IncidentSeverity severity;
    private final IncidentCategory category;
    private final String summary;
    private final LocalDateTime detectedAt;
    private final List<Anomaly> triggeredAnomalies;
    private final List<LogEntry> rootCauseEntries;
    private final List<String> recommendations;

    public Incident(String id,
                    String title,
                    IncidentSeverity severity,
                    IncidentCategory category,
                    String summary,
                    LocalDateTime detectedAt,
                    List<Anomaly> triggeredAnomalies,
                    List<LogEntry> rootCauseEntries,
                    List<String> recommendations) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.severity = Objects.requireNonNullElse(severity, IncidentSeverity.MEDIUM);
        this.category = Objects.requireNonNullElse(category, IncidentCategory.GENERAL_INCIDENT);
        this.summary = Objects.requireNonNullElse(summary, "");
        this.detectedAt = detectedAt != null ? detectedAt : LocalDateTime.now();
        this.triggeredAnomalies = triggeredAnomalies != null ? new ArrayList<>(triggeredAnomalies) : new ArrayList<>();
        this.rootCauseEntries = rootCauseEntries != null ? new ArrayList<>(rootCauseEntries) : new ArrayList<>();
        this.recommendations = recommendations != null ? new ArrayList<>(recommendations) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public List<Anomaly> getTriggeredAnomalies() {
        return Collections.unmodifiableList(triggeredAnomalies);
    }

    public List<LogEntry> getRootCauseEntries() {
        return Collections.unmodifiableList(rootCauseEntries);
    }

    public List<String> getRecommendations() {
        return Collections.unmodifiableList(recommendations);
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] %s (%s)", id, severity, title, category.getDescription());
    }
}
