package com.loganalyzer.report;

import com.loganalyzer.model.AnalysisResult;
import com.loganalyzer.model.Anomaly;
import com.loganalyzer.model.Incident;
import com.loganalyzer.model.LogLevel;
import com.loganalyzer.model.LogStatistics;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Generates structured JSON reports suitable for ingestion by SIEM tools, monitoring dashboards, or CI pipelines.
 */
public class JsonReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public String generate(AnalysisResult result) {
        if (result == null) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        LogStatistics stats = result.getStatistics() != null
                ? result.getStatistics()
                : new LogStatistics(0, Map.of(), null, null, Map.of(), Map.of(), Map.of(), 0.0, 0.0);

        sb.append("{\n");
        sb.append(indent(1)).append("\"sourceName\": \"").append(escape(result.getSourceName())).append("\",\n");
        sb.append(indent(1)).append("\"status\": \"").append(result.hasCriticalIncidents() ? "CRITICAL" : (result.hasIncidents() ? "WARNING" : "HEALTHY")).append("\",\n");
        sb.append(indent(1)).append("\"statistics\": {\n");
        sb.append(indent(2)).append("\"totalLogs\": ").append(stats.getTotalLogs()).append(",\n");
        sb.append(indent(2)).append("\"errorRatePercentage\": ").append(String.format("%.2f", stats.getErrorPercentage())).append(",\n");
        sb.append(indent(2)).append("\"warningRatePercentage\": ").append(String.format("%.2f", stats.getWarningPercentage())).append(",\n");
        sb.append(indent(2)).append("\"firstTimestamp\": ").append(stats.getFirstTimestamp() != null ? "\"" + stats.getFirstTimestamp().format(TIME_FORMAT) + "\"" : "null").append(",\n");
        sb.append(indent(2)).append("\"lastTimestamp\": ").append(stats.getLastTimestamp() != null ? "\"" + stats.getLastTimestamp().format(TIME_FORMAT) + "\"" : "null").append(",\n");

        // countsByLevel
        sb.append(indent(2)).append("\"countsByLevel\": {\n");
        int lvlIdx = 0;
        LogLevel[] levels = LogLevel.values();
        for (LogLevel lvl : levels) {
            sb.append(indent(3)).append("\"").append(lvl.name()).append("\": ").append(stats.getCount(lvl));
            if (++lvlIdx < levels.length) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent(2)).append("}\n");
        sb.append(indent(1)).append("},\n");

        // Anomalies
        sb.append(indent(1)).append("\"anomalies\": [\n");
        for (int i = 0; i < result.getAnomalies().size(); i++) {
            Anomaly a = result.getAnomalies().get(i);
            sb.append(indent(2)).append("{\n");
            sb.append(indent(3)).append("\"id\": \"").append(escape(a.getId())).append("\",\n");
            sb.append(indent(3)).append("\"type\": \"").append(a.getType().name()).append("\",\n");
            sb.append(indent(3)).append("\"description\": \"").append(escape(a.getDescription())).append("\",\n");
            sb.append(indent(3)).append("\"severityScore\": ").append(String.format("%.2f", a.getSeverityScore())).append(",\n");
            sb.append(indent(3)).append("\"affectedLogCount\": ").append(a.getAffectedLogCount()).append("\n");
            sb.append(indent(2)).append("}").append(i < result.getAnomalies().size() - 1 ? "," : "").append("\n");
        }
        sb.append(indent(1)).append("],\n");

        // Incidents
        sb.append(indent(1)).append("\"incidents\": [\n");
        for (int i = 0; i < result.getIncidents().size(); i++) {
            Incident inc = result.getIncidents().get(i);
            sb.append(indent(2)).append("{\n");
            sb.append(indent(3)).append("\"id\": \"").append(escape(inc.getId())).append("\",\n");
            sb.append(indent(3)).append("\"title\": \"").append(escape(inc.getTitle())).append("\",\n");
            sb.append(indent(3)).append("\"severity\": \"").append(inc.getSeverity().name()).append("\",\n");
            sb.append(indent(3)).append("\"category\": \"").append(inc.getCategory().name()).append("\",\n");
            sb.append(indent(3)).append("\"summary\": \"").append(escape(inc.getSummary())).append("\",\n");
            sb.append(indent(3)).append("\"detectedAt\": \"").append(inc.getDetectedAt().format(TIME_FORMAT)).append("\",\n");
            sb.append(indent(3)).append("\"recommendations\": [\n");
            for (int r = 0; r < inc.getRecommendations().size(); r++) {
                sb.append(indent(4)).append("\"").append(escape(inc.getRecommendations().get(r))).append("\"");
                if (r < inc.getRecommendations().size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(indent(3)).append("]\n");
            sb.append(indent(2)).append("}").append(i < result.getIncidents().size() - 1 ? "," : "").append("\n");
        }
        sb.append(indent(1)).append("]\n");

        sb.append("}\n");
        return sb.toString();
    }

    private String indent(int level) {
        return "  ".repeat(level);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
