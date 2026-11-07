package com.loganalyzer.util;

import com.loganalyzer.model.LogLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FrequencyDistributionTest {

    @Test
    @DisplayName("Should track string occurrences and report accurate counts")
    void testStringFrequencyTracking() {
        FrequencyDistribution<String> dist = new FrequencyDistribution<>();

        dist.add("NullPointerException");
        dist.add("NullPointerException");
        dist.add("SQLException");

        assertEquals(2L, dist.getCount("NullPointerException"));
        assertEquals(1L, dist.getCount("SQLException"));
        assertEquals(0L, dist.getCount("TimeoutException"));
        assertEquals(3L, dist.getTotalCount());
        assertEquals(2, dist.getDistinctCount());
    }

    @Test
    @DisplayName("Should work with enum types (LogLevel) demonstrating generic type flexibility")
    void testEnumGenericTracking() {
        FrequencyDistribution<LogLevel> dist = new FrequencyDistribution<>();

        dist.add(LogLevel.ERROR);
        dist.add(LogLevel.ERROR);
        dist.add(LogLevel.WARN);
        dist.add(LogLevel.INFO, 5L);

        assertEquals(2L, dist.getCount(LogLevel.ERROR));
        assertEquals(1L, dist.getCount(LogLevel.WARN));
        assertEquals(5L, dist.getCount(LogLevel.INFO));
        assertEquals(0L, dist.getCount(LogLevel.DEBUG));
        assertEquals(8L, dist.getTotalCount());
        assertEquals(3, dist.getDistinctCount());
    }

    @Test
    @DisplayName("Should add amounts safely and ignore non-positive amounts")
    void testAddWithAmount() {
        FrequencyDistribution<String> dist = new FrequencyDistribution<>();

        dist.add("itemA", 10L);
        dist.add("itemA", 0L);   // Non-positive: ignored
        dist.add("itemA", -5L);  // Negative: ignored

        assertEquals(10L, dist.getCount("itemA"));
        assertEquals(10L, dist.getTotalCount());
    }

    @Test
    @DisplayName("Should safely handle null items without throwing exceptions")
    void testNullItemSafety() {
        FrequencyDistribution<String> dist = new FrequencyDistribution<>();

        assertDoesNotThrow(() -> {
            dist.add(null);
            dist.add(null, 5L);
            assertEquals(0L, dist.getCount(null));
        });

        assertEquals(0L, dist.getTotalCount());
        assertEquals(0, dist.getDistinctCount());
    }

    @Test
    @DisplayName("Should sort getTop() strictly in descending order of frequency")
    void testGetTopSorting() {
        FrequencyDistribution<String> dist = new FrequencyDistribution<>();

        dist.add("Low", 2L);
        dist.add("High", 10L);
        dist.add("Medium", 5L);

        Map<String, Long> top = dist.getTop(3);
        List<String> keys = new ArrayList<>(top.keySet());

        assertEquals(List.of("High", "Medium", "Low"), keys);
        assertEquals(10L, top.get("High"));
        assertEquals(5L, top.get("Medium"));
        assertEquals(2L, top.get("Low"));
    }

    @Test
    @DisplayName("Should break ties deterministically by preserving insertion order")
    void testTieBreakingDeterminism() {
        FrequencyDistribution<String> dist = new FrequencyDistribution<>();

        // Insert Alpha first, Beta second, Gamma third — all with count 5
        dist.add("Alpha", 5L);
        dist.add("Beta", 5L);
        dist.add("Gamma", 5L);

        Map<String, Long> top = dist.getTop(3);
        List<String> keys = new ArrayList<>(top.keySet());

        assertEquals(List.of("Alpha", "Beta", "Gamma"), keys);
    }

    @Test
    @DisplayName("Should handle limit edge cases: non-positive limit and limit exceeding size")
    void testLimitEdgeCases() {
        FrequencyDistribution<String> dist = new FrequencyDistribution<>();
        dist.add("A", 1L);
        dist.add("B", 2L);

        // Non-positive limits
        assertTrue(dist.getTop(0).isEmpty());
        assertTrue(dist.getTop(-5).isEmpty());

        // Limit exceeding distinct count
        Map<String, Long> top = dist.getTop(100);
        assertEquals(2, top.size());
        assertEquals(List.of("B", "A"), new ArrayList<>(top.keySet()));
    }

    @Test
    @DisplayName("Should return an unmodifiable map from getCounts()")
    void testGetCountsUnmodifiable() {
        FrequencyDistribution<String> dist = new FrequencyDistribution<>();
        dist.add("A");

        Map<String, Long> counts = dist.getCounts();
        assertEquals(1L, counts.get("A"));
        assertThrows(UnsupportedOperationException.class, () -> counts.put("B", 2L));
    }

    @Test
    @DisplayName("Empty distribution should behave gracefully")
    void testEmptyDistribution() {
        FrequencyDistribution<String> dist = new FrequencyDistribution<>();

        assertEquals(0L, dist.getTotalCount());
        assertEquals(0, dist.getDistinctCount());
        assertTrue(dist.getTop(5).isEmpty());
        assertTrue(dist.getCounts().isEmpty());
    }
}
