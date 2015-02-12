package org.msf.records.events;

import android.support.annotation.Nullable;

/**
 * An event bus event indicating that submitting an Xform failed.
 */
public class SubmitXformFailedEvent {
    public enum Reason {
        UNKNOWN,
        SERVER_AUTH,
        SERVER_BAD_ENDPOINT,
        SERVER_TIMEOUT,
        SERVER_UNKNOWN,
        CLIENT_UNKNOWN
    }

    public final Reason reason;
    @Nullable
    public final Exception exception;

    public SubmitXformFailedEvent(Reason reason, @Nullable Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }

    public SubmitXformFailedEvent(Reason reason) {
        this.reason = reason;
        this.exception = null;
    }
}
