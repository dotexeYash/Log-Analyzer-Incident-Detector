package com.loganalyzer.analyzer;

import com.loganalyzer.model.Anomaly;
import com.loganalyzer.model.AnomalyType;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnomalyDetectorTest {

    private AnomalyDetector detector;

    @BeforeEach
    void setUp() {
        detector = new AnomalyDetector(5, 60, 4, 3, 2, 3);
    }

    @Test
    @DisplayName("Should detect error burst anomaly when errors exceed threshold within window")
    void testDetectErrorBurst() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 3, 1, 0, 0);
        List<LogEntry> entries = new ArrayList<>();

        // 6 errors within 30 seconds (exceeding burst threshold 5 within 60s)
        for (int i = 0; i < 6; i++) {
            entries.add(LogEntry.builder()
                    .lineNumber(i + 1)
                    .timestamp(start.plusSeconds(i * 5))
                    .level(LogLevel.ERROR)
                    .message("Internal server error " + i)
                    .build());
        }

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        boolean hasBurst = anomalies.stream().anyMatch(a -> a.getType() == AnomalyType.ERROR_BURST);
        assertTrue(hasBurst, "Should have detected ERROR_BURST anomaly");
    }

    @Test
    @DisplayName("Should detect resource exhaustion anomaly on OutOfMemoryError")
    void testDetectResourceExhaustion() {
        LogEntry oomEntry = LogEntry.builder()
                .lineNumber(1)
                .timestamp(LocalDateTime.now())
                .level(LogLevel.ERROR)
                .message("Fatal: java.lang.OutOfMemoryError: Java heap space")
                .build();

        List<Anomaly> anomalies = detector.detectAnomalies(List.of(oomEntry));
        boolean hasResourceExhaustion = anomalies.stream()
                .anyMatch(a -> a.getType() == AnomalyType.RESOURCE_EXHAUSTION);
        assertTrue(hasResourceExhaustion, "Should detect RESOURCE_EXHAUSTION anomaly");
    }

    @Test
    @DisplayName("Should detect security anomaly when unauthorized access attempts exceed threshold")
    void testDetectSecurityAnomaly() {
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            entries.add(LogEntry.builder()
                    .lineNumber(i + 1)
                    .timestamp(LocalDateTime.now().plusSeconds(i))
                    .level(LogLevel.WARN)
                    .message("401 Unauthorized: Invalid credentials from IP 192.168.1.10" + i)
                    .build());
        }

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        boolean hasSecurityAnomaly = anomalies.stream()
                .anyMatch(a -> a.getType() == AnomalyType.SECURITY_SUSPICIOUS_ACCESS);
        assertTrue(hasSecurityAnomaly, "Should detect SECURITY_SUSPICIOUS_ACCESS anomaly");
    }

    @Test
    @DisplayName("INFO messages containing 'unauthorized' must not trigger a security anomaly")
    void testInfoMessagesContainingUnauthorizedDoNotTriggerSecurityAnomaly() {
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            entries.add(LogEntry.builder()
                    .lineNumber(i + 1)
                    .timestamp(LocalDateTime.now().plusSeconds(i))
                    .level(LogLevel.INFO)
                    .message("User requested unauthorized resource, redirected to login page")
                    .build());
        }

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        boolean hasSecurityAnomaly = anomalies.stream()
                .anyMatch(a -> a.getType() == AnomalyType.SECURITY_SUSPICIOUS_ACCESS);
        assertFalse(hasSecurityAnomaly, "INFO messages containing 'unauthorized' must not be flagged as security anomalies");
    }

    @Test
    @DisplayName("DEBUG messages containing 'forbidden' must not trigger a security anomaly")
    void testDebugMessagesContainingForbiddenDoNotTriggerSecurityAnomaly() {
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            entries.add(LogEntry.builder()
                    .lineNumber(i + 1)
                    .timestamp(LocalDateTime.now().plusSeconds(i))
                    .level(LogLevel.DEBUG)
                    .message("Evaluating security filter: path /public is forbidden=false")
                    .build());
        }

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        boolean hasSecurityAnomaly = anomalies.stream()
                .anyMatch(a -> a.getType() == AnomalyType.SECURITY_SUSPICIOUS_ACCESS);
        assertFalse(hasSecurityAnomaly, "DEBUG messages containing 'forbidden' must not be flagged as security anomalies");
    }

    @Test
    @DisplayName("ERROR messages containing suspicious authentication activity should be detected")
    void testErrorMessagesContainingSuspiciousAuthActivityTriggerSecurityAnomaly() {
        List<LogEntry> entries = List.of(
                LogEntry.builder().lineNumber(1).level(LogLevel.ERROR).message("401 Unauthorized: Brute-force attack pattern detected from IP 10.0.0.1").build(),
                LogEntry.builder().lineNumber(2).level(LogLevel.ERROR).message("Access Denied: Unauthorized access attempt to /api/v1/internal/admin-keys").build(),
                LogEntry.builder().lineNumber(3).level(LogLevel.ERROR).message("403 Forbidden: Invalid authentication token signature").build()
        );

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        boolean hasSecurityAnomaly = anomalies.stream()
                .anyMatch(a -> a.getType() == AnomalyType.SECURITY_SUSPICIOUS_ACCESS);
        assertTrue(hasSecurityAnomaly, "ERROR logs containing security attack signatures must be detected");
    }

    @Test
    @DisplayName("Should detect database failure spike when DB errors exceed threshold")
    void testDetectDatabaseSpike() {
        List<LogEntry> entries = List.of(
                LogEntry.builder().lineNumber(1).level(LogLevel.ERROR).message("SQLException: HikariPool connection timeout").build(),
                LogEntry.builder().lineNumber(2).level(LogLevel.ERROR).message("CannotAcquireLockException: Deadlock detected").build()
        );

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        boolean hasDbAnomaly = anomalies.stream()
                .anyMatch(a -> a.getType() == AnomalyType.DATABASE_FAILURE_SPIKE);
        assertTrue(hasDbAnomaly, "Should detect DATABASE_FAILURE_SPIKE anomaly");
    }

    @Test
    @DisplayName("Should return no anomalies for healthy standard logs")
    void testNoAnomaliesOnHealthyLogs() {
        List<LogEntry> entries = List.of(
                LogEntry.builder().lineNumber(1).level(LogLevel.INFO).message("App started").build(),
                LogEntry.builder().lineNumber(2).level(LogLevel.DEBUG).message("Processing order").build(),
                LogEntry.builder().lineNumber(3).level(LogLevel.INFO).message("Health check OK").build()
        );

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        assertTrue(anomalies.isEmpty(), "Healthy logs should not produce any anomalies");
    }

    @Test
    @DisplayName("Overlapping qualifying error windows should be merged into a single anomaly")
    void testOverlappingErrorBurstsMergedIntoSingleAnomaly() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 5, 1, 0, 0);
        int[] offsets = {0, 10, 20, 30, 40, 50, 65, 70, 75, 80};
        List<LogEntry> entries = new ArrayList<>();

        for (int i = 0; i < offsets.length; i++) {
            entries.add(LogEntry.builder()
                    .lineNumber(i + 1)
                    .timestamp(baseTime.plusSeconds(offsets[i]))
                    .level(LogLevel.ERROR)
                    .message("Cascading error #" + (i + 1))
                    .build());
        }

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        List<Anomaly> bursts = anomalies.stream()
                .filter(a -> a.getType() == AnomalyType.ERROR_BURST)
                .toList();

        assertEquals(1, bursts.size(), "Overlapping burst windows must be merged into exactly one anomaly");
        Anomaly merged = bursts.get(0);
        assertEquals(10, merged.getAffectedLogCount(), "Merged burst must contain all 10 affected errors");
        assertEquals(baseTime, merged.getStartTime(), "Merged burst should start at the first error timestamp");
        assertEquals(baseTime.plusSeconds(80), merged.getEndTime(), "Merged burst should end at the last error timestamp");
    }

    @Test
    @DisplayName("Separate error bursts with a large gap must remain distinct anomalies")
    void testSeparateBurstsWithLargeGapRemainDistinct() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 5, 1, 0, 0);
        List<LogEntry> entries = new ArrayList<>();

        // Burst 1: 5 errors across 0s..40s
        int[] burst1Offsets = {0, 10, 20, 30, 40};
        for (int i = 0; i < burst1Offsets.length; i++) {
            entries.add(LogEntry.builder()
                    .lineNumber(i + 1)
                    .timestamp(baseTime.plusSeconds(burst1Offsets[i]))
                    .level(LogLevel.ERROR)
                    .message("Burst 1 Error #" + (i + 1))
                    .build());
        }

        // 10 minute gap (600s), then Burst 2: 5 errors across 640s..680s
        int[] burst2Offsets = {640, 650, 660, 670, 680};
        for (int i = 0; i < burst2Offsets.length; i++) {
            entries.add(LogEntry.builder()
                    .lineNumber(burst1Offsets.length + i + 1)
                    .timestamp(baseTime.plusSeconds(burst2Offsets[i]))
                    .level(LogLevel.ERROR)
                    .message("Burst 2 Error #" + (i + 1))
                    .build());
        }

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        List<Anomaly> bursts = anomalies.stream()
                .filter(a -> a.getType() == AnomalyType.ERROR_BURST)
                .toList();

        assertEquals(2, bursts.size(), "Two distinct bursts separated by a gap must produce exactly 2 anomalies");
        assertEquals(5, bursts.get(0).getAffectedLogCount(), "First burst should contain 5 errors");
        assertEquals(baseTime, bursts.get(0).getStartTime());
        assertEquals(baseTime.plusSeconds(40), bursts.get(0).getEndTime());

        assertEquals(5, bursts.get(1).getAffectedLogCount(), "Second burst should contain 5 errors");
        assertEquals(baseTime.plusSeconds(640), bursts.get(1).getStartTime());
        assertEquals(baseTime.plusSeconds(680), bursts.get(1).getEndTime());
    }

    @Test
    @DisplayName("Unsorted timestamps must be sorted locally and detected accurately")
    void testUnsortedTimestampsHandledCorrectly() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 5, 1, 0, 0);
        // Scrambled offsets: 40s, 10s, 30s, 0s, 20s
        int[] scrambledOffsets = {40, 10, 30, 0, 20};
        List<LogEntry> entries = new ArrayList<>();

        for (int i = 0; i < scrambledOffsets.length; i++) {
            entries.add(LogEntry.builder()
                    .lineNumber(i + 1)
                    .timestamp(baseTime.plusSeconds(scrambledOffsets[i]))
                    .level(LogLevel.ERROR)
                    .message("Unsorted error at offset " + scrambledOffsets[i])
                    .build());
        }

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        List<Anomaly> bursts = anomalies.stream()
                .filter(a -> a.getType() == AnomalyType.ERROR_BURST)
                .toList();

        assertEquals(1, bursts.size(), "Unsorted errors within window must be sorted and detected as 1 burst");
        assertEquals(5, bursts.get(0).getAffectedLogCount());
        assertEquals(baseTime, bursts.get(0).getStartTime(), "Start time should be the earliest chronological error (offset 0)");
        assertEquals(baseTime.plusSeconds(40), bursts.get(0).getEndTime(), "End time should be the latest chronological error (offset 40)");
    }

    @Test
    @DisplayName("Original input list order must remain completely unchanged")
    void testOriginalInputListOrderRemainsUnchanged() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 5, 1, 0, 0);
        List<LogEntry> originalList = new ArrayList<>(List.of(
                LogEntry.builder().lineNumber(1).timestamp(baseTime.plusSeconds(50)).level(LogLevel.ERROR).message("First in file").build(),
                LogEntry.builder().lineNumber(2).timestamp(baseTime.plusSeconds(10)).level(LogLevel.ERROR).message("Second in file").build(),
                LogEntry.builder().lineNumber(3).timestamp(baseTime.plusSeconds(30)).level(LogLevel.ERROR).message("Third in file").build(),
                LogEntry.builder().lineNumber(4).timestamp(baseTime.plusSeconds(0)).level(LogLevel.ERROR).message("Fourth in file").build(),
                LogEntry.builder().lineNumber(5).timestamp(baseTime.plusSeconds(20)).level(LogLevel.ERROR).message("Fifth in file").build()
        ));

        // Detect anomalies
        detector.detectAnomalies(originalList);

        // Verify that the input list was not mutated or reordered
        assertEquals(1, originalList.get(0).getLineNumber());
        assertEquals(50, java.time.Duration.between(baseTime, originalList.get(0).getTimestamp()).getSeconds());

        assertEquals(2, originalList.get(1).getLineNumber());
        assertEquals(10, java.time.Duration.between(baseTime, originalList.get(1).getTimestamp()).getSeconds());

        assertEquals(3, originalList.get(2).getLineNumber());
        assertEquals(30, java.time.Duration.between(baseTime, originalList.get(2).getTimestamp()).getSeconds());

        assertEquals(4, originalList.get(3).getLineNumber());
        assertEquals(0, java.time.Duration.between(baseTime, originalList.get(3).getTimestamp()).getSeconds());

        assertEquals(5, originalList.get(4).getLineNumber());
        assertEquals(20, java.time.Duration.between(baseTime, originalList.get(4).getTimestamp()).getSeconds());
    }

    @Test
    @DisplayName("Null timestamp fallback should detect sequential error concentration")
    void testNullTimestampSequenceFallback() {
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            entries.add(LogEntry.builder()
                    .lineNumber(i + 1)
                    .timestamp(null)
                    .level(LogLevel.ERROR)
                    .message("Error with no timestamp #" + (i + 1))
                    .build());
        }

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        List<Anomaly> bursts = anomalies.stream()
                .filter(a -> a.getType() == AnomalyType.ERROR_BURST)
                .toList();

        assertEquals(1, bursts.size(), "Should trigger sequence fallback when all timestamps are null");
        assertTrue(bursts.get(0).getDescription().contains("Sequential error concentration"));
        assertNull(bursts.get(0).getStartTime());
        assertNull(bursts.get(0).getEndTime());
        assertEquals(6, bursts.get(0).getAffectedLogCount());
    }

    @Test
    @DisplayName("Four errors below threshold of five must produce no burst anomaly")
    void testFourErrorsBelowThresholdProduceNoBurstAnomaly() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 5, 1, 0, 0);
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            entries.add(LogEntry.builder()
                    .lineNumber(i + 1)
                    .timestamp(baseTime.plusSeconds(i * 5))
                    .level(LogLevel.ERROR)
                    .message("Isolated error " + i)
                    .build());
        }

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        boolean hasBurst = anomalies.stream()
                .anyMatch(a -> a.getType() == AnomalyType.ERROR_BURST);
        assertFalse(hasBurst, "4 errors below threshold of 5 must produce no burst anomaly");
    }

    @Test
    @DisplayName("Five errors spanning 60.5 seconds must be rejected when window is 60 seconds")
    void testFiveErrorsSpanningSixtyPointFiveSecondsProducesNoBurstAnomaly() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 5, 1, 0, 0);
        List<LogEntry> entries = List.of(
                LogEntry.builder().lineNumber(1).timestamp(baseTime).level(LogLevel.ERROR).message("Error 1").build(),
                LogEntry.builder().lineNumber(2).timestamp(baseTime.plusSeconds(15)).level(LogLevel.ERROR).message("Error 2").build(),
                LogEntry.builder().lineNumber(3).timestamp(baseTime.plusSeconds(30)).level(LogLevel.ERROR).message("Error 3").build(),
                LogEntry.builder().lineNumber(4).timestamp(baseTime.plusSeconds(45)).level(LogLevel.ERROR).message("Error 4").build(),
                LogEntry.builder().lineNumber(5).timestamp(baseTime.plusSeconds(60).plusNanos(500_000_000)).level(LogLevel.ERROR).message("Error 5").build()
        );

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        boolean hasBurst = anomalies.stream()
                .anyMatch(a -> a.getType() == AnomalyType.ERROR_BURST);
        assertFalse(hasBurst, "Five errors spanning 60.5s must exceed 60s window and produce no burst anomaly");
    }

    @Test
    @DisplayName("Five errors spanning exactly 60.0 seconds must be accepted as an inclusive burst")
    void testFiveErrorsSpanningExactlySixtySecondsProducesBurstAnomaly() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 5, 1, 0, 0);
        List<LogEntry> entries = List.of(
                LogEntry.builder().lineNumber(1).timestamp(baseTime).level(LogLevel.ERROR).message("Error 1").build(),
                LogEntry.builder().lineNumber(2).timestamp(baseTime.plusSeconds(15)).level(LogLevel.ERROR).message("Error 2").build(),
                LogEntry.builder().lineNumber(3).timestamp(baseTime.plusSeconds(30)).level(LogLevel.ERROR).message("Error 3").build(),
                LogEntry.builder().lineNumber(4).timestamp(baseTime.plusSeconds(45)).level(LogLevel.ERROR).message("Error 4").build(),
                LogEntry.builder().lineNumber(5).timestamp(baseTime.plusSeconds(60)).level(LogLevel.ERROR).message("Error 5").build()
        );

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        List<Anomaly> bursts = anomalies.stream()
                .filter(a -> a.getType() == AnomalyType.ERROR_BURST)
                .toList();

        assertEquals(1, bursts.size(), "Five errors spanning exactly 60.0s must produce exactly 1 burst anomaly");
        assertEquals(5, bursts.get(0).getAffectedLogCount());
        assertEquals(baseTime, bursts.get(0).getStartTime());
        assertEquals(baseTime.plusSeconds(60), bursts.get(0).getEndTime());
    }

    @Test
    @DisplayName("Heterogeneous HTTP status codes sharing message phrasing must not trigger identical error anomaly")
    void testHeterogeneousHttpStatusCodesDoNotTriggerIdenticalErrorAnomaly() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 6, 1, 0, 0);
        List<LogEntry> entries = List.of(
                LogEntry.builder().lineNumber(1).timestamp(base).level(LogLevel.ERROR)
                        .message("Call to /api/checkout failed with HTTP status 400").build(),
                LogEntry.builder().lineNumber(2).timestamp(base.plusSeconds(5)).level(LogLevel.ERROR)
                        .message("Call to /api/checkout failed with HTTP status 401").build(),
                LogEntry.builder().lineNumber(3).timestamp(base.plusSeconds(10)).level(LogLevel.ERROR)
                        .message("Call to /api/checkout failed with HTTP status 403").build(),
                LogEntry.builder().lineNumber(4).timestamp(base.plusSeconds(15)).level(LogLevel.ERROR)
                        .message("Call to /api/checkout failed with HTTP status 500").build()
        );

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        boolean hasIdenticalErrorAnomaly = anomalies.stream()
                .anyMatch(a -> a.getType() == AnomalyType.HIGH_FREQUENCY_IDENTICAL_ERRORS);
        assertFalse(hasIdenticalErrorAnomaly,
                "Different HTTP status codes (400, 401, 403, 500) must not be grouped into an identical error anomaly");
    }

    @Test
    @DisplayName("Homogeneous error logs with identical HTTP status code must trigger identical error anomaly")
    void testHomogeneousSameHttpStatusCodeStillTriggersIdenticalErrorAnomaly() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 6, 1, 0, 0);
        List<LogEntry> entries = List.of(
                LogEntry.builder().lineNumber(1).timestamp(base).level(LogLevel.ERROR)
                        .message("Call to /api/checkout failed with HTTP status 503").build(),
                LogEntry.builder().lineNumber(2).timestamp(base.plusSeconds(5)).level(LogLevel.ERROR)
                        .message("Call to /api/checkout failed with HTTP status 503").build(),
                LogEntry.builder().lineNumber(3).timestamp(base.plusSeconds(10)).level(LogLevel.ERROR)
                        .message("Call to /api/checkout failed with HTTP status 503").build(),
                LogEntry.builder().lineNumber(4).timestamp(base.plusSeconds(15)).level(LogLevel.ERROR)
                        .message("Call to /api/checkout failed with HTTP status 503").build()
        );

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        List<Anomaly> identicalAnomalies = anomalies.stream()
                .filter(a -> a.getType() == AnomalyType.HIGH_FREQUENCY_IDENTICAL_ERRORS)
                .toList();

        assertEquals(1, identicalAnomalies.size(),
                "Four identical 503 errors must trigger exactly one HIGH_FREQUENCY_IDENTICAL_ERRORS anomaly");
        assertEquals(4, identicalAnomalies.get(0).getAffectedLogCount());
        assertTrue(identicalAnomalies.get(0).getDescription().contains("HTTP status 503"));
    }

    @Test
    @DisplayName("Security logs with preserved HTTP status codes and scrubbed IPv4 addresses detect security anomalies properly")
    void testSecurityLogsWithPreservedHttpStatusCodes() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 6, 1, 0, 0);
        List<LogEntry> entries = List.of(
                LogEntry.builder().lineNumber(1).timestamp(base).level(LogLevel.ERROR)
                        .message("401 Unauthorized: Brute-force attack pattern detected from source IP 198.51.100.42").build(),
                LogEntry.builder().lineNumber(2).timestamp(base.plusSeconds(1)).level(LogLevel.ERROR)
                        .message("Access Denied: Unauthorized access attempt to /api/v1/internal/admin-keys").build(),
                LogEntry.builder().lineNumber(3).timestamp(base.plusSeconds(2)).level(LogLevel.ERROR)
                        .message("403 Forbidden: Invalid authentication token signature from IP 198.51.100.42").build()
        );

        List<Anomaly> anomalies = detector.detectAnomalies(entries);
        boolean hasSecurityAnomaly = anomalies.stream()
                .anyMatch(a -> a.getType() == AnomalyType.SECURITY_SUSPICIOUS_ACCESS);
        assertTrue(hasSecurityAnomaly, "Should detect SECURITY_SUSPICIOUS_ACCESS on security logs");
    }
}
