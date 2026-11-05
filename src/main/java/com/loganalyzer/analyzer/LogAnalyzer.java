package com.loganalyzer.analyzer;

import com.loganalyzer.exception.InvalidLogFileException;
import com.loganalyzer.exception.LogAnalyzerException;
import com.loganalyzer.model.AnalysisResult;
import com.loganalyzer.model.Anomaly;
import com.loganalyzer.model.Incident;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogStatistics;
import com.loganalyzer.parser.LogParser;
import com.loganalyzer.parser.StandardLogParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Main coordinator that parses, aggregates statistics, detects anomalies,
 * and categorizes operational incidents.
 */
public class LogAnalyzer {

    private final LogParser parser;
    private final StatisticsAggregator statisticsAggregator;
    private final AnomalyDetector anomalyDetector;
    private final IncidentDetector incidentDetector;

    public LogAnalyzer() {
        this(new StandardLogParser(), new StatisticsAggregator(), new AnomalyDetector(), new IncidentDetector());
    }

    public LogAnalyzer(LogParser parser,
                       StatisticsAggregator statisticsAggregator,
                       AnomalyDetector anomalyDetector,
                       IncidentDetector incidentDetector) {
        this.parser = Objects.requireNonNull(parser, "parser cannot be null");
        this.statisticsAggregator = Objects.requireNonNull(statisticsAggregator, "statisticsAggregator cannot be null");
        this.anomalyDetector = Objects.requireNonNull(anomalyDetector, "anomalyDetector cannot be null");
        this.incidentDetector = Objects.requireNonNull(incidentDetector, "incidentDetector cannot be null");
    }

    /**
     * Analyzes a log file by path.
     *
     * @param logFilePath path to log file
     * @return complete AnalysisResult
     * @throws IllegalArgumentException if logFilePath is null
     * @throws InvalidLogFileException if file is invalid or not readable
     */
    public AnalysisResult analyze(Path logFilePath) {
        if (logFilePath == null) {
            throw new IllegalArgumentException("Log file path cannot be null");
        }

        try {
            List<LogEntry> entries = parser.parse(logFilePath);
            String sourceName = logFilePath.getFileName() != null ? logFilePath.getFileName().toString() : logFilePath.toString();
            return process(entries, sourceName);
        } catch (IOException e) {
            throw new InvalidLogFileException("Failed to read log file: " + logFilePath.toAbsolutePath() + " - " + e.getMessage(), e);
        }
    }

    /**
     * Analyzes log content from an InputStream.
     *
     * @param inputStream log data stream
     * @param sourceName descriptive name for the source
     * @return complete AnalysisResult
     */
    public AnalysisResult analyze(InputStream inputStream, String sourceName) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        try {
            List<LogEntry> entries = parser.parse(inputStream);
            return process(entries, sourceName);
        } catch (IOException e) {
            throw new LogAnalyzerException("Failed to parse log stream: " + e.getMessage(), e);
        }
    }

    /**
     * Analyzes pre-parsed log entries directly.
     */
    public AnalysisResult process(List<LogEntry> entries, String sourceName) {
        LogStatistics statistics = statisticsAggregator.aggregate(entries);
        List<Anomaly> anomalies = anomalyDetector.detectAnomalies(entries);
        List<Incident> incidents = incidentDetector.detectIncidents(anomalies, entries);

        return new AnalysisResult(sourceName, entries, statistics, anomalies, incidents);
    }
}
