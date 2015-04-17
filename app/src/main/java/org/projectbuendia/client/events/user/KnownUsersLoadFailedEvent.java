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

/**
 * An event bus event indicating that the set of known users failed to be loaded from local cache.
 */
public final class KnownUsersLoadFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_NO_USERS_RETURNED = 1;
    public static final int REASON_CANCELLED = 2;

    public final int reason;

    public KnownUsersLoadFailedEvent(int reason) {
        this.reason = reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KnownUsersLoadFailedEvent)) {
            return false;
        }

        KnownUsersLoadFailedEvent other = (KnownUsersLoadFailedEvent) obj;
        return other.reason == reason;
    }

    @Override
    public int hashCode() {
        return reason;
    }

    @Override
    public String toString() {
        return KnownUsersLoadFailedEvent.class.getSimpleName() + "(" + reason + ")";
    }
}
