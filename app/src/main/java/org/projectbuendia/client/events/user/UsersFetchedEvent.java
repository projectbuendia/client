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

import com.google.common.collect.ImmutableSet;

import org.projectbuendia.client.json.JsonUser;

/** Indicates that a list of users has been fetched from the server. */
public final class UsersFetchedEvent {
    public final String server;
    public final String username;
    public final String password;
    public final ImmutableSet<JsonUser> users;

    public UsersFetchedEvent(String server, String username, String password, ImmutableSet<JsonUser> users) {
        this.server = server;
        this.username = username;
        this.password = password;
        this.users = users;
    }
}
