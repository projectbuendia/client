package org.msf.records.filter.db.patient;

import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppPatient;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.sync.providers.Contracts;

import java.util.List;

/**
 * LocationUuidFilter returns all patients who reside in the specified subtree of locations.
 *
 * <p>For example, a LocationUuidFilter given a encounterUuid of a zone will return all patients assigned to
 * that zone, tents within that zone, beds within those tents, etc.
 */
public final class LocationUuidFilter implements SimpleSelectionFilter<AppPatient> {

    private final String mTentSelectionString;
    private final String[] mTentSelectionArgs;
    private final String mUuid;

    /**
     * Creates a filter that returns all patients in a valid location.
     */
    public LocationUuidFilter(AppLocationTree tree) {
        this(tree, tree == null ? null : tree.getRoot());
    }

    /**
     * Creates a filter returning only patients under a subroot of the given location tree.
     */
    public LocationUuidFilter(AppLocationTree tree, AppLocation subroot) {
        if (tree == null || subroot == null) {
            mTentSelectionString = "";
            mTentSelectionArgs = new String[0];
            mUuid = null;
            return;
        }
        List<AppLocation> allPossibleLocations = tree.locationsInSubtree(subroot);

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
            mTentSelectionArgs[i] = allPossibleLocations.get(i).uuid;
        }

        mUuid = subroot.uuid;
    }

    /**
     * Returns the UUID of the root location used for filtering.
     */
    public String getFilterRootUuid() {
        return mUuid;
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
