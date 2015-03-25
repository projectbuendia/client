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

package org.msf.records.events.data;

import org.msf.records.events.DefaultCrudEventBus;

/**
 * An event bus event indicating that updating a patient failed.
 *
 * <p>This event should only be posted on a {@link DefaultCrudEventBus}.
 */
public class PatientUpdateFailedEvent {

    public static final int REASON_INTERRUPTED = 0;
    public static final int REASON_NETWORK = 1;
    public static final int REASON_CLIENT = 2;
    public static final int REASON_SERVER = 3;
    public static final int REASON_NO_SUCH_PATIENT = 4;

    public final int reason;
    public final Exception exception;

    public PatientUpdateFailedEvent(int reason, Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }
}
