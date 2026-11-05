package com.loganalyzer.report;

import com.loganalyzer.model.AnalysisResult;
import com.loganalyzer.model.Anomaly;
import com.loganalyzer.model.Incident;
import com.loganalyzer.model.IncidentSeverity;
import com.loganalyzer.model.LogLevel;
import com.loganalyzer.model.LogStatistics;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Generates structured GitHub-flavored Markdown reports for documentation, ticket attachments, or CI/CD pipelines.
 */
public class MarkdownReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String generate(AnalysisResult result) {
        if (result == null) {
            return "# Log Analysis Report\n\nNo analysis result provided.\n";
        }

        StringBuilder sb = new StringBuilder();
        LogStatistics stats = result.getStatistics() != null
                ? result.getStatistics()
                : new LogStatistics(0, Map.of(), null, null, Map.of(), Map.of(), Map.of(), 0.0, 0.0);

        sb.append("# 📋 Log Analysis & Incident Detection Report\n\n");

        // Status Callout
        if (result.hasCriticalIncidents()) {
            sb.append("> [!CAUTION]\n> **CRITICAL INCIDENTS DETECTED**: System stability is compromised. Immediate intervention required.\n\n");
        } else if (result.hasIncidents()) {
            sb.append("> [!WARNING]\n> **OPERATIONAL WARNING**: Anomalies and incidents detected. Review remediation steps below.\n\n");
        } else {
            sb.append("> [!NOTE]\n> **SYSTEM HEALTHY**: No operational anomalies or incidents identified.\n\n");
        }

        // Summary Table
        sb.append("## 📊 Executive Summary\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("| :--- | :--- |\n");
        sb.append(String.format("| **Source Log File** | `%s` |\n", result.getSourceName()));
        sb.append(String.format("| **Time Range** | %s to %s |\n",
                stats.getFirstTimestamp() != null ? stats.getFirstTimestamp().format(TIME_FORMAT) : "N/A",
                stats.getLastTimestamp() != null ? stats.getLastTimestamp().format(TIME_FORMAT) : "N/A"));
        sb.append(String.format("| **Total Log Entries** | %,d |\n", stats.getTotalLogs()));
        sb.append(String.format("| **Error Rate** | **%.2f%%** |\n", stats.getErrorPercentage()));
        sb.append(String.format("| **Warning Rate** | %.2f%% |\n", stats.getWarningPercentage()));
        sb.append(String.format("| **Total Anomalies** | %,d |\n", result.getAnomalies().size()));
        sb.append(String.format("| **Total Incidents** | %,d |\n\n", result.getIncidents().size()));

        // Log Level Table
        sb.append("## 📈 Log Level Distribution\n\n");
        sb.append("| Level | Count | Percentage |\n");
        sb.append("| :--- | :--- | :--- |\n");
        for (LogLevel lvl : LogLevel.values()) {
            long count = stats.getCount(lvl);
            if (count > 0 || lvl.isError() || lvl.isWarning() || lvl == LogLevel.INFO) {
                double pct = stats.getTotalLogs() > 0 ? (count * 100.0) / stats.getTotalLogs() : 0.0;
                sb.append(String.format("| `%s` | %,d | %.2f%% |\n", lvl.name(), count, pct));
            }
        }
        sb.append("\n");

        // Top Errors
        sb.append("## ⚠️ Top Recurring Errors\n\n");
        if (stats.getTopErrors().isEmpty()) {
            sb.append("_No error entries found._\n\n");
        } else {
            sb.append("| # | Occurrences | Error Message Pattern |\n");
            sb.append("| :- | :--- | :--- |\n");
            int rank = 1;
            for (Map.Entry<String, Long> entry : stats.getTopErrors().entrySet()) {
                sb.append(String.format("| %d | %,d | `%s` |\n", rank++, entry.getValue(), escapeMarkdown(entry.getKey())));
            }
            sb.append("\n");
        }

        // Detected Anomalies
        sb.append("## 🔍 Detected Anomalies\n\n");
        if (result.getAnomalies().isEmpty()) {
            sb.append("✅ _No anomalies detected in the log stream._\n\n");
        } else {
            for (Anomaly a : result.getAnomalies()) {
                sb.append(String.format("### `%s` - %s\n\n", a.getId(), a.getType().getDisplayName()));
                sb.append(String.format("- **Description**: %s\n", a.getDescription()));
                sb.append(String.format("- **Severity Score**: `%.2f / 1.00`\n", a.getSeverityScore()));
                sb.append(String.format("- **Affected Entries**: %,d\n", a.getAffectedLogCount()));
                if (a.getStartTime() != null) {
                    sb.append(String.format("- **Observed Window**: %s to %s\n",
                            a.getStartTime().format(TIME_FORMAT),
                            a.getEndTime() != null ? a.getEndTime().format(TIME_FORMAT) : "N/A"));
                }
                sb.append("\n");
            }
        }

        // Operational Incidents
        sb.append("## 🚨 Actionable Operational Incidents\n\n");
        if (result.getIncidents().isEmpty()) {
            sb.append("✅ _No incidents requiring triage._\n\n");
        } else {
            for (Incident inc : result.getIncidents()) {
                String badge = inc.getSeverity() == IncidentSeverity.CRITICAL ? "🔴 CRITICAL" :
                               (inc.getSeverity() == IncidentSeverity.HIGH ? "🟠 HIGH" : "🟡 MEDIUM");

                sb.append(String.format("### [%s] `%s`: %s\n\n", badge, inc.getId(), inc.getTitle()));
                sb.append(String.format("- **Category**: `%s`\n", inc.getCategory().getDescription()));
                sb.append(String.format("- **Detected At**: %s\n", inc.getDetectedAt().format(TIME_FORMAT)));
                sb.append(String.format("- **Summary**: %s\n\n", inc.getSummary()));

                if (!inc.getRootCauseEntries().isEmpty()) {
                    sb.append("**Root Cause Evidence:**\n```text\n");
                    for (int i = 0; i < Math.min(3, inc.getRootCauseEntries().size()); i++) {
                        var sample = inc.getRootCauseEntries().get(i);
                        sb.append(String.format("[Line %d] %s%n", sample.getLineNumber(), sample.getCombinedMessage()));
                    }
                    sb.append("```\n\n");
                }

                sb.append("**Recommended Remediation Checklist:**\n");
                for (String rec : inc.getRecommendations()) {
                    sb.append(String.format("- [ ] %s\n", rec));
                }
                sb.append("\n---\n\n");
            }
        }

        return sb.toString();
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("|", "\\|").replace("\n", " ").replace("\r", "");
    }
}
