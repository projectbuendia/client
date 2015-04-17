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

import org.projectbuendia.client.updater.AvailableUpdateInfo;

/**
 * A sticky event indicating that an .apk update is available on the server
 * with a version number higher than the currently running version of the app.
 */
public final class UpdateAvailableEvent {

    public final AvailableUpdateInfo updateInfo;

    public UpdateAvailableEvent(AvailableUpdateInfo updateInfo) {
        this.updateInfo = updateInfo;
    }
}
