package org.projectbuendia.client.ui.chart;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** A map from division indexes to execution counts, backed by a Multiset. */
public class ExecutionCounter implements Map<Integer, Integer> {
    private Multiset<Integer> indexes;

    public ExecutionCounter() {
        indexes = HashMultiset.create();
    }

    /** Gets the execution count for a given division index.  Returns 0 instead of null. */
    @Override public @NonNull Integer get(@Nullable Object key) {
        return indexes.count(key);
    }

    /** Increments the execution count for a given division index. */
    public void add(int index) {
        indexes.add(index);
    }

    @Override public int size() {
        return indexes.elementSet().size();
    }

    @Override public boolean isEmpty() {
        return indexes.isEmpty();
    }

    @Override public boolean containsKey(@Nullable Object key) {
        return indexes.count(key) > 0;
    }

    @Override @NonNull public Set<Integer> keySet() {
        return indexes.elementSet();
    }

    @Override public void clear() {
        indexes.clear();
    }

    @Override public Integer put(@NonNull Integer key, @NonNull Integer value) {
        throw new UnsupportedOperationException();
    }

    @Override public Integer remove(@Nullable Object key) {
        throw new UnsupportedOperationException();
    }

    @Override public void putAll(@NonNull Map<? extends Integer, ? extends Integer> map) {
        throw new UnsupportedOperationException();
    }

    @Override public boolean containsValue(@Nullable Object value) {
        throw new UnsupportedOperationException();
    }

    @Override public @NonNull Collection<Integer> values() {
        throw new UnsupportedOperationException();
    }

    @Override public @NonNull Set<Entry<Integer, Integer>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
