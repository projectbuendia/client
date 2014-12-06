package org.msf.records.filter;

import org.msf.records.location.LocationTree;
import org.msf.records.sync.PatientProviderContract;

/**
 * LocationUuidFilter returns all patients who reside in the specified subtree of locations
 * (e.g. a LocationUuidFilter given a uuid of a zone will return all patients assigned to that
 * zone, tents within that zone, beds within those tents, etc.).
 */
public class LocationUuidFilter implements SimpleSelectionFilter {
    private String mTentSelectionString;
    private String[] mTentSelectionArgs;

    // Since all we know is that the patient is contained somewhere in the subtree of the given
    // UUID, return any node within that subtree. Treat null as the entire tree.
    public LocationUuidFilter(String rootUuid) {
        LocationTree root = LocationTree.getLocationForUuid(rootUuid);
        if (root == null) {
            mTentSelectionString = "";
            mTentSelectionArgs = new String[0];
            return;
        }

        LocationTree[] subtreeArray = root.getSubtreeLocationArray();

        // The code below may not scale well, but since the order of locations is expected to be
        // relatively small, this should be okay.
        StringBuilder sb = new StringBuilder();
        sb.append(PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID);
        sb.append(" IN (");
        String prefix = "";
        for (int i = 0; i < subtreeArray.length; i++) {
            sb.append(prefix);
            sb.append("?");
            prefix = ",";
        }
        sb.append(")");
        mTentSelectionString = sb.toString();

        mTentSelectionArgs = new String[subtreeArray.length];
        for (int i = 0; i < subtreeArray.length; i++) {
            mTentSelectionArgs[i] = subtreeArray[i].getLocation().uuid;
        }
    }

    @Override
    public String getSelectionString() {
        return mTentSelectionString;
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return mTentSelectionArgs;
    }
}
