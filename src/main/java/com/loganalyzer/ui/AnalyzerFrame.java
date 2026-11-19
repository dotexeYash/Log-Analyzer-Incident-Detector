package com.loganalyzer.ui;

import com.loganalyzer.analyzer.AnomalyDetector;
import com.loganalyzer.analyzer.IncidentDetector;
import com.loganalyzer.analyzer.LogAnalyzer;
import com.loganalyzer.analyzer.StatisticsAggregator;
import com.loganalyzer.model.AnalysisResult;
import com.loganalyzer.model.Anomaly;
import com.loganalyzer.model.Incident;
import com.loganalyzer.model.IncidentSeverity;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogLevel;
import com.loganalyzer.model.LogStatistics;
import com.loganalyzer.parser.StandardLogParser;
import com.loganalyzer.report.JsonReportGenerator;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Main window for the native Swing log analyzer.
 */
@SuppressWarnings("serial")
final class AnalyzerFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND = new Color(245, 246, 248);
    private static final Color SURFACE = Color.WHITE;
    private static final Color BORDER = new Color(218, 222, 228);
    private static final Color TEXT = new Color(31, 35, 41);
    private static final Color MUTED = new Color(100, 108, 120);
    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color DANGER = new Color(185, 28, 28);
    private static final Color WARNING = new Color(180, 83, 9);
    private static final Color SUCCESS = new Color(21, 128, 61);

    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss", Locale.ROOT);
    private static final DateTimeFormatter TABLE_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.ROOT);

    private final JComboBox<SampleOption> sampleSelector = new JComboBox<>(SampleOption.values());
    private final JButton openFileButton = new JButton("Open log file");
    private final JButton settingsButton = new JButton("Detection settings");
    private final JButton exportButton = new JButton("Export JSON");
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel operationLabel = new JLabel("Ready");

    private final JLabel sourceNameLabel = new JLabel("No source analyzed");
    private final JLabel sourceRangeLabel = new JLabel(" ");
    private final JLabel sourceStatusLabel = new JLabel("Not analyzed");

    private final MetricCard totalLogsCard = new MetricCard("Total log entries");
    private final MetricCard errorRateCard = new MetricCard("Error rate");
    private final MetricCard anomalyCard = new MetricCard("Anomalies");
    private final MetricCard incidentCard = new MetricCard("Incidents");

    private final ActivityChartPanel activityChart = new ActivityChartPanel();
    private final JPanel levelDistributionPanel = new JPanel();
    private final DefaultListModel<String> topErrorsModel = new DefaultListModel<>();
    private final JList<String> topErrorsList = new JList<>(topErrorsModel);

    private final DefaultListModel<Incident> incidentModel = new DefaultListModel<>();
    private final JList<Incident> incidentList = new JList<>(incidentModel);
    private final JTextArea incidentDetails = createReadOnlyTextArea();
    private final JButton viewEvidenceButton = new JButton("View related logs");

    private final LogTableModel logTableModel = new LogTableModel();
    private final JTable logTable = new JTable(logTableModel);
    private final TableRowSorter<LogTableModel> logSorter = new TableRowSorter<>(logTableModel);
    private final JTextField logSearchField = new JTextField(28);
    private final JComboBox<String> levelFilter = new JComboBox<>();
    private final JButton clearEvidenceButton = new JButton("Clear evidence filter");
    private final JLabel logCountLabel = new JLabel("0 entries");
    private final JTabbedPane tabs = new JTabbedPane();

    private AnalysisResult currentResult;
    private SampleOption currentSample;
    private Path currentFile;
    private Set<Long> evidenceLineNumbers = Set.of();
    private int burstThreshold = 5;
    private int windowSeconds = 60;
    private boolean suppressSampleSelection;

    AnalyzerFrame() {
        super("Log Analyzer & Incident Detector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 560));
        setSize(1180, 760);
        setLocationByPlatform(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createBody(), BorderLayout.CENTER);
        setContentPane(root);

        configureComponents();
        bindActions();
        SwingUtilities.invokeLater(() -> analyzeSample(SampleOption.DATABASE_INCIDENT));
    }

    private JComponent createHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("Log Analyzer & Incident Detector");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("Analyze application logs and review detected incidents");
        subtitle.setForeground(MUTED);
        titlePanel.add(title);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        sampleSelector.setPreferredSize(new Dimension(190, 30));
        controls.add(new JLabel("Sample:"));
        controls.add(sampleSelector);
        controls.add(settingsButton);
        controls.add(exportButton);
        controls.add(openFileButton);
        header.add(controls, BorderLayout.EAST);

        return header;
    }

    private JComponent createBody() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(BACKGROUND);
        body.setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));

        JPanel summary = new JPanel(new BorderLayout(0, 10));
        summary.setOpaque(false);
        summary.add(createSourceBar(), BorderLayout.NORTH);

        JPanel metrics = new JPanel(new GridLayout(1, 4, 10, 0));
        metrics.setOpaque(false);
        metrics.add(totalLogsCard);
        metrics.add(errorRateCard);
        metrics.add(anomalyCard);
        metrics.add(incidentCard);
        summary.add(metrics, BorderLayout.CENTER);
        body.add(summary, BorderLayout.NORTH);

        tabs.addTab("Overview", createOverviewTab());
        tabs.addTab("Log entries", createLogTab());
        body.add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(150, 16));
        progressBar.setVisible(false);
        operationLabel.setForeground(MUTED);
        footer.add(operationLabel, BorderLayout.WEST);
        footer.add(progressBar, BorderLayout.EAST);
        body.add(footer, BorderLayout.SOUTH);
        return body;
    }

    private JComponent createSourceBar() {
        JPanel sourceBar = new JPanel(new BorderLayout(12, 0));
        sourceBar.setBackground(SURFACE);
        sourceBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel sourceCopy = new JPanel(new GridLayout(2, 1, 0, 2));
        sourceCopy.setOpaque(false);
        sourceNameLabel.setFont(sourceNameLabel.getFont().deriveFont(Font.BOLD));
        sourceNameLabel.setForeground(TEXT);
        sourceRangeLabel.setForeground(MUTED);
        sourceCopy.add(sourceNameLabel);
        sourceCopy.add(sourceRangeLabel);
        sourceBar.add(sourceCopy, BorderLayout.CENTER);

        sourceStatusLabel.setOpaque(true);
        sourceStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sourceStatusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        setStatusLabel("Not analyzed", MUTED, new Color(238, 240, 243));
        sourceBar.add(sourceStatusLabel, BorderLayout.EAST);
        return sourceBar;
    }

    private JComponent createOverviewTab() {
        JPanel overview = new JPanel(new BorderLayout(0, 10));
        overview.setBackground(BACKGROUND);
        overview.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel chartSection = sectionPanel("Log activity", activityChart);
        chartSection.setPreferredSize(new Dimension(700, 260));

        JPanel side = new JPanel(new GridLayout(2, 1, 0, 10));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(320, 260));
        levelDistributionPanel.setLayout(new GridLayout(0, 1, 0, 4));
        levelDistributionPanel.setBackground(SURFACE);
        side.add(sectionPanel("Log levels", levelDistributionPanel));
        topErrorsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        side.add(sectionPanel("Top errors", new JScrollPane(topErrorsList)));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(chartSection, BorderLayout.CENTER);
        top.add(side, BorderLayout.EAST);

        incidentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        incidentList.setCellRenderer(new IncidentCellRenderer());
        JPanel incidentListPanel = sectionPanel("Detected incidents", new JScrollPane(incidentList));

        JPanel detailContent = new JPanel(new BorderLayout(0, 8));
        detailContent.setBackground(SURFACE);
        detailContent.add(new JScrollPane(incidentDetails), BorderLayout.CENTER);
        JPanel detailActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        detailActions.setOpaque(false);
        detailActions.add(viewEvidenceButton);
        detailContent.add(detailActions, BorderLayout.SOUTH);
        JPanel incidentDetailPanel = sectionPanel("Incident details", detailContent);

        JSplitPane incidents = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                incidentListPanel,
                incidentDetailPanel);
        incidents.setResizeWeight(0.42);
        incidents.setDividerLocation(430);
        incidents.setBorder(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, incidents);
        mainSplit.setResizeWeight(0.55);
        mainSplit.setDividerLocation(290);
        mainSplit.setBorder(null);
        overview.add(mainSplit, BorderLayout.CENTER);
        return overview;
    }

    private JComponent createLogTab() {
        JPanel logs = new JPanel(new BorderLayout(0, 8));
        logs.setBackground(BACKGROUND);
        logs.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tools.setOpaque(false);
        tools.add(new JLabel("Search:"));
        tools.add(logSearchField);
        tools.add(new JLabel("Level:"));
        levelFilter.addItem("All levels");
        for (LogLevel level : LogLevel.values()) {
            levelFilter.addItem(level.name());
        }
        tools.add(levelFilter);
        clearEvidenceButton.setVisible(false);
        tools.add(clearEvidenceButton);
        logs.add(tools, BorderLayout.NORTH);

        logTable.setRowSorter(logSorter);
        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logTable.setRowHeight(27);
        logTable.setShowVerticalLines(false);
        logTable.setGridColor(BORDER);
        logTable.setFillsViewportHeight(true);
        logTable.getColumnModel().getColumn(0).setPreferredWidth(105);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(170);
        logTable.getColumnModel().getColumn(4).setPreferredWidth(560);
        logTable.getColumnModel().getColumn(1).setCellRenderer(new LevelCellRenderer());

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        logs.add(scrollPane, BorderLayout.CENTER);

        logCountLabel.setForeground(MUTED);
        logs.add(logCountLabel, BorderLayout.SOUTH);
        return logs;
    }

    private JPanel sectionPanel(String title, Component content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(6, 8, 8, 8), title)));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private void configureComponents() {
        openFileButton.setBackground(PRIMARY);
        openFileButton.setForeground(Color.WHITE);
        exportButton.setEnabled(false);
        viewEvidenceButton.setEnabled(false);
        activityChart.setPreferredSize(new Dimension(600, 220));
        incidentDetails.setText("Select an incident to view details.");
    }

    private void bindActions() {
        sampleSelector.addActionListener(event -> {
            if (!suppressSampleSelection && sampleSelector.getSelectedItem() instanceof SampleOption option) {
                analyzeSample(option);
            }
        });
        openFileButton.addActionListener(event -> chooseLogFile());
        settingsButton.addActionListener(event -> showDetectionSettings());
        exportButton.addActionListener(event -> exportJsonReport());

        incidentList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showIncidentDetails(incidentList.getSelectedValue());
            }
        });
        viewEvidenceButton.addActionListener(event -> showSelectedIncidentEvidence());

        DocumentListener filterListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyLogFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyLogFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyLogFilter();
            }
        };
        logSearchField.getDocument().addDocumentListener(filterListener);
        levelFilter.addActionListener(event -> applyLogFilter());
        clearEvidenceButton.addActionListener(event -> {
            evidenceLineNumbers = Set.of();
            clearEvidenceButton.setVisible(false);
            applyLogFilter();
        });

        logTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && logTable.getSelectedRow() >= 0) {
                    int modelRow = logTable.convertRowIndexToModel(logTable.getSelectedRow());
                    showLogEntryDetails(logTableModel.getEntry(modelRow));
                }
            }
        });
    }

    private void chooseLogFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a log file");
        chooser.setFileFilter(new FileNameExtensionFilter("Log and text files", "log", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            analyzeFile(chooser.getSelectedFile().toPath());
        }
    }

    private void analyzeSample(SampleOption sample) {
        if (sampleSelector.getSelectedItem() != sample) {
            suppressSampleSelection = true;
            sampleSelector.setSelectedItem(sample);
            suppressSampleSelection = false;
        }
        currentSample = sample;
        currentFile = null;
        runAnalysis(sample.fileName, () -> {
            try (InputStream input = AnalyzerFrame.class.getClassLoader()
                    .getResourceAsStream(sample.resourcePath)) {
                if (input == null) {
                    throw new IOException("Bundled sample could not be found: " + sample.resourcePath);
                }
                return createAnalyzer().analyze(input, sample.fileName);
            }
        });
    }

    private void analyzeFile(Path path) {
        suppressSampleSelection = true;
        sampleSelector.setSelectedIndex(-1);
        suppressSampleSelection = false;
        currentSample = null;
        currentFile = path;
        String sourceName = path.getFileName() != null ? path.getFileName().toString() : path.toString();
        runAnalysis(sourceName, () -> createAnalyzer().analyze(path));
    }

    private void runAnalysis(String sourceName, AnalysisTask task) {
        setBusy(true, "Analyzing " + sourceName + "...");
        new SwingWorker<AnalysisResult, Void>() {
            @Override
            protected AnalysisResult doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                try {
                    updateAnalysis(get());
                    operationLabel.setText("Analysis complete");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showAnalysisError("Analysis was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
                    showAnalysisError(cause.getMessage() != null
                            ? cause.getMessage()
                            : "The log file could not be analyzed.");
                } finally {
                    setBusy(false, operationLabel.getText());
                }
            }
        }.execute();
    }

    private LogAnalyzer createAnalyzer() {
        StatisticsAggregator aggregator = new StatisticsAggregator();
        AnomalyDetector anomalyDetector = new AnomalyDetector(
                aggregator, burstThreshold, windowSeconds, 4, 3, 2, 3);
        return new LogAnalyzer(
                new StandardLogParser(),
                aggregator,
                anomalyDetector,
                new IncidentDetector());
    }

    private void updateAnalysis(AnalysisResult result) {
        currentResult = result;
        LogStatistics statistics = result.getStatistics();

        sourceNameLabel.setText(result.getSourceName());
        sourceRangeLabel.setText(formatRange(statistics));
        updateStatus(result);

        long errorCount = statistics.getCount(LogLevel.ERROR) + statistics.getCount(LogLevel.FATAL);
        totalLogsCard.setValue(Long.toString(statistics.getTotalLogs()), "Parsed entries");
        errorRateCard.setValue(
                String.format(Locale.ROOT, "%.1f%%", statistics.getErrorPercentage()),
                errorCount + " error entries");
        anomalyCard.setValue(
                Integer.toString(result.getAnomalies().size()),
                highestAnomalyLabel(result.getAnomalies()));
        long criticalCount = result.getIncidents().stream()
                .filter(incident -> incident.getSeverity() == IncidentSeverity.CRITICAL)
                .count();
        incidentCard.setValue(
                Integer.toString(result.getIncidents().size()),
                criticalCount + " critical");

        activityChart.setEntries(result.getEntries());
        updateLevelDistribution(statistics);
        updateTopErrors(statistics);
        updateIncidents(result.getIncidents());

        evidenceLineNumbers = Set.of();
        clearEvidenceButton.setVisible(false);
        logTableModel.setEntries(result.getEntries());
        applyLogFilter();
        exportButton.setEnabled(true);
    }

    private void updateStatus(AnalysisResult result) {
        if (result.hasCriticalIncidents()) {
            setStatusLabel("Critical incident", DANGER, new Color(254, 226, 226));
        } else if (result.hasIncidents()) {
            setStatusLabel("Incidents detected", WARNING, new Color(255, 237, 213));
        } else if (!result.getAnomalies().isEmpty()) {
            setStatusLabel("Anomalies detected", WARNING, new Color(255, 247, 214));
        } else {
            setStatusLabel("Healthy", SUCCESS, new Color(220, 252, 231));
        }
    }

    private void setStatusLabel(String text, Color foreground, Color background) {
        sourceStatusLabel.setText(text);
        sourceStatusLabel.setForeground(foreground);
        sourceStatusLabel.setBackground(background);
    }

    private void updateLevelDistribution(LogStatistics statistics) {
        levelDistributionPanel.removeAll();
        long total = Math.max(1L, statistics.getTotalLogs());
        LogLevel[] order = {
                LogLevel.INFO,
                LogLevel.WARN,
                LogLevel.ERROR,
                LogLevel.FATAL,
                LogLevel.DEBUG,
                LogLevel.TRACE,
                LogLevel.UNKNOWN
        };

        for (LogLevel level : order) {
            long count = statistics.getCount(level);
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            JLabel label = new JLabel(level.name());
            label.setPreferredSize(new Dimension(55, 20));
            JProgressBar bar = new JProgressBar(0, 100);
            int percentage = (int) Math.round((count * 100.0) / total);
            bar.setValue(percentage);
            bar.setString(count + " (" + percentage + "%)");
            bar.setStringPainted(true);
            bar.setForeground(levelColor(level));
            row.add(label, BorderLayout.WEST);
            row.add(bar, BorderLayout.CENTER);
            levelDistributionPanel.add(row);
        }
        levelDistributionPanel.revalidate();
        levelDistributionPanel.repaint();
    }

    private void updateTopErrors(LogStatistics statistics) {
        topErrorsModel.clear();
        statistics.getTopErrors().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(6)
                .forEach(entry -> topErrorsModel.addElement(entry.getValue() + " × " + entry.getKey()));
        if (topErrorsModel.isEmpty()) {
            topErrorsModel.addElement("No error patterns detected");
        }
    }

    private void updateIncidents(List<Incident> incidents) {
        incidentModel.clear();
        incidents.stream()
                .sorted(Comparator.comparingInt((Incident incident) -> incident.getSeverity().getRank()).reversed())
                .forEach(incidentModel::addElement);
        if (!incidentModel.isEmpty()) {
            incidentList.setSelectedIndex(0);
        } else {
            incidentDetails.setText("No incidents were detected in this log source.");
            viewEvidenceButton.setEnabled(false);
        }
    }

    private void showIncidentDetails(Incident incident) {
        if (incident == null) {
            incidentDetails.setText("Select an incident to view details.");
            viewEvidenceButton.setEnabled(false);
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append(incident.getTitle()).append(System.lineSeparator());
        text.append("Severity: ").append(incident.getSeverity()).append(System.lineSeparator());
        text.append("Category: ").append(humanize(incident.getCategory().name())).append(System.lineSeparator());
        text.append("Detected: ").append(incident.getDetectedAt().format(DISPLAY_TIME))
                .append(System.lineSeparator()).append(System.lineSeparator());
        text.append(incident.getSummary()).append(System.lineSeparator());

        if (!incident.getRecommendations().isEmpty()) {
            text.append(System.lineSeparator()).append("Recommended actions:")
                    .append(System.lineSeparator());
            for (int index = 0; index < incident.getRecommendations().size(); index++) {
                text.append(index + 1).append(". ")
                        .append(incident.getRecommendations().get(index))
                        .append(System.lineSeparator());
            }
        }

        incidentDetails.setText(text.toString());
        incidentDetails.setCaretPosition(0);
        viewEvidenceButton.setEnabled(!incident.getRootCauseEntries().isEmpty());
    }

    private void showSelectedIncidentEvidence() {
        Incident incident = incidentList.getSelectedValue();
        if (incident == null || incident.getRootCauseEntries().isEmpty()) {
            return;
        }
        Set<Long> lines = new HashSet<>();
        for (LogEntry entry : incident.getRootCauseEntries()) {
            lines.add(entry.getLineNumber());
        }
        evidenceLineNumbers = Set.copyOf(lines);
        clearEvidenceButton.setVisible(true);
        logSearchField.setText("");
        levelFilter.setSelectedIndex(0);
        applyLogFilter();
        tabs.setSelectedIndex(1);
    }

    private void applyLogFilter() {
        String query = logSearchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedLevel = levelFilter.getSelectedItem() != null
                ? levelFilter.getSelectedItem().toString()
                : "All levels";

        logSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                LogEntry logEntry = logTableModel.getEntry(entry.getIdentifier());
                if (!evidenceLineNumbers.isEmpty()
                        && !evidenceLineNumbers.contains(logEntry.getLineNumber())) {
                    return false;
                }
                if (!"All levels".equals(selectedLevel)
                        && !logEntry.getLevel().name().equals(selectedLevel)) {
                    return false;
                }
                if (query.isEmpty()) {
                    return true;
                }
                return logEntry.getMessage().toLowerCase(Locale.ROOT).contains(query)
                        || logEntry.getLogger().toLowerCase(Locale.ROOT).contains(query)
                        || logEntry.getThread().toLowerCase(Locale.ROOT).contains(query);
            }
        });
        logCountLabel.setText("Showing " + logTable.getRowCount()
                + " of " + logTableModel.getRowCount() + " entries");
    }

    private void showLogEntryDetails(LogEntry entry) {
        JTextArea details = createReadOnlyTextArea();
        StringBuilder text = new StringBuilder();
        text.append("Line: ").append(entry.getLineNumber()).append(System.lineSeparator());
        text.append("Timestamp: ").append(entry.getTimestamp() != null
                ? entry.getTimestamp().format(DISPLAY_TIME)
                : "Not available").append(System.lineSeparator());
        text.append("Level: ").append(entry.getLevel()).append(System.lineSeparator());
        text.append("Logger: ").append(entry.getLogger()).append(System.lineSeparator());
        text.append("Thread: ").append(entry.getThread()).append(System.lineSeparator())
                .append(System.lineSeparator());
        text.append(entry.getMessage());
        if (!entry.getStackTrace().isEmpty()) {
            text.append(System.lineSeparator()).append(System.lineSeparator());
            text.append(String.join(System.lineSeparator(), entry.getStackTrace()));
        }
        details.setText(text.toString());
        details.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(details);
        scrollPane.setPreferredSize(new Dimension(760, 430));
        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Log entry details",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void showDetectionSettings() {
        JSpinner burstSpinner = new JSpinner(new SpinnerNumberModel(
                burstThreshold, 1, 100_000, 1));
        JSpinner windowSpinner = new JSpinner(new SpinnerNumberModel(
                windowSeconds, 1, 86_400, 1));

        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        fields.add(new JLabel("Error burst threshold:"), constraints);
        constraints.gridx = 1;
        fields.add(burstSpinner, constraints);
        constraints.gridx = 2;
        fields.add(new JLabel("entries"), constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        fields.add(new JLabel("Detection window:"), constraints);
        constraints.gridx = 1;
        fields.add(windowSpinner, constraints);
        constraints.gridx = 2;
        fields.add(new JLabel("seconds"), constraints);

        int choice = JOptionPane.showConfirmDialog(
                this,
                fields,
                "Detection settings",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            burstThreshold = (Integer) burstSpinner.getValue();
            windowSeconds = (Integer) windowSpinner.getValue();
            rerunCurrentSource();
        }
    }

    private void rerunCurrentSource() {
        if (currentFile != null) {
            analyzeFile(currentFile);
        } else if (currentSample != null) {
            analyzeSample(currentSample);
        }
    }

    private void exportJsonReport() {
        if (currentResult == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export JSON report");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        chooser.setSelectedFile(new java.io.File(defaultExportName(currentResult.getSourceName())));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path output = chooser.getSelectedFile().toPath();
        if (!output.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
            output = output.resolveSibling(output.getFileName() + ".json");
        }
        try {
            Files.writeString(output, new JsonReportGenerator().generate(currentResult), StandardCharsets.UTF_8);
            operationLabel.setText("Report saved to " + output.toAbsolutePath());
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to save report: " + exception.getMessage(),
                    "Export failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String defaultExportName(String sourceName) {
        String base = sourceName == null ? "analysis" : sourceName.replaceFirst("\\.[^.]+$", "");
        return base + "-report.json";
    }

    private void setBusy(boolean busy, String message) {
        progressBar.setVisible(busy);
        openFileButton.setEnabled(!busy);
        sampleSelector.setEnabled(!busy);
        settingsButton.setEnabled(!busy);
        exportButton.setEnabled(!busy && currentResult != null);
        setCursor(busy
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());
        operationLabel.setText(message);
    }

    private void showAnalysisError(String message) {
        operationLabel.setText("Analysis failed");
        JOptionPane.showMessageDialog(
                this,
                message,
                "Analysis failed",
                JOptionPane.ERROR_MESSAGE);
    }

    private String formatRange(LogStatistics statistics) {
        LocalDateTime first = statistics.getFirstTimestamp();
        LocalDateTime last = statistics.getLastTimestamp();
        if (first == null || last == null) {
            return "No timestamp range available";
        }
        return first.format(DISPLAY_TIME) + "  –  " + last.format(DISPLAY_TIME);
    }

    private String highestAnomalyLabel(List<Anomaly> anomalies) {
        double highest = anomalies.stream()
                .mapToDouble(Anomaly::getSeverityScore)
                .max()
                .orElse(0.0);
        return anomalies.isEmpty()
                ? "None detected"
                : Math.round(highest * 100) + "% highest score";
    }

    private String humanize(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private Color levelColor(LogLevel level) {
        return switch (level) {
            case FATAL, ERROR -> new Color(220, 38, 38);
            case WARN -> new Color(217, 119, 6);
            case INFO -> new Color(37, 99, 235);
            case DEBUG, TRACE -> new Color(107, 114, 128);
            case UNKNOWN -> new Color(156, 163, 175);
        };
    }

    private static JTextArea createReadOnlyTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(SURFACE);
        textArea.setForeground(TEXT);
        textArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return textArea;
    }

    @FunctionalInterface
    private interface AnalysisTask {
        AnalysisResult run() throws Exception;
    }

    private enum SampleOption {
        DATABASE_INCIDENT(
                "Database outage",
                "critical-incident-database.log",
                "sample-logs/critical-incident-database.log"),
        SECURITY_ATTACK(
                "Security attack",
                "security-attack.log",
                "sample-logs/security-attack.log"),
        OUT_OF_MEMORY(
                "Memory exhaustion",
                "out-of-memory.log",
                "sample-logs/out-of-memory.log"),
        NORMAL_OPERATIONS(
                "Normal operations",
                "normal-operations.log",
                "sample-logs/normal-operations.log");

        private final String displayName;
        private final String fileName;
        private final String resourcePath;

        SampleOption(String displayName, String fileName, String resourcePath) {
            this.displayName = displayName;
            this.fileName = fileName;
            this.resourcePath = resourcePath;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final class MetricCard extends JPanel {

        private static final long serialVersionUID = 1L;

        private final JLabel valueLabel = new JLabel("—");
        private final JLabel detailLabel = new JLabel(" ");

        private MetricCard(String title) {
            super(new GridLayout(3, 1, 0, 2));
            setBackground(SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(MUTED);
            valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 22f));
            valueLabel.setForeground(TEXT);
            detailLabel.setForeground(MUTED);
            detailLabel.setFont(detailLabel.getFont().deriveFont(11f));
            add(titleLabel);
            add(valueLabel);
            add(detailLabel);
        }

        private void setValue(String value, String detail) {
            valueLabel.setText(value);
            detailLabel.setText(detail);
        }
    }

    private static final class LogTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNS = {"Time", "Level", "Logger", "Thread", "Message"};

        private List<LogEntry> entries = new ArrayList<>();

        private void setEntries(List<LogEntry> entries) {
            this.entries = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
            fireTableDataChanged();
        }

        private LogEntry getEntry(int row) {
            return entries.get(row);
        }

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 1 ? LogLevel.class : String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            LogEntry entry = entries.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> entry.getTimestamp() != null
                        ? entry.getTimestamp().format(TABLE_TIME)
                        : "Line " + entry.getLineNumber();
                case 1 -> entry.getLevel();
                case 2 -> entry.getLogger();
                case 3 -> entry.getThread();
                case 4 -> entry.getMessage();
                default -> "";
            };
        }
    }

    private final class LevelCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean selected,
                                                       boolean focused,
                                                       int row,
                                                       int column) {
            Component component = super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            if (!selected && value instanceof LogLevel level) {
                component.setForeground(levelColor(level));
            }
            setFont(getFont().deriveFont(Font.BOLD));
            return component;
        }
    }

    private static final class IncidentCellRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                       Object value,
                                                       int index,
                                                       boolean selected,
                                                       boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, focused);
            if (value instanceof Incident incident) {
                label.setText("[" + incident.getSeverity() + "]  " + incident.getTitle());
                if (!selected) {
                    label.setForeground(switch (incident.getSeverity()) {
                        case CRITICAL -> DANGER;
                        case HIGH -> WARNING;
                        case MEDIUM -> new Color(161, 98, 7);
                        case LOW -> SUCCESS;
                    });
                }
            }
            label.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
            return label;
        }
    }
}
