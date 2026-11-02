package com.loganalyzer.model;

import java.util.Collections;
import java.util.List;

/**
 * Container bundling all analysis outcomes: statistics, detected anomalies, and categorized incidents.
 */
public class AnalysisResult {
    private final String sourceName;
    private final List<LogEntry> entries;
    private final LogStatistics statistics;
    private final List<Anomaly> anomalies;
    private final List<Incident> incidents;

    public AnalysisResult(String sourceName,
                          List<LogEntry> entries,
                          LogStatistics statistics,
                          List<Anomaly> anomalies,
                          List<Incident> incidents) {
        this.sourceName = sourceName != null ? sourceName : "Unknown";
        this.entries = entries != null ? Collections.unmodifiableList(entries) : Collections.emptyList();
        this.statistics = statistics;
        this.anomalies = anomalies != null ? Collections.unmodifiableList(anomalies) : Collections.emptyList();
        this.incidents = incidents != null ? Collections.unmodifiableList(incidents) : Collections.emptyList();
    }

    public String getSourceName() {
        return sourceName;
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public LogStatistics getStatistics() {
        return statistics;
    }

    public List<Anomaly> getAnomalies() {
        return anomalies;
    }

    public List<Incident> getIncidents() {
        return incidents;
    }

    public boolean hasIncidents() {
        return !incidents.isEmpty();
    }

    public boolean hasCriticalIncidents() {
        return incidents.stream().anyMatch(inc -> inc.getSeverity() == IncidentSeverity.CRITICAL);
    }
}
