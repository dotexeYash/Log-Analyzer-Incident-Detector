package com.loganalyzer.analyzer;

import com.loganalyzer.parser.StandardLogParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogAnalyzerTest {

    @Test
    @DisplayName("Phase 3 Task 5: Default LogAnalyzer constructor shares StatisticsAggregator with AnomalyDetector")
    void testDefaultConstructorSharesStatisticsAggregator() {
        LogAnalyzer analyzer = new LogAnalyzer();

        assertNotNull(analyzer.getParser());
        assertNotNull(analyzer.getStatisticsAggregator());
        assertNotNull(analyzer.getAnomalyDetector());
        assertNotNull(analyzer.getIncidentDetector());

        assertSame(analyzer.getStatisticsAggregator(),
                analyzer.getAnomalyDetector().getStatisticsAggregator(),
                "Default constructor must share the exact same StatisticsAggregator instance");
    }

    @Test
    @DisplayName("Phase 3 Task 5: Custom StatisticsAggregator constructor injects and shares aggregator")
    void testCustomStatisticsAggregatorConstructorInjection() {
        StatisticsAggregator custom = new StatisticsAggregator();
        LogAnalyzer analyzer = new LogAnalyzer(custom);

        assertSame(custom, analyzer.getStatisticsAggregator());
        assertSame(custom, analyzer.getAnomalyDetector().getStatisticsAggregator(),
                "Custom StatisticsAggregator must be shared with AnomalyDetector");
    }

    @Test
    @DisplayName("Phase 3 Task 5: Null StatisticsAggregator safely falls back to a shared default instance")
    void testNullStatisticsAggregatorFallback() {
        LogAnalyzer analyzer = new LogAnalyzer((StatisticsAggregator) null);

        assertNotNull(analyzer.getStatisticsAggregator());
        assertNotNull(analyzer.getAnomalyDetector());
        assertSame(analyzer.getStatisticsAggregator(),
                analyzer.getAnomalyDetector().getStatisticsAggregator(),
                "Null StatisticsAggregator must fall back to a single shared instance");
    }

    @Test
    @DisplayName("Phase 3 Task 5: AnomalyDetector constructor shares its StatisticsAggregator with LogAnalyzer")
    void testAnomalyDetectorConstructorInjection() {
        StatisticsAggregator customAgg = new StatisticsAggregator();
        AnomalyDetector customDetector = new AnomalyDetector(customAgg);
        LogAnalyzer analyzer = new LogAnalyzer(customDetector);

        assertSame(customDetector, analyzer.getAnomalyDetector());
        assertSame(customAgg, analyzer.getStatisticsAggregator());
        assertSame(analyzer.getStatisticsAggregator(), analyzer.getAnomalyDetector().getStatisticsAggregator());
    }

    @Test
    @DisplayName("Phase 3 Task 5: Null AnomalyDetector safely falls back to a single shared StatisticsAggregator")
    void testNullAnomalyDetectorFallbackSharesStatisticsAggregator() {
        LogAnalyzer analyzer = new LogAnalyzer((AnomalyDetector) null);

        assertNotNull(analyzer.getParser());
        assertNotNull(analyzer.getStatisticsAggregator());
        assertNotNull(analyzer.getAnomalyDetector());
        assertNotNull(analyzer.getIncidentDetector());

        assertSame(analyzer.getStatisticsAggregator(),
                analyzer.getAnomalyDetector().getStatisticsAggregator(),
                "Null AnomalyDetector must fall back to a single shared StatisticsAggregator instance");
    }

    @Test
    @DisplayName("Phase 3 Task 5: Package-private 3-argument constructor chains shared aggregator from detector")
    void testPackagePrivateThreeArgumentConstructor() {
        StandardLogParser parser = new StandardLogParser();
        StatisticsAggregator agg = new StatisticsAggregator();
        AnomalyDetector detector = new AnomalyDetector(agg);
        IncidentDetector incidentDetector = new IncidentDetector();

        LogAnalyzer analyzer = new LogAnalyzer(parser, detector, incidentDetector);

        assertSame(parser, analyzer.getParser());
        assertSame(detector, analyzer.getAnomalyDetector());
        assertSame(agg, analyzer.getStatisticsAggregator());
        assertSame(incidentDetector, analyzer.getIncidentDetector());
        assertSame(analyzer.getStatisticsAggregator(), analyzer.getAnomalyDetector().getStatisticsAggregator());

        assertThrows(NullPointerException.class, () -> new LogAnalyzer(parser, null, incidentDetector));
    }

    @Test
    @DisplayName("Phase 3 Task 5: 4-argument constructor preserves full dependency injection and null safety")
    void testFullConstructorDependencyInjection() {
        StandardLogParser parser = new StandardLogParser();
        StatisticsAggregator agg = new StatisticsAggregator();
        AnomalyDetector detector = new AnomalyDetector(agg);
        IncidentDetector incidentDetector = new IncidentDetector();

        LogAnalyzer analyzer = new LogAnalyzer(parser, agg, detector, incidentDetector);

        assertSame(parser, analyzer.getParser());
        assertSame(agg, analyzer.getStatisticsAggregator());
        assertSame(detector, analyzer.getAnomalyDetector());
        assertSame(incidentDetector, analyzer.getIncidentDetector());

        // Null argument rejection in 4-arg constructor
        assertThrows(NullPointerException.class, () -> new LogAnalyzer(null, agg, detector, incidentDetector));
        assertThrows(NullPointerException.class, () -> new LogAnalyzer(parser, null, detector, incidentDetector));
        assertThrows(NullPointerException.class, () -> new LogAnalyzer(parser, agg, null, incidentDetector));
        assertThrows(NullPointerException.class, () -> new LogAnalyzer(parser, agg, detector, null));
    }
}
