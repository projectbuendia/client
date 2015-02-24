package org.msf.records.ui;

/**
 * Represents the current loading state of the app--getting data locally, syncing from a remote
 * source, or not waiting on data.
 */
public enum LoadingState {
    SYNCING,
    LOADING,
    LOADED
}
