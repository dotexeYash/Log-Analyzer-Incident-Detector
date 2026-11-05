package com.loganalyzer.exception;

/**
 * Base unchecked exception for Log Analyzer operations.
 */
public class LogAnalyzerException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LogAnalyzerException(String message) {
        super(message);
    }

    public LogAnalyzerException(String message, Throwable cause) {
        super(message, cause);
    }
}
