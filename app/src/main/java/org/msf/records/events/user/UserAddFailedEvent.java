package org.msf.records.events.user;

import org.msf.records.model.NewUser;
import org.msf.records.model.User;

/**
 * An event bus event indicating that a user could not be successfully added.
 */
public class UserAddFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_INVALID_USER = 1;
    public static final int REASON_USER_EXISTS_LOCALLY = 2;
    public static final int REASON_USER_EXISTS_ON_SERVER = 3;
    public static final int REASON_SERVER_ERROR = 4;

    public final NewUser mUser;
    public final int mReason;

    public UserAddFailedEvent(NewUser user, int reason) {
        mUser = user;
        mReason = reason;
    }

    @Override
    public String toString() {
        // TODO(akalachman): Extract as string resources.
        switch (mReason) {
            case REASON_UNKNOWN:
                return "Unknown error";
            case REASON_INVALID_USER:
                return "Invalid user";
            case REASON_USER_EXISTS_LOCALLY:
                return "User already exists on tablet";
            case REASON_USER_EXISTS_ON_SERVER:
                return "User already exists on server";
            case REASON_SERVER_ERROR:
                return "Unknown error on server";
            default:
                return "Unknown error";
        }
    }
}
