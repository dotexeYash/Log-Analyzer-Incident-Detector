package com.loganalyzer.parser;

import com.loganalyzer.exception.InvalidLogFileException;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class StandardLogParserTest {

    private StandardLogParser parser;

    @BeforeEach
    void setUp() {
        parser = new StandardLogParser();
    }

    @Test
    @DisplayName("Should parse standard logback formatted log line correctly")
    void testParseStandardFormat() {
        String line = "2026-11-03 01:14:02.123 [main] INFO com.example.UserService - User authentication successful";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(1, entry.getLineNumber());
        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 14, 2, 123000000), entry.getTimestamp());
        assertEquals("main", entry.getThread());
        assertEquals(LogLevel.INFO, entry.getLevel());
        assertEquals("com.example.UserService", entry.getLogger());
        assertEquals("User authentication successful", entry.getMessage());
        assertFalse(entry.hasStackTrace());
    }

    @Test
    @DisplayName("Should parse bracketed log format correctly")
    void testParseBracketedFormat() {
        String line = "[2026-11-03 01:14:02.500] [WARN] [http-nio-8080-exec-1] [PaymentService] Gateway timeout occurred";
        LogEntry entry = parser.parseLine(line, 2);

        assertNotNull(entry);
        assertEquals(2, entry.getLineNumber());
        assertEquals(LogLevel.WARN, entry.getLevel());
        assertEquals("http-nio-8080-exec-1", entry.getThread());
        assertEquals("PaymentService", entry.getLogger());
        assertEquals("Gateway timeout occurred", entry.getMessage());
    }

    @Test
    @DisplayName("Should parse simple timestamp and level format")
    void testParseSimpleFormat() {
        String line = "2026-11-03 01:14:02 ERROR Server shutting down abruptly";
        LogEntry entry = parser.parseLine(line, 3);

        assertNotNull(entry);
        assertEquals(LogLevel.ERROR, entry.getLevel());
        assertEquals("Server shutting down abruptly", entry.getMessage());
    }

    @Test
    @DisplayName("Should parse multi-line log entry with attached stack trace")
    void testParseMultiLineStackTrace() throws IOException {
        String logContent = """
                2026-11-03 01:15:00.100 [main] ERROR com.example.DbClient - Connection failed
                \tat com.example.DbClient.connect(DbClient.java:42)
                \tat com.example.App.start(App.java:15)
                Caused by: java.net.ConnectException: Connection refused
                \t... 12 more
                2026-11-03 01:15:05.200 [main] INFO com.example.App - Retrying connection
                """;

        List<LogEntry> entries = parser.parse(new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, entries.size(), "Should have exactly 2 distinct log entries");
        LogEntry errorEntry = entries.get(0);
        assertEquals(LogLevel.ERROR, errorEntry.getLevel());
        assertTrue(errorEntry.hasStackTrace(), "Error entry should have attached stack trace");
        assertEquals(4, errorEntry.getStackTrace().size());
        assertTrue(errorEntry.getCombinedMessage().contains("java.net.ConnectException"));

        LogEntry infoEntry = entries.get(1);
        assertEquals(LogLevel.INFO, infoEntry.getLevel());
        assertFalse(infoEntry.hasStackTrace());
    }

    @Test
    @DisplayName("Should throw InvalidLogFileException when file path is invalid or non-existent")
    void testNonExistentFileThrowsException() {
        Path invalidPath = Paths.get("non-existent-directory/non-existent-file.log");
        assertThrows(InvalidLogFileException.class, () -> parser.parse(invalidPath));
    }

    @Test
    @DisplayName("Task 6: Should throw IllegalArgumentException when file path is null")
    void testNullPathThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse((Path) null));
    }

    @Test
    @DisplayName("Task 6: Should throw IllegalArgumentException when InputStream is null")
    void testNullInputStreamThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse((InputStream) null));
    }

    @Test
    @DisplayName("Task 6: Should throw InvalidLogFileException when target path is a directory")
    void testDirectoryPathThrowsInvalidLogFileException() {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        InvalidLogFileException ex = assertThrows(InvalidLogFileException.class, () -> parser.parse(tempDir));
        assertTrue(ex.getMessage().contains("Target is not a regular file"));
    }

    @Test
    @DisplayName("Should parse timestamps with single-digit month and single-digit day")
    void testParseSingleDigitMonthAndDay() {
        String line = "2026-1-6 10:30:45 [main] INFO com.example.Service - Single digit date";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
        assertEquals(LogLevel.INFO, entry.getLevel());
        assertEquals("Single digit date", entry.getMessage());
    }

    @Test
    @DisplayName("Should parse timestamps with two-digit month and single-digit day")
    void testParseTwoDigitMonthAndSingleDigitDay() {
        String line = "2026-01-6 10:30:45 [main] INFO com.example.Service - Two digit month, single digit day";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
    }

    @Test
    @DisplayName("Should parse timestamps with single-digit month and two-digit day")
    void testParseSingleDigitMonthAndTwoDigitDay() {
        String line = "2026-1-06 10:30:45 [main] INFO com.example.Service - Single digit month, two digit day";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
    }

    @Test
    @DisplayName("Should parse slashed timestamps with single-digit month and day")
    void testParseSlashedSingleDigitMonthAndDay() {
        String line = "2026/1/6 10:30:45 INFO Slashed date";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
    }

    @Test
    @DisplayName("Should parse bracketed format with single-digit date")
    void testParseBracketedSingleDigitDate() {
        String line = "[2026-1-6 10:30:45.500] [WARN] [worker-1] [TaskRunner] Processing warning";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45, 500_000_000), entry.getTimestamp());
        assertEquals(LogLevel.WARN, entry.getLevel());
    }

    @Test
    @DisplayName("Should parse timestamps with various ISO timezone offset formats (+05:30, +05, +0500, Z)")
    void testParseTimestampWithTimezoneOffsets() {
        // +05:30 offset
        LogEntry entryCol = parser.parseLine("2026-01-06T10:30:45+05:30 [main] INFO Service - Offset with colon", 1);
        assertNotNull(entryCol);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entryCol.getTimestamp());

        // +05 hour-only offset
        LogEntry entryHour = parser.parseLine("2026-01-06T10:30:45+05 [main] INFO Service - Offset hour only", 2);
        assertNotNull(entryHour);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entryHour.getTimestamp());

        // +0500 no-colon offset
        LogEntry entry4Dig = parser.parseLine("2026-01-06T10:30:45+0500 [main] INFO Service - Offset 4 digits", 3);
        assertNotNull(entry4Dig);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry4Dig.getTimestamp());

        // Z UTC offset
        LogEntry entryZ = parser.parseLine("2026-01-06T10:30:45Z [main] INFO Service - UTC Z", 4);
        assertNotNull(entryZ);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entryZ.getTimestamp());
    }

    @Test
    @DisplayName("Should assign null timestamp for invalid calendar dates without crashing")
    void testInvalidCalendarDatesAssignNullTimestamp() {
        // Month 13 does not exist
        LogEntry month13 = parser.parseLine("2026-13-01 10:30:45 INFO Service - Invalid month 13", 1);
        assertNotNull(month13);
        assertNull(month13.getTimestamp(), "Invalid month 13 should fail timestamp parsing and yield null");

        // Day 35 does not exist
        LogEntry day35 = parser.parseLine("2026-1-35 10:30:45 INFO Service - Invalid day 35", 2);
        assertNotNull(day35);
        assertNull(day35.getTimestamp(), "Invalid day 35 should fail timestamp parsing and yield null");

        // Month 00 does not exist
        LogEntry month0 = parser.parseLine("2026-00-10 10:30:45 INFO Service - Invalid month 00", 3);
        assertNotNull(month0);
        assertNull(month0.getTimestamp(), "Invalid month 00 should fail timestamp parsing and yield null");
    }

    @Test
    @DisplayName("Strict calendar-date validation: Reject invalid dates (Feb 30, non-leap Feb 29) and accept valid leap dates")
    void testStrictCalendarDateValidation() {
        // 2026-02-30 -> null timestamp
        LogEntry feb30 = parser.parseLine("2026-02-30 10:30:45 INFO Service - Invalid date Feb 30", 1);
        assertNotNull(feb30);
        assertNull(feb30.getTimestamp(), "2026-02-30 should yield null timestamp under strict resolution");

        // 2026-02-29 -> null timestamp (2026 is not a leap year)
        LogEntry feb29NonLeap = parser.parseLine("2026-02-29 10:30:45 INFO Service - Non-leap Feb 29", 2);
        assertNotNull(feb29NonLeap);
        assertNull(feb29NonLeap.getTimestamp(), "2026-02-29 should yield null timestamp as 2026 is not a leap year");

        // 2024-02-29 -> valid timestamp (2024 is a leap year)
        LogEntry feb29Leap = parser.parseLine("2024-02-29 10:30:45 INFO Service - Valid leap year Feb 29", 3);
        assertNotNull(feb29Leap);
        assertEquals(LocalDateTime.of(2024, 2, 29, 10, 30, 45), feb29Leap.getTimestamp(),
                "2024-02-29 should parse successfully as 2024 is a leap year");
    }

    @Test
    @DisplayName("Backward compatibility: Existing two-digit formats continue to parse identically")
    void testBackwardCompatibilityTwoDigitFormats() {
        LogEntry entry = parser.parseLine("2026-11-03 01:14:02.123 [main] INFO com.example.UserService - Message", 1);
        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 14, 2, 123_000_000), entry.getTimestamp());
    }

    @Test
    @DisplayName("Phase 3: Zero-byte empty file parses safely to an empty list")
    void testEmptyZeroByteFileParsesToEmptyList(@TempDir Path tempDir) throws IOException, InvalidLogFileException {
        Path emptyFile = Files.createFile(tempDir.resolve("empty.log"));
        List<LogEntry> entries = parser.parse(emptyFile);

        assertNotNull(entries, "Parsed entries list should not be null");
        assertTrue(entries.isEmpty(), "Zero-byte file must produce zero log entries");
    }

    @Test
    @DisplayName("Phase 3: Blank file with only spaces, tabs, and newlines produces zero entries")
    void testBlankFileWithWhitespaceAndNewlines(@TempDir Path tempDir) throws IOException, InvalidLogFileException {
        Path blankFile = tempDir.resolve("blank.log");
        Files.writeString(blankFile, "   \n\t\t\n  \r\n \n");
        List<LogEntry> entries = parser.parse(blankFile);

        assertNotNull(entries);
        assertTrue(entries.isEmpty(), "File with only blank lines must produce zero log entries");
    }

    @Test
    @DisplayName("Phase 3: Corrupted unparseable lines and malformed timestamps are handled safely")
    void testCorruptedUnparseableLinesAndMalformedTimestamps() throws IOException {
        // Completely unparseable line directly via parseLine returns null
        assertNull(parser.parseLine("Random non-log text without structure", 1));

        // When streamed via parse(InputStream), an unformatted leading line creates a fallback entry with UNKNOWN level
        String corruptedContent = """
                Random non-log preamble line
                2026-11-09 10:00:00 [main] INFO com.service.App - Valid entry
                """;
        List<LogEntry> entries = parser.parse(new ByteArrayInputStream(corruptedContent.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, entries.size(), "Should contain 2 entries (fallback + valid)");
        LogEntry fallback = entries.get(0);
        assertEquals(LogLevel.UNKNOWN, fallback.getLevel());
        assertNull(fallback.getTimestamp());
        assertEquals("Random non-log preamble line", fallback.getMessage());

        LogEntry valid = entries.get(1);
        assertEquals(LogLevel.INFO, valid.getLevel());
        assertEquals("Valid entry", valid.getMessage());
    }

    @Test
    @DisplayName("Phase 3: Log lines with out-of-order timestamps parse sequentially preserving file order")
    void testOutOfOrderTimestampsParsing() throws IOException {
        String content = """
                2026-11-09 10:30:00 [main] INFO Service - Third in time, first in file
                2026-11-09 10:10:00 [main] INFO Service - First in time, second in file
                2026-11-09 10:20:00 [main] INFO Service - Second in time, third in file
                """;
        List<LogEntry> entries = parser.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        assertEquals(3, entries.size());
        assertEquals(LocalDateTime.of(2026, 11, 9, 10, 30, 0), entries.get(0).getTimestamp());
        assertEquals(LocalDateTime.of(2026, 11, 9, 10, 10, 0), entries.get(1).getTimestamp());
        assertEquals(LocalDateTime.of(2026, 11, 9, 10, 20, 0), entries.get(2).getTimestamp());
    }

    @Test
    @DisplayName("Phase 3: Mixed input with valid entries, stack traces, blank lines and malformed content")
    void testMixedInputWithValidEntriesCorruptedLinesAndStackTraces() throws IOException {
        String mixedContent = """
                2026-11-09 10:00:00 [main] INFO Service - Starting service
                \t
                2026-11-09 10:00:05 [main] ERROR Service - Failed operation
                \tat com.service.Service.run(Service.java:10)
                Caused by: java.io.IOException: Disk full


                Unformatted unexpected log snippet
                2026-11-09 10:00:10 [main] WARN Service - Warning received
                """;
        List<LogEntry> entries = parser.parse(new ByteArrayInputStream(mixedContent.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(entries);
        assertFalse(entries.isEmpty());

        // Entry 1: Starting service
        LogEntry entry1 = entries.get(0);
        assertEquals(LogLevel.INFO, entry1.getLevel());
        assertEquals("Starting service", entry1.getMessage());

        // Entry 2: Error with attached stack trace
        LogEntry entry2 = entries.get(1);
        assertEquals(LogLevel.ERROR, entry2.getLevel());
        assertTrue(entry2.hasStackTrace());
        assertTrue(entry2.getCombinedMessage().contains("java.io.IOException: Disk full"));
    }
}
