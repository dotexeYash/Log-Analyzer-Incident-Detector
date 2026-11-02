package com.loganalyzer;

import com.loganalyzer.analyzer.AnomalyDetector;
import com.loganalyzer.analyzer.IncidentDetector;
import com.loganalyzer.analyzer.LogAnalyzer;
import com.loganalyzer.analyzer.StatisticsAggregator;
import com.loganalyzer.model.AnalysisResult;
import com.loganalyzer.model.IncidentSeverity;
import com.loganalyzer.parser.StandardLogParser;
import com.loganalyzer.report.ConsoleReportGenerator;
import com.loganalyzer.report.JsonReportGenerator;
import com.loganalyzer.report.MarkdownReportGenerator;
import com.loganalyzer.report.ReportGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Command-Line Interface and entry point for the Log Analyzer and Incident Detector application.
 */
public class Main {

    public static void main(String[] args) {
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

        if (isDemo || (filePath == null && args.length == 0)) {
            runDemonstration();
            return;
        }

        if (filePath == null) {
            System.err.println("Error: No input log file specified.\n");
            printUsage();
            System.exit(1);
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.err.println("Error: Specified file does not exist: " + path.toAbsolutePath());
            System.exit(1);
        }

        AnomalyDetector anomalyDetector = new AnomalyDetector(burstThreshold, windowSeconds, 4, 3, 2, 3);
        LogAnalyzer analyzer = new LogAnalyzer(new StandardLogParser(), new StatisticsAggregator(), anomalyDetector, new IncidentDetector());

        try {
            AnalysisResult result = analyzer.analyze(path);
            String report = renderReport(result, format);

            if (outputPath != null) {
                Path out = Paths.get(outputPath);
                if (out.getParent() != null) {
                    Files.createDirectories(out.getParent());
                }
                Files.writeString(out, report);
                System.out.println("Report successfully written to: " + out.toAbsolutePath());
            } else {
                System.out.println(report);
            }

            // Return non-zero exit code if critical incidents were detected
            if (result.hasCriticalIncidents()) {
                System.exit(2);
            }
        } catch (Exception e) {
            System.err.println("Failed to analyze log file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String renderReport(AnalysisResult result, String format) {
        ReportGenerator generator = switch (format) {
            case "markdown", "md" -> new MarkdownReportGenerator();
            case "json" -> new JsonReportGenerator();
            default -> new ConsoleReportGenerator();
        };
        return generator.generate(result);
    }

    private static void runDemonstration() {
        System.out.println("================================================================================");
        System.out.println("     RUNNING LOG ANALYZER & INCIDENT DETECTOR DEMONSTRATION SUITE               ");
        System.out.println("================================================================================\n");

        String[] demoFiles = {
                "sample-logs/normal-operations.log",
                "sample-logs/critical-incident-database.log",
                "sample-logs/security-attack.log",
                "sample-logs/out-of-memory.log"
        };

        LogAnalyzer analyzer = new LogAnalyzer();
        ConsoleReportGenerator reportGenerator = new ConsoleReportGenerator();

        for (String sample : demoFiles) {
            Path path = Paths.get(sample);
            if (!Files.exists(path)) {
                // Try classpath resource
                try (InputStream is = Main.class.getClassLoader().getResourceAsStream(sample)) {
                    if (is != null) {
                        AnalysisResult result = analyzer.analyze(is, sample);
                        System.out.println(reportGenerator.generate(result));
                        System.out.println("\n");
                        continue;
                    }
                } catch (IOException ignored) {}
            }

            if (Files.exists(path)) {
                AnalysisResult result = analyzer.analyze(path);
                System.out.println(reportGenerator.generate(result));
                System.out.println("\n");
            } else {
                System.out.println("Notice: Demo file " + sample + " not found locally or on classpath.");
            }
        }
    }

    private static void printUsage() {
        System.out.println("""
            Log Analyzer and Incident Detector (Java 21)
            Usage:
              java -jar target/log-analyzer-incident-detector-1.0.0.jar [options]
              
            Options:
              -f,   --file <path>             Path to target log file to inspect
              -fmt, --format <console|md|json> Format of the generated report (default: console)
              -o,   --output <path>           Optional output destination file path
              -b,   --burst-threshold <num>   Consecutive error threshold for bursts (default: 5)
              -w,   --window <seconds>        Time window in seconds for burst detection (default: 60)
              --demo                          Execute demonstration against all bundled sample scenarios
              -h,   --help                    Display this help message
              
            Exit Codes:
              0 - Analysis completed without critical issues
              1 - Error during execution or invalid inputs
              2 - One or more CRITICAL operational incidents detected
            """);
    }
}
