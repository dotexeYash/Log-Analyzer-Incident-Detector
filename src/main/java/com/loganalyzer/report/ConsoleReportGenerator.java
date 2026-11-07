package com.loganalyzer.report;

import com.loganalyzer.model.AnalysisResult;
import com.loganalyzer.model.Anomaly;
import com.loganalyzer.model.Incident;
import com.loganalyzer.model.IncidentSeverity;
import com.loganalyzer.model.LogLevel;
import com.loganalyzer.model.LogStatistics;

import java.util.Map;

/**
 * Renders high-visibility ASCII operational dashboards and summary reports to the console.
 */
public class ConsoleReportGenerator extends AbstractReportGenerator {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String SUB_SEPARATOR = "-".repeat(80);

    @Override
    public String generate(AnalysisResult result) {
        if (result == null) {
            return "No analysis result provided.";
        }

        StringBuilder sb = new StringBuilder();
        LogStatistics stats = resolveStatistics(result);

        // Banner
        sb.append(SEPARATOR).append(System.lineSeparator());
        sb.append("           LOG ANALYZER & INCIDENT DETECTOR - OPERATIONAL REPORT           \n");
        sb.append(SEPARATOR).append(System.lineSeparator());
        sb.append(String.format(" Source Log File : %s%n", result.getSourceName()));
        sb.append(String.format(" Time Range      : %s to %s%n",
                formatTimestamp(stats.getFirstTimestamp()),
                formatTimestamp(stats.getLastTimestamp())));
        sb.append(String.format(" Total Records   : %,d%n", stats.getTotalLogs()));
        sb.append(String.format(" Error Rate      : %.2f%%%n", stats.getErrorPercentage()));
        sb.append(String.format(" Warning Rate    : %.2f%%%n", stats.getWarningPercentage()));
        sb.append(String.format(" Overall Status  : %s%n", computeStatusBadge(result, stats)));
        sb.append(SUB_SEPARATOR).append(System.lineSeparator());

        // Log Level Breakdown
        sb.append("\n[1] LOG LEVEL DISTRIBUTION\n");
        sb.append(String.format("  %-10s | %-12s | %-10s%n", "Level", "Count", "Percentage"));
        sb.append("  " + "-".repeat(38)).append(System.lineSeparator());
        for (LogLevel level : LogLevel.values()) {
            long count = stats.getCount(level);
            if (count > 0 || level.isError() || level.isWarning() || level == LogLevel.INFO) {
                double pct = stats.getTotalLogs() > 0 ? (count * 100.0) / stats.getTotalLogs() : 0.0;
                sb.append(String.format("  %-10s | %-12s | %6.2f%%%n", level.name(), String.format("%,d", count), pct));
            }
        }

        // Top Errors
        sb.append("\n[2] TOP RECURRING ERRORS\n");
        if (stats.getTopErrors().isEmpty()) {
            sb.append("  No error logs recorded.\n");
        } else {
            int rank = 1;
            for (Map.Entry<String, Long> entry : stats.getTopErrors().entrySet()) {
                sb.append(String.format("  %2d. [%,d occurrences] %s%n", rank++, entry.getValue(), entry.getKey()));
            }
        }

        // Detected Anomalies
        sb.append("\n[3] DETECTED ANOMALIES (").append(result.getAnomalies().size()).append(")\n");
        if (result.getAnomalies().isEmpty()) {
            sb.append("  No anomalies or behavioral deviations detected. System operating normally.\n");
        } else {
            for (Anomaly anomaly : result.getAnomalies()) {
                sb.append(String.format("  • [%s] %s | Score: %.2f | Affected Logs: %d%n",
                        anomaly.getId(),
                        anomaly.getType().getDisplayName(),
                        anomaly.getSeverityScore(),
                        anomaly.getAffectedLogCount()));
                sb.append(String.format("    Details: %s%n", anomaly.getDescription()));
                if (anomaly.getStartTime() != null) {
                    sb.append(String.format("    Window : %s to %s%n",
                            formatTimestamp(anomaly.getStartTime()),
                            formatTimestamp(anomaly.getEndTime())));
                }
            }
        }

        // Operational Incidents
        sb.append("\n[4] ACTIONABLE OPERATIONAL INCIDENTS (").append(result.getIncidents().size()).append(")\n");
        if (result.getIncidents().isEmpty()) {
            sb.append("  No operational incidents identified. No immediate triage required.\n");
        } else {
            for (Incident incident : result.getIncidents()) {
                sb.append(SUB_SEPARATOR).append(System.lineSeparator());
                sb.append(String.format("  Incident ID : %s [%s]%n", incident.getId(), incident.getSeverity().getLabel()));
                sb.append(String.format("  Title       : %s%n", incident.getTitle()));
                sb.append(String.format("  Category    : %s%n", incident.getCategory().getDescription()));
                sb.append(String.format("  Detected At : %s%n", formatTimestamp(incident.getDetectedAt())));
                sb.append(String.format("  Summary     : %s%n", incident.getSummary()));

                if (!incident.getRootCauseEntries().isEmpty()) {
                    sb.append("  Evidence Snippets:\n");
                    for (int i = 0; i < Math.min(2, incident.getRootCauseEntries().size()); i++) {
                        var sample = incident.getRootCauseEntries().get(i);
                        sb.append(String.format("    - [Line %d] %s%n", sample.getLineNumber(), sample.getMessage()));
                    }
                }

                sb.append("  Remediation Recommendations:\n");
                int recIdx = 1;
                for (String rec : incident.getRecommendations()) {
                    sb.append(String.format("    %d. %s%n", recIdx++, rec));
                }
            }
            sb.append(SUB_SEPARATOR).append(System.lineSeparator());
        }

        sb.append(SEPARATOR).append(System.lineSeparator());
        return sb.toString();
    }

    private String computeStatusBadge(AnalysisResult result, LogStatistics stats) {
        if (result.hasCriticalIncidents()) {
            return "[CRITICAL ALERT] - Immediate Triage Required";
        }
        if (result.hasIncidents()) {
            return "[WARNING / DEGRADED] - Operational Attention Required";
        }
        if (stats != null && stats.getErrorPercentage() > 5.0) {
            return "[ELEVATED ERRORS] - Investigate Subsystem Logs";
        }
        return "[HEALTHY] - Operational Metrics Nominal";
    }
}
