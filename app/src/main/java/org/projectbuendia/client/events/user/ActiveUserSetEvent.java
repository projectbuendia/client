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

import org.projectbuendia.client.net.model.User;

/** An event bus event that indicates that the active user has been set. */
public class ActiveUserSetEvent {

    /** The previous active user. */
    public final User previousActiveUser;

    /** The current active user. */
    public final User activeUser;

    /** Creates a new {@link ActiveUserSetEvent}. */
    public ActiveUserSetEvent(User previousActiveUser, User activeUser) {
        this.previousActiveUser = previousActiveUser;
        this.activeUser = activeUser;
    }
}
