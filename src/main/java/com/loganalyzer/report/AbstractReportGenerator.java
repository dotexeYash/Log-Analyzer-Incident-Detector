package com.loganalyzer.report;

import com.loganalyzer.model.AnalysisResult;
import com.loganalyzer.model.LogStatistics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Abstract base class for report generators.
 * Provides common formatting utilities, date-time formatters, and defensive data resolution
 * while enforcing the {@link ReportGenerator} contract on subclasses.
 */
public abstract class AbstractReportGenerator implements ReportGenerator {

    /**
     * Standard human-readable date-time format used across textual reports.
     */
    protected static final DateTimeFormatter STANDARD_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Reusable fallback empty statistics object used when an AnalysisResult has null statistics.
     */
    protected static final LogStatistics EMPTY_STATISTICS = new LogStatistics(
            0, Map.of(), null, null, Map.of(), Map.of(), Map.of(), 0.0, 0.0
    );

    /**
     * Generates a formatted report from the provided analysis result.
     * Each concrete report format (Console, Markdown, JSON) provides its own specific implementation.
     *
     * @param result analysis result containing metrics, anomalies, and incidents
     * @return formatted report as a String
     */
    @Override
    public abstract String generate(AnalysisResult result);

    /**
     * Safely resolves the statistics object from an AnalysisResult, returning a non-null
     * fallback if statistics are absent.
     *
     * @param result the analysis result to extract statistics from
     * @return non-null LogStatistics instance
     */
    protected LogStatistics resolveStatistics(AnalysisResult result) {
        if (result != null && result.getStatistics() != null) {
            return result.getStatistics();
        }
        return EMPTY_STATISTICS;
    }

    /**
     * Formats a LocalDateTime using the standard human-readable format ("yyyy-MM-dd HH:mm:ss"),
     * or returns the default fallback string "N/A" if the timestamp is null.
     *
     * @param timestamp the timestamp to format
     * @return formatted date-time string or "N/A"
     */
    protected String formatTimestamp(LocalDateTime timestamp) {
        return formatTimestamp(timestamp, "N/A");
    }

    /**
     * Formats a LocalDateTime using the standard format, or returns the specified fallback if null.
     *
     * @param timestamp the timestamp to format
     * @param fallback the value to return if timestamp is null
     * @return formatted date-time string or fallback
     */
    protected String formatTimestamp(LocalDateTime timestamp, String fallback) {
        return timestamp != null ? timestamp.format(STANDARD_DATE_TIME_FORMATTER) : fallback;
    }

    /**
     * Evaluates the high-level operational status of an analysis result.
     *
     * @param result the analysis result to evaluate
     * @return "CRITICAL" if critical incidents exist, "WARNING" if incidents exist, or "HEALTHY"
     */
    protected String evaluateStatus(AnalysisResult result) {
        if (result != null && result.hasCriticalIncidents()) {
            return "CRITICAL";
        }
        if (result != null && result.hasIncidents()) {
            return "WARNING";
        }
        return "HEALTHY";
    }
}
