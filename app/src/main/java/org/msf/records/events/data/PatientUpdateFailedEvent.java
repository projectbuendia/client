package org.msf.records.events.data;

import org.msf.records.events.DefaultCrudEventBus;

/**
 * An event bus event indicating that the update of a patient failed.
 *
 * <p>This event should only ever be posted on a {@link DefaultCrudEventBus}.
 */
public class PatientUpdateFailedEvent {

    public static final int REASON_INTERRUPTED = 0;
    public static final int REASON_NETWORK = 1;
    public static final int REASON_CLIENT = 2;
    public static final int REASON_SERVER = 3;
    public static final int REASON_NO_SUCH_PATIENT = 4;

    public final int reason;
    public final Exception exception;

    public PatientUpdateFailedEvent(int reason, Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }
}
