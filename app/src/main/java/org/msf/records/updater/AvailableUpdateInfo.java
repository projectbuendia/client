package org.msf.records.updater;

import android.net.Uri;

import com.google.common.base.Strings;

import org.msf.records.model.UpdateInfo;
import org.msf.records.utils.LexicographicVersion;
import org.msf.records.utils.Logger;

import java.util.List;

/**
 * An object containing information about an available application update.
 */
public class AvailableUpdateInfo {

    private static final Logger LOG = Logger.create();

    public final boolean isValid;
    public final LexicographicVersion currentVersion;
    public final LexicographicVersion availableVersion;
    public final Uri updateUri;

    /**
     * Creates an instance of {@link AvailableUpdateInfo} for an invalid update.
     */
    public static AvailableUpdateInfo getInvalid(LexicographicVersion currentVersion) {
        return new AvailableUpdateInfo(
                false /*isValid*/,
                currentVersion,
                UpdateManager.INVALID_VERSION,
                null /*updateUri*/);
    }

    /**
     * Creates an instance of {@link AvailableUpdateInfo} from a server response.
     */
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

        UpdateInfo latestUpdateInfo = response.get(0);

        LexicographicVersion version = latestUpdateInfo.getParsedVersion();
        if (version == null) {
            LOG.w(
                    "The latest update info is missing the version field or its version field is "
                            + "not a valid semantic version.");
            return getInvalid(currentVersion);
        }

        if (Strings.isNullOrEmpty(latestUpdateInfo.source)) {
            LOG.w("The latest update info is missing the src field.");
        }

        Uri updateUri;
        try {
            updateUri = Uri.parse(latestUpdateInfo.source);
        } catch (IllegalArgumentException e) {
            LOG.w(
                    e,
                    "The latest update info response src field is not a valid URI path segment: "
                            + "'%1$s'.",
                    latestUpdateInfo.source);
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

    /**
     * Returns whether this update is valid and is newer than current version.
     */
    public boolean shouldUpdate() {
        return isValid && availableVersion.greaterThan(currentVersion);
    }
}
