package com.loganalyzer.analyzer;

import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogLevel;
import com.loganalyzer.model.LogStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsAggregatorTest {

    private StatisticsAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new StatisticsAggregator();
    }

    @Test
    @DisplayName("Should aggregate empty log list gracefully")
    void testEmptyList() {
        LogStatistics stats = aggregator.aggregate(List.of());
        assertEquals(0, stats.getTotalLogs());
        assertEquals(0.0, stats.getErrorPercentage());
        assertNull(stats.getFirstTimestamp());
        assertNull(stats.getLastTimestamp());
    }

    @Test
    @DisplayName("Should compute correct level counts and error percentage")
    void testCountsAndPercentages() {
        LocalDateTime t1 = LocalDateTime.of(2026, 11, 3, 1, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 11, 3, 1, 5, 0);

        List<LogEntry> entries = List.of(
                LogEntry.builder().lineNumber(1).timestamp(t1).level(LogLevel.INFO).logger("App").message("Ready").build(),
                LogEntry.builder().lineNumber(2).timestamp(t1.plusMinutes(1)).level(LogLevel.WARN).logger("App").message("Slow").build(),
                LogEntry.builder().lineNumber(3).timestamp(t1.plusMinutes(2)).level(LogLevel.ERROR).logger("Db").message("Timeout 101").build(),
                LogEntry.builder().lineNumber(4).timestamp(t1.plusMinutes(3)).level(LogLevel.ERROR).logger("Db").message("Timeout 102").build(),
                LogEntry.builder().lineNumber(5).timestamp(t2).level(LogLevel.DEBUG).logger("Cache").message("Hit").build()
        );

        LogStatistics stats = aggregator.aggregate(entries);

        assertEquals(5, stats.getTotalLogs());
        assertEquals(1, stats.getCount(LogLevel.INFO));
        assertEquals(1, stats.getCount(LogLevel.WARN));
        assertEquals(2, stats.getCount(LogLevel.ERROR));
        assertEquals(1, stats.getCount(LogLevel.DEBUG));
        assertEquals(40.0, stats.getErrorPercentage(), 0.001);
        assertEquals(20.0, stats.getWarningPercentage(), 0.001);
        assertEquals(t1, stats.getFirstTimestamp());
        assertEquals(t2, stats.getLastTimestamp());
    }

    @Test
    @DisplayName("Should normalize and rank top recurring error messages")
    void testNormalizeTopErrors() {
        List<LogEntry> entries = List.of(
                LogEntry.builder().lineNumber(1).level(LogLevel.ERROR).message("Failed to fetch order ID 12345").build(),
                LogEntry.builder().lineNumber(2).level(LogLevel.ERROR).message("Failed to fetch order ID 67890").build(),
                LogEntry.builder().lineNumber(3).level(LogLevel.ERROR).message("Connection pool exhausted").build()
        );

        LogStatistics stats = aggregator.aggregate(entries);

        assertEquals(2, stats.getTopErrors().size());
        assertTrue(stats.getTopErrors().containsKey("Failed to fetch order ID {val}"));
        assertEquals(2, stats.getTopErrors().get("Failed to fetch order ID {val}"));
        assertEquals(1, stats.getTopErrors().get("Connection pool exhausted"));
    }
}
