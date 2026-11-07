# Phase 2 — Task 2: Custom Generics Implementation Report

**Project:** `Log-Analyzer-Incident-Detector` (Java 21, Maven)  
**Academic Context:** 2nd-Year B.Tech Computer Science & Engineering (Object-Oriented Programming — 20 Marks)  
**Task:** Implementation of Custom Java Generics (`FrequencyDistribution<T>`)  
**Date:** 2026-11-07  
**Status:** Completed & 100% Verified (108 Tests Passing)  

---

## 1. Executive Summary

In this task, we implemented **Priority 2** of the Phase 2 roadmap: introducing custom Java Generics to the project.

Previously, the codebase only *consumed* built-in generic collections (`List<T>`, `Map<K, V>`). If asked in a B.Tech viva voce to demonstrate an authored generic class, the student had no custom generic types to present. Additionally, [`StatisticsAggregator.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/analyzer/StatisticsAggregator.java) duplicated manual map-counting operations and repeated a 10-line Stream sorting pipeline for both `topErrors` and `topLoggers`.

We introduced [`FrequencyDistribution<T>`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/util/FrequencyDistribution.java), a clean, beginner-friendly generic data structure parameterized by `<T>`. It was integrated directly into `StatisticsAggregator.java`, replacing repetitive map boilerplate with type-safe operations.

All 99 previous tests plus **9 new comprehensive unit tests** in [`FrequencyDistributionTest.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/test/java/com/loganalyzer/util/FrequencyDistributionTest.java) passed cleanly, bringing the total suite to **108 tests with 0 failures and 0 errors**.

---

## 2. Files Created and Modified

| File | Status | Description |
| :--- | :---: | :--- |
| [`src/main/java/com/loganalyzer/util/FrequencyDistribution.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/util/FrequencyDistribution.java) | **Created** | Reusable generic class parameterized by `<T>`. Manages item occurrence frequencies, total/distinct counts, and deterministic top-N ranking. |
| [`src/main/java/com/loganalyzer/analyzer/StatisticsAggregator.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/analyzer/StatisticsAggregator.java) | **Modified** | Integrated `FrequencyDistribution<String>` for tracking error frequencies and logger occurrences, replacing redundant map boilerplate and stream pipelines. |
| [`src/test/java/com/loganalyzer/util/FrequencyDistributionTest.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/test/java/com/loganalyzer/util/FrequencyDistributionTest.java) | **Created** | Dedicated test suite comprising 9 unit tests verifying type flexibility (`String`, `LogLevel`), counting, sorting, ties, null safety, and boundary conditions. |

---

## 3. Architecture & OOP Concepts Demonstrated

### 3.1 Generic Class Declaration & Type Parameter `<T>`
- **Declaration:**
  ```java
  public class FrequencyDistribution<T>
  ```
- **Type Parameter `<T>`:** Acts as a generic placeholder for any Java reference type.
  - When instantiated as `FrequencyDistribution<String>`, `T` is bound to `String` (used for error messages and logger names).
  - When instantiated as `FrequencyDistribution<LogLevel>`, `T` is bound to the `LogLevel` enum (tested in the test suite).
  - Can accept custom objects or any reference type.

### 3.2 Type Safety Without Explicit Casting
In pre-generics Java (prior to Java 5), collections held `Object`, requiring manual downcasts like `(String) entry.getKey()`.
With `FrequencyDistribution<T>`:
```java
public Map<T, Long> getTop(int limit)
```
- For `FrequencyDistribution<String>`, `getTop()` returns `Map<String, Long>` with compile-time type checking.
- If a developer accidentally attempts to add an integer to `FrequencyDistribution<String>`, the Java compiler flags an error immediately.

### 3.3 Stable Tie-Breaking via `LinkedHashMap`
- `FrequencyDistribution<T>` initializes its internal map with `new LinkedHashMap<>()`.
- In Java, `LinkedHashMap` guarantees predictable iteration order based on **insertion sequence**.
- In `getTop()`, Java's Stream `.sorted(Map.Entry.<T, Long>comparingByValue().reversed())` utilizes TimSort, a stable sorting algorithm. When two items have identical occurrence counts, their relative insertion order is preserved, ensuring **100% deterministic tie-breaking**.

### 3.4 Encapsulation & Defensive Design
- `counts` and `totalCount` are `private final` instance variables.
- `add(item)` and `add(item, amount)` safely ignore `null` items and non-positive amounts (`amount <= 0`) without throwing unexpected runtime exceptions.
- `getCounts()` returns `Collections.unmodifiableMap(counts)` to prevent callers from mutating the internal frequency state.

---

## 4. Code Implementation Summary

### 4.1 `FrequencyDistribution<T>` Highlights
```java
package com.loganalyzer.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FrequencyDistribution<T> {

    private final Map<T, Long> counts;
    private long totalCount;

    public FrequencyDistribution() {
        this.counts = new LinkedHashMap<>();
        this.totalCount = 0;
    }

    public void add(T item) {
        add(item, 1L);
    }

    public void add(T item, long amount) {
        if (item == null || amount <= 0) {
            return;
        }
        counts.put(item, counts.getOrDefault(item, 0L) + amount);
        totalCount += amount;
    }

    public long getCount(T item) {
        if (item == null) {
            return 0L;
        }
        return counts.getOrDefault(item, 0L);
    }

    public long getTotalCount() {
        return totalCount;
    }

    public int getDistinctCount() {
        return counts.size();
    }

    public Map<T, Long> getTop(int limit) {
        if (limit <= 0) {
            return Collections.emptyMap();
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<T, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public Map<T, Long> getCounts() {
        return Collections.unmodifiableMap(counts);
    }
}
```

### 4.2 Integration into `StatisticsAggregator.java`
```java
// Instantiation with generic type parameter String:
FrequencyDistribution<String> errorDistribution = new FrequencyDistribution<>();
FrequencyDistribution<String> loggerDistribution = new FrequencyDistribution<>();

// Within the log processing loop:
if (logger != null && !logger.isBlank()) {
    loggerDistribution.add(logger);
}

if (entry.getLevel().isError()) {
    String normalized = normalizeErrorMessage(entry.getMessage());
    errorDistribution.add(normalized);
}

// Extracting top-10 items in clean, expressive, 1-line statements:
Map<String, Long> topErrors = errorDistribution.getTop(10);
Map<String, Long> topLoggers = loggerDistribution.getTop(10);
```

---

## 5. Automated Test Suite Results

Maven test execution:
```powershell
mvn test
```

### Output:
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.loganalyzer.analyzer.AnomalyDetectorTest
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.analyzer.IncidentDetectorTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.analyzer.StatisticsAggregatorTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.integration.LogAnalyzerEndToEndTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.MainCliTest
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.parser.StandardLogParserTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.report.ReportGeneratorTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.loganalyzer.util.FrequencyDistributionTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 108, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### Coverage in `FrequencyDistributionTest`:
1. `testStringFrequencyTracking`: Accurate count tracking, distinct counts, total counts for `<String>`.
2. `testEnumGenericTracking`: Generic flexibility with `<LogLevel>` enum.
3. `testAddWithAmount`: Adding arbitrary amounts; ignoring non-positive and zero counts.
4. `testNullItemSafety`: Passing null items safely without exceptions.
5. `testGetTopSorting`: Verifying strict descending order by frequency.
6. `testTieBreakingDeterminism`: Ensuring insertion order is preserved during ties.
7. `testLimitEdgeCases`: Testing limit = 0, limit < 0, and limit > distinct items.
8. `testGetCountsUnmodifiable`: Confirming unmodifiable view prevents external mutation.
9. `testEmptyDistribution`: Verifying zero-state behavior.

---

## 6. Known Limitations & Architectural Notes

1. **In-Memory Tracking:** `FrequencyDistribution<T>` stores counts in memory via `LinkedHashMap`. For extremely large log files (> millions of unique distinct error patterns), memory scales with the cardinality of unique keys. Since log templates normalize numbers, cardinality remains low in practice.
2. **Not Synchronized:** Like standard Java collection classes (`HashMap`, `ArrayList`), `FrequencyDistribution<T>` is designed for single-threaded analysis execution within `StatisticsAggregator`. If accessed concurrently by multiple worker threads in the future, it can be wrapped with `Collections.synchronizedMap()` or use `ConcurrentHashMap`.

---

## 7. Viva Voce Cheat Sheet (Q&A for Students)

#### Q: "What are Java Generics, and why did you create `FrequencyDistribution<T>`?"
> **Answer:** "Java Generics enable types (classes and interfaces) to be parameters when defining classes, interfaces, and methods. I created `FrequencyDistribution<T>` to provide a reusable, type-safe data structure for counting and ranking any entity. For example, `StatisticsAggregator` uses `FrequencyDistribution<String>` to track recurring errors and top loggers, but it can equally track `FrequencyDistribution<LogLevel>` or `FrequencyDistribution<Integer>` without changing any code or casting."

#### Q: "What is Type Erasure in Java Generics?"
> **Answer:** "Type Erasure is the process by which the Java compiler removes generic type parameters during compilation to maintain backward compatibility with older JVM versions. At runtime, `FrequencyDistribution<String>` and `FrequencyDistribution<LogLevel>` both execute as the raw type `FrequencyDistribution`, with the compiler inserting the necessary casts at the call sites automatically. This guarantees that generics provide compile-time safety without runtime overhead."

#### Q: "How does `getTop(limit)` handle items with identical frequencies?"
> **Answer:** "It breaks ties deterministically by preserving insertion order. The internal map is a `LinkedHashMap`, which preserves insertion sequence. When we stream and sort the entries with `.sorted(Map.Entry.<T, Long>comparingByValue().reversed())`, Java uses TimSort, which is a stable sorting algorithm. Tied elements retain their original relative order from insertion, preventing random or platform-dependent ranking order."
