package org.projectbuendia.client.events.data;

public class ObsDeleteFailedEvent {

    public final Reason reason;
    public final Exception exception;

    public enum Reason {
        UNKNOWN,
        UNKNOWN_SERVER_ERROR,
        CLIENT_ERROR,
        INTERRUPTED,
        TIMEOUT
    }

    public ObsDeleteFailedEvent(Reason reason, Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }
}
