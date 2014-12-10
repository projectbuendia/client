package org.msf.records.events.user;

import org.msf.records.net.model.User;

/**
 * An event bus event indicating that a user could not be successfully deleted.
 */
public class UserDeleteFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_INVALID_USER = 1;
    public static final int REASON_USER_DOES_NOT_EXIST_LOCALLY = 2;
    public static final int REASON_USER_DOES_NOT_EXIST_ON_SERVER = 3;
    public static final int REASON_SERVER_ERROR = 4;

    public final User user;
    public final int reason;

    public UserDeleteFailedEvent(User user, int reason) {
        this.user = user;
        this.reason = reason;
    }
}
