package org.msf.records.updater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;

import org.msf.records.App;
import org.msf.records.utils.Logger;

/**
 * An object containing information about an update that has been downloaded and is ready to be
 * installed.
 */
public class DownloadedUpdateInfo {

    private static final Logger LOG = Logger.create();

    public final boolean isValid;
    public final Version currentVersion;
    public final Version downloadedVersion;
    public final String path;

    /**
     * Creates an instance of {@link DownloadedUpdateInfo} for an invalid update.
     */
    public static DownloadedUpdateInfo getInvalid(Version currentVersion) {
        return new DownloadedUpdateInfo(
                false /*isValid*/, currentVersion, UpdateManager.INVALID_VERSION, null /*path*/);
    }

    /**
     * Creates an instance of {@link DownloadedUpdateInfo} from a path to an APK on disk.
     */
    public static DownloadedUpdateInfo fromPath(Version currentVersion, String path) {
        if (path == null || path.equals("")) {
            LOG.w("Path was not specified.");
            return getInvalid(currentVersion);
        }

        PackageManager packageManager = App.getInstance().getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(path, 0 /*flags*/);
        if (packageInfo == null) {
            LOG.w(path + " is not a valid APK.");
            return getInvalid(currentVersion);
        }

        Version downloadedVersion;
        try {
            downloadedVersion = Version.valueOf(packageInfo.versionName);
        } catch (ParseException e) {
            LOG.w(path + " has an invalid semantic version: " + packageInfo.versionName + ".");
            return getInvalid(currentVersion);
        }

        return new DownloadedUpdateInfo(true /*isValid*/, currentVersion, downloadedVersion, path);
    }

    private DownloadedUpdateInfo(
            boolean isValid, Version currentVersion, Version downloadedVersion, String path) {
        this.isValid = isValid;
        this.currentVersion = currentVersion;
        this.downloadedVersion = downloadedVersion;
        this.path = path;
    }

    public boolean shouldInstall() {
        return isValid && downloadedVersion.greaterThan(currentVersion);
    }
}
