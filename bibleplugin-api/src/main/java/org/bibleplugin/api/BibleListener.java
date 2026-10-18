package org.bibleplugin.api;

/**
 * Observer for changes to the set of loaded Bibles. Both methods have empty defaults, so an
 * implementation only overrides what it needs.
 */
public interface BibleListener {

    default void onBibleLoaded(String bibleId) {
    }

    default void onBibleUnloaded(String bibleId) {
    }
}
