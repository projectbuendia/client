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

package org.projectbuendia.client.updater;

import android.net.Uri;

import org.projectbuendia.client.model.UpdateInfo;
import org.projectbuendia.client.utils.LexicographicVersion;
import org.projectbuendia.client.utils.Logger;

import java.util.List;

/** An object containing information about an available application update. */
public class AvailableUpdateInfo {

    private static final Logger LOG = Logger.create();

    public final boolean isValid;
    public final LexicographicVersion currentVersion;
    public final LexicographicVersion availableVersion;
    public final Uri updateUri;

    /** Creates an instance of {@link AvailableUpdateInfo} for an invalid update. */
    public static AvailableUpdateInfo getInvalid(LexicographicVersion currentVersion) {
        return new AvailableUpdateInfo(
                false /*isValid*/,
                currentVersion,
                UpdateManager.MINIMAL_VERSION,
                null /*updateUri*/);
    }

    /** Converts the info as a string for display. */
    public String toString() {
        return "AvailableUpdateInfo(isValid=" + isValid + ", "
                + "currentVersion=" + currentVersion + ", "
                + "availableVersion=" + availableVersion + ", "
                + "updateUri=" + updateUri + ")";
    }

    /** Creates an instance of {@link AvailableUpdateInfo} from a server response. */
    public static AvailableUpdateInfo fromResponse(
            LexicographicVersion currentVersion, List<UpdateInfo> response) {
        if (response == null) {
            LOG.w("The update info response is null.");
            return getInvalid(currentVersion);
        }

        if (response.size() == 0) {
            LOG.w("No versions found in the update info.");
            return getInvalid(currentVersion);
        }

        // The package server is responsible for sorting the index in
        // order by increasing version number, so the last is the highest.
        UpdateInfo latestUpdateInfo = response.get(response.size() - 1);

        LexicographicVersion version = latestUpdateInfo.getParsedVersion();
        if (version == null) {
            LOG.w("Invalid version in 'version' field: " + latestUpdateInfo.version);
            return getInvalid(currentVersion);
        }

        Uri updateUri;
        try {
            updateUri = Uri.parse(latestUpdateInfo.url);
        } catch (IllegalArgumentException e) {
            LOG.w(e, "Invalid URL in 'url' field: " + latestUpdateInfo.url);
            return getInvalid(currentVersion);
        }

        return new AvailableUpdateInfo(true /*isValid*/, currentVersion, version, updateUri);
    }

    private AvailableUpdateInfo(
            boolean isValid,
            LexicographicVersion currentVersion,
            LexicographicVersion availableVersion,
            Uri updateUri) {
        this.isValid = isValid;
        this.currentVersion = currentVersion;
        this.availableVersion = availableVersion;
        this.updateUri = updateUri;
    }

    /** Returns true if this is a valid update with a higher version number. */
    public boolean shouldUpdate() {
        return isValid && availableVersion.greaterThan(currentVersion);
    }
}
