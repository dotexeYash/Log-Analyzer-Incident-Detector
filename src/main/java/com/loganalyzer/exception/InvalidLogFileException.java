package com.loganalyzer.exception;

/**
 * Thrown when an input log file does not exist, cannot be read, or is invalid.
 */
public class InvalidLogFileException extends LogAnalyzerException {
    public InvalidLogFileException(String message) {
        super(message);
    }

    public InvalidLogFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
