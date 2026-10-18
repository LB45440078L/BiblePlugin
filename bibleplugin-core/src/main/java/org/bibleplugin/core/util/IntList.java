package org.bibleplugin.core.util;

import java.util.Arrays;

/** Minimal growable primitive int list, avoids boxing when building the search index. */
public final class IntList {

    private int[] values;
    private int size;

    public IntList() {
        this(16);
    }

    public IntList(int initialCapacity) {
        this.values = new int[Math.max(1, initialCapacity)];
    }

    public void add(int value) {
        if (size == values.length) {
            values = Arrays.copyOf(values, values.length << 1);
        }
        values[size++] = value;
    }

    public int get(int index) {
        return values[index];
    }

    public int lastOrDefault(int fallback) {
        return size == 0 ? fallback : values[size - 1];
    }

    public int size() {
        return size;
    }

    /** @return a right-sized copy of the contents. */
    public int[] toArray() {
        return Arrays.copyOf(values, size);
    }
}
