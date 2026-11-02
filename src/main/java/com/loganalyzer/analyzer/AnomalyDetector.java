package com.loganalyzer.analyzer;

import com.loganalyzer.model.Anomaly;
import com.loganalyzer.model.AnomalyType;
import com.loganalyzer.model.LogEntry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Detects patterns, bursts, security threats, and resource exhaustion in log sequences.
 */
public class AnomalyDetector {

    private final int errorBurstThreshold;
    private final int windowSeconds;
    private final int identicalErrorThreshold;
    private final int securityThreshold;
    private final int databaseFailureThreshold;
    private final int timeoutThreshold;

    private static final Pattern SECURITY_PATTERN = Pattern.compile(
            "(?i)(unauthorized|forbidden|access denied|bad credentials|invalid token|jwt expired|auth(?:entication)? failure|login failed|brute[- ]?force|sqli|csrf)"
    );

    private static final Pattern RESOURCE_PATTERN = Pattern.compile(
            "(?i)(outofmemoryerror|java heap space|gc overhead limit|metaspace|unable to create native thread|too many open files|thread starvation)"
    );

    private static final Pattern DATABASE_PATTERN = Pattern.compile(
            "(?i)(sqlexception|connection pool (?:exhausted|timeout)|hikaripool|deadlock (?:found|detected)|communications link failure|cannotacquirelockexception)"
    );

    private static final Pattern TIMEOUT_PATTERN = Pattern.compile(
            "(?i)(sockettimeoutexception|read timed? out|connect(?:ion)? timed? out|gateway timeout|504 gateway|connection refused)"
    );

    public AnomalyDetector() {
        this(5, 60, 4, 3, 2, 3);
    }

    public AnomalyDetector(int errorBurstThreshold,
                           int windowSeconds,
                           int identicalErrorThreshold,
                           int securityThreshold,
                           int databaseFailureThreshold,
                           int timeoutThreshold) {
        this.errorBurstThreshold = errorBurstThreshold;
        this.windowSeconds = windowSeconds;
        this.identicalErrorThreshold = identicalErrorThreshold;
        this.securityThreshold = securityThreshold;
        this.databaseFailureThreshold = databaseFailureThreshold;
        this.timeoutThreshold = timeoutThreshold;
    }

    /**
     * Scans log entries and returns all detected anomalies.
     */
    public List<Anomaly> detectAnomalies(List<LogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Anomaly> anomalies = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(1);

        // 1. Error Bursts
        detectErrorBursts(entries, anomalies, counter);

        // 2. High Frequency Identical Errors
        detectIdenticalErrors(entries, anomalies, counter);

        // 3. Resource Exhaustion
        detectPatternAnomalies(
                entries,
                RESOURCE_PATTERN,
                AnomalyType.RESOURCE_EXHAUSTION,
                1,
                1.0,
                "Critical system resource exhaustion / memory failure detected",
                anomalies,
                counter
        );

        // 4. Security Anomalies
        detectPatternAnomalies(
                entries,
                SECURITY_PATTERN,
                AnomalyType.SECURITY_SUSPICIOUS_ACCESS,
                securityThreshold,
                0.85,
                "Elevated frequency of unauthorized or suspicious access attempts",
                anomalies,
                counter
        );

        // 5. Database Outage / Connection Pool Failures
        detectPatternAnomalies(
                entries,
                DATABASE_PATTERN,
                AnomalyType.DATABASE_FAILURE_SPIKE,
                databaseFailureThreshold,
                0.90,
                "Cascading database connection timeouts, pool exhaustion, or deadlocks",
                anomalies,
                counter
        );

        // 6. Service / Network Timeouts
        detectPatternAnomalies(
                entries,
                TIMEOUT_PATTERN,
                AnomalyType.SERVICE_TIMEOUT_SPIKE,
                timeoutThreshold,
                0.75,
                "Elevated volume of downstream network or socket timeouts",
                anomalies,
                counter
        );

        return anomalies;
    }

    private void detectErrorBursts(List<LogEntry> entries, List<Anomaly> anomalies, AtomicInteger counter) {
        List<LogEntry> errorEntries = entries.stream()
                .filter(e -> e.getLevel().isError())
                .toList();

        if (errorEntries.size() < errorBurstThreshold) {
            return;
        }

        boolean hasTimestamps = errorEntries.stream().anyMatch(e -> e.getTimestamp() != null);

        if (hasTimestamps) {
            int i = 0;
            while (i < errorEntries.size()) {
                LogEntry startEntry = errorEntries.get(i);
                LocalDateTime startTs = startEntry.getTimestamp();
                if (startTs == null) {
                    i++;
                    continue;
                }

                LocalDateTime endWindow = startTs.plusSeconds(windowSeconds);
                List<LogEntry> windowErrors = new ArrayList<>();

                int j = i;
                while (j < errorEntries.size()) {
                    LogEntry curr = errorEntries.get(j);
                    if (curr.getTimestamp() != null && !curr.getTimestamp().isAfter(endWindow)) {
                        windowErrors.add(curr);
                        j++;
                    } else {
                        break;
                    }
                }

                if (windowErrors.size() >= errorBurstThreshold) {
                    LocalDateTime actualEnd = windowErrors.get(windowErrors.size() - 1).getTimestamp();
                    double score = Math.min(1.0, 0.5 + (windowErrors.size() / 20.0));
                    anomalies.add(new Anomaly(
                            "ANOM-" + counter.getAndIncrement(),
                            AnomalyType.ERROR_BURST,
                            String.format("Error burst: %d errors within %d seconds (threshold: %d)",
                                    windowErrors.size(), windowSeconds, errorBurstThreshold),
                            startTs,
                            actualEnd,
                            windowErrors.size(),
                            windowErrors.subList(0, Math.min(5, windowErrors.size())),
                            score
                    ));
                    i = j; // Advance past this burst window
                } else {
                    i++;
                }
            }
        } else {
            // Sequence-based burst fallback when timestamps are absent
            if (errorEntries.size() >= errorBurstThreshold) {
                anomalies.add(new Anomaly(
                        "ANOM-" + counter.getAndIncrement(),
                        AnomalyType.ERROR_BURST,
                        String.format("Sequential error concentration: %d error logs detected in sequence", errorEntries.size()),
                        null,
                        null,
                        errorEntries.size(),
                        errorEntries.subList(0, Math.min(5, errorEntries.size())),
                        0.75
                ));
            }
        }
    }

    private void detectIdenticalErrors(List<LogEntry> entries, List<Anomaly> anomalies, AtomicInteger counter) {
        StatisticsAggregator aggregator = new StatisticsAggregator();
        Map<String, List<LogEntry>> errorGroups = new LinkedHashMap<>();

        for (LogEntry entry : entries) {
            if (entry.getLevel().isError()) {
                String normalized = aggregator.normalizeErrorMessage(entry.getMessage());
                errorGroups.computeIfAbsent(normalized, k -> new ArrayList<>()).add(entry);
            }
        }

        for (Map.Entry<String, List<LogEntry>> group : errorGroups.entrySet()) {
            if (group.getValue().size() >= identicalErrorThreshold) {
                List<LogEntry> matches = group.getValue();
                LocalDateTime start = matches.get(0).getTimestamp();
                LocalDateTime end = matches.get(matches.size() - 1).getTimestamp();
                double score = Math.min(1.0, 0.6 + (matches.size() / 25.0));

                anomalies.add(new Anomaly(
                        "ANOM-" + counter.getAndIncrement(),
                        AnomalyType.HIGH_FREQUENCY_IDENTICAL_ERRORS,
                        String.format("Repetitive identical failure (%d occurrences): \"%s\"",
                                matches.size(), group.getKey()),
                        start,
                        end,
                        matches.size(),
                        matches.subList(0, Math.min(3, matches.size())),
                        score
                ));
            }
        }
    }

    private void detectPatternAnomalies(List<LogEntry> entries,
                                        Pattern pattern,
                                        AnomalyType anomalyType,
                                        int threshold,
                                        double baseScore,
                                        String description,
                                        List<Anomaly> anomalies,
                                        AtomicInteger counter) {
        List<LogEntry> matched = new ArrayList<>();
        for (LogEntry entry : entries) {
            String combined = entry.getCombinedMessage();
            if (pattern.matcher(combined).find()) {
                matched.add(entry);
            }
        }

        if (matched.size() >= threshold) {
            LocalDateTime start = matched.get(0).getTimestamp();
            LocalDateTime end = matched.get(matched.size() - 1).getTimestamp();
            double score = Math.min(1.0, baseScore + (matched.size() * 0.02));

            anomalies.add(new Anomaly(
                    "ANOM-" + counter.getAndIncrement(),
                    anomalyType,
                    String.format("%s (Found %d matching occurrences)", description, matched.size()),
                    start,
                    end,
                    matched.size(),
                    matched.subList(0, Math.min(5, matched.size())),
                    score
            ));
        }
    }
}
