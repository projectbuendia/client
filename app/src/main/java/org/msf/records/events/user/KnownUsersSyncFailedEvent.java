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

/**
 * An event bus event indicating that the set of known users failed to be synced from the server.
 */
public class KnownUsersSyncFailedEvent {

    public static final int REASON_UNKNOWN = 0;

    public final int reason;

    public KnownUsersSyncFailedEvent(int reason) {
        this.reason = reason;
    }
}
