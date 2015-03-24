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

package org.msf.records.events.user;

import com.google.common.collect.ImmutableSet;

import org.msf.records.net.model.User;

/**
 * An event bus event indicating that the set of known users has changed.
 */
public class KnownUsersChangedEvent {

    public final ImmutableSet<User> addedUsers;
    public final ImmutableSet<User> deletedUsers;

    public KnownUsersChangedEvent(ImmutableSet<User> addedUsers, ImmutableSet<User> deletedUsers) {
        this.addedUsers = addedUsers;
        this.deletedUsers = deletedUsers;
    }
}
