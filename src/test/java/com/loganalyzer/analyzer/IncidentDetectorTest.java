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

        List<Incident> incidents = detector.detectIncidents(List.of(memoryAnomaly), List.of());

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

        List<Incident> incidents = detector.detectIncidents(List.of(dbAnomaly), List.of());

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
}
