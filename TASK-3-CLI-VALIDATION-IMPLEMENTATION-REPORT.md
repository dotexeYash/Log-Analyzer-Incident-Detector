# 🛡️ Phase 1 – Task 3: CLI Input Validation Implementation & Test Report

**Project:** Log Analyzer & Incident Detector (Java 21 / Maven)  
**Component:** `com.loganalyzer.Main.java` & `com.loganalyzer.MainCliTest.java`  
**Task:** Phase 1 – Task 3: Command-Line Argument Parsing & Robust Validation  
**Date:** November 5, 2026  
**Status:** ✅ **COMPLETED & FULLY VERIFIED**  

---

## 1. Executive Summary

In Phase 1 – Task 3, we addressed critical command-line interface (CLI) input validation vulnerabilities in `com.loganalyzer.Main.java`. Previously, user inputs with non-numeric values (`-b abc`, `-w xyz`), negative numbers (`-b -5`, `-w -10`), zero values (`-b 0`, `-w 0`), decimal numbers (`-b 2.5`, `-w 1.5`), missing argument values, unsupported report formats (`-fmt xml`), and blank file paths either triggered unhandled JVM crashes (`NumberFormatException`) or caused silent unintended defaults and runtime faults.

The CLI parsing logic has been refactored into a modular, decoupled, testable architecture using a modern Java record `CliConfig` and a pure `parseArguments(String[] args)` method. All `System.exit()` calls remain isolated to `main()`.

All 62 unit and integration tests (including 28 comprehensive test executions in `MainCliTest`) pass with 0 failures, 0 errors, and 0 skipped.

---

## 2. Modified & Created Files

| File Path | Status | Description |
|:---|:---|:---|
| `src/main/java/com/loganalyzer/Main.java` | **Modified** | Extracted `CliConfig` record, `parseArguments()`, `requireValue()`, `isFlag()`, `parsePositiveInteger()`. Integrated clean error handling in `main()`. |
| `src/test/java/com/loganalyzer/MainCliTest.java` | **Created** | Comprehensive JUnit 5 test suite verifying 28 test conditions (numeric bounds, negative numbers, missing values, unsupported formats, blank paths, flag collisions, valid inputs). |
| `TASK-3-CLI-VALIDATION-IMPLEMENTATION-REPORT.md` | **Created** | Complete verification and implementation documentation for AI/peer review. |

---

## 3. Implementation Details

### 3.1. `CliConfig` Record
An immutable Java 21 record encapsulation of all validated CLI options:
```java
public record CliConfig(
        String filePath,
        String format,
        String outputPath,
        int burstThreshold,
        int windowSeconds,
        boolean isDemo,
        boolean isHelp
) {}
```

### 3.2. Pure Parsing & Validation: `parseArguments(String[] args)`
The parsing method extracts configuration without calling `System.exit()`, throwing informative `IllegalArgumentException` instances when input violations occur:
- **Missing Value Handling:** Flags requiring values (`-f`, `-fmt`, `-o`, `-b`, `-w`) invoke `requireValue(args, i, arg)`.
- **Differentiating Flags from Negative Numbers:** The `isFlag(String s)` helper checks if an argument starts with `--` or `-` followed by a non-digit character (`-f`, `--file`). This ensures negative numbers (`-5`, `-10`) are not mistakenly classified as missing flags, but rather passed to `parsePositiveInteger()` to generate accurate diagnostic errors (`"must be a positive integer greater than 0, got: -5"`).
- **Format Validation:** Supported report formats (`console`, `markdown`, `md`, `json`) are verified case-insensitively; any other format triggers an explicit exception listing valid formats.
- **Strict Positive Integer Validation:** `parsePositiveInteger(flag, value)` parses integers and verifies `val > 0`. If `Integer.parseInt()` throws `NumberFormatException`, it catches and wraps it with a clean message: `"Invalid numeric value for <flag>: \"<value>\". Expected a positive integer."`.
- **Blank String Detection:** Checks that file paths and output paths are non-blank via `val.isBlank()`.

---

## 4. Test Verification Suite (`MainCliTest.java`)

A total of 28 test cases were executed and verified against all edge cases:

1. **Default & Null Handling:**
   - `testEmptyArgumentsProduceDefaultConfig`: Produces default threshold `5`, window `60`, format `"console"`.
   - `testNullArgumentsHandledSafely`: Passing `null` gracefully defaults without NPE.

2. **Valid Argument Parsing:**
   - `testValidFullArguments`: Tests `-f`, `-fmt json`, `-o`, `-b 10`, `-w 120`.
   - `testPositionalFileArgument`: Tests passing log file as a positional argument.
   - `testHelpFlags`: Tests `-h` and `--help`.
   - `testDemoFlag`: Tests `--demo`.
   - `testSupportedFormats`: Parameterized test verifying `console`, `markdown`, `md`, `json`, `MD`, `JSON`.

3. **Invalid Numeric Arguments (`NumberFormatException` Elimination):**
   - `testNonNumericBurstThresholdRejected`: `-b abc` -> `Invalid numeric value for -b: "abc". Expected a positive integer.`
   - `testNonNumericWindowSecondsRejected`: `-w xyz` -> `Invalid numeric value for -w: "xyz". Expected a positive integer.`
   - `testDecimalBurstThresholdRejected`: `-b 2.5` -> `Invalid numeric value for -b: "2.5". Expected a positive integer.`
   - `testDecimalWindowSecondsRejected`: `-w 1.5` -> `Invalid numeric value for -w: "1.5". Expected a positive integer.`

4. **Negative and Zero Bounds Checking:**
   - `testNegativeBurstThresholdRejected`: `-b -5` -> `must be a positive integer greater than 0, got: -5`
   - `testNegativeWindowSecondsRejected`: `-w -10` -> `must be a positive integer greater than 0, got: -10`
   - `testZeroBurstThresholdRejected`: `-b 0` -> `must be a positive integer greater than 0, got: 0`
   - `testZeroWindowSecondsRejected`: `-w 0` -> `must be a positive integer greater than 0, got: 0`

5. **Missing Arguments & Flag Collisions:**
   - `testMissingBurstThresholdValueAtEndRejected`: `-f test.log -b` -> `Missing required value for argument: -b`
   - `testMissingWindowValueFollowedByFlagRejected`: `-w -f test.log` -> `Missing required value for argument: -w`
   - `testMissingFileArgumentValueRejected`: `-f` -> `Missing required value for argument: -f`
   - `testMissingOutputArgumentValueRejected`: `-f test.log -o` -> `Missing required value for argument: -o`
   - `testBlankFilePathRejected`: `-f "   "` -> `Value for argument -f cannot be blank`
   - `testBlankOutputPathRejected`: `-f test.log -o "   "` -> `Value for argument -o cannot be blank`
   - `testUnknownOptionRejected`: `--invalid-option` -> `Unknown option: --invalid-option`

---

## 5. Complete Maven Test Results

```text
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.loganalyzer.analyzer.AnomalyDetectorTest
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.218 s -- in com.loganalyzer.analyzer.AnomalyDetectorTest
Running com.loganalyzer.analyzer.IncidentDetectorTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.022 s -- in com.loganalyzer.analyzer.IncidentDetectorTest
Running com.loganalyzer.analyzer.StatisticsAggregatorTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.036 s -- in com.loganalyzer.analyzer.StatisticsAggregatorTest
Running com.loganalyzer.integration.LogAnalyzerEndToEndTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.093 s -- in com.loganalyzer.integration.LogAnalyzerEndToEndTest
Running com.loganalyzer.MainCliTest
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.184 s -- in com.loganalyzer.MainCliTest
Running com.loganalyzer.parser.StandardLogParserTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in com.loganalyzer.parser.StandardLogParserTest
Running com.loganalyzer.report.ReportGeneratorTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.028 s -- in com.loganalyzer.report.ReportGeneratorTest

Results:

Tests run: 62, Failures: 0, Errors: 0, Skipped: 0

------------------------------------------------------------------------
BUILD SUCCESS
------------------------------------------------------------------------
Total time:  10.981 s
Finished at: 2026-11-05T23:58:47+05:30
------------------------------------------------------------------------
```

---

## 6. End-to-End JAR Smoke Test Verification

| Command | Expected Exit Code | Observed Output / Behavior | Status |
|:---|:---:|:---|:---:|
| `java -jar target/...jar -b abc` | 1 | `Error: Invalid numeric value for -b: "abc". Expected a positive integer.` + Usage printed | ✅ PASS |
| `java -jar target/...jar -w 0` | 1 | `Error: Argument for -w must be a positive integer greater than 0, got: 0` + Usage printed | ✅ PASS |
| `java -jar target/...jar -b -5` | 1 | `Error: Argument for -b must be a positive integer greater than 0, got: -5` + Usage printed | ✅ PASS |
| `java -jar target/...jar -fmt xml` | 1 | `Error: Unsupported format "xml". Supported formats: console, markdown (md), json.` | ✅ PASS |
| `java -jar target/...jar -h` | 0 | Usage guide displayed with exit code 0 | ✅ PASS |
| `java -jar target/...jar -f sample-logs/normal-operations.log` | 0 | Full clean console report displayed with exit code 0 | ✅ PASS |

---

## 7. Compliance Checklist

- [x] Extracted CLI argument parsing into separately testable `parseArguments()` method.
- [x] Used `CliConfig` record to store configuration.
- [x] Handled `NumberFormatException` cleanly into informative `IllegalArgumentException`.
- [x] Rejected zero and negative burst thresholds and window values.
- [x] Rejected decimal and non-numeric values.
- [x] Detected missing values, including when another CLI flag immediately follows.
- [x] Validated supported report formats against existing report generators.
- [x] Rejected blank file paths and blank output paths.
- [x] Preserved all existing valid CLI commands and default behaviour.
- [x] Kept `System.exit()` out of `parseArguments()` for unit testing.
- [x] Created `MainCliTest.java` with 28 comprehensive test executions.
- [x] Executed full Maven test suite (62/62 passing).
- [x] Built JAR and verified end-to-end CLI execution.
- [x] Maintained strict non-commit/push rule as requested.
