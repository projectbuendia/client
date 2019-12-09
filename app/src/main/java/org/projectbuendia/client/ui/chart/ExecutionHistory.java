package org.projectbuendia.client.ui.chart;

import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.projectbuendia.models.Order;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A sparse infinite list that maps division indexes to the execution times
 * contained in each division.  To optimize for use in CSS in the chart HTML, each
 * execution time is represented as an integer percentage of the division interval.
 */
public class ExecutionHistory extends AbstractList<List<Integer>> {
    private final Order order;
    private final SparseArray<List<Integer>> timesByIndex = new SparseArray<>();

    private static final List<Integer> EMPTY_LIST = ImmutableList.of();

    /** Constructs a read-only, empty execution history. */
    public ExecutionHistory() {
        this.order = null;
    }

    /** Constructs an appendable execution history. */
    public ExecutionHistory(Order order) {
        this.order = order;
    }

    /** Adds an execution time to the set of times for this order. */
    public void add(DateTime time) {
        if (order == null) {
            throw new UnsupportedOperationException();
        }
        int index = order.getDivisionIndex(time);
        if (timesByIndex.get(index) == null) {
            timesByIndex.put(index, new ArrayList<>());
        }
        Interval division = order.getDivision(index);
        long startMillis = division.getStartMillis();
        long durationMillis = division.toDurationMillis();
        int percentage = (int) (100 * (time.getMillis() - startMillis) / durationMillis);
        timesByIndex.get(index).add(percentage);
    }

    /** Sorts all the lists of execution times within each division. */
    public void sort() {
        for (int i = 0; i < timesByIndex.size(); i++) {
            Collections.sort(timesByIndex.valueAt(i));
        }
    }

    /** Gets the list of execution times for a given division index. */
    @Override public @NonNull List<Integer> get(int index) {
        return timesByIndex.get(index, EMPTY_LIST);
    }

    /** Returns MAX_VALUE to avoid ever throwing IndexOutOfBoundsException. */
    @Override public int size() {
        return Integer.MAX_VALUE;
    }
}
