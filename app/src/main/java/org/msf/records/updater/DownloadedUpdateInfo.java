package org.msf.records.updater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.msf.records.App;
import org.msf.records.BuildConfig;
import org.msf.records.utils.LexicographicVersion;
import org.msf.records.utils.Logger;

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

    /**
     * Creates an instance of {@link DownloadedUpdateInfo} for an invalid update.
     */
    public static DownloadedUpdateInfo getInvalid(LexicographicVersion currentVersion) {
        return new DownloadedUpdateInfo(
                false /*isValid*/, currentVersion, UpdateManager.MINIMAL_VERSION, null /*path*/);
    }

    /**
     * Creates an instance of {@link DownloadedUpdateInfo} from a path to an APK on disk.
     */
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

    public boolean shouldInstall() {
        return isValid && downloadedVersion.greaterThan(currentVersion);
    }
}
