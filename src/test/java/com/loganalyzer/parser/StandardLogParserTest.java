package com.loganalyzer.parser;

import com.loganalyzer.exception.InvalidLogFileException;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

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
}
