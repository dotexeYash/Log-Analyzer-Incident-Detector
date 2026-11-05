package com.loganalyzer.integration;

import com.loganalyzer.analyzer.LogAnalyzer;
import com.loganalyzer.exception.InvalidLogFileException;
import com.loganalyzer.model.AnalysisResult;
import com.loganalyzer.model.IncidentCategory;
import com.loganalyzer.model.IncidentSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class LogAnalyzerEndToEndTest {

    private LogAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new LogAnalyzer();
    }

    @Test
    @DisplayName("End-to-End: Normal operations log produces zero incidents and nominal metrics")
    void testNormalOperationsLog() {
        Path path = Paths.get("sample-logs/normal-operations.log");
        AnalysisResult result = analyzer.analyze(path);

        assertNotNull(result);
        assertTrue(result.getStatistics().getTotalLogs() > 0);
        assertEquals(0, result.getIncidents().size(), "Normal operations should trigger zero incidents");
        assertFalse(result.hasCriticalIncidents());
    }

    @Test
    @DisplayName("End-to-End: Database failure log triggers CRITICAL database outage incident")
    void testCriticalDatabaseLog() {
        Path path = Paths.get("sample-logs/critical-incident-database.log");
        AnalysisResult result = analyzer.analyze(path);

        assertNotNull(result);
        assertTrue(result.hasIncidents());
        assertTrue(result.hasCriticalIncidents());

        boolean hasDbIncident = result.getIncidents().stream()
                .anyMatch(i -> i.getCategory() == IncidentCategory.DATABASE_OUTAGE && i.getSeverity() == IncidentSeverity.CRITICAL);
        assertTrue(hasDbIncident, "Should identify CRITICAL DATABASE_OUTAGE incident");
    }

    @Test
    @DisplayName("End-to-End: Security attack log triggers HIGH security breach attempt incident")
    void testSecurityAttackLog() {
        Path path = Paths.get("sample-logs/security-attack.log");
        AnalysisResult result = analyzer.analyze(path);

        assertNotNull(result);
        assertTrue(result.hasIncidents());

        boolean hasSecIncident = result.getIncidents().stream()
                .anyMatch(i -> i.getCategory() == IncidentCategory.SECURITY_BREACH_ATTEMPT && i.getSeverity() == IncidentSeverity.HIGH);
        assertTrue(hasSecIncident, "Should identify HIGH SECURITY_BREACH_ATTEMPT incident");
    }

    @Test
    @DisplayName("End-to-End: Out of memory log triggers CRITICAL memory failure incident")
    void testOutOfMemoryLog() {
        Path path = Paths.get("sample-logs/out-of-memory.log");
        AnalysisResult result = analyzer.analyze(path);

        assertNotNull(result);
        assertTrue(result.hasCriticalIncidents());

        boolean hasOomIncident = result.getIncidents().stream()
                .anyMatch(i -> i.getCategory() == IncidentCategory.MEMORY_CRITICAL_FAILURE && i.getSeverity() == IncidentSeverity.CRITICAL);
        assertTrue(hasOomIncident, "Should identify CRITICAL MEMORY_CRITICAL_FAILURE incident");
    }

    @Test
    @DisplayName("Task 6: Should throw IllegalArgumentException when log file path is null")
    void testAnalyzeNullPathThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze((Path) null));
    }

    @Test
    @DisplayName("Task 6: Should throw IllegalArgumentException when InputStream is null")
    void testAnalyzeNullInputStreamThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze((InputStream) null, "source"));
    }

    @Test
    @DisplayName("Task 6: Should throw InvalidLogFileException when analyzing non-existent file")
    void testAnalyzeNonExistentFileThrowsInvalidLogFileException() {
        Path invalidPath = Paths.get("non-existent-directory/non-existent-file.log");
        assertThrows(InvalidLogFileException.class, () -> analyzer.analyze(invalidPath));
    }
}
