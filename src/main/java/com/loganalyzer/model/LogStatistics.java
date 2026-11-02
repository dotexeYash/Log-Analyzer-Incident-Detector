package com.loganalyzer.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Encapsulates aggregated statistical metrics of parsed logs.
 */
public class LogStatistics {
    private final long totalLogs;
    private final Map<LogLevel, Long> countsByLevel;
    private final LocalDateTime firstTimestamp;
    private final LocalDateTime lastTimestamp;
    private final Map<String, Long> topErrors;
    private final Map<String, Long> topLoggers;
    private final Map<String, Long> logsPerHour;
    private final double errorPercentage;
    private final double warningPercentage;

    public LogStatistics(long totalLogs,
                         Map<LogLevel, Long> countsByLevel,
                         LocalDateTime firstTimestamp,
                         LocalDateTime lastTimestamp,
                         Map<String, Long> topErrors,
                         Map<String, Long> topLoggers,
                         Map<String, Long> logsPerHour,
                         double errorPercentage,
                         double warningPercentage) {
        this.totalLogs = totalLogs;
        this.countsByLevel = countsByLevel != null ? Map.copyOf(countsByLevel) : Collections.emptyMap();
        this.firstTimestamp = firstTimestamp;
        this.lastTimestamp = lastTimestamp;
        this.topErrors = topErrors != null ? Map.copyOf(topErrors) : Collections.emptyMap();
        this.topLoggers = topLoggers != null ? Map.copyOf(topLoggers) : Collections.emptyMap();
        this.logsPerHour = logsPerHour != null ? Map.copyOf(logsPerHour) : Collections.emptyMap();
        this.errorPercentage = errorPercentage;
        this.warningPercentage = warningPercentage;
    }

    public long getTotalLogs() {
        return totalLogs;
    }

    public Map<LogLevel, Long> getCountsByLevel() {
        return countsByLevel;
    }

    public long getCount(LogLevel level) {
        return countsByLevel.getOrDefault(level, 0L);
    }

    public LocalDateTime getFirstTimestamp() {
        return firstTimestamp;
    }

    public LocalDateTime getLastTimestamp() {
        return lastTimestamp;
    }

    public Map<String, Long> getTopErrors() {
        return topErrors;
    }

    public Map<String, Long> getTopLoggers() {
        return topLoggers;
    }

    public Map<String, Long> getLogsPerHour() {
        return logsPerHour;
    }

    public double getErrorPercentage() {
        return errorPercentage;
    }

    public double getWarningPercentage() {
        return warningPercentage;
    }
}
