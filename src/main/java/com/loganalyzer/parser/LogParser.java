package com.loganalyzer.parser;

import com.loganalyzer.exception.LogAnalyzerException;
import com.loganalyzer.model.LogEntry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface contract for log file parsers.
 */
public interface LogParser {

    /**
     * Parses all log entries from the given file path.
     *
     * @param path target log file path
     * @return list of parsed log entries
     * @throws IOException if an I/O error occurs
     * @throws LogAnalyzerException if log validation fails
     */
    List<LogEntry> parse(Path path) throws IOException, LogAnalyzerException;

    /**
     * Parses log entries from an input stream.
     *
     * @param inputStream stream of log content
     * @return list of parsed log entries
     * @throws IOException if an I/O error occurs
     */
    List<LogEntry> parse(InputStream inputStream) throws IOException;

    /**
     * Parses a single line of log text into a LogEntry.
     *
     * @param line raw text line
     * @param lineNumber 1-based line number in source
     * @return parsed LogEntry or null if the line is part of a continuation
     */
    LogEntry parseLine(String line, long lineNumber);
}
