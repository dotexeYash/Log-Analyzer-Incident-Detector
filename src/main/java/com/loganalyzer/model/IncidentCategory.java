package com.loganalyzer.model;

/**
 * Categorization of operational incidents.
 */
public enum IncidentCategory {
    DATABASE_OUTAGE("Database Outage / Connection Starvation"),
    SECURITY_BREACH_ATTEMPT("Security Breach Attempt / Unauthorized Burst"),
    MEMORY_CRITICAL_FAILURE("Memory Critical Failure / Out Of Memory"),
    NETWORK_SERVICE_DEGRADATION("Network / Downstream Service Failure"),
    APPLICATION_CRASH("Application Instability / Fatal Crash"),
    GENERAL_INCIDENT("General Operational Incident");

    private final String description;

    IncidentCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
