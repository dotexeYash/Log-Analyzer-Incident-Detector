package com.loganalyzer.exception;

/**
 * Thrown when log line parsing encounters critical malformed content.
 */
public class LogParseException extends LogAnalyzerException {
    private static final long serialVersionUID = 1L;

    private final long lineNumber;

    public LogParseException(String message, long lineNumber) {
        super(String.format("Error at line %d: %s", lineNumber, message));
        this.lineNumber = lineNumber;
    }

    public LogParseException(String message, long lineNumber, Throwable cause) {
        super(String.format("Error at line %d: %s", lineNumber, message), cause);
        this.lineNumber = lineNumber;
    }

    public long getLineNumber() {
        return lineNumber;
    }
}
