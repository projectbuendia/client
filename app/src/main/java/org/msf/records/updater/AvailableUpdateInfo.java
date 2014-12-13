package org.msf.records.updater;

import android.net.Uri;
import android.util.Log;

import com.github.zafarkhaja.semver.Version;

import org.msf.records.model.UpdateInfo;

/**
 * An object containing information about an available application update.
 */
public class AvailableUpdateInfo {

    private static final String TAG = AvailableUpdateInfo.class.getName();

    public final boolean isValid;
    public final Version currentVersion;
    public final Version availableVersion;
    public final Uri updateUri;

    public static AvailableUpdateInfo getInvalid(Version currentVersion) {
        return new AvailableUpdateInfo(
                false /*isValid*/,
                currentVersion,
                UpdateManager.INVALID_VERSION,
                null /*updateUri*/);
    }

    public static AvailableUpdateInfo fromResponse(Version currentVersion, UpdateInfo response) {
        if (response == null) {
            Log.w(TAG, "The update info is null.");
            return getInvalid(currentVersion);
        }

        if (response.androidClient == null) {
            Log.w(TAG, "The update info response is missing the androidclient field.");
            return getInvalid(currentVersion);
        }

        Version version = response.androidClient.getLatestVersion();
        if (version == null) {
            Log.w(
                    TAG,
                    "The androidclient update info response is missing the version field or its "
                            + "version field is not a valid semantic version."
            );
            return getInvalid(currentVersion);
        }

        if (response.androidClient.run == null || response.androidClient.run.size() < 1) {
            Log.w(TAG, "The androidclient update info response does not have a run entry.");
            return getInvalid(currentVersion);
        }

        String runEntry = response.androidClient.run.get(0);
        if (runEntry == null) {
            Log.w(TAG, "The androidclient update info response's run entry is null.");
            return getInvalid(currentVersion);
        }

        Uri updateUri;
        try {
            updateUri = Uri.parse(UpdateServer.ROOT_URL + runEntry);
        } catch (IllegalArgumentException e) {
            Log.w(
                    TAG,
                    "The androidclient update info response's run entry is not a valid URI path "
                            + "segment.",
                    e
            );
            return getInvalid(currentVersion);
        }

        return new AvailableUpdateInfo(true /*isValid*/, currentVersion, version, updateUri);
    }

    private AvailableUpdateInfo(
            boolean isValid,
            Version currentVersion,
            Version availableVersion,
            Uri updateUri) {
        this.isValid = isValid;
        this.currentVersion = currentVersion;
        this.availableVersion = availableVersion;
        this.updateUri = updateUri;
    }

    /**
     * Returns whether this update is valid and is newer than current version.
     */
    public boolean shouldUpdate() {
        return isValid && availableVersion.greaterThan(currentVersion);
    }
}
