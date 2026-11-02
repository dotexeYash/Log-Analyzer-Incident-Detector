package com.loganalyzer.model;

/**
 * Types of anomalies detected in log streams.
 */
public enum AnomalyType {
    /**
     * Rapid burst of errors exceeding normal frequency in a short time window.
     */
    ERROR_BURST("Sudden Error Burst"),

    /**
     * Same error message repeating excessively.
     */
    HIGH_FREQUENCY_IDENTICAL_ERRORS("High-Frequency Identical Errors"),

    /**
     * Out of memory, GC overhead limit, or thread pool exhaustion.
     */
    RESOURCE_EXHAUSTION("Resource Exhaustion Detected"),

    /**
     * Multiple unauthorized requests, failed logins, or suspicious access patterns.
     */
    SECURITY_SUSPICIOUS_ACCESS("Suspicious Security Activity"),

    /**
     * Cascading database errors, pool starvation, connection timeouts, or deadlocks.
     */
    DATABASE_FAILURE_SPIKE("Database Connection / Query Failure"),

    /**
     * Service timeouts or communication link breakdown.
     */
    SERVICE_TIMEOUT_SPIKE("Cascading Service Timeouts");

    private final String displayName;

    AnomalyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
