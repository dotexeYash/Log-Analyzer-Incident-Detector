package com.loganalyzer.exception;

/**
 * Thrown when an input log file does not exist, cannot be read, or is invalid.
 */
public class InvalidLogFileException extends LogAnalyzerException {
    private static final long serialVersionUID = 1L;

    public InvalidLogFileException(String message) {
        super(message);
    }

    public InvalidLogFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
