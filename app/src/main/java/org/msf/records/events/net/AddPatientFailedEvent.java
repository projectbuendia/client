package org.msf.records.events.net;

/**
 * An event bus event indicating that adding a patient has failed.
 */
public class AddPatientFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_INVALID_ID = 1;
    public static final int REASON_CONFLICTING_ID = 2;
}
