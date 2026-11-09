package com.loganalyzer.integration;

import com.loganalyzer.analyzer.LogAnalyzer;
import com.loganalyzer.exception.InvalidLogFileException;
import com.loganalyzer.model.AnalysisResult;
import com.loganalyzer.model.IncidentCategory;
import com.loganalyzer.model.IncidentSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import com.loganalyzer.model.LogLevel;

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

    @Test
    @DisplayName("Phase 3: End-to-End zero-byte empty file analysis produces empty result without exception")
    void testEmptyZeroByteFileEndToEnd(@TempDir Path tempDir) throws IOException {
        Path emptyFile = Files.createFile(tempDir.resolve("empty-e2e.log"));
        AnalysisResult result = analyzer.analyze(emptyFile);

        assertNotNull(result);
        assertEquals(0, result.getEntries().size(), "Zero-byte file must have 0 parsed entries");
        assertEquals(0, result.getStatistics().getTotalLogs(), "Total logs must be 0");
        assertTrue(result.getAnomalies().isEmpty(), "No anomalies should be generated");
        assertTrue(result.getIncidents().isEmpty(), "No incidents should be generated");
        assertFalse(result.hasCriticalIncidents());
        assertNull(result.getStatistics().getFirstTimestamp());
        assertNull(result.getStatistics().getLastTimestamp());
    }

    @Test
    @DisplayName("Phase 3: End-to-End blank whitespace-only file produces empty nominal result")
    void testBlankFileWithWhitespaceEndToEnd(@TempDir Path tempDir) throws IOException {
        Path blankFile = tempDir.resolve("blank-e2e.log");
        Files.writeString(blankFile, "   \n\t\r\n   \n\n");
        AnalysisResult result = analyzer.analyze(blankFile);

        assertNotNull(result);
        assertEquals(0, result.getEntries().size());
        assertEquals(0, result.getStatistics().getTotalLogs());
        assertFalse(result.hasIncidents());
    }

    @Test
    @DisplayName("Phase 3: End-to-End out-of-order timestamps: Statistics min/max and Error Burst sorting")
    void testOutOfOrderTimestampsErrorBurstAndStatisticsEndToEnd(@TempDir Path tempDir) throws IOException {
        Path outOfOrderFile = tempDir.resolve("out-of-order.log");
        // 6 errors occurring in reverse/scrambled chronological order within 20 seconds
        String content = """
                2026-11-09 10:00:20 [main] ERROR Service - Operation failed 1
                2026-11-09 10:00:05 [main] ERROR Service - Operation failed 2
                2026-11-09 10:00:15 [main] ERROR Service - Operation failed 3
                2026-11-09 10:00:01 [main] ERROR Service - Operation failed 4
                2026-11-09 10:00:10 [main] ERROR Service - Operation failed 5
                2026-11-09 10:00:18 [main] ERROR Service - Operation failed 6
                """;
        Files.writeString(outOfOrderFile, content);

        AnalysisResult result = analyzer.analyze(outOfOrderFile);

        assertNotNull(result);
        assertEquals(6, result.getEntries().size());

        // StatisticsAggregator correctly identifies true minimum and maximum timestamps
        assertEquals(LocalDateTime.of(2026, 11, 9, 10, 0, 1), result.getStatistics().getFirstTimestamp());
        assertEquals(LocalDateTime.of(2026, 11, 9, 10, 0, 20), result.getStatistics().getLastTimestamp());

        // AnomalyDetector sorts error entries locally, so Error Burst anomaly has startTime <= endTime
        boolean hasBurst = result.getAnomalies().stream().anyMatch(a -> a.getType() == com.loganalyzer.model.AnomalyType.ERROR_BURST);
        assertTrue(hasBurst, "Should detect error burst anomaly despite scrambled order");

        result.getAnomalies().stream()
                .filter(a -> a.getType() == com.loganalyzer.model.AnomalyType.ERROR_BURST)
                .forEach(a -> {
                    assertNotNull(a.getStartTime());
                    assertNotNull(a.getEndTime());
                    assertFalse(a.getStartTime().isAfter(a.getEndTime()),
                            "Error Burst anomaly startTime must not be after endTime");
                });
    }

    @Test
    @DisplayName("Phase 3: End-to-End mixed file containing valid, malformed, blank and stack-trace logs")
    void testMixedContentFileEndToEnd(@TempDir Path tempDir) throws IOException {
        Path mixedFile = tempDir.resolve("mixed-e2e.log");
        String content = """
                Non-standard unparsed log line without structure
                2026-11-09 10:00:00 [main] INFO com.service.App - App initialized
                2026-11-09 10:00:05 [worker-1] ERROR com.service.Db - Database connection failed
                \tat com.service.Db.connect(Db.java:50)
                Caused by: java.sql.SQLException: Connection refused
                \t... 5 more

                2026-11-09 10:00:10 [worker-1] WARN com.service.Cache - Cache miss fallback
                """;
        Files.writeString(mixedFile, content);

        AnalysisResult result = analyzer.analyze(mixedFile);

        assertNotNull(result);
        assertEquals(4, result.getEntries().size(), "Should have 4 parsed entries (INFO, fallback, ERROR with trace, WARN)");
        assertEquals(4, result.getStatistics().getTotalLogs());
        assertEquals(1, result.getStatistics().getCount(LogLevel.INFO));
        assertEquals(1, result.getStatistics().getCount(LogLevel.UNKNOWN));
        assertEquals(1, result.getStatistics().getCount(LogLevel.ERROR));
        assertEquals(1, result.getStatistics().getCount(LogLevel.WARN));

        // Error entry preserved stack trace
        var errorEntry = result.getEntries().stream().filter(e -> e.getLevel() == LogLevel.ERROR).findFirst().orElseThrow();
        assertTrue(errorEntry.hasStackTrace());
        assertTrue(errorEntry.getCombinedMessage().contains("java.sql.SQLException"));
    }
}
