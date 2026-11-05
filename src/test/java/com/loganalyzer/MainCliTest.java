package com.loganalyzer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class MainCliTest {

    @Test
    @DisplayName("Empty arguments should produce default configuration")
    void testEmptyArgumentsProduceDefaultConfig() {
        Main.CliConfig config = Main.parseArguments(new String[0]);
        assertNotNull(config);
        assertNull(config.filePath());
        assertEquals("console", config.format());
        assertNull(config.outputPath());
        assertEquals(5, config.burstThreshold());
        assertEquals(60, config.windowSeconds());
        assertFalse(config.isDemo());
        assertFalse(config.isHelp());
    }

    @Test
    @DisplayName("Null arguments should be handled safely as empty args")
    void testNullArgumentsHandledSafely() {
        Main.CliConfig config = Main.parseArguments(null);
        assertNotNull(config);
        assertEquals(5, config.burstThreshold());
        assertEquals(60, config.windowSeconds());
    }

    @Test
    @DisplayName("Valid full CLI arguments should be parsed correctly")
    void testValidFullArguments() {
        String[] args = {
                "-f", "sample-logs/normal-operations.log",
                "-fmt", "json",
                "-o", "reports/analysis.json",
                "-b", "10",
                "-w", "120"
        };

        Main.CliConfig config = Main.parseArguments(args);
        assertEquals("sample-logs/normal-operations.log", config.filePath());
        assertEquals("json", config.format());
        assertEquals("reports/analysis.json", config.outputPath());
        assertEquals(10, config.burstThreshold());
        assertEquals(120, config.windowSeconds());
        assertFalse(config.isDemo());
        assertFalse(config.isHelp());
    }

    @Test
    @DisplayName("Positional file argument should be accepted")
    void testPositionalFileArgument() {
        String[] args = {"sample-logs/critical-incident-database.log"};
        Main.CliConfig config = Main.parseArguments(args);
        assertEquals("sample-logs/critical-incident-database.log", config.filePath());
    }

    @Test
    @DisplayName("Help flags should set isHelp flag")
    void testHelpFlags() {
        assertTrue(Main.parseArguments(new String[]{"-h"}).isHelp());
        assertTrue(Main.parseArguments(new String[]{"--help"}).isHelp());
    }

    @Test
    @DisplayName("Demo flag should set isDemo flag")
    void testDemoFlag() {
        assertTrue(Main.parseArguments(new String[]{"--demo"}).isDemo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"console", "markdown", "md", "json", "MD", "JSON"})
    @DisplayName("Supported report formats should be accepted")
    void testSupportedFormats(String format) {
        String[] args = {"-f", "test.log", "-fmt", format};
        Main.CliConfig config = Main.parseArguments(args);
        assertEquals(format.toLowerCase(), config.format());
    }

    @Test
    @DisplayName("Unsupported report format should be rejected with clear error")
    void testUnsupportedFormatRejected() {
        String[] args = {"-f", "test.log", "-fmt", "xml"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Unsupported format \"xml\""));
    }

    @Test
    @DisplayName("Non-numeric burst threshold should throw descriptive IllegalArgumentException")
    void testNonNumericBurstThresholdRejected() {
        String[] args = {"-f", "test.log", "-b", "abc"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Invalid numeric value for -b: \"abc\""));
        assertTrue(ex.getMessage().contains("Expected a positive integer"));
        assertInstanceOf(NumberFormatException.class, ex.getCause(), "Must preserve NumberFormatException as cause");
    }

    @Test
    @DisplayName("Task 6: Non-numeric integer option should preserve NumberFormatException as cause")
    void testNonNumericOptionPreservesNumberFormatExceptionCause() {
        String[] args = {"-f", "test.log", "-w", "not-a-number"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertNotNull(ex.getCause(), "IllegalArgumentException must preserve underlying cause");
        assertInstanceOf(NumberFormatException.class, ex.getCause(), "Underlying cause must be NumberFormatException");
    }

    @Test
    @DisplayName("Non-numeric window seconds should throw descriptive IllegalArgumentException")
    void testNonNumericWindowSecondsRejected() {
        String[] args = {"-f", "test.log", "-w", "xyz"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Invalid numeric value for -w: \"xyz\""));
        assertTrue(ex.getMessage().contains("Expected a positive integer"));
    }

    @Test
    @DisplayName("Decimal float burst threshold should throw descriptive IllegalArgumentException")
    void testDecimalBurstThresholdRejected() {
        String[] args = {"-f", "test.log", "-b", "2.5"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Invalid numeric value for -b: \"2.5\""));
        assertTrue(ex.getMessage().contains("Expected a positive integer"));
    }

    @Test
    @DisplayName("Decimal float window seconds should throw descriptive IllegalArgumentException")
    void testDecimalWindowSecondsRejected() {
        String[] args = {"-f", "test.log", "-w", "1.5"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Invalid numeric value for -w: \"1.5\""));
        assertTrue(ex.getMessage().contains("Expected a positive integer"));
    }

    @Test
    @DisplayName("Negative burst threshold should be rejected")
    void testNegativeBurstThresholdRejected() {
        String[] args = {"-f", "test.log", "-b", "-5"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("must be a positive integer greater than 0, got: -5"));
    }

    @Test
    @DisplayName("Negative window seconds should be rejected")
    void testNegativeWindowSecondsRejected() {
        String[] args = {"-f", "test.log", "-w", "-10"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("must be a positive integer greater than 0, got: -10"));
    }

    @Test
    @DisplayName("Zero burst threshold should be rejected")
    void testZeroBurstThresholdRejected() {
        String[] args = {"-f", "test.log", "-b", "0"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("must be a positive integer greater than 0, got: 0"));
    }

    @Test
    @DisplayName("Zero window seconds should be rejected")
    void testZeroWindowSecondsRejected() {
        String[] args = {"-f", "test.log", "-w", "0"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("must be a positive integer greater than 0, got: 0"));
    }

    @Test
    @DisplayName("Missing burst threshold value at end of args should be rejected")
    void testMissingBurstThresholdValueAtEndRejected() {
        String[] args = {"-f", "test.log", "-b"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Missing required value for argument: -b"));
    }

    @Test
    @DisplayName("Missing window value followed immediately by another flag should be rejected")
    void testMissingWindowValueFollowedByFlagRejected() {
        String[] args = {"-w", "-f", "test.log"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Missing required value for argument: -w"));
    }

    @Test
    @DisplayName("Missing file argument value should be rejected")
    void testMissingFileArgumentValueRejected() {
        String[] args = {"-f"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Missing required value for argument: -f"));
    }

    @Test
    @DisplayName("Missing output argument value should be rejected")
    void testMissingOutputArgumentValueRejected() {
        String[] args = {"-f", "test.log", "-o"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Missing required value for argument: -o"));
    }

    @Test
    @DisplayName("Blank file path should be rejected")
    void testBlankFilePathRejected() {
        String[] args = {"-f", "   "};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Value for argument -f cannot be blank"));
    }

    @Test
    @DisplayName("Blank output path should be rejected")
    void testBlankOutputPathRejected() {
        String[] args = {"-f", "test.log", "-o", "   "};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Value for argument -o cannot be blank"));
    }

    @Test
    @DisplayName("Unknown option should be rejected")
    void testUnknownOptionRejected() {
        String[] args = {"--invalid-option"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.parseArguments(args));
        assertTrue(ex.getMessage().contains("Unknown option: --invalid-option"));
    }

    @Test
    @DisplayName("Task 6: writeReport should throw IOException when target path is an existing directory")
    void testWriteReportToDirectoryThrowsIOException() {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        assertThrows(IOException.class, () -> Main.writeReport(tempDir, "Sample Report Content"));
    }

    @Test
    @DisplayName("Task 6: writeReport should throw IllegalArgumentException when path is null")
    void testWriteReportNullPathThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Main.writeReport(null, "Sample Report Content"));
    }
}
