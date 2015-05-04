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

package org.projectbuendia.client.events;

import android.support.annotation.Nullable;

/** An event bus event indicating that fetching an Xform failed. */
public class FetchXformFailedEvent {
    public enum Reason {
        UNKNOWN,
        NO_FORMS_FOUND,
        SERVER_AUTH,
        SERVER_BAD_ENDPOINT,
        SERVER_FAILED_TO_FETCH,
        SERVER_UNKNOWN
    }

    public final Reason reason;
    @Nullable public final Exception exception;

    public FetchXformFailedEvent(Reason reason, @Nullable Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }

    public FetchXformFailedEvent(Reason reason) {
        this.reason = reason;
        this.exception = null;
    }
}
