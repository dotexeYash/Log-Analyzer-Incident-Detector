# 🛡️ Phase 1 - Task 3: CLI Input Validation Analysis & Design Report
**Project:** Log Analyzer & Incident Detector (Java 21 / Maven)  
**Component Under Review:** `com.loganalyzer.Main.java`  
**Subject:** Command-line argument parsing vulnerabilities, unhandled `NumberFormatException`, missing value detection, and input bounds validation.  
**Date:** November 5, 2026  
**Status:** Design Proposal (Pre-Implementation)  

---

## 1. Current Implementation Inspection

The command-line argument parsing loop in [`Main.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/Main.java#L35-L64):

```java
for (int i = 0; i < args.length; i++) {
    String arg = args[i];
    switch (arg) {
        case "-h", "--help" -> {
            printUsage();
            System.exit(0);
        }
        case "-f", "--file" -> {
            if (i + 1 < args.length) filePath = args[++i];
        }
        case "-fmt", "--format" -> {
            if (i + 1 < args.length) format = args[++i].toLowerCase(Locale.ROOT);
        }
        case "-o", "--output" -> {
            if (i + 1 < args.length) outputPath = args[++i];
        }
        case "-b", "--burst-threshold" -> {
            if (i + 1 < args.length) burstThreshold = Integer.parseInt(args[++i]);
        }
        case "-w", "--window" -> {
            if (i + 1 < args.length) windowSeconds = Integer.parseInt(args[++i]);
        }
        case "--demo" -> isDemo = true;
        default -> {
            if (arg.startsWith("-")) {
                System.err.println("Unknown option: " + arg);
                printUsage();
                System.exit(1);
            } else if (filePath == null) {
                filePath = arg;
            }
        }
    }
}
```

---

## 2. Identified Vulnerabilities & Edge Cases

### A. Unhandled `NumberFormatException` on Non-Numeric Strings (`-b abc`, `-w xyz`)
* Lines 49 & 52 call raw `Integer.parseInt(args[++i])` with no exception handling.
* Passing `-b abc` or `-w xyz` results in an abrupt unhandled JVM crash:
  ```text
  Exception in thread "main" java.lang.NumberFormatException: For input string: "abc"
      at java.base/java.lang.NumberFormatException.forInputString(...)
      at com.loganalyzer.Main.main(Main.java:49)
  ```
* No user-friendly diagnostic guidance or usage hints are displayed.

### B. Unhandled `NumberFormatException` on Decimal Floats (`-b 2.5`, `-w 1.5`)
* `Integer.parseInt("2.5")` throws `NumberFormatException: For input string: "2.5"`.
* Users attempting to provide fractional time windows or thresholds crash the application.

### C. Silent Missing Values / Premature End-of-Arguments (`-b`, `-w`, `-f`)
* The check `if (i + 1 < args.length)` quietly skips assignment if a flag is passed as the last CLI token (e.g. `java -jar app.jar -f test.log -b`).
* The user's `-b` flag is silently ignored, and the default (`5`) is retained without warning.
* Furthermore, if `-b` is followed by another flag (e.g. `-b -f sample.log`), `args[++i]` consumes `"-f"`, crashing with `NumberFormatException: For input string: "-f"`.

### D. Negative Values Permitted Without Domain Validation (`-b -5`, `-w -10`)
* `Integer.parseInt("-5")` succeeds and sets `burstThreshold = -5`.
  * In `detectErrorBursts()`, `errorEntries.size() < -5` is false for any non-negative size.
  * Even an empty log file with 0 errors will trigger burst detection!
* `Integer.parseInt("-10")` succeeds and sets `windowSeconds = -10`.
  * `Duration.ofSeconds(-10)` creates a negative window duration, breaking sliding window time arithmetic.

### E. Zero Values Permitted Without Domain Validation (`-b 0`, `-w 0`)
* `-b 0` implies that zero errors qualify as a burst, triggering false-positive alerts on healthy logs.
* `-w 0` creates a degenerate 0-second duration window.
* Both thresholds must be strictly positive integers ($\ge 1$).

### F. Unsupported Report Format Fallback (`-fmt xml`, `-fmt docx`)
* Passing `-fmt xml` silently falls back to console output without informing the user that `xml` is unsupported.

---

## 3. Proposed Minimal Fix

To fix all issues while keeping argument parsing **100% testable in unit tests without invoking `System.exit()`**, we introduce a `CliConfig` record and a clean `parseArguments()` method in `Main.java`.

### Code Specification for `Main.java`:

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

public static CliConfig parseArguments(String[] args) {
    String filePath = null;
    String format = "console";
    String outputPath = null;
    boolean isDemo = false;
    int burstThreshold = 5;
    int windowSeconds = 60;

    for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        switch (arg) {
            case "-h", "--help" -> {
                return new CliConfig(null, format, outputPath, burstThreshold, windowSeconds, false, true);
            }
            case "-f", "--file" -> {
                filePath = requireValue(args, i++, arg);
            }
            case "-fmt", "--format" -> {
                String fmt = requireValue(args, i++, arg).toLowerCase(Locale.ROOT);
                if (!fmt.equals("console") && !fmt.equals("markdown") && !fmt.equals("md") && !fmt.equals("json")) {
                    throw new IllegalArgumentException(
                            String.format("Unsupported format \"%s\". Supported formats: console, markdown (md), json.", fmt)
                    );
                }
                format = fmt;
            }
            case "-o", "--output" -> {
                outputPath = requireValue(args, i++, arg);
            }
            case "-b", "--burst-threshold" -> {
                String val = requireValue(args, i++, arg);
                burstThreshold = parsePositiveInteger(arg, val);
            }
            case "-w", "--window" -> {
                String val = requireValue(args, i++, arg);
                windowSeconds = parsePositiveInteger(arg, val);
            }
            case "--demo" -> isDemo = true;
            default -> {
                if (arg.startsWith("-")) {
                    throw new IllegalArgumentException("Unknown option: " + arg);
                } else if (filePath == null) {
                    filePath = arg;
                }
            }
        }
    }

    return new CliConfig(filePath, format, outputPath, burstThreshold, windowSeconds, isDemo, false);
}

private static String requireValue(String[] args, int index, String flag) {
    if (index + 1 >= args.length || args[index + 1].startsWith("-")) {
        throw new IllegalArgumentException("Missing required value for argument: " + flag);
    }
    return args[index + 1];
}

private static int parsePositiveInteger(String flag, String value) {
    try {
        int parsed = Integer.parseInt(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException(
                    String.format("Argument for %s must be a positive integer greater than 0, got: %d", flag, parsed)
            );
        }
        return parsed;
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
                String.format("Invalid numeric value for %s: \"%s\". Expected a positive integer.", flag, value)
        );
    }
}
```

### Clean Integration into `main(String[] args)`:
```java
public static void main(String[] args) {
    CliConfig config;
    try {
        config = parseArguments(args);
    } catch (IllegalArgumentException e) {
        System.err.println("Error: " + e.getMessage() + "\n");
        printUsage();
        System.exit(1);
        return;
    }

    if (config.isHelp()) {
        printUsage();
        System.exit(0);
        return;
    }

    if (config.isDemo() || (config.filePath() == null && args.length == 0)) {
        runDemonstration();
        return;
    }

    if (config.filePath() == null) {
        System.err.println("Error: No input log file specified.\n");
        printUsage();
        System.exit(1);
        return;
    }

    Path path = Paths.get(config.filePath());
    if (!Files.exists(path)) {
        System.err.println("Error: Specified file does not exist: " + path.toAbsolutePath());
        System.exit(1);
        return;
    }
    
    // Continues with analyzer execution using validated config parameters...
}
```

---

## 4. Planned Unit Test Suite (`MainCliTest.java`)

We will create [`src/test/java/com/loganalyzer/MainCliTest.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/test/java/com/loganalyzer/MainCliTest.java) to test all validation branches:

| Test Case | Arguments | Expected Outcome |
| :--- | :--- | :--- |
| **Invalid Numeric String** | `-b abc` | Throws `IllegalArgumentException` ("Invalid numeric value for -b: \"abc\"") |
| **Invalid Numeric String** | `-w xyz` | Throws `IllegalArgumentException` ("Invalid numeric value for -w: \"xyz\"") |
| **Decimal Float** | `-b 2.5` | Throws `IllegalArgumentException` ("Expected a positive integer") |
| **Decimal Float** | `-w 1.5` | Throws `IllegalArgumentException` ("Expected a positive integer") |
| **Negative Value** | `-b -5` | Throws `IllegalArgumentException` ("must be a positive integer greater than 0") |
| **Negative Value** | `-w -10` | Throws `IllegalArgumentException` ("must be a positive integer greater than 0") |
| **Zero Value** | `-b 0` | Throws `IllegalArgumentException` ("must be a positive integer greater than 0") |
| **Zero Value** | `-w 0` | Throws `IllegalArgumentException` ("must be a positive integer greater than 0") |
| **Missing Value at End** | `-b` | Throws `IllegalArgumentException` ("Missing required value for argument: -b") |
| **Missing Value followed by Flag** | `-w -f app.log` | Throws `IllegalArgumentException` ("Missing required value for argument: -w") |
| **Missing File Value** | `-f` | Throws `IllegalArgumentException` ("Missing required value for argument: -f") |
| **Unsupported Format** | `-fmt xml` | Throws `IllegalArgumentException` ("Unsupported format \"xml\"") |
| **Unknown Option** | `--unknown-flag`| Throws `IllegalArgumentException` ("Unknown option: --unknown-flag") |
| **Valid Full Configuration** | `-f app.log -b 10 -w 120 -fmt json -o report.json` | Returns `CliConfig` with all fields matched |
| **Valid Help Flag** | `--help` | Returns `CliConfig` with `isHelp == true` |
| **Valid Demo Flag** | `--demo` | Returns `CliConfig` with `isDemo == true` |
