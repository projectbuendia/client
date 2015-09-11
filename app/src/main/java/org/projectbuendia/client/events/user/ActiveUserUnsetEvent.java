// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.events.user;

import org.projectbuendia.client.net.json.JsonUser;

/** An event bus event that indicates that the active user has been unset. */
public class ActiveUserUnsetEvent {

    /**
     * Indicates that the reason is unknown.
     *
     * <p>This value should not generally be used.
     */
    public static final int REASON_UNKNOWN = 0;

    /** Indicates that the active user was unset because the unset method was invoked. */
    public static final int REASON_UNSET_INVOKED = 1;

    /**
     * Indicates that the active user was unset because the active user was deleted from the server.
     */
    public static final int REASON_USER_DELETED = 2;

    /**
     * The previous active user. If the reason why the active user was unset is because the user was
     * deleted from the server, this object will no longer be known to
     * {@link org.projectbuendia.client.user.UserManager}.
     */
    public final JsonUser previousActiveUser;

    /** The reason why the active user was unset. */
    public final int reason;

    /**
     * Creates a new {@link ActiveUserUnsetEvent}.
     *
     * @param previousActiveUser the previous active user
     * @param reason the reason why the active user was unset
     */
    public ActiveUserUnsetEvent(JsonUser previousActiveUser, int reason) {
        this.previousActiveUser = previousActiveUser;
        this.reason = reason;
    }
}
