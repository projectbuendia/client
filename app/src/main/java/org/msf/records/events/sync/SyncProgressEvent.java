package org.msf.records.events.sync;

import android.support.annotation.Nullable;

/**
 * An event bus event giving details on latest sync progress.
 */
public class SyncProgressEvent {
    public int progress;
    @Nullable public String label;

    public SyncProgressEvent(int progress) {
        this.progress = progress;
        this.label = null;
    }

    public SyncProgressEvent(int progress, String label) {
        this.progress = progress;
        this.label = label;
    }
}
