package com.loganalyzer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a detected behavioral anomaly across log records.
 */
public class Anomaly {
    private final String id;
    private final AnomalyType type;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int affectedLogCount;
    private final List<LogEntry> sampleEntries;
    private final double severityScore; // 0.0 to 1.0

    public Anomaly(String id,
                   AnomalyType type,
                   String description,
                   LocalDateTime startTime,
                   LocalDateTime endTime,
                   int affectedLogCount,
                   List<LogEntry> sampleEntries,
                   double severityScore) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.startTime = startTime;
        this.endTime = endTime;
        this.affectedLogCount = affectedLogCount;
        this.sampleEntries = sampleEntries != null ? new ArrayList<>(sampleEntries) : new ArrayList<>();
        this.severityScore = Math.max(0.0, Math.min(1.0, severityScore));
    }

    public String getId() {
        return id;
    }

    public AnomalyType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getAffectedLogCount() {
        return affectedLogCount;
    }

    public List<LogEntry> getSampleEntries() {
        return Collections.unmodifiableList(sampleEntries);
    }

    public double getSeverityScore() {
        return severityScore;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s (Affected: %d, Score: %.2f)",
                id, type.getDisplayName(), description, affectedLogCount, severityScore);
    }
}
