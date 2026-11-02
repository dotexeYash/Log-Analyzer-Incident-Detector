# 🔍 Log Analyzer & Incident Detector

A high-performance, modular Java 21 application engineered to parse application logs, detect anomalous patterns and bursts, categorize critical operational incidents with actionable remediation steps, and generate executive reports in Console, Markdown, and JSON formats.

---

## 🌟 Key Features

1. **Log Parsing & Streaming:**
   * Enterprise-grade regex parser supporting standard Spring/Logback, bracketed, ISO-8601, and custom timestamp formats.
   * Multi-line stack trace stitching: automatically links nested exceptions and stack traces to parent error records.
   * Resilient parsing: gracefully handles malformed or unformatted lines without crashing.

2. **Error & Warning Detection:**
   * Accurate classification across `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, and `FATAL` levels.
   * Message normalization: strips dynamic UUIDs, memory addresses, and timestamps to group and rank recurring errors.

3. **Behavioral Anomaly Detection:**
   * **Error Burst Detection:** Identifies rapid error spikes exceeding configurable thresholds within a sliding time window.
   * **Identical Failure Clustering:** Detects high-frequency repeating exceptions and failures.
   * **Resource Exhaustion:** Detects `OutOfMemoryError`, heap saturation, GC overhead limit violations, and thread exhaustion.
   * **Security Threat Surge:** Flags coordinated authentication failures, 401/403 surges, brute-force attempts, and suspicious SQLi patterns.
   * **Database Outage Spikes:** Detects connection pool starvation (HikariCP timeouts), SQL exceptions, and deadlocks.
   * **Service Latency / Timeout Surges:** Detects network connection refused, socket timeouts, and 504 Gateway errors.

4. **Incident Categorization & Triage:**
   * Synthesizes anomalies into operational incidents ranked by severity (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`).
   * Categorizes incidents into `MEMORY_CRITICAL_FAILURE`, `DATABASE_OUTAGE`, `SECURITY_BREACH_ATTEMPT`, `NETWORK_SERVICE_DEGRADATION`, and `APPLICATION_CRASH`.
   * Correlates root-cause log snippets and generates actionable remediation recommendations.

5. **Multi-Format Operational Reports:**
   * **Console Dashboard:** High-visibility ASCII summary cards, level distribution tables, top errors, anomalies, and incident triage recommendations.
   * **Markdown Reports:** GitHub-flavored markdown with summary tables, alerts (`> [!CAUTION]`), and remediation checklists.
   * **JSON Reports:** Structured machine-readable schema ready for SIEM ingestion (Splunk, Elastic, Datadog) or CI/CD pipelines.

---

## 📂 Project Structure

```text
Log-Analyzer-Incident-Detector/
├── .gitignore
├── pom.xml
├── README.md
├── sample-logs/                                      # Realistic log scenarios
│   ├── normal-operations.log                         # Healthy service baseline
│   ├── critical-incident-database.log                # Connection starvation & deadlocks
│   ├── security-attack.log                           # Brute-force & unauthorized bursts
│   └── out-of-memory.log                             # JVM heap exhaustion & OOM crashes
└── src/
    ├── main/
    │   ├── java/com/loganalyzer/
    │   │   ├── Main.java                             # CLI application entry point
    │   │   ├── analyzer/
    │   │   │   ├── AnomalyDetector.java              # Pattern & burst anomaly engine
    │   │   │   ├── IncidentDetector.java             # Incident triage & prioritization
    │   │   │   ├── LogAnalyzer.java                  # Central orchestration facade
    │   │   │   └── StatisticsAggregator.java         # Metric aggregation & error normalization
    │   │   ├── exception/
    │   │   │   ├── InvalidLogFileException.java      # File validation errors
    │   │   │   ├── LogAnalyzerException.java         # Base domain exception
    │   │   │   └── LogParseException.java            # Malformed line exception
    │   │   ├── model/
    │   │   │   ├── AnalysisResult.java               # Container for complete analysis output
    │   │   │   ├── Anomaly.java                      # Anomaly model with score & evidence
    │   │   │   ├── AnomalyType.java                  # Enumeration of anomaly categories
    │   │   │   ├── Incident.java                     # Actionable incident model
    │   │   │   ├── IncidentCategory.java             # Incident classifications
    │   │   │   ├── IncidentSeverity.java             # CRITICAL, HIGH, MEDIUM, LOW
    │   │   │   ├── LogEntry.java                     # Parsed log record with stack trace
    │   │   │   ├── LogLevel.java                     # Log levels with severity rankings
    │   │   │   └── LogStatistics.java                # Aggregated metrics & distributions
    │   │   ├── parser/
    │   │   │   ├── LogParser.java                    # Parser contract interface
    │   │   │   └── StandardLogParser.java            # Multi-format & multi-line parser
    │   │   └── report/
    │   │       ├── ConsoleReportGenerator.java       # ASCII operational dashboard
    │   │       ├── JsonReportGenerator.java          # SIEM-ready JSON serializer
    │   │       ├── MarkdownReportGenerator.java      # GitHub-flavored Markdown
    │   │       └── ReportGenerator.java              # Report generator interface
    │   └── resources/
    │       └── sample-logs/                          # Packaged classpath test logs
    └── test/
        └── java/com/loganalyzer/
            ├── analyzer/
            │   ├── AnomalyDetectorTest.java          # Burst, security, DB & OOM tests
            │   ├── IncidentDetectorTest.java         # Triage & recommendation verification
            │   └── StatisticsAggregatorTest.java     # Metrics & normalization tests
            ├── integration/
            │   └── LogAnalyzerEndToEndTest.java      # End-to-end scenario validation
            ├── parser/
            │   └── StandardLogParserTest.java        # Multi-format & stack trace parsing tests
            └── report/
                └── ReportGeneratorTest.java          # Console, Markdown & JSON tests
```

---

## 🛠️ Prerequisites

* **Java Development Kit (JDK):** Version 21 or higher
* **Apache Maven:** Version 3.8+ (or 3.10+)

Verify your local installation:
```powershell
java -version
mvn -version
```

---

## 🚀 Build Instructions

Navigate to the project root directory:
```powershell
cd "c:\Users\YASH\Desktop\log analyser\Log-Analyzer-Incident-Detector"
```

Compile the project, execute all unit and integration tests, and package the executable JAR:
```powershell
mvn clean package
```

To run tests only:
```powershell
mvn clean test
```

---

## 💻 Running the Application

### 1. Built-in Demonstration Mode
Runs the analysis against all 4 bundled scenario logs (`normal`, `database outage`, `security attack`, `out-of-memory`):
```powershell
java -jar target/log-analyzer-incident-detector-1.0.0.jar --demo
```

### 2. Inspect a Specific Log File
Analyze any custom log file with the default console dashboard:
```powershell
java -jar target/log-analyzer-incident-detector-1.0.0.jar --file sample-logs/critical-incident-database.log
```

### 3. Generate a Markdown Report
Generate a GitHub-flavored Markdown report and save it to disk:
```powershell
java -jar target/log-analyzer-incident-detector-1.0.0.jar -f sample-logs/security-attack.log -fmt markdown -o reports/security-incident.md
```

### 4. Generate a JSON Report (SIEM Integration)
Output structured JSON for piping or ingest:
```powershell
java -jar target/log-analyzer-incident-detector-1.0.0.jar -f sample-logs/out-of-memory.log -fmt json -o reports/oom-analysis.json
```

### 5. Custom Anomaly Thresholds
Adjust sliding window time (in seconds) and error burst trigger thresholds:
```powershell
java -jar target/log-analyzer-incident-detector-1.0.0.jar -f /var/log/application.log --burst-threshold 10 --window 120
```

---

## 📊 Command-Line Options Reference

| Option | Flag | Description | Default |
| :--- | :--- | :--- | :--- |
| `--file <path>` | `-f` | Path to log file to analyze | *None (Required unless `--demo`)* |
| `--format <type>` | `-fmt` | Output format: `console`, `markdown` (`md`), `json` | `console` |
| `--output <path>` | `-o` | Optional file path to save report output | *stdout* |
| `--burst-threshold <n>`| `-b` | Number of errors within time window to trigger burst | `5` |
| `--window <sec>` | `-w` | Time window (seconds) for burst anomaly detection | `60` |
| `--demo` | | Execute demonstration against all bundled sample scenarios | `false` |
| `--help` | `-h` | Display usage instructions and CLI options | |

### Exit Codes
* `0`: Analysis completed cleanly; no critical incidents found.
* `1`: Invalid arguments, missing file, or execution error.
* `2`: One or more **CRITICAL** operational incidents detected (useful for automated CI/CD deployment gates or alerting scripts).

---

## 🧪 Testing

The project includes unit and integration tests covering:
* Single-line and multi-line parsing with stack traces.
* ISO-8601, bracketed, and standard timestamp formats.
* Sliding window burst anomaly detection.
* Security threat identification (SQLi, brute-force, unauthorized access).
* JVM resource exhaustion and database pool starvation detection.
* Report rendering across Console, Markdown, and JSON.
* End-to-end integration tests on real-world log scenarios.

Run tests using:
```powershell
mvn test
```
