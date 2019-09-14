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

import com.google.common.base.Joiner;

import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.utils.Utils;

import java.util.Arrays;
import java.util.List;

/** LocationUuidFilter matches all patients in a specified subtree of locations. */
public final class LocationUuidFilter extends SimpleSelectionFilter<Patient> {

    private final String mSelection;
    private final String[] mSelectionArgs;
    private final String mRootUuid;
    private final String mDescription;

    /** Creates a filter returning only patients under a subroot of the given location forest. */
    public LocationUuidFilter(LocationForest forest, Location subroot) {
        if (forest == null || subroot == null) {
            mSelection = "";
            mSelectionArgs = new String[0];
            mRootUuid = null;
            mDescription = "";
            return;
        }
        List<Location> subtreeNodes = forest.getSubtree(subroot);
        int numNodes = subtreeNodes.size();

        String[] placeholders = new String[numNodes];
        Arrays.fill(placeholders, "?");
        String nodePlaceholders = Joiner.on(",").join(placeholders);
        mSelection = Utils.format("%s in (%s)", Patients.LOCATION_UUID, nodePlaceholders);

        mSelectionArgs = new String[numNodes];
        for (int i = 0; i < subtreeNodes.size(); i++) {
            mSelectionArgs[i] = subtreeNodes.get(i).uuid;
        }

        mRootUuid = subroot.uuid;
        String indentedName = subroot.name;
        for (int i = 0; i < forest.getDepth(subroot); i++) {
            indentedName = "        " + indentedName;
        }
        mDescription = indentedName;
    }

    /** Returns the UUID of the root location used for filtering. */
    public String getFilterRootUuid() {
        return mRootUuid;
    }

    @Override public String getSelectionString() {
        return mSelection;
    }

    @Override public String[] getSelectionArgs(CharSequence constraint) {
        return mSelectionArgs;
    }

    @Override public String getDescription() {
        return mDescription;
    }
}
