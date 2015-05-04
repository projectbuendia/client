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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.projectbuendia.client.App;
import org.projectbuendia.client.BuildConfig;
import org.projectbuendia.client.utils.LexicographicVersion;
import org.projectbuendia.client.utils.Logger;

/**
 * An object containing information about an update that has been downloaded and is ready to be
 * installed.
 */
public class DownloadedUpdateInfo {

    private static final Logger LOG = Logger.create();

    public final boolean isValid;
    public final LexicographicVersion currentVersion;
    public final LexicographicVersion downloadedVersion;
    public final String path;

    /** Creates an instance of {@link DownloadedUpdateInfo} for an invalid update. */
    public static DownloadedUpdateInfo getInvalid(LexicographicVersion currentVersion) {
        return new DownloadedUpdateInfo(
                false /*isValid*/, currentVersion, UpdateManager.MINIMAL_VERSION, null /*path*/);
    }

    /** Creates an instance of {@link DownloadedUpdateInfo} from a path to an APK on disk. */
    public static DownloadedUpdateInfo fromUri(LexicographicVersion currentVersion, String uri) {
        if (uri == null || uri.equals("") || !uri.startsWith("file://")) {
            LOG.w("URI was not specified or invalid.");
            return getInvalid(currentVersion);
        }

        // Remove the leading "file://".
        String path = uri.substring(7);
        PackageManager packageManager = App.getInstance().getPackageManager();

        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(path, 0 /*flags*/);
        if (packageInfo == null) {
            LOG.w("'%1$s' is not a valid APK.", uri);
            return getInvalid(currentVersion);
        }

        if (!packageInfo.packageName.equals(BuildConfig.APPLICATION_ID)) {
            LOG.w(
                    "'%1$s' does not have the correct package name. Expected: '%2$s'; actual: "
                            + "'%3$s'.",
                    uri,
                    BuildConfig.APPLICATION_ID,
                    packageInfo.packageName);
            return getInvalid(currentVersion);
        }

        LexicographicVersion downloadedVersion;
        try {
            downloadedVersion = LexicographicVersion.parse(packageInfo.versionName);
        } catch (IllegalArgumentException e) {
            LOG.w("%1$s has an invalid version: %2$s.", uri, packageInfo.versionName);
            return getInvalid(currentVersion);
        }

        return new DownloadedUpdateInfo(true /*isValid*/, currentVersion, downloadedVersion, uri);
    }

    private DownloadedUpdateInfo(
            boolean isValid,
            LexicographicVersion currentVersion,
            LexicographicVersion downloadedVersion,
            String uri) {
        this.isValid = isValid;
        this.currentVersion = currentVersion;
        this.downloadedVersion = downloadedVersion;
        this.path = uri;
    }

    /** Converts the info as a string for display. */
    public String toString() {
        return "DownloadedUpdateInfo(isValid=" + isValid + ", "
                + "currentVersion=" + currentVersion + ", "
                + "availableVersion=" + downloadedVersion + ", "
                + "path=" + path + ")";
    }

    public boolean shouldInstall() {
        return isValid && downloadedVersion.greaterThan(currentVersion);
    }
}
