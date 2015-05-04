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

import org.projectbuendia.client.events.DefaultCrudEventBus;

/**
 * An event bus event indicating that adding a patient failed.
 *
 * <p>This event should only be posted on a {@link DefaultCrudEventBus}.
 */
public class PatientAddFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_INTERRUPTED = 1;
    public static final int REASON_NETWORK = 2;
    public static final int REASON_CLIENT = 3;
    public static final int REASON_SERVER = 4;
    public static final int REASON_INVALID_ID = 5;
    public static final int REASON_DUPLICATE_ID = 6;
    public static final int REASON_INVALID_GIVEN_NAME = 7;
    public static final int REASON_INVALID_FAMILY_NAME = 8;


    public final int reason;
    public final Exception exception;

    public PatientAddFailedEvent(int reason, Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }
}
