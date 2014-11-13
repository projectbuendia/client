package org.msf.records.updater;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.msf.records.App;

/**
 * An object containing information about an update that has been downloaded and is ready to be
 * installed.
 */
public class DownloadedUpdateInfo {

    /**
     * The path to the downloaded update.
     */
    public final String mPath;

    /**
     * The version code of the downloaded update or {@code INVALID_VERSION_CODE} if the update is
     * invalid.
     */
    public final int mDownloadedVersionCode;

    public DownloadedUpdateInfo(String path) {
        mPath = path;
        mDownloadedVersionCode = getVersionCode(mPath);
    }

    private int getVersionCode(String mPath) {
        PackageManager packageManager = App.getInstance().getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(mPath, 0 /*flags*/);

        return packageInfo == null ? UpdateManager.INVALID_VERSION_CODE : packageInfo.versionCode;
    }
}
