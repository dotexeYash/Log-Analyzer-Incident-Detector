package com.loganalyzer.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A generic data structure for tracking, counting, and ranking item frequencies.
 * Demonstrates Java Generics using type parameter {@code <T>}.
 *
 * @param <T> the type of elements being tracked
 */
public class FrequencyDistribution<T> {

    private final Map<T, Long> counts;
    private long totalCount;

    /**
     * Constructs an empty FrequencyDistribution with insertion-order preservation for tie breaking.
     */
    public FrequencyDistribution() {
        this.counts = new LinkedHashMap<>();
        this.totalCount = 0;
    }

    /**
     * Increments the frequency count for the given item by 1.
     * Safely ignores null items.
     *
     * @param item the element to record
     */
    public void add(T item) {
        add(item, 1L);
    }

    /**
     * Increments the frequency count for the given item by the specified amount.
     * Safely ignores null items and non-positive amounts (amount <= 0).
     *
     * @param item the element to record
     * @param amount the number of occurrences to add
     */
    public void add(T item, long amount) {
        if (item == null || amount <= 0) {
            return;
        }
        counts.put(item, counts.getOrDefault(item, 0L) + amount);
        totalCount += amount;
    }

    /**
     * Returns the recorded count for a given item, or 0 if not present.
     *
     * @param item the element to check
     * @return the count of occurrences
     */
    public long getCount(T item) {
        if (item == null) {
            return 0L;
        }
        return counts.getOrDefault(item, 0L);
    }

    /**
     * Returns the total sum of all item occurrences recorded.
     *
     * @return total count across all items
     */
    public long getTotalCount() {
        return totalCount;
    }

    /**
     * Returns the number of distinct elements tracked.
     *
     * @return number of unique items
     */
    public int getDistinctCount() {
        return counts.size();
    }

    /**
     * Returns a map of the top N elements ordered by frequency descending.
     * If multiple elements have identical frequency, insertion order is preserved.
     *
     * @param limit maximum number of top items to return
     * @return ordered map of top entries, or an empty map if limit is non-positive
     */
    public Map<T, Long> getTop(int limit) {
        if (limit <= 0) {
            return Collections.emptyMap();
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<T, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Returns an unmodifiable view of all frequency mappings.
     *
     * @return unmodifiable map of items and their counts
     */
    public Map<T, Long> getCounts() {
        return Collections.unmodifiableMap(counts);
    }
}
