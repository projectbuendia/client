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

package org.msf.records.diagnostics;

/**
 * An issue that can be reported by {@link HealthCheck}s.
 *
 * <p>A {@link HealthIssue} is an ongoing issue with the application (rather than a one-time
 * problematic event): when a new issue is discovered, it will be reported to an instance of
 * {@link HealthMonitor}, which will consider that issue active until the issue is resolved.
 */
enum HealthIssue {

    WIFI_DISABLED,

    WIFI_NOT_CONNECTED,

    SERVER_AUTHENTICATION_ISSUE,

    SERVER_CONFIGURATION_INVALID,

    SERVER_HOST_UNREACHABLE,

    SERVER_INTERNAL_ISSUE,

    SERVER_NOT_RESPONDING,

    UPDATE_SERVER_HOST_UNREACHABLE,

    UPDATE_SERVER_INDEX_NOT_FOUND;

    /**
     * The event to be posted when a health issue is discovered.
     */
    public final DiscoveredEvent discovered = new DiscoveredEvent();

    /**
     * The event to be posted when a health issue is resolved.
     */
    public final ResolvedEvent resolved = new ResolvedEvent();

    class Event {

        public HealthIssue getIssue() {
            return HealthIssue.this;
        }

        private Event() {}
    }

    class DiscoveredEvent extends Event {}

    class ResolvedEvent extends Event {}
}
