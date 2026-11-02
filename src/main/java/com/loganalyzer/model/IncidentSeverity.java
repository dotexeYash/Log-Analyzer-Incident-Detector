package com.loganalyzer.model;

/**
 * Severity ranking for operational incidents.
 */
public enum IncidentSeverity {
    LOW(1, "LOW"),
    MEDIUM(2, "MEDIUM"),
    HIGH(3, "HIGH"),
    CRITICAL(4, "CRITICAL");

    private final int rank;
    private final String label;

    IncidentSeverity(int rank, String label) {
        this.rank = rank;
        this.label = label;
    }

    public int getRank() {
        return rank;
    }

    public String getLabel() {
        return label;
    }

    public boolean isHigherThan(IncidentSeverity other) {
        return other != null && this.rank > other.rank;
    }
}
