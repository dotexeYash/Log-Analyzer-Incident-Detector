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

    @Test
    @DisplayName("Should preserve contextual HTTP status codes specified in requirements")
    void testPreserveTargetContextualHttpStatusCodes() {
        assertEquals("HTTP 401 Unauthorized", aggregator.normalizeErrorMessage("HTTP 401 Unauthorized"));
        assertEquals("HTTP 403 Forbidden", aggregator.normalizeErrorMessage("HTTP 403 Forbidden"));
        assertEquals("HTTP status 500", aggregator.normalizeErrorMessage("HTTP status 500"));
        assertEquals("status code: 503", aggregator.normalizeErrorMessage("status code: 503"));
        assertEquals("HTTP/1.1 502 Bad Gateway", aggregator.normalizeErrorMessage("HTTP/1.1 502 Bad Gateway"));
    }

    @Test
    @DisplayName("Should preserve HTTP status codes followed by reason phrases case-insensitively")
    void testPreserveHttpStatusCodesWithSuffixReasonPhrasesCaseInsensitive() {
        assertEquals("401 Unauthorized: Invalid API token",
                aggregator.normalizeErrorMessage("401 Unauthorized: Invalid API token"));
        assertEquals("403 FORBIDDEN: Access denied",
                aggregator.normalizeErrorMessage("403 FORBIDDEN: Access denied"));
        assertEquals("500 internal server error: Database failure",
                aggregator.normalizeErrorMessage("500 internal server error: Database failure"));
        assertEquals("404 Not Found: Resource missing",
                aggregator.normalizeErrorMessage("404 Not Found: Resource missing"));
    }

    @Test
    @DisplayName("Should normalize complete IPv4 address as a single {val} placeholder")
    void testNormalizeIpv4AddressAsSinglePlaceholder() {
        String input = "Failed authentication from IP 198.51.100.42 on interface";
        assertEquals("Failed authentication from IP {val} on interface", aggregator.normalizeErrorMessage(input));
    }

    @Test
    @DisplayName("Should validate IPv4 octets between 0 and 255 with boundary tests")
    void testIpv4BoundaryAndInvalidAddresses() {
        // Valid boundaries (0.0.0.0 and 255.255.255.255)
        assertEquals("IP {val} connected", aggregator.normalizeErrorMessage("IP 0.0.0.0 connected"));
        assertEquals("IP {val} broadcast", aggregator.normalizeErrorMessage("IP 255.255.255.255 broadcast"));

        // Invalid octets (> 255) must NOT be treated as valid IPv4 addresses
        assertEquals("IP {val}.{val}.{val}.{val} invalid", aggregator.normalizeErrorMessage("IP 256.1.1.1 invalid"));
        assertEquals("IP {val}.{val}.{val}.{val} invalid", aggregator.normalizeErrorMessage("IP 999.999.999.999 invalid"));
        assertEquals("IP {val}.{val}.{val}.{val} invalid", aggregator.normalizeErrorMessage("IP 10.0.0.300 invalid"));
    }

    @Test
    @DisplayName("Should not protect arbitrary three-digit numbers without HTTP context")
    void testDoNotProtectArbitraryThreeDigitNumbersWithoutHttpContext() {
        assertEquals("user {val} error", aggregator.normalizeErrorMessage("user 401 error"));
        assertEquals("order {val} failed", aggregator.normalizeErrorMessage("order 500 failed"));
        assertEquals("port {val} closed", aggregator.normalizeErrorMessage("port 404 closed"));
        assertEquals("batch {val} item", aggregator.normalizeErrorMessage("batch 101 item"));
    }

    @Test
    @DisplayName("Should preserve HTTP status while normalizing dynamic order and user IDs in same message")
    void testNormalizeMixedMessageWithHttpStatusAndDynamicIds() {
        String input = "HTTP 500 Internal Server Error processing order 98765 for user 1234";
        assertEquals("HTTP 500 Internal Server Error processing order {val} for user {val}",
                aggregator.normalizeErrorMessage(input));
    }

    @Test
    @DisplayName("Should normalize UUIDs and hexadecimal literals correctly")
    void testNormalizeUuidsAndHexPointers() {
        assertEquals("Transaction {val} failed",
                aggregator.normalizeErrorMessage("Transaction 123e4567-e89b-12d3-a456-426614174000 failed"));
        assertEquals("Memory address {val} fault",
                aggregator.normalizeErrorMessage("Memory address 0x7fff fault"));
    }

    @Test
    @DisplayName("Top recurring errors should maintain separate buckets for distinct HTTP status codes")
    void testTopErrorsReflectsSeparateHttpStatusBuckets() {
        List<LogEntry> entries = List.of(
                LogEntry.builder().lineNumber(1).level(LogLevel.ERROR).message("Call failed with HTTP status 401").build(),
                LogEntry.builder().lineNumber(2).level(LogLevel.ERROR).message("Call failed with HTTP status 401").build(),
                LogEntry.builder().lineNumber(3).level(LogLevel.ERROR).message("Call failed with HTTP status 500").build()
        );

        LogStatistics stats = aggregator.aggregate(entries);

        assertEquals(2, stats.getTopErrors().size());
        assertEquals(2, stats.getTopErrors().get("Call failed with HTTP status 401"));
        assertEquals(1, stats.getTopErrors().get("Call failed with HTTP status 500"));
    }
}
