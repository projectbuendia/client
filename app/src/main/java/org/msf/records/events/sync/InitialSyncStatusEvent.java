package org.msf.records.events.sync;

/**
 * A sticky event describing the status of the last initial sync operation. A partially-completed
 * initial sync will typically need to be redone.
 */
public class InitialSyncStatusEvent {
    public enum SyncStatus {
        UNKNOWN,
        REQUESTED,
        STARTED,
        FAILED,
        CANCELED,
        SUCCEEDED
    }

    public SyncStatus status;

    public InitialSyncStatusEvent(SyncStatus status) {
        this.status = status;
    }
}
