package org.msf.records.filter;

import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.sync.providers.Contracts;

import java.util.List;

import javax.annotation.Nullable;

/**
 * LocationUuidFilter returns all patients who reside in the specified subtree of locations.
 *
 * <p>For example, a LocationUuidFilter given a uuid of a zone will return all patients assigned to that
 * zone, tents within that zone, beds within those tents, etc.
 */
public final class LocationUuidFilter implements SimpleSelectionFilter {

    private final String mTentSelectionString;
    private final String[] mTentSelectionArgs;

    // Since all we know is that the patient is contained somewhere in the subtree of the given
    // UUID, return any node within that subtree. Treat null as the entire tree.
    public LocationUuidFilter(@Nullable LocationSubtree subtree) {
        if (subtree == null) {
            mTentSelectionString = "";
            mTentSelectionArgs = new String[0];
            return;
        }
        List<LocationSubtree> allPossibleLocations = subtree.thisAndAllDescendents();

        // The code below may not scale well, but since the number of locations is expected to be
        // relatively small, this should be okay.
        StringBuilder sb = new StringBuilder()
        		.append(Contracts.Patients.LOCATION_UUID)
        		.append(" IN (");
        String prefix = "";
        for (int i = 0; i < allPossibleLocations.size(); i++) {
            sb.append(prefix).append("?");
            prefix = ",";
        }
        sb.append(")");
        mTentSelectionString = sb.toString();

        mTentSelectionArgs = new String[allPossibleLocations.size()];
        for (int i = 0; i < allPossibleLocations.size(); i++) {
            mTentSelectionArgs[i] = allPossibleLocations.get(i).getLocation().uuid;
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
