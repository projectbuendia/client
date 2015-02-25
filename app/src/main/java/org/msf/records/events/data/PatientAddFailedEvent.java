package org.msf.records.events.data;

import org.msf.records.events.DefaultCrudEventBus;

/**
 * An event bus event indicating that the add of a patient failed.
 *
 * <p>This event should only ever be posted on a {@link DefaultCrudEventBus}.
 */
public class PatientAddFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_INTERRUPTED = 1;
    public static final int REASON_NETWORK = 2;
    public static final int REASON_CLIENT = 3;
    public static final int REASON_SERVER = 4;
    public static final int REASON_INVALID_ID = 5;
    public static final int REASON_INVALID_GIVEN_NAME = 6;
    public static final int REASON_INVALID_FAMILY_NAME = 7;


    public final int reason;
    public final Exception exception;

    public PatientAddFailedEvent(int reason, Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }
}
