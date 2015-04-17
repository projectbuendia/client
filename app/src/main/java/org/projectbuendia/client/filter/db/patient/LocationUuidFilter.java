// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.filter.db.patient;

import org.projectbuendia.client.data.app.AppLocation;
import org.projectbuendia.client.data.app.AppLocationTree;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.sync.providers.Contracts;

import java.util.List;

/**
 * LocationUuidFilter matches all patients who reside in the specified subtree of locations.
 *
 * <p>For example, a LocationUuidFilter given a UUID of a zone will return all patients assigned to
 * that zone, tents within that zone, beds within those tents, etc.
 */
public final class LocationUuidFilter extends SimpleSelectionFilter<AppPatient> {

    private final String mTentSelectionString;
    private final String[] mTentSelectionArgs;
    private final String mUuid;
    private final String mDescription;

    /** Creates a filter that returns all patients in a valid location. */
    public LocationUuidFilter(AppLocationTree tree) {
        this(tree, tree == null ? null : tree.getRoot());
    }

    /** Creates a filter returning only patients under a subroot of the given location tree. */
    public LocationUuidFilter(AppLocationTree tree, AppLocation subroot) {
        if (tree == null || subroot == null) {
            mTentSelectionString = "";
            mTentSelectionArgs = new String[0];
            mUuid = null;
            mDescription = "";
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
        mDescription = subroot.name;
    }

    /** Returns the UUID of the root location used for filtering. */
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

    @Override
    public String getDescription() {
        return mDescription;
    }
}
