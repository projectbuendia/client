package org.msf.records.updater;

import android.net.Uri;

/**
 * An object containing information about an available application update.
 */
public class AvailableUpdateInfo {

    public final int mCurrentVersionCode;
    public final int mAvailableVersionCode;
    public final Uri mUpdateUri;

    public AvailableUpdateInfo(int currentVersionCode, int availableVersionCode, Uri updateUri) {
        mCurrentVersionCode = currentVersionCode;
        mAvailableVersionCode = availableVersionCode;
        mUpdateUri = updateUri;
    }
}
