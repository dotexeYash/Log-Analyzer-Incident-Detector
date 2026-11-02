package com.loganalyzer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a parsed log event with metadata, message, and optional stack trace.
 */
public class LogEntry {
    private final long lineNumber;
    private final LocalDateTime timestamp;
    private final LogLevel level;
    private final String thread;
    private final String logger;
    private final String message;
    private final List<String> stackTrace;
    private final String rawLine;

    public LogEntry(long lineNumber,
                    LocalDateTime timestamp,
                    LogLevel level,
                    String thread,
                    String logger,
                    String message,
                    List<String> stackTrace,
                    String rawLine) {
        this.lineNumber = lineNumber;
        this.timestamp = timestamp;
        this.level = Objects.requireNonNullElse(level, LogLevel.UNKNOWN);
        this.thread = Objects.requireNonNullElse(thread, "main");
        this.logger = Objects.requireNonNullElse(logger, "ROOT");
        this.message = Objects.requireNonNullElse(message, "");
        this.stackTrace = stackTrace != null ? new ArrayList<>(stackTrace) : new ArrayList<>();
        this.rawLine = Objects.requireNonNullElse(rawLine, "");
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getThread() {
        return thread;
    }

    public String getLogger() {
        return logger;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getStackTrace() {
        return Collections.unmodifiableList(stackTrace);
    }

    public void addStackTraceLine(String line) {
        if (line != null) {
            this.stackTrace.add(line);
        }
    }

    public boolean hasStackTrace() {
        return !stackTrace.isEmpty();
    }

    public String getRawLine() {
        return rawLine;
    }

    /**
     * Returns the full message including any attached stack trace.
     */
    public String getCombinedMessage() {
        if (stackTrace.isEmpty()) {
            return message;
        }
        return message + System.lineSeparator() + String.join(System.lineSeparator(), stackTrace);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return lineNumber == logEntry.lineNumber &&
                Objects.equals(timestamp, logEntry.timestamp) &&
                level == logEntry.level &&
                Objects.equals(thread, logEntry.thread) &&
                Objects.equals(logger, logEntry.logger) &&
                Objects.equals(message, logEntry.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumber, timestamp, level, thread, logger, message);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s [%s] %s - %s",
                timestamp != null ? timestamp.toString() : "NO_TIME",
                level,
                thread,
                logger,
                message);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long lineNumber;
        private LocalDateTime timestamp;
        private LogLevel level = LogLevel.UNKNOWN;
        private String thread = "main";
        private String logger = "ROOT";
        private String message = "";
        private List<String> stackTrace = new ArrayList<>();
        private String rawLine = "";

        public Builder lineNumber(long lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder level(LogLevel level) {
            this.level = level;
            return this;
        }

        public Builder thread(String thread) {
            this.thread = thread;
            return this;
        }

        public Builder logger(String logger) {
            this.logger = logger;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder stackTrace(List<String> stackTrace) {
            if (stackTrace != null) {
                this.stackTrace = new ArrayList<>(stackTrace);
            }
            return this;
        }

        public Builder rawLine(String rawLine) {
            this.rawLine = rawLine;
            return this;
        }

        public LogEntry build() {
            return new LogEntry(lineNumber, timestamp, level, thread, logger, message, stackTrace, rawLine);
        }
    }
}
