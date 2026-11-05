# 🔬 Phase 1 – Task 5: Timestamp Parsing Investigation Report

**Project:** Log Analyzer & Incident Detector (Java 21 / Maven)  
**Component Under Review:** `com.loganalyzer.parser.StandardLogParser.java`  
**Subject:** Timestamp parsing failures on single-digit months and days (`yyyy-M-d`, `yyyy-MM-d`, `yyyy-M-dd`)  
**Date:** November 6, 2026  
**Status:** 🔍 Investigation Complete (Pre-Implementation / Awaiting Review)  

---

## 1. Executive Summary

An audit of [`StandardLogParser.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/parser/StandardLogParser.java) confirms the user's suspicion: **the current log parser fails completely on valid dates containing single-digit months or days**, such as:
- `2026-1-6 10:30:45` (single-digit month and single-digit day)
- `2026-01-6 10:30:45` (two-digit month and single-digit day)
- `2026-1-06 10:30:45` (single-digit month and two-digit day)
- `2026/1/6 10:30:45` (slashed dates with single digits)

The failure occurs at **two independent defensive layers**:
1. **The Log Line Regex Matchers:** `PATTERN_STANDARD`, `PATTERN_BRACKETED`, and `PATTERN_SIMPLE` all hardcode `\\d{4}[-/]\\d{2}[-/]\\d{2}`. Because `\\d{2}` strictly requires two digits, any line with a single-digit month or day fails regex matching and returns `null` from `parseLine()`. In a file stream, such a line is misclassified as a stack trace continuation line of the preceding entry or dropped into a fallback unparsed entry.
2. **The `DateTimeFormatter` Pattern:** `FLEXIBLE_FORMATTER` uses `.appendPattern("[yyyy-MM-dd][yyyy/MM/dd]")`. In Java's `java.time` package, `MM` and `dd` strictly enforce 2-digit padding; even if the regex captured the string, `DateTimeFormatter` would throw `DateTimeParseException` and set `timestamp` to `null`.

---

## 2. Complete Timestamp Parsing Implementation

Timestamp extraction in [`StandardLogParser.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/parser/StandardLogParser.java) is implemented through two components:

### 2.1. Date Formatter Definition ([Lines 28–39](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/parser/StandardLogParser.java#L28-L39))
```java
    private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd][yyyy/MM/dd]")
            .appendPattern("['T'][' ']")
            .appendPattern("HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .optionalStart()
            .appendPattern("[X][XXX][Z]")
            .optionalEnd()
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();
```

### 2.2. Timestamp Regex Patterns in Log Format Matchers ([Lines 42–57](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/parser/StandardLogParser.java#L42-L57))
```java
    // Pattern 1: 2026-11-03 01:14:02.123 [thread-1] INFO com.service.UserService - Message
    private static final Pattern PATTERN_STANDARD = Pattern.compile(
            "^(\\d{4}[-/]\\d{2}[-/]\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)" +
            "\\s+(?:\\[([^\\]]+)\\]\\s+)?([A-Za-z]{3,8})\\s+(?:(?:(?:\\[([^\\]]+)\\])|([a-zA-Z0-9_.$#\\-]+))\\s*[-:]\\s+)(.*)$"
    );

    // Pattern 2: [2026-11-03 01:14:02.123] [INFO] [thread-1] [com.service.UserService] Message
    private static final Pattern PATTERN_BRACKETED = Pattern.compile(
            "^\\[(\\d{4}[-/]\\d{2}[-/]\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)\\]" +
            "\\s+\\[([A-Za-z]{3,8})\\](?:\s+\\[([^\\]]+)\\])?(?:\s+\\[([^\\]]+)\\])?\\s*[-:]?\\s*(.*)$"
    );

    // Pattern 3: 2026-11-03 01:14:02 INFO Message
    private static final Pattern PATTERN_SIMPLE = Pattern.compile(
            "^(\\d{4}[-/]\\d{2}[-/]\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)" +
            "\\s+([A-Za-z]{3,8})\\s*[-:]?\\s*(.*)$"
    );
```

### 2.3. Timestamp Parsing Helper ([Lines 202–213](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/parser/StandardLogParser.java#L202-L213))
```java
    private LocalDateTime parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            // Normalize commas used in milliseconds (e.g. 2026-11-03 01:14:02,123)
            String normalized = raw.replace(',', '.');
            return LocalDateTime.parse(normalized, FLEXIBLE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
```

---

## 3. Working vs. Failing Formats Audit

| Timestamp Format | Example | Line Regex Match | Formatter Parse | Overall Result |
|:---|:---|:---:|:---:|:---:|
| **Hyphenated 2-digit month & day** | `2026-11-03 01:14:02.123` | ✅ PASS | ✅ PASS | ✅ **WORKS** |
| **Slashed 2-digit month & day** | `2026/11/03 01:14:02` | ✅ PASS | ✅ PASS | ✅ **WORKS** |
| **Comma in fractional seconds** | `2026-11-03 01:14:02,123` | ✅ PASS | ✅ PASS | ✅ **WORKS** |
| **ISO-8601 with `T` separator** | `2026-11-03T01:14:02` | ✅ PASS | ✅ PASS | ✅ **WORKS** |
| **UTC `Z` indicator** | `2026-11-03T01:14:02Z` | ✅ PASS | ✅ PASS | ✅ **WORKS** |
| **Bracketed format** | `[2026-11-03 01:14:02.500]` | ✅ PASS | ✅ PASS | ✅ **WORKS** |
| **Single-digit month & day** | `2026-1-6 10:30:45` | ❌ **FAIL** (`\\d{2}`) | ❌ **FAIL** (`MM`, `dd`) | ❌ **FAILS** |
| **Two-digit month, single-digit day** | `2026-01-6 10:30:45` | ❌ **FAIL** (`\\d{2}`) | ❌ **FAIL** (`dd`) | ❌ **FAILS** |
| **Single-digit month, two-digit day** | `2026-1-06 10:30:45` | ❌ **FAIL** (`\\d{2}`) | ❌ **FAIL** (`MM`) | ❌ **FAILS** |
| **Slashed single-digit month & day** | `2026/1/6 10:30:45` | ❌ **FAIL** (`\\d{2}`) | ❌ **FAIL** (`MM`, `dd`) | ❌ **FAILS** |
| **ISO timezone offset with colon** | `2026-11-03T01:14:02+05:30` | ✅ PASS | ❌ **FAIL** (`[X]` greedy) | ❌ **FAILS** |

---

## 4. Are Single-Digit Months and Days Supported?

**No.** Single-digit months and days are currently **completely unsupported**:
1. At the regular expression boundary, `\\d{4}[-/]\\d{2}[-/]\\d{2}` explicitly rejects any token that does not have exactly two digits between delimiters.
2. At the date-time parsing boundary, `[yyyy-MM-dd][yyyy/MM/dd]` uses `MM` and `dd`. According to the Java `DateTimeFormatter` specification, `MM` and `dd` require strictly two digits. Parsing a single digit throws `DateTimeParseException`.

---

## 5. Impact Analysis: Does Fixing This Affect Existing Formats?

**No.** A fix using `\\d{1,2}` in the regexes and `[yyyy-M-d][yyyy/M/d]` in `DateTimeFormatterBuilder` is **100% backward-compatible**:

1. **Regex Safety:**
   - In regex, `\\d{1,2}` matches 1 or 2 digits.
   - Existing 2-digit months (`11`, `03`) match `\\d{1,2}` unconditionally.
   - The delimiter `[-/]` strictly separates year from month, and month from day.
   - The delimiter `[ T]` strictly separates day from time (`\\d{2}:\\d{2}:\\d{2}`).
   - Because the surrounding delimiters are fixed, there is zero ambiguity or greedy bleed into adjacent fields.

2. **`DateTimeFormatter` Specification:**
   - According to Java `DateTimeFormatter` documentation:
     - `M` matches 1 or 2 digits (e.g. `1`, `01`, `12`).
     - `d` matches 1 or 2 digits (e.g. `6`, `06`, `31`).
   - Therefore, `yyyy-M-d` parses both `2026-11-03` (two digits) and `2026-1-6` (single digit) without any conflicts.

3. **Bonus Timezone Fix:**
   - In `FLEXIBLE_FORMATTER`, line 36 has `.appendPattern("[X][XXX][Z]")`. Because `[X]` comes first, it greedily parses `+05` from `+05:30`, leaving `:30` unparsed. Changing this to `.appendPattern("[XXX][XX][X][Z]")` allows longer ISO offsets like `+05:30` to parse cleanly without affecting `Z` or `+05`.

---

## 6. Proposed Minimal, Backward-Compatible Fix

### Step 1: Update Regexes in `StandardLogParser.java` ([Lines 43, 49, 55](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/parser/StandardLogParser.java#L43))
Replace `\\d{2}[-/]\\d{2}` with `\\d{1,2}[-/]\\d{1,2}` across all three log patterns:

```java
    // Pattern 1: Standard log format
    private static final Pattern PATTERN_STANDARD = Pattern.compile(
            "^(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)" +
            "\\s+(?:\\[([^\\]]+)\\]\\s+)?([A-Za-z]{3,8})\\s+(?:(?:(?:\\[([^\\]]+)\\])|([a-zA-Z0-9_.$#\\-]+))\\s*[-:]\\s+)(.*)$"
    );

    // Pattern 2: Bracketed log format
    private static final Pattern PATTERN_BRACKETED = Pattern.compile(
            "^\\[(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)\\]" +
            "\\s+\\[([A-Za-z]{3,8})\\](?:\s+\\[([^\\]]+)\\])?(?:\s+\\[([^\\]]+)\\])?\\s*[-:]?\\s*(.*)$"
    );

    // Pattern 3: Simple log format
    private static final Pattern PATTERN_SIMPLE = Pattern.compile(
            "^(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)" +
            "\\s+([A-Za-z]{3,8})\\s*[-:]?\\s*(.*)$"
    );
```

### Step 2: Update `FLEXIBLE_FORMATTER` in `StandardLogParser.java` ([Lines 29 & 36](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/parser/StandardLogParser.java#L29))
Replace `[yyyy-MM-dd][yyyy/MM/dd]` with `[yyyy-M-d][yyyy/M/d]`, and reorder zone patterns so `[XXX]` precedes `[X]`:

```java
    private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-M-d][yyyy/M/d]")
            .appendPattern("['T'][' ']")
            .appendPattern("HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .optionalStart()
            .appendPattern("[XXX][XX][X][Z]")
            .optionalEnd()
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();
```

---

## 7. Community Wisdom Interlude

> [!NOTE]
> **Community Wisdom (dev.to / Java Time Best Practices):**  
> When parsing real-world application logs across heterogeneous systems, format patterns with `MM` and `dd` are a frequent cause of brittle parsing failures. In Java's `java.time.format.DateTimeFormatter`, pattern letter count dictates padding: `MM` strictly enforces 2 digits, whereas a single `M` is flexible and parses both 1 and 2 digits (`1` through `12`).  
> 
> The idiomatic standard for resilient log parsers is to use `yyyy-M-d` in conjunction with `DateTimeFormatterBuilder`, ensuring single-digit dates (`2026-1-6`) and zero-padded dates (`2026-01-06`) parse with equal fidelity.

---

## 8. Proposed Unit & Regression Tests (`StandardLogParserTest.java`)

We propose adding the following tests to [`StandardLogParserTest.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/test/java/com/loganalyzer/parser/StandardLogParserTest.java):

```java
    @Test
    @DisplayName("Should parse timestamps with single-digit month and single-digit day")
    void testParseSingleDigitMonthAndDay() {
        String line = "2026-1-6 10:30:45 [main] INFO com.example.Service - Single digit date";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
        assertEquals(LogLevel.INFO, entry.getLevel());
    }

    @Test
    @DisplayName("Should parse timestamps with two-digit month and single-digit day")
    void testParseTwoDigitMonthAndSingleDigitDay() {
        String line = "2026-01-6 10:30:45 [main] INFO com.example.Service - Two digit month, single digit day";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
    }

    @Test
    @DisplayName("Should parse timestamps with single-digit month and two-digit day")
    void testParseSingleDigitMonthAndTwoDigitDay() {
        String line = "2026-1-06 10:30:45 [main] INFO com.example.Service - Single digit month, two digit day";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
    }

    @Test
    @DisplayName("Should parse slashed timestamps with single-digit month and day")
    void testParseSlashedSingleDigitMonthAndDay() {
        String line = "2026/1/6 10:30:45 INFO Slashed date";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
    }

    @Test
    @DisplayName("Should parse bracketed format with single-digit date")
    void testParseBracketedSingleDigitDate() {
        String line = "[2026-1-6 10:30:45.500] [WARN] [worker-1] [TaskRunner] Processing warning";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45, 500_000_000), entry.getTimestamp());
        assertEquals(LogLevel.WARN, entry.getLevel());
    }

    @Test
    @DisplayName("Should parse timestamps with ISO timezone offset")
    void testParseTimestampWithTimezoneOffset() {
        String line = "2026-01-06T10:30:45+05:30 [main] INFO Service - Offset timestamp";
        LogEntry entry = parser.parseLine(line, 1);

        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
    }

    @Test
    @DisplayName("Should reject invalid calendar dates by assigning null timestamp")
    void testInvalidCalendarDatesAssignNullTimestamp() {
        // Month 13 does not exist
        LogEntry month13 = parser.parseLine("2026-13-01 10:30:45 INFO Service - Invalid month", 1);
        assertNotNull(month13);
        assertNull(month13.getTimestamp(), "Invalid month 13 should fail timestamp parsing and yield null");

        // Day 35 does not exist
        LogEntry day35 = parser.parseLine("2026-1-35 10:30:45 INFO Service - Invalid day", 2);
        assertNotNull(day35);
        assertNull(day35.getTimestamp(), "Invalid day 35 should fail timestamp parsing and yield null");

        // Month 00 does not exist
        LogEntry month0 = parser.parseLine("2026-00-10 10:30:45 INFO Service - Invalid month 00", 3);
        assertNotNull(month0);
        assertNull(month0.getTimestamp(), "Invalid month 00 should fail timestamp parsing and yield null");
    }

    @Test
    @DisplayName("Backward compatibility: Existing two-digit formats continue to parse identically")
    void testBackwardCompatibilityTwoDigitFormats() {
        LogEntry entry = parser.parseLine("2026-11-03 01:14:02.123 [main] INFO com.example.UserService - Message", 1);
        assertNotNull(entry);
        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 14, 2, 123_000_000), entry.getTimestamp());
    }
```
