# 🛡️ Phase 1 - Task 1: Fix False-Positive Security Anomaly Detection
**Project:** Log Analyzer & Incident Detector (Java 21 / Maven)  
**Task:** Eliminate false-positive security anomaly detections on non-critical (`INFO`/`DEBUG`/`TRACE`) log messages.  
**Date:** November 4, 2026  
**Status:** ✅ Successfully Implemented, Tested, and Verified  

---

## 1. Executive Summary

During the initial code audit of the **Log Analyzer & Incident Detector** project, a false-positive vulnerability was identified in `AnomalyDetector.java`. The system was matching regex security attack keywords (`unauthorized`, `forbidden`, `access denied`, etc.) across **all** log entries regardless of their log severity level (`LogLevel`).

As a result, benign application events—such as an `INFO` message informing a user that an unauthorized URL was redirected, or a `DEBUG` log evaluating access control rules—were falsely counted as security breach attempts. If 3 such entries occurred, the system raised an anomaly (`SECURITY_SUSPICIOUS_ACCESS`) and escalated it into a `HIGH: Coordinated Authentication / Unauthorized Access Surge` incident.

### Solution Overview
1. Modified `AnomalyDetector.java` to filter entries by minimum log severity (`LogLevel.WARN`) before evaluating the security pattern.
2. Leveraged **Method Overloading** to introduce a clean, backwards-compatible helper method `detectPatternAnomalies()`.
3. Created 3 comprehensive unit tests in `AnomalyDetectorTest.java` targeting `INFO` and `DEBUG` benign lines, as well as `ERROR` security threats.
4. Validated with the full test suite (`26/26` tests passing) and confirmed that real attacks in `security-attack.log` continue to be correctly detected.

---

## 2. Root Cause Analysis

### Before the Fix:
In `com.loganalyzer.analyzer.AnomalyDetector.java`, the detection method was invoked as follows:

```java
// Line ~95 of AnomalyDetector.java
detectPatternAnomalies(
        entries,
        SECURITY_PATTERN,
        AnomalyType.SECURITY_SUSPICIOUS_ACCESS,
        securityThreshold,
        0.85,
        "Elevated frequency of unauthorized or suspicious access attempts",
        anomalies,
        counter
);
```

Inside `detectPatternAnomalies()`:
```java
private void detectPatternAnomalies(List<LogEntry> entries,
                                    Pattern pattern,
                                    AnomalyType anomalyType,
                                    int threshold,
                                    double baseScore,
                                    String description,
                                    List<Anomaly> anomalies,
                                    AtomicInteger counter) {
    List<LogEntry> matched = new ArrayList<>();
    for (LogEntry entry : entries) {
        String combined = entry.getCombinedMessage();
        // ❌ BUG: Matches regardless of whether entry is INFO, DEBUG, or TRACE!
        if (pattern.matcher(combined).find()) {
            matched.add(entry);
        }
    }
    // If matched.size() >= 3, flagged as ANOMALY!
}
```

Because `pattern.matcher(combined).find()` had no log level check, any log containing words like `"unauthorized"` or `"forbidden"` was counted, even if it was just informational:
```text
2026-11-04 10:00:00 [main] INFO com.sec.Filter - User clicked unauthorized link, redirecting to login
```

---

## 3. Exact Code Changes (Git Diff)

### File 1: `src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java`

#### 1. Added import for `LogLevel`:
```diff
--- a/src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java
+++ b/src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java
@@ -3,6 +3,7 @@ package com.loganalyzer.analyzer;
 import com.loganalyzer.model.Anomaly;
 import com.loganalyzer.model.AnomalyType;
 import com.loganalyzer.model.LogEntry;
+import com.loganalyzer.model.LogLevel;
```

#### 2. Updated security anomaly trigger to enforce `LogLevel.WARN`:
```diff
@@ -95,6 +96,7 @@ public class AnomalyDetector {
         // 4. Security Anomalies (only consider WARN, ERROR, and FATAL entries to avoid false positives on INFO/DEBUG)
         detectPatternAnomalies(
                 entries,
                 SECURITY_PATTERN,
                 AnomalyType.SECURITY_SUSPICIOUS_ACCESS,
+                LogLevel.WARN,
                 securityThreshold,
                 0.85,
                 "Elevated frequency of unauthorized or suspicious access attempts",
```

#### 3. Added overloaded `detectPatternAnomalies()` method:
```diff
@@ -235,6 +237,21 @@ public class AnomalyDetector {
     private void detectPatternAnomalies(List<LogEntry> entries,
                                         Pattern pattern,
                                         AnomalyType anomalyType,
+                                        int threshold,
+                                        double baseScore,
+                                        String description,
+                                        List<Anomaly> anomalies,
+                                        AtomicInteger counter) {
+        detectPatternAnomalies(entries, pattern, anomalyType, null, threshold, baseScore, description, anomalies, counter);
+    }
+
+    private void detectPatternAnomalies(List<LogEntry> entries,
+                                        Pattern pattern,
+                                        AnomalyType anomalyType,
+                                        LogLevel minimumLevel,
                                         int threshold,
                                         double baseScore,
                                         String description,
@@ -244,6 +261,9 @@ public class AnomalyDetector {
         List<LogEntry> matched = new ArrayList<>();
         for (LogEntry entry : entries) {
+            if (minimumLevel != null && !entry.getLevel().isAtLeast(minimumLevel)) {
+                continue;
+            }
             String combined = entry.getCombinedMessage();
             if (pattern.matcher(combined).find()) {
                 matched.add(entry);
```

---

## 4. Unit Tests Added

### File: `src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java`

Three dedicated test cases were added to guarantee no regression and prove the fix works:

```java
@Test
@DisplayName("INFO messages containing 'unauthorized' must not trigger a security anomaly")
void testInfoMessagesContainingUnauthorizedDoNotTriggerSecurityAnomaly() {
    List<LogEntry> entries = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
        entries.add(LogEntry.builder()
                .lineNumber(i + 1)
                .timestamp(LocalDateTime.now().plusSeconds(i))
                .level(LogLevel.INFO)
                .message("User requested unauthorized resource, redirected to login page")
                .build());
    }

    List<Anomaly> anomalies = detector.detectAnomalies(entries);
    boolean hasSecurityAnomaly = anomalies.stream()
            .anyMatch(a -> a.getType() == AnomalyType.SECURITY_SUSPICIOUS_ACCESS);
    assertFalse(hasSecurityAnomaly, "INFO messages containing 'unauthorized' must not be flagged as security anomalies");
}

@Test
@DisplayName("DEBUG messages containing 'forbidden' must not trigger a security anomaly")
void testDebugMessagesContainingForbiddenDoNotTriggerSecurityAnomaly() {
    List<LogEntry> entries = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
        entries.add(LogEntry.builder()
                .lineNumber(i + 1)
                .timestamp(LocalDateTime.now().plusSeconds(i))
                .level(LogLevel.DEBUG)
                .message("Evaluating security filter: path /public is forbidden=false")
                .build());
    }

    List<Anomaly> anomalies = detector.detectAnomalies(entries);
    boolean hasSecurityAnomaly = anomalies.stream()
            .anyMatch(a -> a.getType() == AnomalyType.SECURITY_SUSPICIOUS_ACCESS);
    assertFalse(hasSecurityAnomaly, "DEBUG messages containing 'forbidden' must not be flagged as security anomalies");
}

@Test
@DisplayName("ERROR messages containing suspicious authentication activity should be detected")
void testErrorMessagesContainingSuspiciousAuthActivityTriggerSecurityAnomaly() {
    List<LogEntry> entries = List.of(
            LogEntry.builder().lineNumber(1).level(LogLevel.ERROR).message("401 Unauthorized: Brute-force attack pattern detected from IP 10.0.0.1").build(),
            LogEntry.builder().lineNumber(2).level(LogLevel.ERROR).message("Access Denied: Unauthorized access attempt to /api/v1/internal/admin-keys").build(),
            LogEntry.builder().lineNumber(3).level(LogLevel.ERROR).message("403 Forbidden: Invalid authentication token signature").build()
    );

    List<Anomaly> anomalies = detector.detectAnomalies(entries);
    boolean hasSecurityAnomaly = anomalies.stream()
            .anyMatch(a -> a.getType() == AnomalyType.SECURITY_SUSPICIOUS_ACCESS);
    assertTrue(hasSecurityAnomaly, "ERROR logs containing security attack signatures must be detected");
}
```

---

## 5. Verification & Maven Test Results

Command executed:
```powershell
mvn test
```

Console Output:
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.loganalyzer.analyzer.AnomalyDetectorTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.157 s -- in com.loganalyzer.analyzer.AnomalyDetectorTest
[INFO] Running com.loganalyzer.analyzer.IncidentDetectorTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.029 s -- in com.loganalyzer.analyzer.IncidentDetectorTest
[INFO] Running com.loganalyzer.analyzer.StatisticsAggregatorTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.039 s -- in com.loganalyzer.analyzer.StatisticsAggregatorTest
[INFO] Running com.loganalyzer.integration.LogAnalyzerEndToEndTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.095 s -- in com.loganalyzer.integration.LogAnalyzerEndToEndTest
[INFO] Running com.loganalyzer.parser.StandardLogParserTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.026 s -- in com.loganalyzer.parser.StandardLogParserTest
[INFO] Running com.loganalyzer.report.ReportGeneratorTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.028 s -- in com.loganalyzer.report.ReportGeneratorTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 6. End-to-End Real World Validation

Command executed:
```powershell
java -jar target/log-analyzer-incident-detector-1.0.0.jar -f sample-logs/security-attack.log
```

Result:
```text
[3] DETECTED ANOMALIES (1)
  • [ANOM-1] Suspicious Security Activity | Score: 0.99 | Affected Logs: 7
    Details: Elevated frequency of unauthorized or suspicious access attempts (Found 7 matching occurrences)
    Window : 2026-11-03 01:20:00 to 2026-11-03 01:20:05

[4] ACTIONABLE OPERATIONAL INCIDENTS (1)
  Incident ID : INC-001 [HIGH]
  Title       : HIGH: Coordinated Authentication / Unauthorized Access Surge
  Category    : Security Breach Attempt / Unauthorized Burst
  Remediation Recommendations:
    1. Isolate and ban abusive source IP addresses at Cloudflare / API Gateway rate-limiter.
    2. Validate status of identity provider (OAuth/JWT signer) and check token expiration policies.
    3. Check for credential stuffing or brute-force attack patterns on authentication endpoints.
    4. Enforce Multi-Factor Authentication (MFA) and enable CAPTCHA on exposed login forms.
```

All 7 real warning/error attack vectors were properly detected, while informational messages were cleanly ignored.

---

## 7. Modified Files Summary

| File | Status | Description of Change |
| :--- | :---: | :--- |
| `src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java` | Modified | Added `LogLevel.WARN` filter via method overloading for security checks |
| `src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java` | Modified | Added 3 unit tests verifying `INFO`/`DEBUG` exclusion and `ERROR` detection |
