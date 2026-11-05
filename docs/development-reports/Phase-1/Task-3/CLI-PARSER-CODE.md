# 📋 CLI Argument Parsing & Validation Reference Implementation

**File Location:** [`src/main/java/com/loganalyzer/Main.java`](file:///c:/Users/YASH/Desktop/log%20analyser/Log-Analyzer-Incident-Detector/src/main/java/com/loganalyzer/Main.java)  
**Package:** `com.loganalyzer`  
**Java Version:** Java 21  

---

## 1. `CliConfig` Record

*Defined at lines 28–38 of `Main.java`.*

```java
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
```

---

## 2. `parseArguments(String[] args)` Method

*Defined at lines 105–175 of `Main.java`.*

```java
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
```

---

## 3. Helper Methods

### 3.1. `requireValue(String[] args, int index, String flag)`
*Defined at lines 177–186 of `Main.java`.*  
Validates that a required parameter value is present, non-blank, and not another CLI flag.

```java
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
```

### 3.2. `isFlag(String s)`
*Defined at lines 188–199 of `Main.java`.*  
Distinguishes CLI option flags (e.g. `--help`, `-f`, `-w`) from negative numbers (e.g. `-5`, `-10`).

```java
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
```

### 3.3. `parsePositiveInteger(String flag, String value)`
*Defined at lines 201–215 of `Main.java`.*  
Converts input string to integer, catching `NumberFormatException` (for alphabetic strings and floating-point decimals) and validating that the value is strictly positive ($> 0$).

```java
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

---

## 4. Integration into `main(String[] args)`

*Defined at lines 40–55 of `Main.java`.*  
Decoupled invocation with user-friendly error reporting:

```java
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
        // ... rest of main logic ...
    }
```
