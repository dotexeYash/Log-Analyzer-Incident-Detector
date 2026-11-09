# Phase 3 – Task 4: Edge-Case Testing & Production Defect Resolution Report

**Project:** Log Analyzer & Incident Detector (Academic B.Tech CSE Project — 20 Marks)  
**Date:** November 9, 2026  
**Phase:** Phase 3 – Testing & Final Quality Assurance  
**Task:** Task 4 – Edge-Case Testing for Log Parsing and End-to-End Analysis  
**Status:** Completed & Verified (Production Defect Approved, Fixed & Regression Tested)

---

## 1. Executive Summary

Phase 3 – Task 4 focused on edge-case testing of the log parsing pipeline, anomaly detection, statistical aggregation, and full end-to-end analysis workflows.

During initial edge-case validation, an **out-of-order timestamp calculation defect** was identified in `AnomalyDetector.java` (lines 303–305 and 354–355). As required by protocol, work stopped and the defect was formally reported. Following user approval, the defect was resolved using a private helper method (`extractSortedTimestamps`), and **5 targeted regression tests** were added to `AnomalyDetectorTest.java`.

A total of **14 new test cases** (5 in `StandardLogParserTest`, 4 in `LogAnalyzerEndToEndTest`, 5 in `AnomalyDetectorTest`) have been implemented and verified.

All **136 tests** in the project test suite pass with zero failures, zero errors, and zero skipped tests.

---

## 2. Test Scenarios Implemented & Verified Behaviors

### Scenario 1: Empty File (Zero-Byte)
* **Goal:** Verify that a zero-byte file is handled safely without throwing unexpected exceptions, yielding 0 parsed entries and nominal empty statistics.
* **Tests Added:**
  - `StandardLogParserTest#testEmptyZeroByteFileParsesToEmptyList`
  - `LogAnalyzerEndToEndTest#testEmptyZeroByteFileEndToEnd`
* **Verified Behavior:**
  - `StandardLogParser.parse(emptyPath)` returns an empty `List<LogEntry>` (size 0).
  - `LogAnalyzer.analyze(emptyPath)` produces an `AnalysisResult` with:
    - 0 parsed entries.
    - Total logs = 0 in `LogStatistics`.
    - First and last timestamps are `null`.
    - Empty anomaly list and empty incident list.
    - `hasCriticalIncidents()` returns `false`.
    - No `NullPointerException`, `IndexOutOfBoundsException`, or `InvalidLogFileException` is thrown.

### Scenario 2: Blank File (Spaces, Tabs, Newlines)
* **Goal:** Verify that a file consisting solely of whitespace characters (`' '`, `'\t'`, `'\r'`, `'\n'`) does not produce spurious or invalid log entries.
* **Tests Added:**
  - `StandardLogParserTest#testBlankFileWithWhitespaceAndNewlines`
  - `LogAnalyzerEndToEndTest#testBlankFileWithWhitespaceEndToEnd`
* **Verified Behavior:**
  - `StandardLogParser` checks `line.trim().isEmpty()` and skips empty/blank lines.
  - Returns 0 log entries.
  - End-to-end analysis produces nominal empty results with 0 total logs and no incidents.

### Scenario 3: Corrupted Log Entries & Malformed Timestamps
* **Goal:** Test handling of completely unparseable lines and malformed timestamps according to intended specifications.
* **Tests Added:**
  - `StandardLogParserTest#testCorruptedUnparseableLinesAndMalformedTimestamps`
* **Verified Behavior:**
  - Direct single-line parsing (`parseLine`) on unparseable unstructured text returns `null`.
  - Stream/file parsing via `parse(InputStream)`:
    - When an unparseable line appears at the beginning of a stream (before any valid entry exists, `currentEntry == null`), the parser creates a fallback entry with `LogLevel.UNKNOWN`, logger `UNKNOWN`, `null` timestamp, and the raw line as the message.
    - Invalid calendar timestamps (e.g. month 13, day 35, Feb 30) safely fail timestamp parsing and assign `null` without throwing exceptions or terminating analysis.

### Scenario 4: Out-of-Order Timestamps
* **Goal:** Test log entries whose timestamps appear in non-chronological order and observe parser, statistics aggregator, and anomaly detector behavior.
* **Tests Added:**
  - `StandardLogParserTest#testOutOfOrderTimestampsParsing`
  - `LogAnalyzerEndToEndTest#testOutOfOrderTimestampsErrorBurstAndStatisticsEndToEnd`
  - `AnomalyDetectorTest#testIdenticalErrorsWithOutOfOrderTimestamps`
  - `AnomalyDetectorTest#testPatternAnomaliesWithOutOfOrderTimestamps`
* **Verified Behavior:**
  - `StandardLogParser` processes lines strictly sequentially as they appear in the file, preserving raw order in the parsed entry list.
  - `StatisticsAggregator`:
    - Compares each timestamp using `firstTimestamp.isAfter(ts)` and `lastTimestamp.isBefore(ts)`.
    - Correctly calculates the true minimum timestamp (`firstTimestamp`) and true maximum timestamp (`lastTimestamp`) regardless of input order.
  - `AnomalyDetector#detectErrorBursts`:
    - Locally sorts all error entries chronologically using `Comparator.comparing(LogEntry::getTimestamp)`.
    - Successfully detects error bursts within a sliding window and produces valid time ranges where `startTime <= endTime`.
  - `AnomalyDetector#detectIdenticalErrors` & `AnomalyDetector#detectPatternAnomalies`:
    - Non-null timestamps are extracted and chronologically sorted.
    - `startTime` is guaranteed to be $\le$ `endTime`.

### Scenario 5: Mixed Input (Valid, Malformed, Blank, Stack Traces)
* **Goal:** Test realistic log files containing a mixture of valid structured entries, unparseable preamble, multi-line Java stack traces, blank lines, and warnings.
* **Tests Added:**
  - `StandardLogParserTest#testMixedInputWithValidEntriesCorruptedLinesAndStackTraces`
  - `LogAnalyzerEndToEndTest#testMixedContentFileEndToEnd`
* **Verified Behavior:**
  - Preamble unformatted lines create a fallback `UNKNOWN` entry.
  - Valid log lines create their respective entries (`INFO`, `ERROR`, `WARN`).
  - Continuation lines (indented stack traces, `Caused by:`) following a valid log entry are appended to that entry's stack trace collection.
  - Blank lines are skipped without breaking multi-line association.
  - All log level counts and stack trace text (`java.sql.SQLException: Connection refused`) are preserved accurately.

---

## 3. Production Defect Resolution (AnomalyDetector.java)

### Defect Identified
`detectIdenticalErrors()` and `detectPatternAnomalies()` previously extracted `matches.get(0).getTimestamp()` as `start` and `matches.get(size - 1).getTimestamp()` as `end`.
- When log entries were out-of-order, `start.isAfter(end)` could evaluate to `true` (an inverted time window).
- When the first matched entry had a `null` timestamp, `start` became `null` even if subsequent entries had valid timestamps.

### Approved Fix Implemented
In `src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java`:
1. Introduced a private helper method `extractSortedTimestamps(List<LogEntry> entries)`:
   ```java
   private List<LocalDateTime> extractSortedTimestamps(List<LogEntry> entries) {
       List<LocalDateTime> timestamps = new ArrayList<>();
       for (LogEntry entry : entries) {
           if (entry != null && entry.getTimestamp() != null) {
               timestamps.add(entry.getTimestamp());
           }
       }
       Collections.sort(timestamps);
       return timestamps;
   }
   ```
2. Updated `detectIdenticalErrors()`:
   ```java
   List<LocalDateTime> timestamps = extractSortedTimestamps(matches);
   LocalDateTime start = timestamps.isEmpty() ? null : timestamps.get(0);
   LocalDateTime end = timestamps.isEmpty() ? null : timestamps.get(timestamps.size() - 1);
   ```
3. Updated `detectPatternAnomalies()`:
   ```java
   List<LocalDateTime> timestamps = extractSortedTimestamps(matched);
   LocalDateTime start = timestamps.isEmpty() ? null : timestamps.get(0);
   LocalDateTime end = timestamps.isEmpty() ? null : timestamps.get(timestamps.size() - 1);
   ```

### Regression Tests Added (`AnomalyDetectorTest.java`)
1. `testIdenticalErrorsWithOutOfOrderTimestamps`: Verifies that identical errors occurring in reverse chronological order produce a window where `startTime` is the earliest timestamp and `endTime` is the latest (`assertFalse(startTime.isAfter(endTime))`).
2. `testPatternAnomaliesWithOutOfOrderTimestamps`: Verifies that pattern-matched timeout entries with scrambled timestamps set `startTime` and `endTime` chronologically.
3. `testMatchingEntryWithNullTimestampFollowedByValidTimestamps`: Verifies that a leading entry with `null` timestamp does not discard subsequent valid timestamps.
4. `testAllMatchingEntriesWithNullTimestamps`: Verifies that when all matching entries lack timestamps, safe null behavior is preserved (`startTime == null`, `endTime == null`, affected count preserved, no exception).
5. `testPatternAnomalyWithLeadingNullTimestamp`: Verifies that security pattern anomalies preserve valid timestamps even when preceded by an unparseable null timestamp.

---

## 4. Complete Test Suite Execution Results

Executed command:
```powershell
mvn test
```

### Results by Test Class:
| Test Class | Tests Run | Failures | Errors | Skipped | Status |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `AnomalyDetectorTest` | 29 | 0 | 0 | 0 | PASSED |
| `IncidentDetectorTest` | 11 | 0 | 0 | 0 | PASSED |
| `StatisticsAggregatorTest` | 11 | 0 | 0 | 0 | PASSED |
| `LogAnalyzerEndToEndTest` | 11 | 0 | 0 | 0 | PASSED |
| `MainCliTest` | 31 | 0 | 0 | 0 | PASSED |
| `StandardLogParserTest` | 22 | 0 | 0 | 0 | PASSED |
| `ReportGeneratorTest` | 12 | 0 | 0 | 0 | PASSED |
| `FrequencyDistributionTest` | 9 | 0 | 0 | 0 | PASSED |
| **TOTAL** | **136** | **0** | **0** | **0** | **BUILD SUCCESS** |

Total execution time: ~11.5 seconds on Windows 11 / OpenJDK 21.0.13.

---

## 5. Modified Files

1. `src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java` (+20 lines, -4 lines)
   - Fixed timestamp range calculation for `detectIdenticalErrors` and `detectPatternAnomalies`.
   - Added `extractSortedTimestamps` private helper method.
2. `src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java` (+128 lines)
   - Added 5 regression tests covering out-of-order identical errors, out-of-order pattern anomalies, leading null timestamp preservation, all null timestamps, and security pattern null timestamp handling.
3. `src/test/java/com/loganalyzer/parser/StandardLogParserTest.java` (+92 lines)
   - Added 5 edge-case tests: empty file, whitespace file, corrupted lines/timestamps, out-of-order parsing, mixed input.
4. `src/test/java/com/loganalyzer/integration/LogAnalyzerEndToEndTest.java` (+100 lines)
   - Added 4 end-to-end edge-case tests: empty file e2e, blank file e2e, out-of-order timestamps statistics/bursts e2e, mixed content e2e.

*(Note: `JsonReportGenerator.java`, `ReportGeneratorTest.java`, and `IncidentDetectorTest.java` reflect previous Phase 3 Tasks 2 & 3 changes, uncommitted as requested).*

No git staging or commits have been performed.
