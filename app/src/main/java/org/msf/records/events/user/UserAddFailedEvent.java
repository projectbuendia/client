package org.msf.records.events.user;

import org.msf.records.net.model.NewUser;

/**
 * An event bus event indicating that a user could not be successfully added.
 */
public class UserAddFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_INVALID_USER = 1;
    public static final int REASON_USER_EXISTS_LOCALLY = 2;
    public static final int REASON_USER_EXISTS_ON_SERVER = 3;
    public static final int REASON_CONNECTION_ERROR = 4;

    public final NewUser user;
    public final int reason;

    public UserAddFailedEvent(NewUser user, int reason) {
        this.user = user;
        this.reason = reason;
    }
}
