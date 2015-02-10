package org.msf.records.events.data;

/**
 * An event bus event indicating that the add of an encounter failed.
 *
 * <p>This event should only ever be posted on a {@link org.msf.records.events.DefaultCrudEventBus}.
 */
public class EncounterAddFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_INTERRUPTED = 1;
    public static final int REASON_NETWORK = 2;
    public static final int REASON_CLIENT = 3;
    public static final int REASON_SERVER = 4;

    public final int reason;
    public final Exception exception;

    public EncounterAddFailedEvent(int reason, Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }
}
