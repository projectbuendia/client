package org.msf.records.events.user;

/**
 * An event bus event indicating that a user could not be successfully added.
 */
public class UserAddFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_INVALID_USER = 1;
    public static final int REASON_USER_EXISTS_LOCALLY = 2;
    public static final int REASON_USER_EXISTS_ON_SERVER = 3;
    public static final int REASON_SERVER_ERROR = 4;
}
