package org.msf.records.updater;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;

import org.msf.records.App;

/**
 * An object containing information about an update that has been downloaded and is ready to be
 * installed.
 */
public class DownloadedUpdateInfo {

    private static final String TAG = DownloadedUpdateInfo.class.getName();

    public final boolean mIsValid;
    private final Version mCurrentVersion;
    public Version mDownloadedVersion;
    public final String mPath;

    public static DownloadedUpdateInfo getInvalid(Version currentVersion) {
        return new DownloadedUpdateInfo(
                false /*isValid*/, currentVersion, UpdateManager.INVALID_VERSION, null /*path*/);
    }

    public static DownloadedUpdateInfo fromPath(Version currentVersion, String path) {
        if (path == null || path.equals("")) {
            Log.w(TAG, "Path was not specified.");
            return getInvalid(currentVersion);
        }

        PackageManager packageManager = App.getInstance().getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(path, 0 /*flags*/);
        if (packageInfo == null) {
            Log.w(TAG, path + " is not a valid APK.");
            return getInvalid(currentVersion);
        }

        Version downloadedVersion;
        try {
            downloadedVersion = Version.valueOf(packageInfo.versionName);
        } catch (ParseException e) {
            Log.w(TAG, path + " has an invalid semantic version: " + packageInfo.versionName + ".");
            return getInvalid(currentVersion);
        }

        return new DownloadedUpdateInfo(true /*isValid*/, currentVersion, downloadedVersion, path);
    }

    private DownloadedUpdateInfo(
            boolean isValid, Version currentVersion, Version downloadedVersion, String path) {
        mIsValid = isValid;
        mCurrentVersion = currentVersion;
        mDownloadedVersion = downloadedVersion;
        mPath = path;
    }

    public boolean shouldInstall() {
        return mIsValid && mDownloadedVersion.greaterThan(mCurrentVersion);
    }
}
