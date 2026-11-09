package com.loganalyzer.analyzer;

import com.loganalyzer.model.Anomaly;
import com.loganalyzer.model.AnomalyType;
import com.loganalyzer.model.Incident;
import com.loganalyzer.model.IncidentCategory;
import com.loganalyzer.model.IncidentSeverity;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncidentDetectorTest {

    private IncidentDetector detector;

    @BeforeEach
    void setUp() {
        detector = new IncidentDetector();
    }

    @Test
    @DisplayName("Should detect CRITICAL incident with remediation steps for memory failure")
    void testMemoryIncidentDetection() {
        Anomaly memoryAnomaly = new Anomaly(
                "ANOM-1",
                AnomalyType.RESOURCE_EXHAUSTION,
                "Out of memory detected",
                LocalDateTime.now(),
                LocalDateTime.now(),
                2,
                List.of(LogEntry.builder().lineNumber(1).level(LogLevel.ERROR).message("java.lang.OutOfMemoryError").build()),
                1.0
        );

        List<Incident> incidents = detector.detectIncidents(List.of(memoryAnomaly));

        assertEquals(1, incidents.size());
        Incident incident = incidents.get(0);
        assertEquals(IncidentSeverity.CRITICAL, incident.getSeverity());
        assertEquals(IncidentCategory.MEMORY_CRITICAL_FAILURE, incident.getCategory());
        assertFalse(incident.getRecommendations().isEmpty());
        assertTrue(incident.getRecommendations().stream().anyMatch(r -> r.contains("-Xmx") || r.contains("heap dump")));
    }

    @Test
    @DisplayName("Should detect CRITICAL incident for database failure spike")
    void testDatabaseIncidentDetection() {
        Anomaly dbAnomaly = new Anomaly(
                "ANOM-2",
                AnomalyType.DATABASE_FAILURE_SPIKE,
                "Database connection pool exhausted",
                LocalDateTime.now(),
                LocalDateTime.now(),
                5,
                List.of(LogEntry.builder().lineNumber(10).level(LogLevel.ERROR).message("HikariPool connection timeout").build()),
                0.95
        );

        List<Incident> incidents = detector.detectIncidents(List.of(dbAnomaly));

        assertEquals(1, incidents.size());
        Incident incident = incidents.get(0);
        assertEquals(IncidentSeverity.CRITICAL, incident.getSeverity());
        assertEquals(IncidentCategory.DATABASE_OUTAGE, incident.getCategory());
        assertTrue(incident.getRecommendations().stream().anyMatch(r -> r.contains("connection pool") || r.contains("database")));
    }

    @Test
    @DisplayName("Should prioritize severity correctly")
    void testSeverityComparison() {
        assertTrue(IncidentSeverity.CRITICAL.isHigherThan(IncidentSeverity.HIGH));
        assertTrue(IncidentSeverity.HIGH.isHigherThan(IncidentSeverity.MEDIUM));
        assertTrue(IncidentSeverity.MEDIUM.isHigherThan(IncidentSeverity.LOW));
        assertFalse(IncidentSeverity.LOW.isHigherThan(IncidentSeverity.HIGH));
    }

    @Test
    @DisplayName("Should detect HIGH incident with remediation steps for security breach attempt")
    void testSecurityIncidentDetection() {
        LogEntry sample = LogEntry.builder()
                .lineNumber(15)
                .level(LogLevel.WARN)
                .message("401 Unauthorized: Failed login attempt for admin")
                .build();

        Anomaly secAnomaly = new Anomaly(
                "ANOM-SEC-1",
                AnomalyType.SECURITY_SUSPICIOUS_ACCESS,
                "Suspicious unauthorized access surge",
                LocalDateTime.of(2026, 11, 9, 10, 0, 0),
                LocalDateTime.of(2026, 11, 9, 10, 5, 0),
                6,
                List.of(sample),
                0.85
        );

        List<Incident> incidents = detector.detectIncidents(List.of(secAnomaly));

        assertEquals(1, incidents.size(), "Should produce exactly 1 security incident");
        Incident incident = incidents.get(0);
        assertEquals("INC-001", incident.getId());
        assertEquals(IncidentSeverity.HIGH, incident.getSeverity());
        assertEquals(IncidentCategory.SECURITY_BREACH_ATTEMPT, incident.getCategory());
        assertTrue(incident.getTitle().contains("Authentication") || incident.getTitle().contains("Unauthorized"));
        assertFalse(incident.getRecommendations().isEmpty());
        assertTrue(incident.getRecommendations().stream().anyMatch(r -> r.contains("source IP") || r.contains("MFA")));
        assertEquals(1, incident.getRootCauseEntries().size());
        assertEquals(sample, incident.getRootCauseEntries().get(0));
    }

    @Test
    @DisplayName("Should detect HIGH incident with remediation steps for downstream network/timeout degradation")
    void testNetworkDegradationIncidentDetection() {
        LogEntry sample = LogEntry.builder()
                .lineNumber(25)
                .level(LogLevel.ERROR)
                .message("java.net.SocketTimeoutException: Read timed out calling PaymentGateway")
                .build();

        Anomaly timeoutAnomaly = new Anomaly(
                "ANOM-NET-1",
                AnomalyType.SERVICE_TIMEOUT_SPIKE,
                "Cascading service timeouts detected",
                LocalDateTime.of(2026, 11, 9, 10, 10, 0),
                LocalDateTime.of(2026, 11, 9, 10, 15, 0),
                8,
                List.of(sample),
                0.75
        );

        List<Incident> incidents = detector.detectIncidents(List.of(timeoutAnomaly));

        assertEquals(1, incidents.size(), "Should produce exactly 1 network degradation incident");
        Incident incident = incidents.get(0);
        assertEquals("INC-001", incident.getId());
        assertEquals(IncidentSeverity.HIGH, incident.getSeverity());
        assertEquals(IncidentCategory.NETWORK_SERVICE_DEGRADATION, incident.getCategory());
        assertTrue(incident.getTitle().contains("Service Unavailability") || incident.getTitle().contains("Network Timeout"));
        assertFalse(incident.getRecommendations().isEmpty());
        assertTrue(incident.getRecommendations().stream().anyMatch(r -> r.contains("circuit-breaker") || r.contains("timeout")));
        assertEquals(1, incident.getRootCauseEntries().size());
    }

    @Test
    @DisplayName("Should detect HIGH incident for standalone error burst (APPLICATION_CRASH)")
    void testApplicationCrashBurstIncidentDetection() {
        LogEntry sample = LogEntry.builder()
                .lineNumber(30)
                .level(LogLevel.ERROR)
                .message("Unhandled NullPointerException in RequestDispatcher")
                .build();

        Anomaly burstAnomaly = new Anomaly(
                "ANOM-BURST-1",
                AnomalyType.ERROR_BURST,
                "Error burst: 15 errors within 60 seconds",
                LocalDateTime.of(2026, 11, 9, 10, 20, 0),
                LocalDateTime.of(2026, 11, 9, 10, 21, 0),
                15,
                List.of(sample),
                0.90
        );

        List<Incident> incidents = detector.detectIncidents(List.of(burstAnomaly));

        assertEquals(1, incidents.size(), "Should produce exactly 1 application crash incident");
        Incident incident = incidents.get(0);
        assertEquals("INC-001", incident.getId());
        assertEquals(IncidentSeverity.HIGH, incident.getSeverity());
        assertEquals(IncidentCategory.APPLICATION_CRASH, incident.getCategory());
        assertTrue(incident.getTitle().contains("Error Spike") || incident.getTitle().contains("Application Instability"));
        assertFalse(incident.getRecommendations().isEmpty());
        assertTrue(incident.getRecommendations().stream().anyMatch(r -> r.contains("recurring error traces") || r.contains("Rollback")));
    }

    @Test
    @DisplayName("Should detect MEDIUM incident for standalone repetitive identical failures (GENERAL_INCIDENT)")
    void testGeneralIncidentRepetitiveErrorDetection() {
        LogEntry sample = LogEntry.builder()
                .lineNumber(40)
                .level(LogLevel.ERROR)
                .message("Repetitive failure: Connection reset by peer")
                .build();

        Anomaly repetitiveAnomaly = new Anomaly(
                "ANOM-REP-1",
                AnomalyType.HIGH_FREQUENCY_IDENTICAL_ERRORS,
                "Repetitive identical failure (10 occurrences)",
                LocalDateTime.of(2026, 11, 9, 10, 30, 0),
                LocalDateTime.of(2026, 11, 9, 10, 35, 0),
                10,
                List.of(sample),
                0.70
        );

        List<Incident> incidents = detector.detectIncidents(List.of(repetitiveAnomaly));

        assertEquals(1, incidents.size(), "Should produce exactly 1 general recurring incident");
        Incident incident = incidents.get(0);
        assertEquals("INC-001", incident.getId());
        assertEquals(IncidentSeverity.MEDIUM, incident.getSeverity());
        assertEquals(IncidentCategory.GENERAL_INCIDENT, incident.getCategory());
        assertTrue(incident.getTitle().contains("Recurring Component Failure"));
        assertFalse(incident.getRecommendations().isEmpty());
        assertTrue(incident.getRecommendations().stream().anyMatch(r -> r.contains("stack trace") || r.contains("tight loops")));
    }

    @Test
    @DisplayName("Should handle empty and null anomaly inputs safely without exception")
    void testEmptyAndNullAnomaliesHandling() {
        // Empty list (primary 1-arg method)
        List<Incident> emptyIncidents = detector.detectIncidents(List.of());
        assertNotNull(emptyIncidents);
        assertTrue(emptyIncidents.isEmpty(), "Empty anomalies list must produce empty incidents list");

        // Null anomalies (primary 1-arg method)
        List<Incident> nullIncidents = detector.detectIncidents(null);
        assertNotNull(nullIncidents);
        assertTrue(nullIncidents.isEmpty(), "Null anomalies must produce empty incidents list");

        // Backward compatibility: 2-arg method with null entries
        List<Incident> nullEntriesResult = detector.detectIncidents(List.of(), null);
        assertNotNull(nullEntriesResult);
        assertTrue(nullEntriesResult.isEmpty());

        // Backward compatibility: 2-arg method with legacy entries list
        List<Incident> legacyResult = detector.detectIncidents(List.of(), List.of());
        assertNotNull(legacyResult);
        assertTrue(legacyResult.isEmpty());
    }

    @Test
    @DisplayName("Phase 3 Task 5: 1-arg detectIncidents and 2-arg legacy overload produce identical results")
    void testSingleArgAndOverloadConsistency() {
        Anomaly anomaly = new Anomaly(
                "A1", AnomalyType.RESOURCE_EXHAUSTION, "OOM",
                LocalDateTime.now(), LocalDateTime.now(), 3, List.of(), 1.0
        );

        List<Incident> res1 = detector.detectIncidents(List.of(anomaly));
        List<Incident> res2 = detector.detectIncidents(List.of(anomaly), List.of());

        assertEquals(1, res1.size());
        assertEquals(1, res2.size());
        assertEquals(res1.get(0).getTitle(), res2.get(0).getTitle());
    }

    @Test
    @DisplayName("Should correctly synthesize multiple distinct anomalies into prioritized incidents")
    void testMultipleAnomaliesCoexistenceAndOrdering() {
        LocalDateTime t1 = LocalDateTime.of(2026, 11, 9, 9, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 11, 9, 9, 5, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 11, 9, 9, 10, 0);

        Anomaly memoryAnomaly = new Anomaly(
                "A1", AnomalyType.RESOURCE_EXHAUSTION, "OOM",
                t1, t1.plusMinutes(1), 3, List.of(), 1.0
        );
        Anomaly secAnomaly = new Anomaly(
                "A2", AnomalyType.SECURITY_SUSPICIOUS_ACCESS, "Brute force",
                t2, t2.plusMinutes(2), 5, List.of(), 0.85
        );
        Anomaly timeoutAnomaly = new Anomaly(
                "A3", AnomalyType.SERVICE_TIMEOUT_SPIKE, "Gateway timeout",
                t3, t3.plusMinutes(3), 4, List.of(), 0.75
        );

        List<Incident> incidents = detector.detectIncidents(List.of(memoryAnomaly, secAnomaly, timeoutAnomaly));

        assertEquals(3, incidents.size(), "Should produce 3 distinct incidents");

        assertEquals("INC-001", incidents.get(0).getId());
        assertEquals(IncidentCategory.MEMORY_CRITICAL_FAILURE, incidents.get(0).getCategory());
        assertEquals(IncidentSeverity.CRITICAL, incidents.get(0).getSeverity());

        assertEquals("INC-002", incidents.get(1).getId());
        assertEquals(IncidentCategory.SECURITY_BREACH_ATTEMPT, incidents.get(1).getCategory());
        assertEquals(IncidentSeverity.HIGH, incidents.get(1).getSeverity());

        assertEquals("INC-003", incidents.get(2).getId());
        assertEquals(IncidentCategory.NETWORK_SERVICE_DEGRADATION, incidents.get(2).getCategory());
        assertEquals(IncidentSeverity.HIGH, incidents.get(2).getSeverity());
    }

    @Test
    @DisplayName("Should safely handle anomalies with null timestamps")
    void testAnomalyWithNullTimestampsHandledSafely() {
        Anomaly anomalyNoTime = new Anomaly(
                "A-NOTIME",
                AnomalyType.RESOURCE_EXHAUSTION,
                "Resource exhaustion without timestamp",
                null,
                null,
                1,
                List.of(),
                1.0
        );

        List<Incident> incidents = detector.detectIncidents(List.of(anomalyNoTime));
        assertEquals(1, incidents.size());
        assertNotNull(incidents.get(0).getDetectedAt(), "Detected timestamp should fall back to a non-null timestamp");
    }

    @Test
    @DisplayName("Should detect APPLICATION_CRASH when multiple error bursts coexist with specific incident")
    void testMultipleBurstsWithSpecificIncidentProducesBoth() {
        Anomaly dbAnomaly = new Anomaly(
                "A-DB", AnomalyType.DATABASE_FAILURE_SPIKE, "DB deadlocks",
                LocalDateTime.now(), LocalDateTime.now(), 5, List.of(), 0.9
        );
        Anomaly burst1 = new Anomaly(
                "A-B1", AnomalyType.ERROR_BURST, "Burst 1",
                LocalDateTime.now(), LocalDateTime.now(), 10, List.of(), 0.8
        );
        Anomaly burst2 = new Anomaly(
                "A-B2", AnomalyType.ERROR_BURST, "Burst 2",
                LocalDateTime.now(), LocalDateTime.now(), 12, List.of(), 0.85
        );

        List<Incident> incidents = detector.detectIncidents(List.of(dbAnomaly, burst1, burst2));

        assertEquals(2, incidents.size(), "Should produce both DATABASE_OUTAGE and APPLICATION_CRASH");
        assertEquals(IncidentCategory.DATABASE_OUTAGE, incidents.get(0).getCategory());
        assertEquals(IncidentCategory.APPLICATION_CRASH, incidents.get(1).getCategory());
    }
}
