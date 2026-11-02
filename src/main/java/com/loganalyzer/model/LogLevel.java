package com.loganalyzer.model;

import java.util.Locale;

/**
 * Enumeration representing standard application log severity levels.
 */
public enum LogLevel {
    TRACE(10),
    DEBUG(20),
    INFO(30),
    WARN(40),
    ERROR(50),
    FATAL(60),
    UNKNOWN(0);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

    public boolean isError() {
        return this == ERROR || this == FATAL;
    }

    public boolean isWarning() {
        return this == WARN;
    }

    public boolean isAtLeast(LogLevel other) {
        return other != null && this.severity >= other.severity;
    }

    /**
     * Parses a string representation into a LogLevel, with fallback to UNKNOWN.
     */
    public static LogLevel fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }

        String cleaned = raw.trim()
                .replaceAll("[\\[\\]:()]", "")
                .toUpperCase(Locale.ROOT);

        return switch (cleaned) {
            case "TRACE" -> TRACE;
            case "DEBUG" -> DEBUG;
            case "INFO", "NOTICE" -> INFO;
            case "WARN", "WARNING" -> WARN;
            case "ERROR", "ERR", "SEVERE" -> ERROR;
            case "FATAL", "CRITICAL", "EMERGENCY" -> FATAL;
            default -> UNKNOWN;
        };
    }
}
