# 📐 Solution B Design Report: Sliding Window with Overlapping Interval Merging
**Project:** Log Analyzer & Incident Detector (Java 21 / Maven)  
**Component:** `com.loganalyzer.analyzer.AnomalyDetector.java` -> `detectErrorBursts()`  
**Status:** Design Proposal (Pre-Implementation)  
**Author:** Pair Programming Agent  

---

## 1. Requirements & Design Objectives

1. **Preserve Configuration:** Retain existing `errorBurstThreshold` (default: 5) and `windowSeconds` (default: 60) without breaking current defaults.
2. **True Two-Pointer Sliding Window:** Use two pointers $(L, R)$ over timestamp-sorted error entries.
3. **Comprehensive Detection:** Identify every qualifying window where $R - L + 1 \ge \text{errorBurstThreshold}$ within $t_R - t_L \le \text{windowSeconds}$.
4. **Interval Merging:** Merge overlapping qualifying windows into a single unified burst anomaly to prevent duplicate alerts.
5. **Separation of Distinct Events:** Ensure bursts separated by a gap greater than the window remain independent anomalies.
6. **Backward Compatibility:** Preserve existing `Anomaly` fields, scoring conventions, sample logs, and existing tests.
7. **Resilience:** Safely handle null timestamps and preserve the sequence-based fallback.
8. **Optimal Complexity:** Achieve strict $\mathcal{O}(N)$ linear time complexity.

---

## 2. Proposed Java Implementation for `detectErrorBursts()`

```java
private void detectErrorBursts(List<LogEntry> entries, List<Anomaly> anomalies, AtomicInteger counter) {
    List<LogEntry> errorEntries = entries.stream()
            .filter(e -> e.getLevel().isError())
            .toList();

    if (errorEntries.size() < errorBurstThreshold) {
        return;
    }

    // Filter timestamped entries while safely handling null timestamps
    List<LogEntry> timestampedErrors = errorEntries.stream()
            .filter(e -> e.getTimestamp() != null)
            .toList();

    if (!timestampedErrors.isEmpty()) {
        int n = timestampedErrors.size();
        int left = 0;

        // Tracks the active merged burst index interval [burstStartIdx, burstEndIdx]
        int burstStartIdx = -1;
        int burstEndIdx = -1;

        for (int right = 0; right < n; right++) {
            LocalDateTime rightTs = timestampedErrors.get(right).getTimestamp();

            // Shrink window from the left until duration <= windowSeconds
            while (left <= right) {
                LocalDateTime leftTs = timestampedErrors.get(left).getTimestamp();
                if (Duration.between(leftTs, rightTs).getSeconds() > windowSeconds) {
                    left++;
                } else {
                    break;
                }
            }

            int currentWindowCount = right - left + 1;

            // Check if current sliding window meets the burst threshold
            if (currentWindowCount >= errorBurstThreshold) {
                if (burstStartIdx == -1) {
                    // Initialize first active burst interval
                    burstStartIdx = left;
                    burstEndIdx = right;
                } else if (left <= burstEndIdx) {
                    // OVERLAP DETECTED: New window overlaps with active burst -> merge and extend
                    burstEndIdx = Math.max(burstEndIdx, right);
                } else {
                    // GAP DETECTED: New window is separated from active burst -> commit previous
                    emitBurstAnomaly(timestampedErrors, burstStartIdx, burstEndIdx, anomalies, counter);
                    burstStartIdx = left;
                    burstEndIdx = right;
                }
            }
        }

        // Commit the final active burst if one exists
        if (burstStartIdx != -1) {
            emitBurstAnomaly(timestampedErrors, burstStartIdx, burstEndIdx, anomalies, counter);
        }

    } else {
        // Sequence-based fallback when timestamps are absent
        if (errorEntries.size() >= errorBurstThreshold) {
            anomalies.add(new Anomaly(
                    "ANOM-" + counter.getAndIncrement(),
                    AnomalyType.ERROR_BURST,
                    String.format("Sequential error concentration: %d error logs detected in sequence", errorEntries.size()),
                    null,
                    null,
                    errorEntries.size(),
                    errorEntries.subList(0, Math.min(5, errorEntries.size())),
                    0.75
            ));
        }
    }
}

private void emitBurstAnomaly(List<LogEntry> errors, int startIdx, int endIdx, 
                              List<Anomaly> anomalies, AtomicInteger counter) {
    int totalCount = endIdx - startIdx + 1;
    LocalDateTime startTs = errors.get(startIdx).getTimestamp();
    LocalDateTime endTs = errors.get(endIdx).getTimestamp();

    long actualDuration = Math.max(1, Duration.between(startTs, endTs).getSeconds());
    long displayWindow = Math.max(windowSeconds, actualDuration);

    double score = Math.min(1.0, 0.5 + (totalCount / 20.0));

    anomalies.add(new Anomaly(
            "ANOM-" + counter.getAndIncrement(),
            AnomalyType.ERROR_BURST,
            String.format("Error burst: %d errors within %d seconds (threshold: %d)",
                    totalCount, displayWindow, errorBurstThreshold),
            startTs,
            endTs,
            totalCount,
            errors.subList(startIdx, Math.min(startIdx + 5, endIdx + 1)),
            score
    ));
}
```

---

## 3. Mathematical Walkthrough of the User's Scenario

* **Parameters:** `windowSeconds = 60`, `errorBurstThreshold = 5`
* **Error Timestamps:** $0, 10, 20, 30, 40, 50, 65, 70, 75, 80$ (10 errors: $E_0 \dots E_9$)

### Step-by-Step Trace:

| `right` | Timestamp | `left` adjustment | Window $[L, R]$ | Window Count | Threshold Met ($\ge 5$)? | Active Burst $[B_{\text{start}}, B_{\text{end}}]$ Action |
| :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| `0` | $0\text{s}$ | $L=0$ | $[0, 0]$ | 1 | No | None |
| `1` | $10\text{s}$ | $L=0$ | $[0, 1]$ | 2 | No | None |
| `2` | $20\text{s}$ | $L=0$ | $[0, 2]$ | 3 | No | None |
| `3` | $30\text{s}$ | $L=0$ | $[0, 3]$ | 4 | No | None |
| `4` | $40\text{s}$ | $L=0$ | $[0, 4]$ | 5 | **Yes** | First burst initialized: $[0, 4]$ |
| `5` | $50\text{s}$ | $L=0$ | $[0, 5]$ | 6 | **Yes** | $L(0) \le B_{\text{end}}(4) \implies$ Overlap! Extended to $[0, 5]$ |
| `6` | $65\text{s}$ | $65-0 > 60 \implies L=1$ | $[1, 6]$ | 6 | **Yes** | $L(1) \le B_{\text{end}}(5) \implies$ Overlap! Extended to $[0, 6]$ |
| `7` | $70\text{s}$ | $70-10 \le 60 \implies L=1$ | $[1, 7]$ | 7 | **Yes** | $L(1) \le B_{\text{end}}(6) \implies$ Overlap! Extended to $[0, 7]$ |
| `8` | $75\text{s}$ | $75-10 > 60 \implies L=2$ | $[2, 8]$ | 7 | **Yes** | $L(2) \le B_{\text{end}}(7) \implies$ Overlap! Extended to $[0, 8]$ |
| `9` | $80\text{s}$ | $80-20 \le 60 \implies L=2$ | $[2, 9]$ | 8 | **Yes** | $L(2) \le B_{\text{end}}(8) \implies$ Overlap! Extended to $[0, 9]$ |

### Result:
* When the loop finishes, active burst $[0, 9]$ is emitted.
* **Total Anomalies:** Exactly **1** (`ANOM-1`).
* **Error Count:** `10`.
* **Span:** `0s` to `80s`.
* **Conclusion:** 
  * Eliminates the false negative (previously, $E_6 \dots E_9$ was skipped).
  * Prevents duplicate alert spam.

---

## 4. Behavior with Two Separate Bursts (With a Gap)

### Scenario:
* **Burst 1:** 5 errors at $t = 0\text{s}, 10\text{s}, 20\text{s}, 30\text{s}, 40\text{s}$ ($E_0 \dots E_4$).
* **Gap:** No errors for 600 seconds (10 minutes).
* **Burst 2:** 5 errors at $t = 640\text{s}, 650\text{s}, 660\text{s}, 670\text{s}, 680\text{s}$ ($E_5 \dots E_9$).

### Execution Trace:
1. At $R = 4$ ($40\text{s}$), the first qualifying window $[0, 4]$ is detected:
   $$\text{Active Burst}: [B_{\text{start}} = 0, B_{\text{end}} = 4]$$
2. For $R = 5, 6, 7, 8$: duration from $L$ exceeds $60\text{s}$, $L$ moves forward, count remains $< 5$.
3. At $R = 9$ ($680\text{s}$):
   * $L$ shrinks to index $5$ ($640\text{s}$). Duration: $680 - 640 = 40\text{s} \le 60\text{s}$.
   * Count is $9 - 5 + 1 = 5 \ge 5$ (qualifying window $[5, 9]$).
   * **Overlap Check:**
     $$L (5) \le B_{\text{end}} (4) \implies 5 \le 4 \quad (\mathbf{FALSE})$$
   * **Action:**
     1. Emits **Burst 1** as `ANOM-1` (Start: $0\text{s}$, End: $40\text{s}$, Count: 5).
     2. Sets new Active Burst: $[B_{\text{start}} = 5, B_{\text{end}} = 9]$.
4. At end of loop:
   * Emits **Burst 2** as `ANOM-2` (Start: $640\text{s}$, End: $680\text{s}$, Count: 5).
* **Conclusion:** Separate, distinct incidents with a large gap are correctly reported as independent anomalies.

---

## 5. Complexity Analysis

| Metric | Complexity | Explanation |
| :--- | :---: | :--- |
| **Time Complexity** | **$\mathcal{O}(N)$** | The pointer `right` advances from $0$ to $N-1$ ($N$ steps). The pointer `left` only moves forward and advances at most $N$ steps across the entire function. Inside the loop, window math and index checks are $\mathcal{O}(1)$. Total time is strictly linear $\mathcal{O}(N)$. |
| **Space Complexity** | **$\mathcal{O}(M)$** | Stores error entries where $M \le N$. The two pointers and interval index variables consume $\mathcal{O}(1)$ auxiliary space. |

---

## 6. Planned Unit Tests (`AnomalyDetectorTest.java`)

1. **`testOverlappingErrorBurstsMergedIntoSingleAnomaly`:**
   * Feed: 10 errors at timestamps $[0, 10, 20, 30, 40, 50, 65, 70, 75, 80]$.
   * Assertions: Exactly 1 anomaly produced; affected count = 10; start = 0s, end = 80s.
2. **`testSeparateBurstsWithLargeGapRemainDistinct`:**
   * Feed: 5 errors at $[0, 10, 20, 30, 40]$ followed by 5 errors at $[640, 650, 660, 670, 680]$.
   * Assertions: Exactly 2 separate anomalies; Anomaly 1 has 5 errors (0s..40s); Anomaly 2 has 5 errors (640s..680s).
3. **`testNullTimestampSafetyAndSequenceFallback`:**
   * Feed: Error logs with null timestamps.
   * Assertion: Sequence fallback runs cleanly without `NullPointerException`.
4. **`testSubThresholdErrorsDoNotTriggerBurst`:**
   * Feed: 4 errors within 30 seconds (threshold 5).
   * Assertion: 0 anomalies detected.
