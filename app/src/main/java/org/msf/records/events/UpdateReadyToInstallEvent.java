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

package org.msf.records.events;

import org.msf.records.updater.DownloadedUpdateInfo;

/**
 * A sticky event that indicates that an update has been downloaded and is ready to be
 * installed.  An update is considered suitable for installation if it has a version number
 * higher than the currently running app and at least as high as the highest version
 * available on the package server.
 */
public class UpdateReadyToInstallEvent {

    public final DownloadedUpdateInfo updateInfo;

    public UpdateReadyToInstallEvent(DownloadedUpdateInfo downloadedUpdateInfo) {
        updateInfo = downloadedUpdateInfo;
    }
}
