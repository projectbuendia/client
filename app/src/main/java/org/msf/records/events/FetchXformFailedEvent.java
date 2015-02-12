package org.msf.records.events;

import android.support.annotation.Nullable;

/**
 * An event bus event indicating that fetching an Xform failed.
 */
public class FetchXformFailedEvent {
    public enum Reason {
        UNKNOWN,
        NO_FORMS_FOUND,
        SERVER_AUTH,
        SERVER_BAD_ENDPOINT,
        SERVER_FAILED_TO_FETCH,
        SERVER_UNKNOWN
    }

    public final Reason reason;
    @Nullable public final Exception exception;

    public FetchXformFailedEvent(Reason reason, @Nullable Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }

    public FetchXformFailedEvent(Reason reason) {
        this.reason = reason;
        this.exception = null;
    }
}
