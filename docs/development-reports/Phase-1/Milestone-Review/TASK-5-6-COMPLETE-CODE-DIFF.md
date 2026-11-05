# 📋 Phase 1 – Tasks 5 & 6: Complete Git Code Diff

**Project:** Log Analyzer & Incident Detector (Java 21 / Maven)  
**Scope:** Full Git Diff for all modified Java source and test files from Phase 1 – Tasks 5 & 6  
**Date:** November 6, 2026  
**Status:** Working Tree Diff (Zero changes staged, committed, or pushed)  

---

## Table of Contents

- [Working Tree Summary](#working-tree-summary)
- [1. Production Java Source Files](#1-production-java-source-files)
  - [1.1 StandardLogParser.java](#11-srcmainjavacomloganalyzerparserstandardlogparserjava)
  - [1.2 LogAnalyzer.java](#12-srcmainjavacomloganalyzeranalyzerloganalyzerjava)
  - [1.3 Main.java](#13-srcmainjavacomloganalyzermainjava)
  - [1.4 LogAnalyzerException.java](#14-srcmainjavacomloganalyzerexceptionloganalyzerexceptionjava)
  - [1.5 InvalidLogFileException.java](#15-srcmainjavacomloganalyzerexceptioninvalidlogfileexceptionjava)
  - [1.6 LogParseException.java](#16-srcmainjavacomloganalyzerexceptionlogparseexceptionjava)
  - [1.7 ConsoleReportGenerator.java](#17-srcmainjavacomloganalyzerreportconsolereportgeneratorjava)
  - [1.8 JsonReportGenerator.java](#18-srcmainjavacomloganalyzerreportjsonreportgeneratorjava)
  - [1.9 MarkdownReportGenerator.java](#19-srcmainjavacomloganalyzerreportmarkdownreportgeneratorjava)
- [2. Test Java Source Files](#2-test-java-source-files)
  - [2.1 StandardLogParserTest.java](#21-srctestjavacomloganalyzerparserstandardlogparsertestjava)
  - [2.2 MainCliTest.java](#22-srctestjavacomloganalyzermainclitestjava)
  - [2.3 ReportGeneratorTest.java](#23-srctestjavacomloganalyzerreportreportgeneratortestjava)
  - [2.4 LogAnalyzerEndToEndTest.java](#24-srctestjavacomloganalyzerintegrationloganalyzerendtoendtestjava)

---

## Working Tree Summary

```text
 13 files modified:
   src/main/java/com/loganalyzer/Main.java                            | 111 +++++++++++-----
   src/main/java/com/loganalyzer/analyzer/LogAnalyzer.java             |   3 +-
   src/main/java/com/loganalyzer/exception/InvalidLogFileException.java |   2 +
   src/main/java/com/loganalyzer/exception/LogAnalyzerException.java    |   2 +
   src/main/java/com/loganalyzer/exception/LogParseException.java       |   2 +
   src/main/java/com/loganalyzer/parser/StandardLogParser.java          |  19 +--
   src/main/java/com/loganalyzer/report/ConsoleReportGenerator.java     |  10 +-
   src/main/java/com/loganalyzer/report/JsonReportGenerator.java        |   4 +-
   src/main/java/com/loganalyzer/report/MarkdownReportGenerator.java    |   4 +-
   src/test/java/com/loganalyzer/MainCliTest.java                     |  27 ++++
   src/test/java/com/loganalyzer/integration/LogAnalyzerEndToEndTest.java |  21 +++
   src/test/java/com/loganalyzer/parser/StandardLogParserTest.java      | 145 +++++++++++++++++++++
   src/test/java/com/loganalyzer/report/ReportGeneratorTest.java        |  39 ++++++
 13 files changed, 345 insertions(+), 44 deletions(-)
```

---

## 1. Production Java Source Files

### 1.1 `src/main/java/com/loganalyzer/parser/StandardLogParser.java`

```diff
diff --git a/src/main/java/com/loganalyzer/parser/StandardLogParser.java b/src/main/java/com/loganalyzer/parser/StandardLogParser.java
index 73eb3f5..80600dc 100644
--- a/src/main/java/com/loganalyzer/parser/StandardLogParser.java
+++ b/src/main/java/com/loganalyzer/parser/StandardLogParser.java
@@ -14,6 +14,8 @@ import java.nio.file.Path;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.time.format.DateTimeFormatterBuilder;
+import java.time.format.DateTimeParseException;
+import java.time.format.ResolverStyle;
 import java.time.temporal.ChronoField;
 import java.util.ArrayList;
 import java.util.List;
@@ -26,33 +28,34 @@ import java.util.regex.Pattern;
 public class StandardLogParser implements LogParser {
 
     private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
-            .appendPattern("[yyyy-MM-dd][yyyy/MM/dd]")
+            .appendPattern("[uuuu-M-d][uuuu/M/d]")
             .appendPattern("['T'][' ']")
             .appendPattern("HH:mm:ss")
             .optionalStart()
             .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
             .optionalEnd()
             .optionalStart()
-            .appendPattern("[X][XXX][Z]")
+            .appendPattern("[XXX][XX][X][Z]")
             .optionalEnd()
             .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
-            .toFormatter();
+            .toFormatter()
+            .withResolverStyle(ResolverStyle.STRICT);
 
     // Pattern 1: 2026-11-03 01:14:02.123 [thread-1] INFO com.service.UserService - Message
     private static final Pattern PATTERN_STANDARD = Pattern.compile(
-            "^(\\d{4}[-/]\\d{2}[-/]\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)" +
+            "^(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}(?::?\\d{2})?)?)" +
             "\\s+(?:\\[([^\\]]+)\\]\\s+)?([A-Za-z]{3,8})\\s+(?:(?:(?:\\[([^\\]]+)\\])|([a-zA-Z0-9_.$#\\-]+))\\s*[-:]\\s+)(.*)$"
     );
 
     // Pattern 2: [2026-11-03 01:14:02.123] [INFO] [thread-1] [com.service.UserService] Message
     private static final Pattern PATTERN_BRACKETED = Pattern.compile(
-            "^\\[(\\d{4}[-/]\\d{2}[-/]\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)\\]" +
+            "^\\[(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}(?::?\\d{2})?)?)\\]" +
             "\\s+\\[([A-Za-z]{3,8})\\](?:\s+\\[([^\\]]+)\\])?(?:\s+\\[([^\\]]+)\\])?\\s*[-:]?\\s*(.*)$"
     );
 
     // Pattern 3: 2026-11-03 01:14:02 INFO Message
     private static final Pattern PATTERN_SIMPLE = Pattern.compile(
-            "^(\\d{4}[-/]\\d{2}[-/]\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)" +
+            "^(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}(?::?\\d{2})?)?)" +
             "\\s+([A-Za-z]{3,8})\\s*[-:]?\\s*(.*)$"
     );
 
@@ -64,7 +67,7 @@ public class StandardLogParser implements LogParser {
     @Override
     public List<LogEntry> parse(Path path) throws IOException, InvalidLogFileException {
         if (path == null) {
-            throw new InvalidLogFileException("Log file path cannot be null");
+            throw new IllegalArgumentException("Log file path cannot be null");
         }
         if (!Files.exists(path)) {
             throw new InvalidLogFileException("Log file does not exist: " + path.toAbsolutePath());
@@ -207,7 +210,7 @@ public class StandardLogParser implements LogParser {
             // Normalize commas used in milliseconds (e.g. 2026-11-03 01:14:02,123)
             String normalized = raw.replace(',', '.');
             return LocalDateTime.parse(normalized, FLEXIBLE_FORMATTER);
-        } catch (Exception e) {
+        } catch (DateTimeParseException e) {
             return null;
         }
     }
```

---

### 1.2 `src/main/java/com/loganalyzer/analyzer/LogAnalyzer.java`

```diff
diff --git a/src/main/java/com/loganalyzer/analyzer/LogAnalyzer.java b/src/main/java/com/loganalyzer/analyzer/LogAnalyzer.java
index 143c28c..4658803 100644
--- a/src/main/java/com/loganalyzer/analyzer/LogAnalyzer.java
+++ b/src/main/java/com/loganalyzer/analyzer/LogAnalyzer.java
@@ -46,11 +46,12 @@ public class LogAnalyzer {
      *
      * @param logFilePath path to log file
      * @return complete AnalysisResult
+     * @throws IllegalArgumentException if logFilePath is null
      * @throws InvalidLogFileException if file is invalid or not readable
      */
     public AnalysisResult analyze(Path logFilePath) {
         if (logFilePath == null) {
-            throw new InvalidLogFileException("Log file path cannot be null");
+            throw new IllegalArgumentException("Log file path cannot be null");
         }
 
         try {
```

---

### 1.3 `src/main/java/com/loganalyzer/Main.java`

```diff
diff --git a/src/main/java/com/loganalyzer/Main.java b/src/main/java/com/loganalyzer/Main.java
index 28c201b..2e465a1 100644
--- a/src/main/java/com/loganalyzer/Main.java
+++ b/src/main/java/com/loganalyzer/Main.java
@@ -4,6 +4,8 @@ import com.loganalyzer.analyzer.AnomalyDetector;
 import com.loganalyzer.analyzer.IncidentDetector;
 import com.loganalyzer.analyzer.LogAnalyzer;
 import com.loganalyzer.analyzer.StatisticsAggregator;
+import com.loganalyzer.exception.InvalidLogFileException;
+import com.loganalyzer.exception.LogAnalyzerException;
 import com.loganalyzer.model.AnalysisResult;
 import com.loganalyzer.model.IncidentSeverity;
 import com.loganalyzer.parser.StandardLogParser;
@@ -76,29 +78,56 @@ public class Main {
         AnomalyDetector anomalyDetector = new AnomalyDetector(config.burstThreshold(), config.windowSeconds(), 4, 3, 2, 3);
         LogAnalyzer analyzer = new LogAnalyzer(new StandardLogParser(), new StatisticsAggregator(), anomalyDetector, new IncidentDetector());
 
+        AnalysisResult result;
         try {
-            AnalysisResult result = analyzer.analyze(path);
-            String report = renderReport(result, config.format());
-
-            if (config.outputPath() != null) {
-                Path out = Paths.get(config.outputPath());
-                if (out.getParent() != null) {
-                    Files.createDirectories(out.getParent());
-                }
-                Files.writeString(out, report);
-                System.out.println("Report successfully written to: " + out.toAbsolutePath());
-            } else {
-                System.out.println(report);
-            }
+            result = analyzer.analyze(path);
+        } catch (InvalidLogFileException e) {
+            System.err.println("Error: " + e.getMessage());
            System.exit(1);
+            return;
+        } catch (LogAnalyzerException e) {
+            System.err.println("Error analyzing log file: " + e.getMessage());
+            System.exit(1);
+            return;
         } catch (Exception e) {
-            System.err.println("Failed to analyze log file: " + e.getMessage());
+            System.err.println("Unexpected error during analysis: " + e.getMessage());
             e.printStackTrace();
             System.exit(1);
+            return;
+        }
+
+        String report;
+        try {
+            report = renderReport(result, config.format());
         } catch (Exception e) {
-            System.err.println("Failed to analyze log file: " + e.getMessage());
+            System.err.println("Unexpected error rendering report: " + e.getMessage());
             e.printStackTrace();
             System.exit(1);
+            return;
+        }
+
+        if (config.outputPath() != null) {
+            Path out = Paths.get(config.outputPath());
+            try {
+                writeReport(out, report);
+                System.out.println("Report successfully written to: " + out.toAbsolutePath());
+            } catch (IOException e) {
+                System.err.println("Error: Failed to write report to file " + out.toAbsolutePath() + " - " + e.getMessage());
+                System.exit(1);
+                return;
+            } catch (Exception e) {
+                System.err.println("Unexpected error writing report to file: " + e.getMessage());
+                e.printStackTrace();
+                System.exit(1);
+                return;
+            }
+        } else {
+            System.out.println(report);
+        }
+
+        // Return non-zero exit code if critical incidents were detected
+        if (result.hasCriticalIncidents()) {
+            System.exit(2);
         }
     }
 
@@ -209,11 +238,22 @@ public class Main {
             return parsed;
         } catch (NumberFormatException e) {
             throw new IllegalArgumentException(
-                    String.format("Invalid numeric value for %s: \"%s\". Expected a positive integer.", flag, value)
+                    String.format("Invalid numeric value for %s: \"%s\". Expected a positive integer.", flag, value),
+                    e
             );
         }
     }
 
+    static void writeReport(Path out, String report) throws IOException {
+        if (out == null) {
+            throw new IllegalArgumentException("Output path cannot be null");
+        }
+        if (out.getParent() != null) {
+            Files.createDirectories(out.getParent());
+        }
+        Files.writeString(out, report);
+    }
+
     private static String renderReport(AnalysisResult result, String format) {
         ReportGenerator generator = switch (format) {
             case "markdown", "md" -> new MarkdownReportGenerator();
@@ -240,23 +280,36 @@ public class Main {
 
         for (String sample : demoFiles) {
             Path path = Paths.get(sample);
-            if (!Files.exists(path)) {
+            boolean executed = false;
+
+            if (Files.exists(path)) {
+                try {
+                    AnalysisResult result = analyzer.analyze(path);
+                    System.out.println(reportGenerator.generate(result));
+                    System.out.println("\n");
+                    executed = true;
+                } catch (Exception e) {
+                    System.err.println("Warning: Failed to analyze local demo file " + sample + ": " + e.getMessage());
+                }
+            }
+
+            if (!executed) {
                 // Try classpath resource
                 try (InputStream is = Main.class.getClassLoader().getResourceAsStream(sample)) {
                     if (is != null) {
                         AnalysisResult result = analyzer.analyze(is, sample);
                         System.out.println(reportGenerator.generate(result));
                         System.out.println("\n");
-                        continue;
+                        executed = true;
                     }
-                } catch (IOException ignored) {}
+                } catch (IOException e) {
+                    System.err.println("Warning: I/O error reading classpath resource " + sample + ": " + e.getMessage());
+                } catch (Exception e) {
+                    System.err.println("Warning: Failed to analyze classpath resource " + sample + ": " + e.getMessage());
+                }
             }
 
-            if (Files.exists(path)) {
-                AnalysisResult result = analyzer.analyze(path);
-                System.out.println(reportGenerator.generate(result));
-                System.out.println("\n");
-            } else {
+            if (!executed) {
                 System.out.println("Notice: Demo file " + sample + " not found locally or on classpath.");
             }
         }
@@ -267,7 +320,7 @@ public class Main {
             Log Analyzer and Incident Detector (Java 21)
             Usage:
               java -jar target/log-analyzer-incident-detector-1.0.0.jar [options]
-              
+
             Options:
               -f,   --file <path>             Path to target log file to inspect
               -fmt, --format <console|md|json> Format of the generated report (default: console)
@@ -276,7 +329,7 @@ public class Main {
               -w,   --window <seconds>        Time window in seconds for burst detection (default: 60)
               --demo                          Execute demonstration against all bundled sample scenarios
               -h,   --help                    Display this help message
-              
+
             Exit Codes:
               0 - Analysis completed without critical issues
               1 - Error during execution or invalid inputs
```

---

### 1.4 `src/main/java/com/loganalyzer/exception/LogAnalyzerException.java`

```diff
diff --git a/src/main/java/com/loganalyzer/exception/LogAnalyzerException.java b/src/main/java/com/loganalyzer/exception/LogAnalyzerException.java
index b68455a..b6c7b0e 100644
--- a/src/main/java/com/loganalyzer/exception/LogAnalyzerException.java
+++ b/src/main/java/com/loganalyzer/exception/LogAnalyzerException.java
@@ -4,6 +4,8 @@ package com.loganalyzer.exception;
  * Base unchecked exception for Log Analyzer operations.
  */
 public class LogAnalyzerException extends RuntimeException {
+    private static final long serialVersionUID = 1L;
+
     public LogAnalyzerException(String message) {
         super(message);
     }
```

---

### 1.5 `src/main/java/com/loganalyzer/exception/InvalidLogFileException.java`

```diff
diff --git a/src/main/java/com/loganalyzer/exception/InvalidLogFileException.java b/src/main/java/com/loganalyzer/exception/InvalidLogFileException.java
index 69b71b0..43aee4a 100644
--- a/src/main/java/com/loganalyzer/exception/InvalidLogFileException.java
+++ b/src/main/java/com/loganalyzer/exception/InvalidLogFileException.java
@@ -4,6 +4,8 @@ package com.loganalyzer.exception;
  * Thrown when an input log file does not exist, cannot be read, or is invalid.
  */
 public class InvalidLogFileException extends LogAnalyzerException {
+    private static final long serialVersionUID = 1L;
+
     public InvalidLogFileException(String message) {
         super(message);
     }
```

---

### 1.6 `src/main/java/com/loganalyzer/exception/LogParseException.java`

```diff
diff --git a/src/main/java/com/loganalyzer/exception/LogParseException.java b/src/main/java/com/loganalyzer/exception/LogParseException.java
index ecbbba8..c8c8787 100644
--- a/src/main/java/com/loganalyzer/exception/LogParseException.java
+++ b/src/main/java/com/loganalyzer/exception/LogParseException.java
@@ -4,6 +4,8 @@ package com.loganalyzer.exception;
  * Thrown when log line parsing encounters critical malformed content.
  */
 public class LogParseException extends LogAnalyzerException {
+    private static final long serialVersionUID = 1L;
+
     private final long lineNumber;
 
     public LogParseException(String message, long lineNumber) {
```

---

### 1.7 `src/main/java/com/loganalyzer/report/ConsoleReportGenerator.java`

```diff
diff --git a/src/main/java/com/loganalyzer/report/ConsoleReportGenerator.java b/src/main/java/com/loganalyzer/report/ConsoleReportGenerator.java
index a863f40..7443808 100644
--- a/src/main/java/com/loganalyzer/report/ConsoleReportGenerator.java
+++ b/src/main/java/com/loganalyzer/report/ConsoleReportGenerator.java
@@ -26,7 +26,9 @@ public class ConsoleReportGenerator implements ReportGenerator {
         }
 
         StringBuilder sb = new StringBuilder();
-        LogStatistics stats = result.getStatistics();
+        LogStatistics stats = result.getStatistics() != null
+                ? result.getStatistics()
+                : new LogStatistics(0, Map.of(), null, null, Map.of(), Map.of(), Map.of(), 0.0, 0.0);
 
         // Banner
         sb.append(SEPARATOR).append(System.lineSeparator());
@@ -39,7 +41,7 @@ public class ConsoleReportGenerator implements ReportGenerator {
         sb.append(String.format(" Total Records   : %,d%n", stats.getTotalLogs()));
         sb.append(String.format(" Error Rate      : %.2f%%%n", stats.getErrorPercentage()));
         sb.append(String.format(" Warning Rate    : %.2f%%%n", stats.getWarningPercentage()));
-        sb.append(String.format(" Overall Status  : %s%n", computeStatusBadge(result)));
+        sb.append(String.format(" Overall Status  : %s%n", computeStatusBadge(result, stats)));
         sb.append(SUB_SEPARATOR).append(System.lineSeparator());
 
         // Log Level Breakdown
@@ -119,14 +121,14 @@ public class ConsoleReportGenerator implements ReportGenerator {
         return sb.toString();
     }
 
-    private String computeStatusBadge(AnalysisResult result) {
+    private String computeStatusBadge(AnalysisResult result, LogStatistics stats) {
         if (result.hasCriticalIncidents()) {
             return "[CRITICAL ALERT] - Immediate Triage Required";
         }
         if (result.hasIncidents()) {
             return "[WARNING / DEGRADED] - Operational Attention Required";
         }
-        if (result.getStatistics().getErrorPercentage() > 5.0) {
+        if (stats != null && stats.getErrorPercentage() > 5.0) {
             return "[ELEVATED ERRORS] - Investigate Subsystem Logs";
         }
         return "[HEALTHY] - Operational Metrics Nominal";
```

---

### 1.8 `src/main/java/com/loganalyzer/report/JsonReportGenerator.java`

```diff
diff --git a/src/main/java/com/loganalyzer/report/JsonReportGenerator.java b/src/main/java/com/loganalyzer/report/JsonReportGenerator.java
index 6063385..484b15e 100644
--- a/src/main/java/com/loganalyzer/report/JsonReportGenerator.java
+++ b/src/main/java/com/loganalyzer/report/JsonReportGenerator.java
@@ -23,7 +23,9 @@ public class JsonReportGenerator implements ReportGenerator {
         }
 
         StringBuilder sb = new StringBuilder();
-        LogStatistics stats = result.getStatistics();
+        LogStatistics stats = result.getStatistics() != null
+                ? result.getStatistics()
+                : new LogStatistics(0, Map.of(), null, null, Map.of(), Map.of(), Map.of(), 0.0, 0.0);
 
         sb.append("{\n");
         sb.append(indent(1)).append("\"sourceName\": \"").append(escape(result.getSourceName())).append("\",\n");
```

---

### 1.9 `src/main/java/com/loganalyzer/report/MarkdownReportGenerator.java`

```diff
diff --git a/src/main/java/com/loganalyzer/report/MarkdownReportGenerator.java b/src/main/java/com/loganalyzer/report/MarkdownReportGenerator.java
index 41182f5..e115741 100644
--- a/src/main/java/com/loganalyzer/report/MarkdownReportGenerator.java
+++ b/src/main/java/com/loganalyzer/report/MarkdownReportGenerator.java
@@ -24,7 +24,9 @@ public class MarkdownReportGenerator implements ReportGenerator {
         }
 
         StringBuilder sb = new StringBuilder();
-        LogStatistics stats = result.getStatistics();
+        LogStatistics stats = result.getStatistics() != null
+                ? result.getStatistics()
+                : new LogStatistics(0, Map.of(), null, null, Map.of(), Map.of(), Map.of(), 0.0, 0.0);
 
         sb.append("# 📋 Log Analysis & Incident Detection Report\n\n");
```

---

## 2. Test Java Source Files

### 2.1 `src/test/java/com/loganalyzer/parser/StandardLogParserTest.java`

```diff
diff --git a/src/test/java/com/loganalyzer/parser/StandardLogParserTest.java b/src/test/java/com/loganalyzer/parser/StandardLogParserTest.java
index 324f815..62a912d 100644
--- a/src/test/java/com/loganalyzer/parser/StandardLogParserTest.java
+++ b/src/test/java/com/loganalyzer/parser/StandardLogParserTest.java
@@ -9,6 +9,7 @@ import org.junit.jupiter.api.Test;
 
 import java.io.ByteArrayInputStream;
 import java.io.IOException;
+import java.io.InputStream;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Path;
 import java.nio.file.Paths;
@@ -99,4 +100,148 @@ class StandardLogParserTest {
         Path invalidPath = Paths.get("non-existent-directory/non-existent-file.log");
         assertThrows(InvalidLogFileException.class, () -> parser.parse(invalidPath));
     }
+
+    @Test
+    @DisplayName("Task 6: Should throw IllegalArgumentException when file path is null")
+    void testNullPathThrowsIllegalArgumentException() {
+        assertThrows(IllegalArgumentException.class, () -> parser.parse((Path) null));
+    }
+
+    @Test
+    @DisplayName("Task 6: Should throw IllegalArgumentException when InputStream is null")
+    void testNullInputStreamThrowsIllegalArgumentException() {
+        assertThrows(IllegalArgumentException.class, () -> parser.parse((InputStream) null));
+    }
+
+    @Test
+    @DisplayName("Task 6: Should throw InvalidLogFileException when target path is a directory")
+    void testDirectoryPathThrowsInvalidLogFileException() {
+        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
+        InvalidLogFileException ex = assertThrows(InvalidLogFileException.class, () -> parser.parse(tempDir));
+        assertTrue(ex.getMessage().contains("Target is not a regular file"));
+    }
+
+    @Test
+    @DisplayName("Should parse timestamps with single-digit month and single-digit day")
+    void testParseSingleDigitMonthAndDay() {
+        String line = "2026-1-6 10:30:45 [main] INFO com.example.Service - Single digit date";
+        LogEntry entry = parser.parseLine(line, 1);
+
+        assertNotNull(entry);
+        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
+        assertEquals(LogLevel.INFO, entry.getLevel());
+        assertEquals("Single digit date", entry.getMessage());
+    }
+
+    @Test
+    @DisplayName("Should parse timestamps with two-digit month and single-digit day")
+    void testParseTwoDigitMonthAndSingleDigitDay() {
+        String line = "2026-01-6 10:30:45 [main] INFO com.example.Service - Two digit month, single digit day";
+        LogEntry entry = parser.parseLine(line, 1);
+
+        assertNotNull(entry);
+        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
+    }
+
+    @Test
+    @DisplayName("Should parse timestamps with single-digit month and two-digit day")
+    void testParseSingleDigitMonthAndTwoDigitDay() {
+        String line = "2026-1-06 10:30:45 [main] INFO com.example.Service - Single digit month, two digit day";
+        LogEntry entry = parser.parseLine(line, 1);
+
+        assertNotNull(entry);
+        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
+    }
+
+    @Test
+    @DisplayName("Should parse slashed timestamps with single-digit month and day")
+    void testParseSlashedSingleDigitMonthAndDay() {
+        String line = "2026/1/6 10:30:45 INFO Slashed date";
+        LogEntry entry = parser.parseLine(line, 1);
+
+        assertNotNull(entry);
+        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry.getTimestamp());
+    }
+
+    @Test
+    @DisplayName("Should parse bracketed format with single-digit date")
+    void testParseBracketedSingleDigitDate() {
+        String line = "[2026-1-6 10:30:45.500] [WARN] [worker-1] [TaskRunner] Processing warning";
+        LogEntry entry = parser.parseLine(line, 1);
+
+        assertNotNull(entry);
+        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45, 500_000_000), entry.getTimestamp());
+        assertEquals(LogLevel.WARN, entry.getLevel());
+    }
+
+    @Test
+    @DisplayName("Should parse timestamps with various ISO timezone offset formats (+05:30, +05, +0500, Z)")
+    void testParseTimestampWithTimezoneOffsets() {
+        // +05:30 offset
+        LogEntry entryCol = parser.parseLine("2026-01-06T10:30:45+05:30 [main] INFO Service - Offset with colon", 1);
+        assertNotNull(entryCol);
+        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entryCol.getTimestamp());
+
+        // +05 hour-only offset
+        LogEntry entryHour = parser.parseLine("2026-01-06T10:30:45+05 [main] INFO Service - Offset hour only", 2);
+        assertNotNull(entryHour);
+        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entryHour.getTimestamp());
+
+        // +0500 no-colon offset
+        LogEntry entry4Dig = parser.parseLine("2026-01-06T10:30:45+0500 [main] INFO Service - Offset 4 digits", 3);
+        assertNotNull(entry4Dig);
+        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entry4Dig.getTimestamp());
+
+        // Z UTC offset
+        LogEntry entryZ = parser.parseLine("2026-01-06T10:30:45Z [main] INFO Service - UTC Z", 4);
+        assertNotNull(entryZ);
+        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 30, 45), entryZ.getTimestamp());
+    }
+
+    @Test
+    @DisplayName("Should assign null timestamp for invalid calendar dates without crashing")
+    void testInvalidCalendarDatesAssignNullTimestamp() {
+        // Month 13 does not exist
+        LogEntry month13 = parser.parseLine("2026-13-01 10:30:45 INFO Service - Invalid month 13", 1);
+        assertNotNull(month13);
+        assertNull(month13.getTimestamp(), "Invalid month 13 should fail timestamp parsing and yield null");
+
+        // Day 35 does not exist
+        LogEntry day35 = parser.parseLine("2026-1-35 10:30:45 INFO Service - Invalid day 35", 2);
+        assertNotNull(day35);
+        assertNull(day35.getTimestamp(), "Invalid day 35 should fail timestamp parsing and yield null");
+
+        // Month 00 does not exist
+        LogEntry month0 = parser.parseLine("2026-00-10 10:30:45 INFO Service - Invalid month 00", 3);
+        assertNotNull(month0);
+        assertNull(month0.getTimestamp(), "Invalid month 00 should fail timestamp parsing and yield null");
+    }
+
+    @Test
+    @DisplayName("Strict calendar-date validation: Reject invalid dates (Feb 30, non-leap Feb 29) and accept valid leap dates")
+    void testStrictCalendarDateValidation() {
+        // 2026-02-30 -> null timestamp
+        LogEntry feb30 = parser.parseLine("2026-02-30 10:30:45 INFO Service - Invalid date Feb 30", 1);
+        assertNotNull(feb30);
+        assertNull(feb30.getTimestamp(), "2026-02-30 should yield null timestamp under strict resolution");
+
+        // 2026-02-29 -> null timestamp (2026 is not a leap year)
+        LogEntry feb29NonLeap = parser.parseLine("2026-02-29 10:30:45 INFO Service - Non-leap Feb 29", 2);
+        assertNotNull(feb29NonLeap);
+        assertNull(feb29NonLeap.getTimestamp(), "2026-02-29 should yield null timestamp as 2026 is not a leap year");
+
+        // 2024-02-29 -> valid timestamp (2024 is a leap year)
+        LogEntry feb29Leap = parser.parseLine("2024-02-29 10:30:45 INFO Service - Valid leap year Feb 29", 3);
+        assertNotNull(feb29Leap);
+        assertEquals(LocalDateTime.of(2024, 2, 29, 10, 30, 45), feb29Leap.getTimestamp(),
+                "2024-02-29 should parse successfully as 2024 is a leap year");
+    }
+
+    @Test
+    @DisplayName("Backward compatibility: Existing two-digit formats continue to parse identically")
+    void testBackwardCompatibilityTwoDigitFormats() {
+        LogEntry entry = parser.parseLine("2026-11-03 01:14:02.123 [main] INFO com.example.UserService - Message", 1);
+        assertNotNull(entry);
+        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 14, 2, 123_000_000), entry.getTimestamp());
+    }
 }
```

---

### 2.2 `src/test/java/com/loganalyzer/MainCliTest.java`

```diff
diff --git a/src/test/java/com/loganalyzer/MainCliTest.java b/src/test/java/com/loganalyzer/MainCliTest.java
index dd23224..00f6643 100644
--- a/src/test/java/com/loganalyzer/MainCliTest.java
+++ b/src/test/java/com/loganalyzer/MainCliTest.java
@@ -5,6 +5,10 @@ import org.junit.jupiter.api.Test;
 import org.junit.jupiter.params.ParameterizedTest;
 import org.junit.jupiter.params.provider.ValueSource;
 
+import java.io.IOException;
+import java.nio.file.Path;
+import java.nio.file.Paths;
+
 import static org.junit.jupiter.api.Assertions.*;
 
 class MainCliTest {
@@ -98,6 +102,16 @@ class MainCliTest {
         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
         assertTrue(ex.getMessage().contains("Invalid numeric value for -b: \"abc\""));
         assertTrue(ex.getMessage().contains("Expected a positive integer"));
+        assertInstanceOf(NumberFormatException.class, ex.getCause(), "Must preserve NumberFormatException as cause");
+    }
+
+    @Test
+    @DisplayName("Task 6: Non-numeric integer option should preserve NumberFormatException as cause")
+    void testNonNumericOptionPreservesNumberFormatExceptionCause() {
+        String[] args = {"-f", "test.log", "-w", "not-a-number"};
+        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
+        assertNotNull(ex.getCause(), "IllegalArgumentException must preserve underlying cause");
+        assertInstanceOf(NumberFormatException.class, ex.getCause(), "Underlying cause must be NumberFormatException");
     }
 
     @Test
@@ -214,4 +228,17 @@ class MainCliTest {
         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
         assertTrue(ex.getMessage().contains("Unknown option: --invalid-option"));
     }
+
+    @Test
+    @DisplayName("Task 6: writeReport should throw IOException when target path is an existing directory")
+    void testWriteReportToDirectoryThrowsIOException() {
+        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
+        assertThrows(IOException.class, () -> Main.writeReport(tempDir, "Sample Report Content"));
+    }
+
+    @Test
+    @DisplayName("Task 6: writeReport should throw IllegalArgumentException when path is null")
+    void testWriteReportNullPathThrowsIllegalArgumentException() {
+        assertThrows(IllegalArgumentException.class, () -> Main.writeReport(null, "Sample Report Content"));
+    }
 }
```

---

### 2.3 `src/test/java/com/loganalyzer/report/ReportGeneratorTest.java`

```diff
diff --git a/src/test/java/com/loganalyzer/report/ReportGeneratorTest.java b/src/test/java/com/loganalyzer/report/ReportGeneratorTest.java
index 5b6bde5..bdaf5fb 100644
--- a/src/test/java/com/loganalyzer/report/ReportGeneratorTest.java
+++ b/src/test/java/com/loganalyzer/report/ReportGeneratorTest.java
@@ -104,4 +104,43 @@ class ReportGeneratorTest {
         assertTrue(json.contains("\"totalLogs\": 100"));
         assertTrue(json.contains("\"id\": \"INC-001\""));
     }
+
+    @Test
+    @DisplayName("Task 6: ConsoleReportGenerator safely handles AnalysisResult with null statistics")
+    void testConsoleReportWithNullStatistics() {
+        AnalysisResult nullStatsResult = new AnalysisResult("null-stats.log", List.of(), null, List.of(), List.of());
+        ConsoleReportGenerator generator = new ConsoleReportGenerator();
+        assertDoesNotThrow(() -> {
+            String report = generator.generate(nullStatsResult);
+            assertNotNull(report);
+            assertTrue(report.contains("Total Records   : 0"));
+            assertTrue(report.contains("Time Range      : N/A to N/A"));
+        });
+    }
+
+    @Test
+    @DisplayName("Task 6: MarkdownReportGenerator safely handles AnalysisResult with null statistics")
+    void testMarkdownReportWithNullStatistics() {
+        AnalysisResult nullStatsResult = new AnalysisResult("null-stats.log", List.of(), null, List.of(), List.of());
+        MarkdownReportGenerator generator = new MarkdownReportGenerator();
+        assertDoesNotThrow(() -> {
+            String report = generator.generate(nullStatsResult);
+            assertNotNull(report);
+            assertTrue(report.contains("| **Total Log Entries** | 0 |"));
+            assertTrue(report.contains("| **Time Range** | N/A to N/A |"));
+        });
+    }
+
+    @Test
+    @DisplayName("Task 6: JsonReportGenerator safely handles AnalysisResult with null statistics")
+    void testJsonReportWithNullStatistics() {
+        AnalysisResult nullStatsResult = new AnalysisResult("null-stats.log", List.of(), null, List.of(), List.of());
+        JsonReportGenerator generator = new JsonReportGenerator();
+        assertDoesNotThrow(() -> {
+            String json = generator.generate(nullStatsResult);
+            assertNotNull(json);
+            assertTrue(json.contains("\"totalLogs\": 0"));
+            assertTrue(json.contains("\"firstTimestamp\": null"));
+        });
+    }
 }
```

---

### 2.4 `src/test/java/com/loganalyzer/integration/LogAnalyzerEndToEndTest.java`

```diff
diff --git a/src/test/java/com/loganalyzer/integration/LogAnalyzerEndToEndTest.java b/src/test/java/com/loganalyzer/integration/LogAnalyzerEndToEndTest.java
index fc7346f..c15542e 100644
--- a/src/test/java/com/loganalyzer/integration/LogAnalyzerEndToEndTest.java
+++ b/src/test/java/com/loganalyzer/integration/LogAnalyzerEndToEndTest.java
@@ -1,6 +1,7 @@
 package com.loganalyzer.integration;
 
 import com.loganalyzer.analyzer.LogAnalyzer;
+import com.loganalyzer.exception.InvalidLogFileException;
 import com.loganalyzer.model.AnalysisResult;
 import com.loganalyzer.model.IncidentCategory;
 import com.loganalyzer.model.IncidentSeverity;
@@ -8,6 +9,7 @@ import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.DisplayName;
 import org.junit.jupiter.api.Test;
 
+import java.io.InputStream;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 
@@ -76,4 +78,23 @@ class LogAnalyzerEndToEndTest {
                 .anyMatch(i -> i.getCategory() == IncidentCategory.MEMORY_CRITICAL_FAILURE && i.getSeverity() == IncidentSeverity.CRITICAL);
         assertTrue(hasOomIncident, "Should identify CRITICAL MEMORY_CRITICAL_FAILURE incident");
     }
+
+    @Test
+    @DisplayName("Task 6: Should throw IllegalArgumentException when log file path is null")
+    void testAnalyzeNullPathThrowsIllegalArgumentException() {
+        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze((Path) null));
+    }
+
+    @Test
+    @DisplayName("Task 6: Should throw IllegalArgumentException when InputStream is null")
+    void testAnalyzeNullInputStreamThrowsIllegalArgumentException() {
+        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze((InputStream) null, "source"));
+    }
+
+    @Test
+    @DisplayName("Task 6: Should throw InvalidLogFileException when analyzing non-existent file")
+    void testAnalyzeNonExistentFileThrowsInvalidLogFileException() {
+        Path invalidPath = Paths.get("non-existent-directory/non-existent-file.log");
+        assertThrows(InvalidLogFileException.class, () -> analyzer.analyze(invalidPath));
+    }
 }
```
