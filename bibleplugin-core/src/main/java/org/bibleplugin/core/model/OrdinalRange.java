package org.bibleplugin.core.model;

/**
 * A half-open range of global verse ordinals.
 *
 * @param from first ordinal, inclusive
 * @param to   last ordinal, exclusive
 */
public record OrdinalRange(int from, int to) {

    public OrdinalRange {
        if (to < from) {
            throw new IllegalArgumentException("range end precedes start: " + from + " > " + to);
        }
    }

    public int size() {
        return to - from;
    }
}
