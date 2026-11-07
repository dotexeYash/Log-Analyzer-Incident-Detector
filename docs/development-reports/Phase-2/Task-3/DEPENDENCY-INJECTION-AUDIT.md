# Phase 2 — Task 3: Dependency Injection Architecture Audit Report

**Project:** `Log-Analyzer-Incident-Detector` (Java 21, Maven)  
**Academic Context:** 2nd-Year B.Tech Computer Science & Engineering (Object-Oriented Programming — 20 Marks)  
**Task:** Read-Only Dependency Injection Audit of `AnomalyDetector`  
**Date:** 2026-11-07  
**Status:** Investigation Completed — Awaiting Approval Before Implementation  

---

## 1. Executive Summary

In software architecture and object-oriented design, **Dependency Injection (DI)** is a technique where an object receives its collaborating dependencies from the outside rather than creating them internally using the `new` keyword.

During the Phase 2 OOP Architecture Audit, a hidden dependency was identified inside [`AnomalyDetector.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java):
```java
// AnomalyDetector.java line 243 (inside detectIdenticalErrors):
StatisticsAggregator aggregator = new StatisticsAggregator();
```

This audit evaluated all occurrences of the `new` keyword inside `AnomalyDetector`, investigated all locations where `AnomalyDetector` is instantiated across the project, and designed a beginner-friendly constructor injection pattern that eliminates hard-coded coupling while maintaining 100% backward compatibility and test stability.

---

## 2. Investigation Findings

### 2.1 Audit of the `new` Keyword inside `AnomalyDetector.java`

A search for all `new` instantiations in `AnomalyDetector.java` revealed the following:

| Line # | Code Construct | Classification | Needs Injection? |
| :---: | :--- | :--- | :---: |
| 74 | `new ArrayList<>()` | Internal data structure (local collection) | No |
| 75 | `new AtomicInteger(1)` | Local anomaly ID sequencer | No |
| 199 | `new Anomaly(...)` | Domain model output value object | No |
| 229 | `new Anomaly(...)` | Domain model output value object | No |
| **243** | **`new StatisticsAggregator()`** | **Collaborating Service Dependency** | **YES** |
| 244 | `new LinkedHashMap<>()` | Internal data structure (local grouping map) | No |
| 249 | `new ArrayList<>()` | Internal bucket collection | No |
| 260 | `new Anomaly(...)` | Domain model output value object | No |
| 295 | `new ArrayList<>()` | Internal matching collection | No |
| 311 | `new Anomaly(...)` | Domain model output value object | No |

**Finding:** `StatisticsAggregator` is the **only** collaborating service dependency instantiated internally within `AnomalyDetector`. All other uses of `new` are local data structures or domain output records.

---

### 2.2 The Identified Coupling Issue (Hidden Dependency Anti-Pattern)

Inside `detectIdenticalErrors()` ([line 243](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java#L243)):
```java
private void detectIdenticalErrors(List<LogEntry> entries, List<Anomaly> anomalies, AtomicInteger counter) {
    StatisticsAggregator aggregator = new StatisticsAggregator(); // <-- Hardcoded Dependency!
    Map<String, List<LogEntry>> errorGroups = new LinkedHashMap<>();

    for (LogEntry entry : entries) {
        if (entry.getLevel().isError()) {
            String normalized = aggregator.normalizeErrorMessage(entry.getMessage());
            errorGroups.computeIfAbsent(normalized, k -> new ArrayList<>()).add(entry);
        }
    }
    ...
```

#### Why This Is an Anti-Pattern:
1. **Hidden Dependency:** Callers of `AnomalyDetector` have no indication from its constructor parameters that it depends on `StatisticsAggregator`.
2. **Redundant Allocations:** A new instance of `StatisticsAggregator` is created on every call to `detectIdenticalErrors()` (and therefore on every log analysis run).
3. **Redundant Instances in the Pipeline:**
   - In [`Main.java:78-79`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/Main.java#L78-L79):
     `Main` instantiates `new StatisticsAggregator()`, passes it to `LogAnalyzer`, and also instantiates `new AnomalyDetector()`.
   - `AnomalyDetector` then creates a *second* `new StatisticsAggregator()`.
   - In [`LogAnalyzer.java:31`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/analyzer/LogAnalyzer.java#L31):
     `LogAnalyzer` creates a `new StatisticsAggregator()`, and also creates an `AnomalyDetector` which creates another `StatisticsAggregator`.
4. **Untestable in Isolation:** If a developer or tester wishes to supply a custom or mock `StatisticsAggregator` with specific normalization behavior, they cannot do so because `AnomalyDetector` forces instantiation of the default class.

---

### 2.3 Inventory of All Instantiations of `AnomalyDetector`

Across `src/main/java` and `src/test/java`, `AnomalyDetector` is instantiated in exactly **3 places**:

1. [`src/main/java/com/loganalyzer/Main.java:78`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/Main.java#L78):
   ```java
   AnomalyDetector anomalyDetector = new AnomalyDetector(config.burstThreshold(), config.windowSeconds(), 4, 3, 2, 3);
   LogAnalyzer analyzer = new LogAnalyzer(new StandardLogParser(), new StatisticsAggregator(), anomalyDetector, new IncidentDetector());
   ```
2. [`src/main/java/com/loganalyzer/analyzer/LogAnalyzer.java:31`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/analyzer/LogAnalyzer.java#L31):
   ```java
   public LogAnalyzer() {
       this(new StandardLogParser(), new StatisticsAggregator(), new AnomalyDetector(), new IncidentDetector());
   }
   ```
3. [`src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java:23`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java#L23):
   ```java
   @BeforeEach
   void setUp() {
       detector = new AnomalyDetector(5, 60, 4, 3, 2, 3);
   }
   ```

---

## 3. Recommended Design: Pure Constructor Injection

We recommend implementing **Pure Constructor Injection** (also known as Dependency Injection without a framework). This maintains simplicity and is easily explainable by a second-year B.Tech student during a viva.

### 3.1 Field & Constructor Architecture in `AnomalyDetector`

```java
public class AnomalyDetector {

    // Injected Collaborator
    private final StatisticsAggregator statisticsAggregator;

    // Detection Thresholds
    private final int errorBurstThreshold;
    private final int windowSeconds;
    private final int identicalErrorThreshold;
    private final int securityThreshold;
    private final int databaseFailureThreshold;
    private final int timeoutThreshold;

    /**
     * Primary constructor with explicit dependency injection and custom thresholds.
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
     */
    public AnomalyDetector(StatisticsAggregator statisticsAggregator) {
        this(statisticsAggregator, 5, 60, 4, 3, 2, 3);
    }

    /**
     * Overloaded constructor for threshold customization with default aggregator.
     * Preserves 100% backwards compatibility with existing callers.
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
     * Default zero-argument constructor for standard operations.
     */
    public AnomalyDetector() {
        this(new StatisticsAggregator(), 5, 60, 4, 3, 2, 3);
    }
```

### 3.2 Refactoring `detectIdenticalErrors()`
Line 243 simply changes from:
```java
// BEFORE:
StatisticsAggregator aggregator = new StatisticsAggregator();
String normalized = aggregator.normalizeErrorMessage(entry.getMessage());
```
to:
```java
// AFTER:
String normalized = this.statisticsAggregator.normalizeErrorMessage(entry.getMessage());
```

### 3.3 Sharing the Dependency in `Main.java` and `LogAnalyzer.java`
In [`Main.java:78-80`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/Main.java#L78-L80):
```java
StatisticsAggregator statisticsAggregator = new StatisticsAggregator();
AnomalyDetector anomalyDetector = new AnomalyDetector(statisticsAggregator, config.burstThreshold(), config.windowSeconds(), 4, 3, 2, 3);
LogAnalyzer analyzer = new LogAnalyzer(new StandardLogParser(), statisticsAggregator, anomalyDetector, new IncidentDetector());
```
Now, a single `StatisticsAggregator` instance is shared cleanly across both `AnomalyDetector` and `LogAnalyzer`.

In [`LogAnalyzer.java:31`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/analyzer/LogAnalyzer.java#L31):
```java
public LogAnalyzer() {
    this(new StandardLogParser(), new StatisticsAggregator());
}

private LogAnalyzer(LogParser parser, StatisticsAggregator statsAggregator) {
    this(parser, statsAggregator, new AnomalyDetector(statsAggregator), new IncidentDetector());
}
```
*(Or keep `this(new StandardLogParser(), new StatisticsAggregator(), new AnomalyDetector(), new IncidentDetector());` which automatically leverages the chaining constructor).*

---

## 4. Impact Assessment & Risk Analysis

| Aspect | Impact | Risk Level |
| :--- | :--- | :---: |
| **Existing Tests (`AnomalyDetectorTest`)** | Zero changes required due to constructor overloading. | **None** |
| **CLI Functionality & Flags** | Untouched. Flags `-b` and `-w` continue to configure burst threshold and window. | **None** |
| **Analysis Accuracy & Output** | Untouched. Normalization logic and identical error grouping remain identical. | **None** |
| **Performance & Memory** | Improved. Eliminates repetitive `new StatisticsAggregator()` allocations during loops. | **Positive** |
| **Complexity** | Minimal. Uses pure Java constructor chaining (`this(...)`) without reflection or external frameworks. | **None** |

---

## 5. Files Requiring Modification

| File | Status | Description |
| :--- | :---: | :--- |
| [`src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/analyzer/AnomalyDetector.java) | **Modify** | Add `private final StatisticsAggregator statisticsAggregator;` field, add DI constructors, and use field in `detectIdenticalErrors()`. |
| [`src/main/java/com/loganalyzer/Main.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/Main.java) | **Modify** | Share single `StatisticsAggregator` instance between `AnomalyDetector` and `LogAnalyzer`. |
| [`src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/test/java/com/loganalyzer/analyzer/AnomalyDetectorTest.java) | **Modify** | Add dedicated test cases verifying constructor injection and fallback null-safety. |

---

## 6. Suggested Test Cases for Verification

1. **Explicit Injection Test:** Verify `new AnomalyDetector(customAggregator, ...)` correctly utilizes the injected aggregator during `detectAnomalies()`.
2. **Null-Safety Test:** Verify passing `null` as the `StatisticsAggregator` argument safely falls back to a non-null default without throwing `NullPointerException`.
3. **Overloaded Constructor Tests:** Verify `new AnomalyDetector()`, `new AnomalyDetector(aggregator)`, and `new AnomalyDetector(thresholds...)` all initialize valid, functioning detectors.
4. **Full Regression Suite:** Ensure all 108 existing tests continue to pass with 0 failures and 0 errors.

---

## 7. Viva Voce Cheat Sheet (Q&A for Students)

#### Q: "What is Dependency Injection, and why did you use it in `AnomalyDetector`?"
> **Answer:** "Dependency Injection is a design pattern where an object receives its dependencies from an external assembler rather than creating them internally using `new`. In `AnomalyDetector`, it previously called `new StatisticsAggregator()` inside a private method, which created a tight, hidden coupling. By injecting `StatisticsAggregator` through the constructor, `AnomalyDetector` becomes loosely coupled, avoids redundant object creation, and can easily be supplied with custom or mock aggregators for testing."

#### Q: "Why did you use Pure Java Constructor Injection instead of Spring or Guice?"
> **Answer:** "For a focused core application, introducing large enterprise frameworks like Spring or Google Guice adds unnecessary complexity, configuration overhead, and heavy JAR dependencies. Pure constructor injection in plain Java fulfills Inversion of Control (IoC) and the Dependency Inversion Principle (DIP) naturally, keeping the codebase lightweight, readable, and easy to explain."

---

## 8. Conclusion

The proposed dependency injection refactoring is clean, solves a concrete architectural issue, and preserves complete backwards compatibility.

Awaiting user approval before proceeding with implementation.
