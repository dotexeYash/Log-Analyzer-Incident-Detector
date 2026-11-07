package com.loganalyzer.report;

import com.loganalyzer.model.AnalysisResult;
import com.loganalyzer.model.Anomaly;
import com.loganalyzer.model.AnomalyType;
import com.loganalyzer.model.Incident;
import com.loganalyzer.model.IncidentCategory;
import com.loganalyzer.model.IncidentSeverity;
import com.loganalyzer.model.LogLevel;
import com.loganalyzer.model.LogStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {

    private AnalysisResult sampleResult;

    @BeforeEach
    void setUp() {
        LogStatistics stats = new LogStatistics(
                100,
                Map.of(LogLevel.INFO, 80L, LogLevel.WARN, 10L, LogLevel.ERROR, 10L),
                LocalDateTime.of(2026, 11, 3, 1, 0, 0),
                LocalDateTime.of(2026, 11, 3, 1, 30, 0),
                Map.of("NullPointerException in service", 10L),
                Map.of("com.service.UserService", 50L),
                Map.of("2026-11-03 01:00", 100L),
                10.0,
                10.0
        );

        Anomaly anomaly = new Anomaly(
                "ANOM-1",
                AnomalyType.ERROR_BURST,
                "Burst of 10 errors",
                LocalDateTime.of(2026, 11, 3, 1, 15, 0),
                LocalDateTime.of(2026, 11, 3, 1, 16, 0),
                10,
                List.of(),
                0.85
        );

        Incident incident = new Incident(
                "INC-001",
                "High Error Spike",
                IncidentSeverity.HIGH,
                IncidentCategory.APPLICATION_CRASH,
                "Burst of 10 errors logged in short timeframe",
                LocalDateTime.of(2026, 11, 3, 1, 15, 0),
                List.of(anomaly),
                List.of(),
                List.of("Check server memory and restart process")
        );

        sampleResult = new AnalysisResult("test.log", List.of(), stats, List.of(anomaly), List.of(incident));
    }

    @Test
    @DisplayName("ConsoleReportGenerator should contain key report sections")
    void testConsoleReport() {
        ConsoleReportGenerator generator = new ConsoleReportGenerator();
        String report = generator.generate(sampleResult);

        assertNotNull(report);
        assertTrue(report.contains("LOG ANALYZER & INCIDENT DETECTOR"));
        assertTrue(report.contains("LOG LEVEL DISTRIBUTION"));
        assertTrue(report.contains("TOP RECURRING ERRORS"));
        assertTrue(report.contains("DETECTED ANOMALIES"));
        assertTrue(report.contains("ACTIONABLE OPERATIONAL INCIDENTS"));
        assertTrue(report.contains("INC-001"));
    }

    @Test
    @DisplayName("MarkdownReportGenerator should produce valid markdown format")
    void testMarkdownReport() {
        MarkdownReportGenerator generator = new MarkdownReportGenerator();
        String report = generator.generate(sampleResult);

        assertNotNull(report);
        assertTrue(report.contains("# 📋 Log Analysis & Incident Detection Report"));
        assertTrue(report.contains("## 📊 Executive Summary"));
        assertTrue(report.contains("## 📈 Log Level Distribution"));
        assertTrue(report.contains("`INC-001`"));
        assertTrue(report.contains("- [ ] Check server memory and restart process"));
    }

    @Test
    @DisplayName("JsonReportGenerator should produce parseable JSON structure")
    void testJsonReport() {
        JsonReportGenerator generator = new JsonReportGenerator();
        String json = generator.generate(sampleResult);

        assertNotNull(json);
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}\n") || json.endsWith("}"));
        assertTrue(json.contains("\"sourceName\": \"test.log\""));
        assertTrue(json.contains("\"totalLogs\": 100"));
        assertTrue(json.contains("\"id\": \"INC-001\""));
    }

    @Test
    @DisplayName("Task 6: ConsoleReportGenerator safely handles AnalysisResult with null statistics")
    void testConsoleReportWithNullStatistics() {
        AnalysisResult nullStatsResult = new AnalysisResult("null-stats.log", List.of(), null, List.of(), List.of());
        ConsoleReportGenerator generator = new ConsoleReportGenerator();
        assertDoesNotThrow(() -> {
            String report = generator.generate(nullStatsResult);
            assertNotNull(report);
            assertTrue(report.contains("Total Records   : 0"));
            assertTrue(report.contains("Time Range      : N/A to N/A"));
        });
    }

    @Test
    @DisplayName("Task 6: MarkdownReportGenerator safely handles AnalysisResult with null statistics")
    void testMarkdownReportWithNullStatistics() {
        AnalysisResult nullStatsResult = new AnalysisResult("null-stats.log", List.of(), null, List.of(), List.of());
        MarkdownReportGenerator generator = new MarkdownReportGenerator();
        assertDoesNotThrow(() -> {
            String report = generator.generate(nullStatsResult);
            assertNotNull(report);
            assertTrue(report.contains("| **Total Log Entries** | 0 |"));
            assertTrue(report.contains("| **Time Range** | N/A to N/A |"));
        });
    }

    @Test
    @DisplayName("Task 6: JsonReportGenerator safely handles AnalysisResult with null statistics")
    void testJsonReportWithNullStatistics() {
        AnalysisResult nullStatsResult = new AnalysisResult("null-stats.log", List.of(), null, List.of(), List.of());
        JsonReportGenerator generator = new JsonReportGenerator();
        assertDoesNotThrow(() -> {
            String json = generator.generate(nullStatsResult);
            assertNotNull(json);
            assertTrue(json.contains("\"totalLogs\": 0"));
            assertTrue(json.contains("\"firstTimestamp\": null"));
        });
    }

    @Test
    @DisplayName("Phase 2: All report generators extend AbstractReportGenerator")
    void testInheritanceHierarchy() {
        ReportGenerator console = new ConsoleReportGenerator();
        ReportGenerator markdown = new MarkdownReportGenerator();
        ReportGenerator json = new JsonReportGenerator();

        assertInstanceOf(AbstractReportGenerator.class, console);
        assertInstanceOf(AbstractReportGenerator.class, markdown);
        assertInstanceOf(AbstractReportGenerator.class, json);
    }

    @Test
    @DisplayName("Phase 2: AbstractReportGenerator resolves statistics safely")
    void testAbstractReportGeneratorResolveStatistics() {
        TestReportGenerator generator = new TestReportGenerator();

        // Non-null result with stats
        LogStatistics resolved = generator.resolveStatistics(sampleResult);
        assertNotNull(resolved);
        assertEquals(100, resolved.getTotalLogs());

        // Non-null result with null stats
        AnalysisResult nullStatsResult = new AnalysisResult("test.log", List.of(), null, List.of(), List.of());
        LogStatistics fallback1 = generator.resolveStatistics(nullStatsResult);
        assertNotNull(fallback1);
        assertEquals(0, fallback1.getTotalLogs());

        // Null result
        LogStatistics fallback2 = generator.resolveStatistics(null);
        assertNotNull(fallback2);
        assertEquals(0, fallback2.getTotalLogs());
    }

    @Test
    @DisplayName("Phase 2: AbstractReportGenerator formats timestamps consistently")
    void testAbstractReportGeneratorFormatTimestamp() {
        TestReportGenerator generator = new TestReportGenerator();

        LocalDateTime ts = LocalDateTime.of(2026, 11, 7, 14, 30, 45);
        assertEquals("2026-11-07 14:30:45", generator.formatTimestamp(ts));
        assertEquals("N/A", generator.formatTimestamp(null));
        assertEquals("UNKNOWN", generator.formatTimestamp(null, "UNKNOWN"));
    }

    @Test
    @DisplayName("Phase 2: AbstractReportGenerator evaluates operational status correctly")
    void testAbstractReportGeneratorEvaluateStatus() {
        TestReportGenerator generator = new TestReportGenerator();

        // Critical incident
        Incident critical = new Incident("C1", "Crit", IncidentSeverity.CRITICAL, IncidentCategory.MEMORY_CRITICAL_FAILURE, "Crit", null, List.of(), List.of(), List.of());
        AnalysisResult critResult = new AnalysisResult("crit.log", List.of(), null, List.of(), List.of(critical));
        assertEquals("CRITICAL", generator.evaluateStatus(critResult));

        // Warning incident
        Incident warning = new Incident("W1", "Warn", IncidentSeverity.HIGH, IncidentCategory.APPLICATION_CRASH, "Warn", null, List.of(), List.of(), List.of());
        AnalysisResult warnResult = new AnalysisResult("warn.log", List.of(), null, List.of(), List.of(warning));
        assertEquals("WARNING", generator.evaluateStatus(warnResult));

        // Healthy result
        AnalysisResult healthyResult = new AnalysisResult("health.log", List.of(), null, List.of(), List.of());
        assertEquals("HEALTHY", generator.evaluateStatus(healthyResult));

        // Null result
        assertEquals("HEALTHY", generator.evaluateStatus(null));
    }

    @Test
    @DisplayName("Phase 2: All report generators handle null AnalysisResult gracefully")
    void testNullAnalysisResultHandling() {
        assertEquals("No analysis result provided.", new ConsoleReportGenerator().generate(null));
        assertEquals("{}", new JsonReportGenerator().generate(null));
        assertEquals("# Log Analysis Report\n\nNo analysis result provided.\n", new MarkdownReportGenerator().generate(null));
    }

    static class TestReportGenerator extends AbstractReportGenerator {
        @Override
        public String generate(AnalysisResult result) {
            return "test";
        }
    }
}
