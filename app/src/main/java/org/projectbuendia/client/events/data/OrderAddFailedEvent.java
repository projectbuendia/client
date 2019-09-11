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

package org.projectbuendia.client.events.data;

/** Indicates that an attempt to submit a new order to the server failed. */
public class OrderAddFailedEvent {
    public final Reason reason;
    public final Exception exception;

    public enum Reason {
        UNKNOWN,
        UNKNOWN_SERVER_ERROR,
        CLIENT_ERROR,
        INTERRUPTED
    }

    public OrderAddFailedEvent(Reason reason, Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }
}