package org.msf.records.events.data;

/**
 * An event bus event indicating that the add of an encounter failed.
 *
 * <p>This event should only ever be posted on a {@link org.msf.records.events.DefaultCrudEventBus}.
 */
public class EncounterAddFailedEvent {
    public enum Reason {
        UNKNOWN,
        UNKNOWN_SERVER_ERROR,
        INTERRUPTED,
        FAILED_TO_VALIDATE,
        FAILED_TO_AUTHENTICATE,
        FAILED_TO_SAVE_ON_SERVER,
        INVALID_NUMBER_OF_OBSERVATIONS_SAVED,
        FAILED_TO_FETCH_SAVED_OBSERVATION
    }

    public final Reason reason;
    public final Exception exception;

    public EncounterAddFailedEvent(Reason reason, Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }
}
