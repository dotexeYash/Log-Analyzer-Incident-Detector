# Comprehensive Report: CLI Exit-Code Behavior & Clarification (Option A)

**Project:** Log Analyzer & Incident Detector (Java 21 / Apache Maven)  
**Academic Context:** 2nd-Year B.Tech Computer Science & Engineering (Object-Oriented Programming — 20 Marks)  
**Date:** November 9, 2026  
**Scope:** CLI Exit-Code Behavior Investigation & Option A Implementation  
**Status:** Completed, Documented, and 100% Verified (144/144 Passing Tests)  

---

## 1. Executive Summary & Problem Statement

### The Issue
When evaluating the command-line interface via:
```bash
java -jar target/log-analyzer-incident-detector-1.0.0.jar --demo
```
The application executes the demonstration suite and displays `CRITICAL` operational incidents (such as `Database Connection Pool Exhaustion` and `JVM Out-of-Memory Fatal Crash`), but the operating system process exit code in PowerShell (`$LASTEXITCODE`) returns `0`.

However, the documented exit code specification states:
- `0` = Analysis completed without critical issues
- `1` = Error during execution or invalid inputs
- `2` = One or more `CRITICAL` operational incidents detected

### Objective of the Investigation & Task
1. Investigate the codebase in read-only mode to locate where exit codes are assigned and returned.
2. Determine whether `--demo` bypasses normal exit code handling.
3. Verify whether single-file analysis (`-f <file>`) returns exit code `2` on critical incidents.
4. Assess whether this behavior is an architectural design decision or a defect.
5. Implement **Option A** (Documentation & CLI Help Clarification):
   - Preserve all existing runtime application behavior and single-file exit code `2`.
   - Update `printUsage()` in `Main.java` to explicitly distinguish single-file analysis from demo mode.
   - Update `README.md` with structured reference tables.
   - Explicitly document that `--demo` returns `0` upon successful showcase completion, even if individual scenarios detect `CRITICAL` incidents.
   - Verify that the full Maven test suite continues passing.

---

## 2. Root Cause Analysis & Architectural Design

### Code Walkthrough: `src/main/java/com/loganalyzer/Main.java`

#### 1. How Exit Codes Are Dispatched in Single-File Mode:
```java
// Main.java: Lines 129-132
// Return non-zero exit code if critical incidents were detected
if (result.hasCriticalIncidents()) {
    System.exit(2);
}
```
In single-file mode (`-f <path>` or positional `<path>`), `Main.java` invokes `analyzer.analyze(path)`, yielding an `AnalysisResult`. 
In `src/main/java/com/loganalyzer/model/AnalysisResult.java`:
```java
public boolean hasCriticalIncidents() {
    return incidents.stream().anyMatch(inc -> inc.getSeverity() == IncidentSeverity.CRITICAL);
}
```
If any incident has severity `IncidentSeverity.CRITICAL`, `hasCriticalIncidents()` returns `true`, and `System.exit(2)` is invoked. If no critical incidents exist, `main()` terminates normally without calling `System.exit(2)`, and the JVM automatically exits with status `0`. If an argument or I/O error occurs, `System.exit(1)` is invoked.

#### 2. Why `--demo` Returned Exit Code `0`:
In `Main.java`, lines 59–62:
```java
if (config.isDemo() || (config.filePath() == null && args.length == 0)) {
    runDemonstration();
    return;
}
```
When `--demo` is passed:
1. Control jumps directly to `runDemonstration()` (lines 267–317).
2. `runDemonstration()` processes 4 diverse demo files sequentially (`normal-operations.log`, `critical-incident-database.log`, `security-attack.log`, and `out-of-memory.log`).
3. It prints console reports for all 4 scenarios.
4. `runDemonstration()` is a `void` method. It does not inspect `result.hasCriticalIncidents()` to determine process termination, nor does it call `System.exit(...)`.
5. Upon loop completion, control returns to line 61 (`return;`), immediately terminating `main()`.
6. Standard JVM semantics dictate that when `main()` returns normally, the process exits with code `0`.

#### 3. Intentional Design vs. Defect Assessment:
- **Interactive Showcase vs. Automated Monitoring Pipeline:**
  - Single-file analysis (`-f`) is an operational tool designed for server administration, monitoring daemons, and CI/CD quality gates, where exit code `2` indicates that human intervention is required.
  - Demonstration mode (`--demo`) is an **interactive presentation showcase** created for college evaluators, faculty grading, and user demonstrations to prove that the analyzer can parse and detect diverse incidents (normal traffic, database crashes, security breaches, OOM errors) without crashing.
  - If `--demo` returned exit code `2`, any automated CI test runner, shell script, or packaging verification harness running the demo would treat the showcase as a failed execution rather than a successful presentation.
- **Ambiguity in Documentation:**
  - The previous help text described exit codes generically without noting that exit code `2` was specific to single-target file evaluations.

---

## 3. Implementation of Option A

To eliminate user confusion while preserving seamless automated execution:

### 1. Update in `src/main/java/com/loganalyzer/Main.java`
Updated `printUsage()` to clearly separate single-file analysis exit codes from demonstration mode exit codes:
```java
    private static void printUsage() {
        System.out.println("""
            Log Analyzer and Incident Detector (Java 21)
            Usage:
              java -jar target/log-analyzer-incident-detector-1.0.0.jar [options]

            Options:
              -f,   --file <path>             Path to target log file to inspect
              -fmt, --format <console|md|json> Format of the generated report (default: console)
              -o,   --output <path>           Optional output destination file path
              -b,   --burst-threshold <num>   Consecutive error threshold for bursts (default: 5)
              -w,   --window <seconds>        Time window in seconds for burst detection (default: 60)
              --demo                          Execute demonstration against all bundled sample scenarios
              -h,   --help                    Display this help message

            Exit Codes (Single-File Analysis):
              0 - Analysis completed without critical issues
              1 - Error during execution or invalid inputs
              2 - One or more CRITICAL operational incidents detected

            Exit Codes (Demonstration Mode --demo):
              0 - Demonstration suite finished execution successfully (even if
                  individual sample scenarios detect CRITICAL incidents)
              1 - Execution error occurred during demonstration
            """);
    }
```

### 2. Update in `README.md`
Updated the Exit Codes documentation section into two dedicated reference tables:
```markdown
### Exit Codes

#### Single-File Analysis (`-f` / `--file`)

| Code | Meaning                                              |
| ---- | ---------------------------------------------------- |
| `0`  | Analysis completed without critical issues.          |
| `1`  | Invalid arguments, input, or execution error.        |
| `2`  | One or more critical operational incidents detected. |

#### Demonstration Mode (`--demo`)

| Code | Meaning                                                                                                              |
| ---- | -------------------------------------------------------------------------------------------------------------------- |
| `0`  | Demonstration suite finished execution successfully, even if individual sample scenarios detect critical incidents. |
| `1`  | Execution error occurred during demonstration suite.                                                                 |
```

---

## 4. Empirical Verification & Test Matrix

The compiled JAR (`target/log-analyzer-incident-detector-1.0.0.jar`) was tested across all operating scenarios using PowerShell `$LASTEXITCODE`:

| Scenario | Command Executed | Severity Detected | Actual Exit Code | Documented Contract | Status |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **CLI Help** | `java -jar ... -h` | N/A | **`0`** | `0` (Success) | ✅ Verified |
| **Demo Mode** | `java -jar ... --demo` | Multiple (incl. Critical) | **`0`** | `0` (Suite Completed) | ✅ Verified |
| **Critical DB Log** | `java -jar ... -f sample-logs/critical-incident-database.log` | `CRITICAL` (DB Pool Exhaustion) | **`2`** | `2` (Critical Incident) | ✅ Verified |
| **Critical OOM Log** | `java -jar ... -f sample-logs/out-of-memory.log` | `CRITICAL` (Heap Crash) | **`2`** | `2` (Critical Incident) | ✅ Verified |
| **Security Attack Log** | `java -jar ... -f sample-logs/security-attack.log` | `HIGH` (Security Breach) | **`0`** | `0` (Non-Critical) | ✅ Verified |
| **Normal Operations** | `java -jar ... -f sample-logs/normal-operations.log` | None | **`0`** | `0` (Clean Run) | ✅ Verified |
| **Non-Existent File** | `java -jar ... -f missing.log` | Execution Error | **`1`** | `1` (Input/IO Error) | ✅ Verified |
| **Invalid CLI Option** | `java -jar ... -b not_a_number` | Argument Parsing Error | **`1`** | `1` (Input/IO Error) | ✅ Verified |

---

## 5. Full Maven Test Suite Execution

All unit, parameterized, and end-to-end integration tests were executed via `mvn test`:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.loganalyzer.analyzer.AnomalyDetectorTest
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.analyzer.IncidentDetectorTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.analyzer.LogAnalyzerTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.analyzer.StatisticsAggregatorTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.integration.LogAnalyzerEndToEndTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.MainCliTest
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.parser.StandardLogParserTest
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.report.ReportGeneratorTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.util.FrequencyDistributionTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] Tests run: 144, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 6. Exact Git Diff of Changes

```diff
diff --git a/README.md b/README.md
index 2c9589c..7da5dad 100644
--- a/README.md
+++ b/README.md
@@ -291,12 +291,21 @@ Example:
 
 ### Exit Codes
 
+#### Single-File Analysis (`-f` / `--file`)
+
 | Code | Meaning                                              |
 | ---- | ---------------------------------------------------- |
 | `0`  | Analysis completed without critical issues.          |
 | `1`  | Invalid arguments, input, or execution error.        |
 | `2`  | One or more critical operational incidents detected. |
 
+#### Demonstration Mode (`--demo`)
+
+| Code | Meaning                                                                                                              |
+| ---- | -------------------------------------------------------------------------------------------------------------------- |
+| `0`  | Demonstration suite finished execution successfully, even if individual sample scenarios detect critical incidents. |
+| `1`  | Execution error occurred during demonstration suite.                                                                 |
+
 ---
 
 ## 🧪 Testing
diff --git a/src/main/java/com/loganalyzer/Main.java b/src/main/java/com/loganalyzer/Main.java
index 5719a94..00eddb4 100644
--- a/src/main/java/com/loganalyzer/Main.java
+++ b/src/main/java/com/loganalyzer/Main.java
@@ -331,10 +331,15 @@ public class Main {
               --demo                          Execute demonstration against all bundled sample scenarios
               -h,   --help                    Display this help message
 
-            Exit Codes:
+            Exit Codes (Single-File Analysis):
               0 - Analysis completed without critical issues
               1 - Error during execution or invalid inputs
               2 - One or more CRITICAL operational incidents detected
+
+            Exit Codes (Demonstration Mode --demo):
               0 - Demonstration suite finished execution successfully (even if
                   individual sample scenarios detect CRITICAL incidents)
               1 - Execution error occurred during demonstration
             """);
     }
 }
```

---

## 7. Modified Files Summary

1. [`src/main/java/com/loganalyzer/Main.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/Main.java) — Clarified CLI help output string in `printUsage()`.
2. [`README.md`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/README.md) — Documented single-file vs. demonstration mode exit codes.
3. [`docs/development-reports/Phase-3/CLI-EXIT-CODE-REPORT.md`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/docs/development-reports/Phase-3/CLI-EXIT-CODE-REPORT.md) — Permanent project documentation report.
