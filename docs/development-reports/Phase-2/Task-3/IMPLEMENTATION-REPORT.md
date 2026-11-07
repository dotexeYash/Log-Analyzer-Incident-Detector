# Phase 2 — Task 3: Dependency Injection Implementation Report

**Project:** `Log-Analyzer-Incident-Detector` (Java 21, Maven)  
**Academic Context:** 2nd-Year B.Tech Computer Science & Engineering (Object-Oriented Programming — 20 Marks)  
**Task:** Phase 2 – Task 3: Dependency Injection in `AnomalyDetector`  
**Date:** 2026-11-07  
**Status:** Completed & Verified — Awaiting Code Review & User Manual Commit  

---

## 1. Executive Summary

In Phase 2 – Task 3, we eliminated the hidden dependency anti-pattern inside [`AnomalyDetector`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java). Previously, `AnomalyDetector` instantiated its collaborating service directly inside a private method (`detectIdenticalErrors`) using `new StatisticsAggregator()`.

We implemented **Pure Constructor Injection** in standard Java:
1. `AnomalyDetector` now declares an explicit `private final StatisticsAggregator statisticsAggregator;` collaborator.
2. A new primary 7-parameter constructor accepts the `StatisticsAggregator` dependency and provides safe fallback against `null` inputs.
3. All existing constructors (0-arg and 6-arg) were preserved and chained using `this(...)` to maintain 100% backward compatibility.
4. An overloaded 1-argument constructor `AnomalyDetector(StatisticsAggregator)` was added for convenient testing and assembly.
5. In [`Main.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/Main.java), a single `StatisticsAggregator` instance is created and shared across both `AnomalyDetector` and `LogAnalyzer`.
6. Five new unit tests were added to [`AnomalyDetectorTest.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java) validating explicit injection, null fallback safety, constructor overloads, and normalization behavior.
7. The full Maven test suite executed **113 tests** with **0 failures and 0 errors**.

---

## 2. Code Changes Detailed

### 2.1 `src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java`

#### Added Dependency Field
```java
private final StatisticsAggregator statisticsAggregator;
```

#### Constructor Injection & Chaining Architecture
```java
/**
 * Primary constructor with explicit dependency injection and custom thresholds.
 *
 * @param statisticsAggregator the StatisticsAggregator instance for log processing and normalization
 * @param errorBurstThreshold threshold for error burst count
 * @param windowSeconds time window in seconds for error burst detection
 * @param identicalErrorThreshold threshold for repetitive identical error count
 * @param securityThreshold threshold for security incident occurrences
 * @param databaseFailureThreshold threshold for database failures
 * @param timeoutThreshold threshold for timeouts
 */
public AnomalyDetector(StatisticsAggregator statisticsAggregator,
                       int errorBurstThreshold,
                       int windowSeconds,
                       int identicalErrorThreshold,
                       int securityThreshold,
                       int databaseFailureThreshold,
                       int timeoutThreshold) {
    this.statisticsAggregator = statisticsAggregator != null ? statisticsAggregator : new StatisticsAggregator();
    this.errorBurstThreshold = errorBurstThreshold;
    this.windowSeconds = windowSeconds;
    this.identicalErrorThreshold = identicalErrorThreshold;
    this.securityThreshold = securityThreshold;
    this.databaseFailureThreshold = databaseFailureThreshold;
    this.timeoutThreshold = timeoutThreshold;
}

/**
 * Overloaded constructor with dependency injection and default thresholds.
 *
 * @param statisticsAggregator the StatisticsAggregator instance to inject
 */
public AnomalyDetector(StatisticsAggregator statisticsAggregator) {
    this(statisticsAggregator, 5, 60, 4, 3, 2, 3);
}

/**
 * Overloaded constructor for threshold customization with default StatisticsAggregator.
 * Preserves 100% backward compatibility with existing callers.
 */
public AnomalyDetector(int errorBurstThreshold,
                       int windowSeconds,
                       int identicalErrorThreshold,
                       int securityThreshold,
                       int databaseFailureThreshold,
                       int timeoutThreshold) {
    this(new StatisticsAggregator(), errorBurstThreshold, windowSeconds, identicalErrorThreshold, securityThreshold, databaseFailureThreshold, timeoutThreshold);
}

/**
 * Default zero-argument constructor using default thresholds and default StatisticsAggregator.
 */
public AnomalyDetector() {
    this(new StatisticsAggregator(), 5, 60, 4, 3, 2, 3);
}

/**
 * Returns the injected StatisticsAggregator instance.
 *
 * @return the StatisticsAggregator dependency
 */
public StatisticsAggregator getStatisticsAggregator() {
    return statisticsAggregator;
}
```

#### Refactored `detectIdenticalErrors()` Method
Replaced the local `StatisticsAggregator aggregator = new StatisticsAggregator();` instantiation with the injected instance:
```java
private void detectIdenticalErrors(List<LogEntry> entries, List<Anomaly> anomalies, AtomicInteger counter) {
    Map<String, List<LogEntry>> errorGroups = new LinkedHashMap<>();

    for (LogEntry entry : entries) {
        if (entry.getLevel().isError()) {
            String normalized = this.statisticsAggregator.normalizeErrorMessage(entry.getMessage());
            errorGroups.computeIfAbsent(normalized, k -> new ArrayList<>()).add(entry);
        }
    }
    // ... grouping and threshold checks unchanged
}
```

---

### 2.2 `src/main/java/com/loganalyzer/Main.java`

Instantiated a shared `StatisticsAggregator` in the application composition root and injected it into both `AnomalyDetector` and `LogAnalyzer`:

```java
// BEFORE:
AnomalyDetector anomalyDetector = new AnomalyDetector(config.burstThreshold(), config.windowSeconds(), 4, 3, 2, 3);
LogAnalyzer analyzer = new LogAnalyzer(new StandardLogParser(), new StatisticsAggregator(), anomalyDetector, new IncidentDetector());

// AFTER:
StatisticsAggregator statisticsAggregator = new StatisticsAggregator();
AnomalyDetector anomalyDetector = new AnomalyDetector(statisticsAggregator, config.burstThreshold(), config.windowSeconds(), 4, 3, 2, 3);
LogAnalyzer analyzer = new LogAnalyzer(new StandardLogParser(), statisticsAggregator, anomalyDetector, new IncidentDetector());
```

---

### 2.3 `src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java`

Added 5 comprehensive unit tests targeting dependency injection capabilities:

1. **`testExplicitDependencyInjection()`**: Verifies that when a caller passes a specific `StatisticsAggregator` instance, `AnomalyDetector` stores and uses that exact instance (`assertSame`).
2. **`testNullDependencyFallback()`**: Verifies that passing `null` as the dependency does not cause a `NullPointerException`; the constructor safely falls back to a default non-null aggregator and anomaly detection runs without exception.
3. **`testNullDependencyFallbackOneArgConstructor()`**: Verifies that the overloaded `AnomalyDetector((StatisticsAggregator) null)` also safely falls back to a default non-null instance.
4. **`testConstructorOverloads()`**: Verifies that all 4 constructor variants (default 0-arg, 1-arg DI, 6-arg thresholds, and 7-arg primary DI) correctly initialize the detector and its dependency.
5. **`testInjectedAggregatorNormalizesInIdenticalErrorDetection()`**: Verifies that varying error messages (e.g. differing Order IDs) are properly normalized through the injected `StatisticsAggregator` during identical error anomaly detection.

---

## 3. Verification & Test Results

### 3.1 Maven Test Suite Execution

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.loganalyzer.analyzer.AnomalyDetectorTest
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.240 s -- in com.loganalyzer.analyzer.AnomalyDetectorTest
[INFO] Running com.loganalyzer.analyzer.IncidentDetectorTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.020 s -- in com.loganalyzer.analyzer.IncidentDetectorTest
[INFO] Running com.loganalyzer.analyzer.StatisticsAggregatorTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.060 s -- in com.loganalyzer.analyzer.StatisticsAggregatorTest
[INFO] Running com.loganalyzer.integration.LogAnalyzerEndToEndTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.188 s -- in com.loganalyzer.integration.LogAnalyzerEndToEndTest
[INFO] Running com.loganalyzer.MainCliTest
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.198 s -- in com.loganalyzer.MainCliTest
[INFO] Running com.loganalyzer.parser.StandardLogParserTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.058 s -- in com.loganalyzer.parser.StandardLogParserTest
[INFO] Running com.loganalyzer.report.ReportGeneratorTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.039 s -- in com.loganalyzer.report.ReportGeneratorTest
[INFO] Running com.loganalyzer.util.FrequencyDistributionTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.020 s -- in com.loganalyzer.util.FrequencyDistributionTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 113, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 3.2 Test Growth Summary

| Phase & Task | Total Tests | Pass Count | Failures | Status |
| :--- | :---: | :---: | :---: | :---: |
| Phase 1 Milestone Baseline | 94 | 94 | 0 | PASSED |
| Phase 2 – Task 1 (`AbstractReportGenerator`) | 99 | 99 | 0 | PASSED |
| Phase 2 – Task 2 (`FrequencyDistribution<T>`) | 108 | 108 | 0 | PASSED |
| **Phase 2 – Task 3 (Dependency Injection)** | **113** | **113** | **0** | **PASSED** |

---

## 4. OOP Design & Viva Voce Guide

### Q1: What is Dependency Injection (DI) and why was it applied to `AnomalyDetector`?
> **Answer:** "Dependency Injection is a design technique where an object receives its collaborating dependencies from an external assembler (such as a composition root or caller) rather than creating them internally using `new`. In `AnomalyDetector`, it previously called `new StatisticsAggregator()` inside a private method, which hid the dependency and caused redundant object allocations on every analysis run. By injecting `StatisticsAggregator` through the constructor, we decoupled the classes, improved testability, and enabled dependency sharing."

### Q2: Why did you choose Constructor Injection over Setter or Field Injection?
> **Answer:** "Constructor injection ensures that an object cannot be created in an incomplete, partially initialized, or invalid state. It allows collaborating fields to be marked `final`, ensuring immutability and thread safety. Setter injection allows objects to change dependencies after creation (mutability risk), and Field injection often requires reflection or framework annotations that bypass the compiler."

### Q3: Why did you use Pure Java DI instead of Spring or Google Guice?
> **Answer:** "For a standalone command-line application and academic project, adding heavyweight inversion-of-control frameworks introduces unwanted JAR overhead, startup latency, and unnecessary complexity. Pure constructor injection in plain Java fulfills the Dependency Inversion Principle (DIP) and Inversion of Control (IoC) naturally without third-party libraries."

### Q4: How did you ensure backward compatibility when adding the new constructor?
> **Answer:** "We utilized Java constructor overloading and constructor chaining (`this(...)`). The existing 6-parameter and default zero-parameter constructors continue to work identically by delegating to the primary 7-parameter constructor with a default `new StatisticsAggregator()`. No existing caller or test was broken."

---

## 5. Files Changed Summary

| File | Type | Changes Made |
| :--- | :---: | :--- |
| [`src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java) | Main Code | Added `private final StatisticsAggregator` field, primary DI constructor, null-fallback, overloaded constructors, and replaced internal `new StatisticsAggregator()` in `detectIdenticalErrors()`. |
| [`src/main/java/com/loganalyzer/Main.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/Main.java) | Application Entry | Created a single `StatisticsAggregator` instance and shared it between `AnomalyDetector` and `LogAnalyzer`. |
| [`src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java) | Test Code | Added 5 unit tests verifying explicit injection, null fallback, constructor overloads, and normalization detection. |

---

## 6. Known Limitations & Next Steps

- **Known Limitations:** None. All CLI flags, output formats (Console, Markdown, JSON), and analysis metrics behave identically to baseline.
- **Git State:** Working tree has uncommitted modifications in the 3 targeted files. No files have been staged or committed. Ready for user code review.
