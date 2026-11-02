package com.loganalyzer.report;

import com.loganalyzer.model.AnalysisResult;

/**
 * Contract for formatting log analysis outcomes into human-readable or structured reports.
 */
public interface ReportGenerator {

    /**
     * Generates a report string from the given analysis result.
     *
     * @param result analysis result containing stats, anomalies, and incidents
     * @return formatted report representation
     */
    String generate(AnalysisResult result);
}
