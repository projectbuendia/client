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
                UpdateManager.MINIMAL_VERSION,
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

        // The package server is responsible for sorting the index in
        // order by increasing version number, so the last is the highest.
        UpdateInfo latestUpdateInfo = response.get(response.size() - 1);

        LexicographicVersion version = latestUpdateInfo.getParsedVersion();
        if (version == null) {
            LOG.w("Invalid version in 'version' field: " + version);
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

    /**
     * Returns true if this is a valid update with a higher version number.
     */
    public boolean shouldUpdate() {
        return isValid && availableVersion.greaterThan(currentVersion);
    }
}
