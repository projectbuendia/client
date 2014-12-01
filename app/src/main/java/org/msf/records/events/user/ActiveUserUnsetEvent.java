package org.msf.records.events.user;

import org.msf.records.net.model.User;

/**
 * An event bus event that indicates that the active user has been unset.
 */
public class ActiveUserUnsetEvent {

    /**
     * Indicates that the reason is unknown.
     *
     * <p>This value should not generally be used.
     */
    public static final int REASON_UNKNOWN = 0;

    /**
     * Indicates that the active user was unset because the unset method was invoked.
     */
    public static final int REASON_UNSET_INVOKED = 1;

    /**
     * Indicates that the active user was unset because the active user was deleted from the server.
     */
    public static final int REASON_USER_DELETED = 2;

    /**
     * The previous active user. If the reason why the active user was unset is because the user was
     * deleted from the server, this object will no longer be known to
     * {@link org.msf.records.user.UserManager}.
     */
    public final User mPreviousActiveUser;

    /**
     * The reason why the active user was unset.
     */
    public final int mReason;

    /**
     * Creates a new {@link ActiveUserUnsetEvent}.
     *
     * @param previousActiveUser the previous active user
     * @param reason the reason why the active user was unset
     */
    public ActiveUserUnsetEvent(User previousActiveUser, int reason) {
        mPreviousActiveUser = previousActiveUser;
        mReason = reason;
    }
}
