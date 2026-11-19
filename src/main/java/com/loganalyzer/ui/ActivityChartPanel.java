package com.loganalyzer.ui;

import com.loganalyzer.model.LogEntry;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Small dependency-free chart showing total and error events over time.
 */
@SuppressWarnings("serial")
final class ActivityChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int BUCKET_COUNT = 12;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Color GRID_COLOR = new Color(226, 230, 235);
    private static final Color BAR_COLOR = new Color(219, 234, 254);
    private static final Color BAR_BORDER_COLOR = new Color(147, 197, 253);
    private static final Color ERROR_COLOR = new Color(220, 38, 38);
    private static final Color TEXT_COLOR = new Color(100, 108, 120);

    private List<LogEntry> entries = List.of();

    ActivityChartPanel() {
        setBackground(Color.WHITE);
    }

    void setEntries(List<LogEntry> entries) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            List<LogEntry> timestamped = entries.stream()
                    .filter(entry -> entry.getTimestamp() != null)
                    .sorted(Comparator.comparing(LogEntry::getTimestamp))
                    .toList();

            if (timestamped.isEmpty()) {
                drawEmptyState(g);
                return;
            }

            int left = 48;
            int right = 20;
            int top = 22;
            int bottom = 34;
            int chartWidth = Math.max(1, getWidth() - left - right);
            int chartHeight = Math.max(1, getHeight() - top - bottom);

            Bucket[] buckets = createBuckets(timestamped);
            int maximum = 1;
            for (Bucket bucket : buckets) {
                maximum = Math.max(maximum, bucket.total);
            }
            maximum = niceMaximum(maximum);

            drawGrid(g, left, top, chartWidth, chartHeight, maximum);
            drawBarsAndErrors(g, buckets, left, top, chartWidth, chartHeight, maximum);
            drawTimeLabels(g, timestamped, left, top + chartHeight, chartWidth);
        } finally {
            g.dispose();
        }
    }

    private Bucket[] createBuckets(List<LogEntry> timestamped) {
        Bucket[] buckets = new Bucket[BUCKET_COUNT];
        for (int i = 0; i < BUCKET_COUNT; i++) {
            buckets[i] = new Bucket();
        }

        LocalDateTime first = timestamped.getFirst().getTimestamp();
        LocalDateTime last = timestamped.getLast().getTimestamp();
        long firstNanos = toNanos(first);
        long lastNanos = toNanos(last);
        long span = Math.max(1L, lastNanos - firstNanos);

        for (int index = 0; index < timestamped.size(); index++) {
            LogEntry entry = timestamped.get(index);
            int bucketIndex;
            if (lastNanos == firstNanos) {
                bucketIndex = Math.min(BUCKET_COUNT - 1,
                        (int) (((long) index * BUCKET_COUNT) / timestamped.size()));
            } else {
                long offset = Math.max(0L, toNanos(entry.getTimestamp()) - firstNanos);
                bucketIndex = Math.min(BUCKET_COUNT - 1,
                        (int) ((offset / (double) span) * BUCKET_COUNT));
            }
            buckets[bucketIndex].total++;
            if (entry.getLevel().isError()) {
                buckets[bucketIndex].errors++;
            }
        }
        return buckets;
    }

    private void drawGrid(Graphics2D g, int left, int top, int width, int height, int maximum) {
        FontMetrics metrics = g.getFontMetrics();
        g.setStroke(new BasicStroke(1f));
        for (int line = 0; line <= 4; line++) {
            int y = top + (height * line) / 4;
            g.setColor(GRID_COLOR);
            g.drawLine(left, y, left + width, y);
            String label = Integer.toString(Math.round(maximum * (1f - line / 4f)));
            g.setColor(TEXT_COLOR);
            g.drawString(label, left - metrics.stringWidth(label) - 9, y + metrics.getAscent() / 2 - 2);
        }
    }

    private void drawBarsAndErrors(Graphics2D g,
                                   Bucket[] buckets,
                                   int left,
                                   int top,
                                   int width,
                                   int height,
                                   int maximum) {
        double step = width / (double) BUCKET_COUNT;
        int barWidth = Math.max(6, Math.min(28, (int) (step * 0.55)));
        List<int[]> errorPoints = new ArrayList<>(BUCKET_COUNT);

        for (int index = 0; index < buckets.length; index++) {
            Bucket bucket = buckets[index];
            int centerX = left + (int) (step * index + step / 2.0);
            int barHeight = Math.round((bucket.total / (float) maximum) * height);
            int barY = top + height - barHeight;

            g.setColor(BAR_COLOR);
            g.fillRect(centerX - barWidth / 2, barY, barWidth, Math.max(1, barHeight));
            g.setColor(BAR_BORDER_COLOR);
            g.drawRect(centerX - barWidth / 2, barY, barWidth, Math.max(1, barHeight));

            int errorY = top + height - Math.round((bucket.errors / (float) maximum) * height);
            errorPoints.add(new int[]{centerX, errorY});
        }

        g.setColor(ERROR_COLOR);
        g.setStroke(new BasicStroke(2f));
        for (int index = 1; index < errorPoints.size(); index++) {
            int[] previous = errorPoints.get(index - 1);
            int[] current = errorPoints.get(index);
            g.drawLine(previous[0], previous[1], current[0], current[1]);
        }
        for (int index = 0; index < errorPoints.size(); index++) {
            if (buckets[index].errors > 0) {
                int[] point = errorPoints.get(index);
                g.fillOval(point[0] - 3, point[1] - 3, 6, 6);
            }
        }
    }

    private void drawTimeLabels(Graphics2D g,
                                List<LogEntry> timestamped,
                                int left,
                                int baseline,
                                int width) {
        String first = timestamped.getFirst().getTimestamp().format(TIME_FORMAT);
        String last = timestamped.getLast().getTimestamp().format(TIME_FORMAT);
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(TEXT_COLOR);
        g.drawString(first, left, baseline + 23);
        g.drawString(last, left + width - metrics.stringWidth(last), baseline + 23);
    }

    private void drawEmptyState(Graphics2D g) {
        String message = entries.isEmpty()
                ? "No log entries to display"
                : "No timestamped entries to display";
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(TEXT_COLOR);
        g.drawString(message,
                Math.max(12, (getWidth() - metrics.stringWidth(message)) / 2),
                Math.max(24, getHeight() / 2));
    }

    private int niceMaximum(int value) {
        if (value <= 4) {
            return 4;
        }
        int magnitude = (int) Math.pow(10, (int) Math.floor(Math.log10(value)));
        return ((value + magnitude - 1) / magnitude) * magnitude;
    }

    private long toNanos(LocalDateTime value) {
        return value.toLocalTime().toNanoOfDay()
                + value.toLocalDate().toEpochDay() * 86_400_000_000_000L;
    }

    private static final class Bucket {
        private int total;
        private int errors;
    }
}
