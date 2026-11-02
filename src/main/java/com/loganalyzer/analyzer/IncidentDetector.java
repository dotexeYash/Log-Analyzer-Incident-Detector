package com.loganalyzer.analyzer;

import com.loganalyzer.model.Anomaly;
import com.loganalyzer.model.AnomalyType;
import com.loganalyzer.model.Incident;
import com.loganalyzer.model.IncidentCategory;
import com.loganalyzer.model.IncidentSeverity;
import com.loganalyzer.model.LogEntry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Correlates detected anomalies and log evidence to identify and categorize operational incidents.
 */
public class IncidentDetector {

    /**
     * Synthesizes detected anomalies into categorized, actionable incidents.
     *
     * @param anomalies list of detected anomalies
     * @param entries original log entries for root-cause context
     * @return prioritized list of incidents
     */
    public List<Incident> detectIncidents(List<Anomaly> anomalies, List<LogEntry> entries) {
        if (anomalies == null || anomalies.isEmpty()) {
            return Collections.emptyList();
        }

        List<Incident> incidents = new ArrayList<>();
        AtomicInteger incidentCounter = new AtomicInteger(1);

        // Group anomalies by nature
        List<Anomaly> memoryAnomalies = findAnomalies(anomalies, AnomalyType.RESOURCE_EXHAUSTION);
        List<Anomaly> dbAnomalies = findAnomalies(anomalies, AnomalyType.DATABASE_FAILURE_SPIKE);
        List<Anomaly> secAnomalies = findAnomalies(anomalies, AnomalyType.SECURITY_SUSPICIOUS_ACCESS);
        List<Anomaly> timeoutAnomalies = findAnomalies(anomalies, AnomalyType.SERVICE_TIMEOUT_SPIKE);
        List<Anomaly> burstAnomalies = findAnomalies(anomalies, AnomalyType.ERROR_BURST);
        List<Anomaly> repetitiveAnomalies = findAnomalies(anomalies, AnomalyType.HIGH_FREQUENCY_IDENTICAL_ERRORS);

        // 1. Critical Out of Memory Incident
        if (!memoryAnomalies.isEmpty()) {
            List<LogEntry> rootCauses = extractSamples(memoryAnomalies);
            incidents.add(new Incident(
                    formatIncidentId(incidentCounter),
                    "CRITICAL: JVM Out-of-Memory / Heap Exhaustion Detected",
                    IncidentSeverity.CRITICAL,
                    IncidentCategory.MEMORY_CRITICAL_FAILURE,
                    "Application process encountered unrecoverable memory exhaustion (OutOfMemoryError or GC overhead limit).",
                    findEarliestTimestamp(memoryAnomalies),
                    memoryAnomalies,
                    rootCauses,
                    List.of(
                            "Trigger automated heap dump capture: -XX:+HeapDumpOnOutOfMemoryError.",
                            "Inspect memory profiler traces for memory leaks in caching layers or static collections.",
                            "Increase JVM maximum heap allocation (-Xmx) if memory footprint conforms to workload scaling.",
                            "Restart affected service containers to restore operational stability."
                    )
            ));
        }

        // 2. Database Outage / Connection Starvation Incident
        if (!dbAnomalies.isEmpty()) {
            List<LogEntry> rootCauses = extractSamples(dbAnomalies);
            incidents.add(new Incident(
                    formatIncidentId(incidentCounter),
                    "CRITICAL: Database Connectivity Outage / Connection Pool Exhaustion",
                    IncidentSeverity.CRITICAL,
                    IncidentCategory.DATABASE_OUTAGE,
                    "High volume of database connection failures, deadlocks, or pool starvation causing cascading application errors.",
                    findEarliestTimestamp(dbAnomalies),
                    dbAnomalies,
                    rootCauses,
                    List.of(
                            "Verify database cluster status, read-replica replication lag, and active server connections.",
                            "Review connection pool settings (maximumPoolSize, connectionTimeout) in HikariCP / datasource configuration.",
                            "Analyze recent slow queries and uncommitted transactions causing row or table deadlocks.",
                            "Check network routing and firewall rules between application tier and database host."
                    )
            ));
        }

        // 3. Security Breach / Unauthorized Attack Incident
        if (!secAnomalies.isEmpty()) {
            List<LogEntry> rootCauses = extractSamples(secAnomalies);
            incidents.add(new Incident(
                    formatIncidentId(incidentCounter),
                    "HIGH: Coordinated Authentication / Unauthorized Access Surge",
                    IncidentSeverity.HIGH,
                    IncidentCategory.SECURITY_BREACH_ATTEMPT,
                    "Elevated surge in failed login attempts, unauthorized API access, or suspicious security payloads detected.",
                    findEarliestTimestamp(secAnomalies),
                    secAnomalies,
                    rootCauses,
                    List.of(
                            "Isolate and ban abusive source IP addresses at Cloudflare / API Gateway rate-limiter.",
                            "Validate status of identity provider (OAuth/JWT signer) and check token expiration policies.",
                            "Check for credential stuffing or brute-force attack patterns on authentication endpoints.",
                            "Enforce Multi-Factor Authentication (MFA) and enable CAPTCHA on exposed login forms."
                    )
            ));
        }

        // 4. Downstream Network / Service Degradation Incident
        if (!timeoutAnomalies.isEmpty()) {
            List<LogEntry> rootCauses = extractSamples(timeoutAnomalies);
            incidents.add(new Incident(
                    formatIncidentId(incidentCounter),
                    "HIGH: Downstream Service Unavailability / Network Timeout Surge",
                    IncidentSeverity.HIGH,
                    IncidentCategory.NETWORK_SERVICE_DEGRADATION,
                    "Sustained communication timeouts, socket errors, or 504 Gateway errors when calling upstream/downstream dependencies.",
                    findEarliestTimestamp(timeoutAnomalies),
                    timeoutAnomalies,
                    rootCauses,
                    List.of(
                            "Inspect circuit-breaker metrics (e.g. Resilience4j) and ensure fallback degradation paths are active.",
                            "Verify health of downstream microservices, third-party APIs, and DNS resolution.",
                            "Adjust client connect/read timeout configurations to prevent cascading thread starvation.",
                            "Review network ingress controllers and service-mesh telemetry for latency spikes."
                    )
            ));
        }

        // 5. General Error Burst / High Application Instability (if not already accounted for by specific root cause)
        boolean hasSpecificIncident = !memoryAnomalies.isEmpty() || !dbAnomalies.isEmpty() || !secAnomalies.isEmpty() || !timeoutAnomalies.isEmpty();
        if (!burstAnomalies.isEmpty() && (!hasSpecificIncident || burstAnomalies.size() > 1)) {
            List<LogEntry> rootCauses = extractSamples(burstAnomalies);
            incidents.add(new Incident(
                    formatIncidentId(incidentCounter),
                    "HIGH: Uncontrolled Error Spike / Application Instability Burst",
                    IncidentSeverity.HIGH,
                    IncidentCategory.APPLICATION_CRASH,
                    "An intense concentration of unhandled errors and exceptions was logged in a short timeframe.",
                    findEarliestTimestamp(burstAnomalies),
                    burstAnomalies,
                    rootCauses,
                    List.of(
                            "Inspect top recurring error traces in the report for unhandled runtime exceptions.",
                            "Rollback recent code deployments if error spike correlates with latest release timestamp.",
                            "Validate environment configuration variables and external credentials."
                    )
            ));
        }

        // 6. Repetitive Failure Pattern (if standalone)
        if (!repetitiveAnomalies.isEmpty() && incidents.isEmpty()) {
            List<LogEntry> rootCauses = extractSamples(repetitiveAnomalies);
            incidents.add(new Incident(
                    formatIncidentId(incidentCounter),
                    "MEDIUM: Persistent Recurring Component Failure",
                    IncidentSeverity.MEDIUM,
                    IncidentCategory.GENERAL_INCIDENT,
                    "A specific error condition is continuously repeating across the system.",
                    findEarliestTimestamp(repetitiveAnomalies),
                    repetitiveAnomalies,
                    rootCauses,
                    List.of(
                            "Identify the recurring stack trace and apply hotfix or defensive validation.",
                            "Ensure error handling avoids tight loops that flood disk space with log writes."
                    )
            ));
        }

        return incidents;
    }

    private List<Anomaly> findAnomalies(List<Anomaly> anomalies, AnomalyType type) {
        return anomalies.stream()
                .filter(a -> a.getType() == type)
                .toList();
    }

    private List<LogEntry> extractSamples(List<Anomaly> anomalies) {
        List<LogEntry> samples = new ArrayList<>();
        for (Anomaly a : anomalies) {
            samples.addAll(a.getSampleEntries());
            if (samples.size() >= 5) break;
        }
        return samples;
    }

    private LocalDateTime findEarliestTimestamp(List<Anomaly> anomalies) {
        return anomalies.stream()
                .map(Anomaly::getStartTime)
                .filter(java.util.Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
    }

    private String formatIncidentId(AtomicInteger counter) {
        return String.format("INC-%03d", counter.getAndIncrement());
    }
}
