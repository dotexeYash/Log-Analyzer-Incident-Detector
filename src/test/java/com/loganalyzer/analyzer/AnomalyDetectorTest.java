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
}
