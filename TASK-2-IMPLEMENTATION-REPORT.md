# 🚀 Phase 1 - Task 2 Implementation Report: Overlapping Error-Burst Detection
**Project:** Log Analyzer & Incident Detector (Java 21 / Maven)  
**Task:** Replace naive `i = j` skipping with Solution B (Two-Pointer Sliding Window with Overlapping Interval Merging and Local Timestamp Sorting).  
**Date:** November 5, 2026  
**Status:** ✅ Successfully Implemented, Tested, and Verified (32/32 tests passing)  

---

## 1. List of Modified Files

Only two files were modified (no unrelated files touched, no commits made):

| File | Status | Description of Change |
| :--- | :---: | :--- |
| `src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java` | **Modified** | Replaced `i = j` skip logic in `detectErrorBursts()` with local stable sorting and a two-pointer sliding window that merges overlapping burst intervals. Extracted `emitBurstAnomaly()`. |
| `src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java` | **Modified** | Added 6 new unit tests verifying overlapping merges, distinct bursts with gaps, unsorted timestamp handling, input list immutability, null fallback, and sub-threshold non-triggers. |

---

## 2. Explanation of the Algorithm

### Overview
The algorithm operates on error entries using a two-pointer sliding window $(L, R)$ over chronologically sorted timestamps to identify all windows satisfying:
$$R - L + 1 \ge \text{errorBurstThreshold} \quad \text{within} \quad t_R - t_L \le \text{windowSeconds}$$

### Step-by-Step Flow:

1. **Local Safe Sorting (Preserving Original List):**
   * Filters all error entries with non-null timestamps into a local collection `sortedTimestampedErrors`.
   * Sorts this local collection using:
     ```java
     Comparator.comparing(LogEntry::getTimestamp)
               .thenComparingLong(LogEntry::getLineNumber)
     ```
   * **Crucial Invariant:** The caller's original `entries` list in `LogAnalyzer` remains strictly in its original input order. Each `LogEntry` retains its original immutable physical `lineNumber`.

2. **Two-Pointer Sliding Window:**
   * Pointers `left` and `right` initialize at index 0.
   * `right` advances from $0$ to $N - 1$.
   * For each `right`, `left` advances forward until:
     $$\text{Duration}(t_{\text{left}}, t_{\text{right}}) \le \text{windowSeconds}$$
   * The current window size is:
     $$\text{currentWindowCount} = \text{right} - \text{left} + 1$$

3. **Interval Merging Logic:**
   * An active burst interval $[\text{burstStartIdx}, \text{burstEndIdx}]$ is maintained.
   * When $\text{currentWindowCount} \ge \text{errorBurstThreshold}$:
     * **First Window:** If no active burst exists ($\text{burstStartIdx} == -1$), initialize:
       $$\text{burstStartIdx} = \text{left}, \quad \text{burstEndIdx} = \text{right}$$
     * **Overlapping Window ($\text{left} \le \text{burstEndIdx}$):** The new window starts before or at the end of the current active burst. It is merged by expanding:
       $$\text{burstEndIdx} = \max(\text{burstEndIdx}, \text{right})$$
     * **Non-Overlapping Window ($\text{left} > \text{burstEndIdx}$):** A temporal gap exists. The previous active burst is committed and emitted via `emitBurstAnomaly()`, and a new active burst starts at $[\text{left}, \text{right}]$.

4. **Final Flush:**
   * After the loop finishes, if an active burst remains ($\text{burstStartIdx} \ne -1$), it is emitted.

5. **Sequence-Based Fallback for Missing Timestamps:**
   * If all error timestamps are `null`, the algorithm preserves the legacy sequential concentration detection:
     $$\text{errorEntries.size()} \ge \text{errorBurstThreshold}$$

---

## 3. Complexity Analysis

| Metric | Complexity | Explanation |
| :--- | :---: | :--- |
| **Time Complexity** | **$\mathcal{O}(M \log M)$ worst-case / $\mathcal{O}(M)$ best-case** | Local sorting with Java's TimSort takes $\mathcal{O}(M)$ for already-sorted logs and $\mathcal{O}(M \log M)$ for scrambled logs ($M \le N$). The two-pointer sliding window runs in strict $\mathcal{O}(M)$ linear time because `right` moves forward $M$ times and `left` advances at most $M$ times total. |
| **Space Complexity** | **$\mathcal{O}(M)$** | Stores error entries where $M \le N$. The two pointers, interval index variables, and anomaly builders require $\mathcal{O}(1)$ auxiliary memory. |

---

## 4. Test Results

Command executed:
```powershell
mvn test
```

### Complete Test Results:
```text
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.loganalyzer.analyzer.AnomalyDetectorTest
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
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
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### New Unit Tests Added (6 Tests):

1. **`testOverlappingErrorBurstsMergedIntoSingleAnomaly`:**
   * Input: 10 errors at offsets $[0, 10, 20, 30, 40, 50, 65, 70, 75, 80\text{s}]$.
   * Verified: Exactly **1 anomaly** produced, containing all **10 affected errors**, spanning $0\text{s}$ to $80\text{s}$. (Previously, the second 6-error spike was missed).
2. **`testSeparateBurstsWithLargeGapRemainDistinct`:**
   * Input: 5 errors at $[0\dots40\text{s}]$ followed by a 600s gap, then 5 errors at $[640\dots680\text{s}]$.
   * Verified: Exactly **2 separate anomalies** (`ANOM-1` with 5 errors, `ANOM-2` with 5 errors).
3. **`testUnsortedTimestampsHandledCorrectly`:**
   * Input: Scrambled offsets $[40\text{s}, 10\text{s}, 30\text{s}, 0\text{s}, 20\text{s}]$.
   * Verified: Correctly sorted and merged into 1 burst spanning $0\text{s}$ to $40\text{s}$.
4. **`testOriginalInputListOrderRemainsUnchanged`:**
   * Input: An `ArrayList<LogEntry>` with entries in lines $[1, 2, 3, 4, 5]$ with scrambled timestamps $[50\text{s}, 10\text{s}, 30\text{s}, 0\text{s}, 20\text{s}]$.
   * Verified: The input list was not mutated or reordered; line numbers and positions remain identical.
5. **`testNullTimestampSequenceFallback`:**
   * Input: 6 error logs with `null` timestamps.
   * Verified: Emits `ERROR_BURST` with `"Sequential error concentration"` without `NullPointerException`.
6. **`testFourErrorsBelowThresholdProduceNoBurstAnomaly`:**
   * Input: 4 errors within 30 seconds (threshold 5).
   * Verified: 0 anomalies detected.

---

## 5. Limitations Discovered & Trade-offs

1. **Very Long Sustained Outages Coalesce into One Large Anomaly:**
   * If a system experiences a continuous failure storm where an error arrives every 10 seconds for 2 hours, all overlapping windows will merge into a single prolonged anomaly (e.g. `Error burst: 720 errors within 7200 seconds`).
   * *Trade-off analysis:* This is usually preferred by SRE/incident response teams because 1 unified incident ticket is generated rather than 50 duplicate alerts for the same underlying database crash.
2. **Mixed Timestamp Boundaries:**
   * In files where 90% of logs have timestamps and 10% have `null` timestamps, the null-timestamp entries do not participate in the temporal window. However, they are still counted in `LogStatistics` and evaluated by pattern rules (OOM, security).
