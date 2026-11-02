package com.loganalyzer.parser;

import com.loganalyzer.exception.InvalidLogFileException;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enterprise standard log parser supporting multiple log formats and multi-line stack traces.
 */
public class StandardLogParser implements LogParser {

    private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd][yyyy/MM/dd]")
            .appendPattern("['T'][' ']")
            .appendPattern("HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .optionalStart()
            .appendPattern("[X][XXX][Z]")
            .optionalEnd()
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();

    // Pattern 1: 2026-11-03 01:14:02.123 [thread-1] INFO com.service.UserService - Message
    private static final Pattern PATTERN_STANDARD = Pattern.compile(
            "^(\\d{4}[-/]\\d{2}[-/]\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)" +
            "\\s+(?:\\[([^\\]]+)\\]\\s+)?([A-Za-z]{3,8})\\s+(?:(?:(?:\\[([^\\]]+)\\])|([a-zA-Z0-9_.$#\\-]+))\\s*[-:]\\s+)(.*)$"
    );

    // Pattern 2: [2026-11-03 01:14:02.123] [INFO] [thread-1] [com.service.UserService] Message
    private static final Pattern PATTERN_BRACKETED = Pattern.compile(
            "^\\[(\\d{4}[-/]\\d{2}[-/]\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)\\]" +
            "\\s+\\[([A-Za-z]{3,8})\\](?:\s+\\[([^\\]]+)\\])?(?:\s+\\[([^\\]]+)\\])?\\s*[-:]?\\s*(.*)$"
    );

    // Pattern 3: 2026-11-03 01:14:02 INFO Message
    private static final Pattern PATTERN_SIMPLE = Pattern.compile(
            "^(\\d{4}[-/]\\d{2}[-/]\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)" +
            "\\s+([A-Za-z]{3,8})\\s*[-:]?\\s*(.*)$"
    );

    // Stack trace line indicators
    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile(
            "^(\\s+at\\s+|Caused by:|\\s*\\.\\.\\.\\s*\\d+\\s+more|[a-zA-Z0-9_.$]+(?:Exception|Error):|\\t).*"
    );

    @Override
    public List<LogEntry> parse(Path path) throws IOException, InvalidLogFileException {
        if (path == null) {
            throw new InvalidLogFileException("Log file path cannot be null");
        }
        if (!Files.exists(path)) {
            throw new InvalidLogFileException("Log file does not exist: " + path.toAbsolutePath());
        }
        if (!Files.isRegularFile(path)) {
            throw new InvalidLogFileException("Target is not a regular file: " + path.toAbsolutePath());
        }
        if (!Files.isReadable(path)) {
            throw new InvalidLogFileException("Log file is not readable: " + path.toAbsolutePath());
        }

        try (InputStream is = Files.newInputStream(path)) {
            return parse(is);
        }
    }

    @Override
    public List<LogEntry> parse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        List<LogEntry> entries = new ArrayList<>();
        LogEntry currentEntry = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            long lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                // Check for stack trace or continuation
                if (currentEntry != null && isStackTraceOrContinuation(line)) {
                    currentEntry.addStackTraceLine(line);
                    continue;
                }

                LogEntry parsed = parseLine(line, lineNumber);
                if (parsed != null) {
                    currentEntry = parsed;
                    entries.add(currentEntry);
                } else if (currentEntry != null) {
                    // Treat as continuation line
                    currentEntry.addStackTraceLine(line);
                } else {
                    // Fallback for isolated line before any recognized entry
                    currentEntry = new LogEntry(lineNumber, null, LogLevel.UNKNOWN, "main", "SYSTEM", line, new ArrayList<>(), line);
                    entries.add(currentEntry);
                }
            }
        }

        return entries;
    }

    @Override
    public LogEntry parseLine(String line, long lineNumber) {
        if (line == null || line.isBlank()) {
            return null;
        }

        // Try standard format: Timestamp [thread] LEVEL logger - message
        Matcher matcher = PATTERN_STANDARD.matcher(line);
        if (matcher.matches()) {
            LocalDateTime timestamp = parseTimestamp(matcher.group(1));
            String thread = matcher.group(2) != null ? matcher.group(2) : "main";
            LogLevel level = LogLevel.fromString(matcher.group(3));
            String logger = matcher.group(4) != null ? matcher.group(4) : (matcher.group(5) != null ? matcher.group(5) : "ROOT");
            String message = matcher.group(6);

            return LogEntry.builder()
                    .lineNumber(lineNumber)
                    .timestamp(timestamp)
                    .level(level)
                    .thread(thread)
                    .logger(logger)
                    .message(message)
                    .rawLine(line)
                    .build();
        }

        // Try bracketed format: [Timestamp] [LEVEL] [thread] [logger] - message
        matcher = PATTERN_BRACKETED.matcher(line);
        if (matcher.matches()) {
            LocalDateTime timestamp = parseTimestamp(matcher.group(1));
            LogLevel level = LogLevel.fromString(matcher.group(2));
            String thread = matcher.group(3) != null ? matcher.group(3) : "main";
            String logger = matcher.group(4) != null ? matcher.group(4) : "ROOT";
            String message = matcher.group(5);

            return LogEntry.builder()
                    .lineNumber(lineNumber)
                    .timestamp(timestamp)
                    .level(level)
                    .thread(thread)
                    .logger(logger)
                    .message(message)
                    .rawLine(line)
                    .build();
        }

        // Try simple format: Timestamp LEVEL message
        matcher = PATTERN_SIMPLE.matcher(line);
        if (matcher.matches()) {
            LocalDateTime timestamp = parseTimestamp(matcher.group(1));
            LogLevel level = LogLevel.fromString(matcher.group(2));
            String message = matcher.group(3);

            return LogEntry.builder()
                    .lineNumber(lineNumber)
                    .timestamp(timestamp)
                    .level(level)
                    .thread("main")
                    .logger("ROOT")
                    .message(message)
                    .rawLine(line)
                    .build();
        }

        return null;
    }

    private boolean isStackTraceOrContinuation(String line) {
        if (line == null) return false;
        if (line.startsWith("\t") || line.startsWith("    ") || line.startsWith("  ")) {
            return true;
        }
        return STACK_TRACE_PATTERN.matcher(line).matches();
    }

    private LocalDateTime parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            // Normalize commas used in milliseconds (e.g. 2026-11-03 01:14:02,123)
            String normalized = raw.replace(',', '.');
            return LocalDateTime.parse(normalized, FLEXIBLE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}
