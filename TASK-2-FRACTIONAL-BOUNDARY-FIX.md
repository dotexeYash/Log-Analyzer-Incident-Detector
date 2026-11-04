# ⏱️ Phase 1 - Task 2: Fractional-Second Boundary Precision Fix
**Project:** Log Analyzer & Incident Detector (Java 21 / Maven)  
**Component:** `com.loganalyzer.analyzer.AnomalyDetector.java`  
**Date:** November 5, 2026  
**Status:** ✅ Successfully Implemented and Verified (34/34 tests passing)  

---

## 1. Problem Description

Previously, the sliding-window boundary condition inside `detectErrorBursts()` evaluated elapsed time using:
```java
if (Duration.between(leftTs, rightTs).getSeconds() > windowSeconds)
```

### The Flaw:
`Duration.getSeconds()` returns only the whole-second component of the duration, truncating any fractional seconds (milliseconds/nanoseconds).
* If `Duration.between(leftTs, rightTs)` was **60 seconds and 500 milliseconds (60.5s)**:
  * `.getSeconds()` returned `60`.
  * The condition `60 > 60` evaluated to **false**.
  * Consequently, an error occurring 60.5 seconds later was incorrectly treated as being within the 60.0-second window!

---

## 2. Precise Correction Implemented

### 1. Exact Duration Comparison
Replaced whole-second truncation with an exact `Duration` comparison:

```java
Duration maxWindowDuration = Duration.ofSeconds(windowSeconds);

// Inside sliding-window loop:
while (left <= right) {
    LocalDateTime leftTs = sortedTimestampedErrors.get(left).getTimestamp();
    // Exact duration comparison preserving fractional seconds
    if (Duration.between(leftTs, rightTs).compareTo(maxWindowDuration) > 0) {
        left++;
    } else {
        break;
    }
}
```

### Boundary Invariants Verified:
* **Exactly 60.0 seconds:**
  * `Duration.between(...)` is `Duration.ofSeconds(60)`.
  * `duration.compareTo(maxWindowDuration)` returns `0`.
  * Condition `0 > 0` is **false** $\implies$ `left` does not advance $\implies$ **accepted (inclusive boundary)**.
* **60.5 seconds (60 seconds and 500 milliseconds):**
  * `Duration.between(...)` is `Duration.ofSeconds(60, 500_000_000)`.
  * `duration.compareTo(maxWindowDuration)` returns `1` ($> 0$).
  * Condition `1 > 0` is **true** $\implies$ `left++` advances $\implies$ **rejected from this window**.

### 2. Sensible Duration Display Calculation in `emitBurstAnomaly()`
Reviewed and refined `emitBurstAnomaly()` so fractional durations are cleanly rounded up to the nearest whole second without truncating or under-reporting elapsed incident duration:

```java
Duration elapsed = Duration.between(startTs, endTs);
long elapsedSeconds = elapsed.toSeconds();
if (elapsed.toNanosPart() > 0) {
    elapsedSeconds++; // Ceiling rounding for fractional seconds
}
long actualDuration = Math.max(1, elapsedSeconds);
long displayWindow = Math.max(windowSeconds, actualDuration);
```
* **Scoring Conventions:** Completely preserved:
  ```java
  double score = Math.min(1.0, 0.5 + (totalCount / 20.0));
  ```

---

## 3. Unit Tests Added to `AnomalyDetectorTest.java`

Two targeted unit tests were added:

### Test 1: Five errors spanning 60.5 seconds must be rejected (No Anomaly)
```java
@Test
@DisplayName("Five errors spanning 60.5 seconds must be rejected when window is 60 seconds")
void testFiveErrorsSpanningSixtyPointFiveSecondsProducesNoBurstAnomaly() {
    LocalDateTime baseTime = LocalDateTime.of(2026, 11, 5, 1, 0, 0);
    List<LogEntry> entries = List.of(
            LogEntry.builder().lineNumber(1).timestamp(baseTime).level(LogLevel.ERROR).message("Error 1").build(),
            LogEntry.builder().lineNumber(2).timestamp(baseTime.plusSeconds(15)).level(LogLevel.ERROR).message("Error 2").build(),
            LogEntry.builder().lineNumber(3).timestamp(baseTime.plusSeconds(30)).level(LogLevel.ERROR).message("Error 3").build(),
            LogEntry.builder().lineNumber(4).timestamp(baseTime.plusSeconds(45)).level(LogLevel.ERROR).message("Error 4").build(),
            LogEntry.builder().lineNumber(5).timestamp(baseTime.plusSeconds(60).plusNanos(500_000_000)).level(LogLevel.ERROR).message("Error 5").build()
    );

    List<Anomaly> anomalies = detector.detectAnomalies(entries);
    boolean hasBurst = anomalies.stream()
            .anyMatch(a -> a.getType() == AnomalyType.ERROR_BURST);
    assertFalse(hasBurst, "Five errors spanning 60.5s must exceed 60s window and produce no burst anomaly");
}
```

### Test 2: Five errors spanning exactly 60.0 seconds must be accepted (1 Anomaly)
```java
@Test
@DisplayName("Five errors spanning exactly 60.0 seconds must be accepted as an inclusive burst")
void testFiveErrorsSpanningExactlySixtySecondsProducesBurstAnomaly() {
    LocalDateTime baseTime = LocalDateTime.of(2026, 11, 5, 1, 0, 0);
    List<LogEntry> entries = List.of(
            LogEntry.builder().lineNumber(1).timestamp(baseTime).level(LogLevel.ERROR).message("Error 1").build(),
            LogEntry.builder().lineNumber(2).timestamp(baseTime.plusSeconds(15)).level(LogLevel.ERROR).message("Error 2").build(),
            LogEntry.builder().lineNumber(3).timestamp(baseTime.plusSeconds(30)).level(LogLevel.ERROR).message("Error 3").build(),
            LogEntry.builder().lineNumber(4).timestamp(baseTime.plusSeconds(45)).level(LogLevel.ERROR).message("Error 4").build(),
            LogEntry.builder().lineNumber(5).timestamp(baseTime.plusSeconds(60)).level(LogLevel.ERROR).message("Error 5").build()
    );

    List<Anomaly> anomalies = detector.detectAnomalies(entries);
    List<Anomaly> bursts = anomalies.stream()
            .filter(a -> a.getType() == AnomalyType.ERROR_BURST)
            .toList();

    assertEquals(1, bursts.size(), "Five errors spanning exactly 60.0s must produce exactly 1 burst anomaly");
    assertEquals(5, bursts.get(0).getAffectedLogCount());
    assertEquals(baseTime, bursts.get(0).getStartTime());
    assertEquals(baseTime.plusSeconds(60), bursts.get(0).getEndTime());
}
```

---

## 4. Maven Test Suite Execution Results

Command executed:
```powershell
mvn test
```

Console Output:
```text
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.loganalyzer.analyzer.AnomalyDetectorTest
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
Running com.loganalyzer.analyzer.IncidentDetectorTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
Running com.loganalyzer.analyzer.StatisticsAggregatorTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
Running com.loganalyzer.integration.LogAnalyzerEndToEndTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
Running com.loganalyzer.parser.StandardLogParserTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
Running com.loganalyzer.report.ReportGeneratorTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0

Results:
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

All **34 tests** in the project pass with zero failures and zero errors.
