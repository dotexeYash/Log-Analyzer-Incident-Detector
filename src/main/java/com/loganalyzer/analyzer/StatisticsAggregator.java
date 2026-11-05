package com.loganalyzer.analyzer;

import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogLevel;
import com.loganalyzer.model.LogStatistics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Aggregates logs into meaningful statistical metrics, breakdowns, and recurring error summaries.
 */
public class StatisticsAggregator {

    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
    private static final Pattern NORMALIZATION_PATTERN = Pattern.compile(
            // Branch 1 (PROTECT): HTTP status code with prefix context
            "(?<httpPrefix>(?i)\\b(?:HTTP(?:/[0-9.]+)?(?:\\s+(?:status|error))?|status(?:[ _-]?code)?|response\\s+code)\\s*[:=]?\\s*[1-5]\\d{2}\\b)" +
            // Branch 2 (PROTECT): HTTP status code followed by standard HTTP reason phrase (case-insensitive)
            "|(?<httpSuffix>(?i)\\b[1-5]\\d{2}\\s+(?:Unauthorized|Forbidden|Not\\s+Found|Internal\\s+Server\\s+Error|Bad\\s+Gateway|Service\\s+Unavailable|Gateway\\s+Timeout|Bad\\s+Request|Too\\s+Many\\s+Requests)\\b)" +
            // Branch 3 (NORMALIZE): IPv4 address candidate
            "|(?<ipv4Candidate>\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b)" +
            // Branch 4 (NORMALIZE): Standard 36-character hexadecimal UUIDs
            "|(?<uuid>\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b)" +
            // Branch 5 (NORMALIZE): Hexadecimal literals / memory addresses
            "|(?<hex>\\b0x[0-9a-fA-F]+\\b)" +
            // Branch 6 (NORMALIZE): All remaining generic digits (Order IDs, User IDs, ports, timestamps)
            "|(?<genericDigit>\\b\\d+\\b)"
    );

    /**
     * Calculates complete statistics for a given list of log entries.
     *
     * @param entries the parsed log entries
     * @return LogStatistics instance containing calculated metrics
     */
    public LogStatistics aggregate(List<LogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return new LogStatistics(0, Map.of(), null, null, Map.of(), Map.of(), Map.of(), 0.0, 0.0);
        }

        long totalLogs = entries.size();
        Map<LogLevel, Long> countsByLevel = new EnumMap<>(LogLevel.class);
        for (LogLevel lvl : LogLevel.values()) {
            countsByLevel.put(lvl, 0L);
        }

        LocalDateTime firstTimestamp = null;
        LocalDateTime lastTimestamp = null;
        Map<String, Long> errorFrequency = new LinkedHashMap<>();
        Map<String, Long> loggerFrequency = new LinkedHashMap<>();
        Map<String, Long> hourlyCounts = new LinkedHashMap<>();

        for (LogEntry entry : entries) {
            // Count levels
            countsByLevel.put(entry.getLevel(), countsByLevel.get(entry.getLevel()) + 1);

            // Timestamps
            LocalDateTime ts = entry.getTimestamp();
            if (ts != null) {
                if (firstTimestamp == null || ts.isBefore(firstTimestamp)) {
                    firstTimestamp = ts;
                }
                if (lastTimestamp == null || ts.isAfter(lastTimestamp)) {
                    lastTimestamp = ts;
                }

                String hourKey = ts.format(HOUR_FORMATTER);
                hourlyCounts.put(hourKey, hourlyCounts.getOrDefault(hourKey, 0L) + 1);
            }

            // Top loggers
            String logger = entry.getLogger();
            if (logger != null && !logger.isBlank()) {
                loggerFrequency.put(logger, loggerFrequency.getOrDefault(logger, 0L) + 1);
            }

            // Error normalization
            if (entry.getLevel().isError()) {
                String normalized = normalizeErrorMessage(entry.getMessage());
                errorFrequency.put(normalized, errorFrequency.getOrDefault(normalized, 0L) + 1);
            }
        }

        long errorCount = countsByLevel.getOrDefault(LogLevel.ERROR, 0L) + countsByLevel.getOrDefault(LogLevel.FATAL, 0L);
        long warnCount = countsByLevel.getOrDefault(LogLevel.WARN, 0L);

        double errorPercentage = totalLogs > 0 ? (errorCount * 100.0) / totalLogs : 0.0;
        double warningPercentage = totalLogs > 0 ? (warnCount * 100.0) / totalLogs : 0.0;

        // Top 10 errors
        Map<String, Long> topErrors = errorFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // Top 10 loggers
        Map<String, Long> topLoggers = loggerFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        return new LogStatistics(
                totalLogs,
                countsByLevel,
                firstTimestamp,
                lastTimestamp,
                topErrors,
                topLoggers,
                hourlyCounts,
                errorPercentage,
                warningPercentage
        );
    }

    /**
     * Normalizes error messages by replacing variable identifiers and numbers with placeholders,
     * while preserving contextual HTTP status codes (e.g. HTTP 401 Unauthorized, HTTP status 500,
     * status code: 503) and scrubbing complete IPv4 addresses as a single placeholder.
     */
    public String normalizeErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Unspecified error";
        }
        String firstLine = message.lines().findFirst().orElse(message).trim();
        Matcher matcher = NORMALIZATION_PATTERN.matcher(firstLine);
        return matcher.replaceAll(mr -> {
            if (mr.group("httpPrefix") != null || mr.group("httpSuffix") != null) {
                return mr.group();
            }
            if (mr.group("ipv4Candidate") != null) {
                String candidate = mr.group("ipv4Candidate");
                if (isValidIpv4(candidate)) {
                    return "{val}";
                }
                return candidate.replaceAll("\\d+", "{val}");
            }
            return "{val}";
        }).trim();
    }

    private static boolean isValidIpv4(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            try {
                int val = Integer.parseInt(octet);
                if (val < 0 || val > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
}
