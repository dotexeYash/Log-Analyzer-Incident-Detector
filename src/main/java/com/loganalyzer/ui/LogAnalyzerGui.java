package com.loganalyzer.ui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Entry point for the native Java desktop interface.
 */
public final class LogAnalyzerGui {

    private static final String WAYLAND_BOOTSTRAP_MARKER = "LOG_ANALYZER_WAYLAND_BOOTSTRAPPED";

    private LogAnalyzerGui() {
    }

    public static void launch() {
        launch(new String[]{"--gui"});
    }

    public static void launch(String[] applicationArgs) {
        String[] safeArgs = applicationArgs == null ? new String[]{"--gui"} : applicationArgs.clone();
        if (requiresWaylandBootstrap()) {
            relaunchForWayland(safeArgs);
            return;
        }

        configureGraphicsPipeline();
        if (GraphicsEnvironment.isHeadless()) {
            String display = System.getenv("DISPLAY");
            if (isWaylandSession() && (display == null || display.isBlank())) {
                if (isNiriSession()) {
                    throw new IllegalStateException(
                            "Java 21 Swing on niri requires xwayland-satellite and a valid DISPLAY");
                }
                throw new IllegalStateException(
                        "Swing on Wayland requires XWayland; install and enable xorg-xwayland");
            }
            throw new IllegalStateException("a graphical desktop environment is required");
        }

        Runnable openWindow = () -> {
            configureLookAndFeel();
            AnalyzerFrame frame = new AnalyzerFrame();
            frame.setVisible(true);
            SwingUtilities.invokeLater(() -> {
                frame.validate();
                frame.repaint();
                Toolkit.getDefaultToolkit().sync();
            });
        };

        if (SwingUtilities.isEventDispatchThread()) {
            openWindow.run();
        } else {
            SwingUtilities.invokeLater(openWindow);
        }
    }

    public static void main(String[] args) {
        launch(args == null || args.length == 0 ? new String[]{"--gui"} : args);
    }

    private static void configureLookAndFeel() {
        try {
            String lookAndFeel = isWaylandSession()
                    ? UIManager.getCrossPlatformLookAndFeelClassName()
                    : UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (ClassNotFoundException | InstantiationException |
                 IllegalAccessException | UnsupportedLookAndFeelException ignored) {
            // Swing's cross-platform look and feel remains available as a fallback.
        }

        JFrame.setDefaultLookAndFeelDecorated(false);

        Font defaultFont = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("CheckBox.font", defaultFont);
        UIManager.put("ComboBox.font", defaultFont);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("List.font", defaultFont);
        UIManager.put("Menu.font", defaultFont);
        UIManager.put("MenuItem.font", defaultFont);
        UIManager.put("OptionPane.font", defaultFont);
        UIManager.put("Panel.font", defaultFont);
        UIManager.put("ProgressBar.font", defaultFont.deriveFont(11f));
        UIManager.put("TabbedPane.font", defaultFont);
        UIManager.put("Table.font", defaultFont);
        UIManager.put("TableHeader.font", defaultFont.deriveFont(Font.BOLD));
        UIManager.put("TextArea.font", defaultFont);
        UIManager.put("TextField.font", defaultFont);
    }

    private static void configureGraphicsPipeline() {
        if (!isWaylandSession()) {
            return;
        }
        System.setProperty("sun.awt.disablegrab", "true");
        System.setProperty("sun.java2d.opengl", "false");
        System.setProperty("sun.java2d.xrender", "true");
    }

    private static boolean requiresWaylandBootstrap() {
        if (!isWaylandSession()) {
            return false;
        }
        String configured = System.getenv("_JAVA_AWT_WM_NONREPARENTING");
        boolean alreadyConfigured = "1".equals(configured)
                || "true".equalsIgnoreCase(configured);
        return !alreadyConfigured && System.getenv(WAYLAND_BOOTSTRAP_MARKER) == null;
    }

    private static void relaunchForWayland(String[] applicationArgs) {
        String javaHome = System.getProperty("java.home");
        String classPath = System.getProperty("java.class.path");
        if (javaHome == null || classPath == null || classPath.isBlank()) {
            throw new IllegalStateException("could not prepare the Wayland Java compatibility launcher");
        }

        List<String> command = new ArrayList<>();
        command.add(Path.of(javaHome, "bin", "java").toString());
        command.add("-cp");
        command.add(classPath);
        command.add("com.loganalyzer.Main");
        command.addAll(Arrays.asList(applicationArgs));

        ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();
        processBuilder.environment().put("_JAVA_AWT_WM_NONREPARENTING", "1");
        processBuilder.environment().put(WAYLAND_BOOTSTRAP_MARKER, "1");

        try {
            Process child = processBuilder.start();
            int exitCode = child.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException(
                        "Wayland compatibility process exited with code " + exitCode);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "could not start the Wayland compatibility process", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Wayland compatibility process was interrupted", exception);
        }
    }

    private static boolean isHyprlandSession() {
        String signature = System.getenv("HYPRLAND_INSTANCE_SIGNATURE");
        String desktop = System.getenv("XDG_CURRENT_DESKTOP");
        return (signature != null && !signature.isBlank())
                || (desktop != null && desktop.toLowerCase(Locale.ROOT).contains("hyprland"));
    }

    private static boolean isNiriSession() {
        String socket = System.getenv("NIRI_SOCKET");
        String desktop = System.getenv("XDG_CURRENT_DESKTOP");
        return (socket != null && !socket.isBlank())
                || (desktop != null && desktop.toLowerCase(Locale.ROOT).contains("niri"));
    }

    private static boolean isWaylandSession() {
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        return isHyprlandSession()
                || isNiriSession()
                || (waylandDisplay != null && !waylandDisplay.isBlank())
                || "wayland".equalsIgnoreCase(sessionType);
    }
}
