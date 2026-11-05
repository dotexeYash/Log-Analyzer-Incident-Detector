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

    /**
     * Immutable configuration record storing parsed command-line options.
     */
    public record CliConfig(
            String filePath,
            String format,
            String outputPath,
            int burstThreshold,
            int windowSeconds,
            boolean isDemo,
            boolean isHelp
    ) {}

    public static void main(String[] args) {
        CliConfig config;
        try {
            config = parseArguments(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage() + System.lineSeparator());
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
            System.err.println("Error: No input log file specified." + System.lineSeparator());
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

        AnomalyDetector anomalyDetector = new AnomalyDetector(config.burstThreshold(), config.windowSeconds(), 4, 3, 2, 3);
        LogAnalyzer analyzer = new LogAnalyzer(new StandardLogParser(), new StatisticsAggregator(), anomalyDetector, new IncidentDetector());

        try {
            AnalysisResult result = analyzer.analyze(path);
            String report = renderReport(result, config.format());

            if (config.outputPath() != null) {
                Path out = Paths.get(config.outputPath());
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

    /**
     * Parses and validates CLI arguments into a CliConfig object.
     *
     * @param args command-line arguments array
     * @return validated CliConfig instance
     * @throws IllegalArgumentException if arguments violate CLI constraints
     */
    public static CliConfig parseArguments(String[] args) {
        if (args == null) {
            args = new String[0];
        }

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
                    filePath = requireValue(args, i, arg);
                    i++;
                }
                case "-fmt", "--format" -> {
                    String fmt = requireValue(args, i, arg).toLowerCase(Locale.ROOT);
                    if (!fmt.equals("console") && !fmt.equals("markdown") && !fmt.equals("md") && !fmt.equals("json")) {
                        throw new IllegalArgumentException(
                                String.format("Unsupported format \"%s\". Supported formats: console, markdown (md), json.", fmt)
                        );
                    }
                    format = fmt;
                    i++;
                }
                case "-o", "--output" -> {
                    outputPath = requireValue(args, i, arg);
                    i++;
                }
                case "-b", "--burst-threshold" -> {
                    String val = requireValue(args, i, arg);
                    burstThreshold = parsePositiveInteger(arg, val);
                    i++;
                }
                case "-w", "--window" -> {
                    String val = requireValue(args, i, arg);
                    windowSeconds = parsePositiveInteger(arg, val);
                    i++;
                }
                case "--demo" -> isDemo = true;
                default -> {
                    if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    } else if (filePath == null) {
                        if (arg.isBlank()) {
                            throw new IllegalArgumentException("File path cannot be blank");
                        }
                        filePath = arg;
                    } else {
                        throw new IllegalArgumentException("Unexpected argument: " + arg);
                    }
                }
            }
        }

        return new CliConfig(filePath, format, outputPath, burstThreshold, windowSeconds, isDemo, false);
    }

    private static String requireValue(String[] args, int index, String flag) {
        if (index + 1 >= args.length || isFlag(args[index + 1])) {
            throw new IllegalArgumentException("Missing required value for argument: " + flag);
        }
        String val = args[index + 1];
        if (val.isBlank()) {
            throw new IllegalArgumentException("Value for argument " + flag + " cannot be blank");
        }
        return val;
    }

    private static boolean isFlag(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        if (s.startsWith("--")) {
            return true;
        }
        if (s.startsWith("-") && s.length() > 1) {
            return !Character.isDigit(s.charAt(1));
        }
        return false;
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
