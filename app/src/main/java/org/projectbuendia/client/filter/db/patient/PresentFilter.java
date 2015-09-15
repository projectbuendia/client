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

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.Zones;
import org.projectbuendia.client.sync.providers.Contracts;

/**
 * Matches patients that are present (not discharged). This filter does NOT account for any
 * child locations of the Discharged zone, if any such location exists. This constraint allows
 * this filter to function even when locations have not been loaded, since the UUID for Discharged
 * Zone is a known constant.
 */
public class PresentFilter extends SimpleSelectionFilter<Patient> {
    private static final String SELECTION_STRING = Contracts.Patients.LOCATION_UUID + "!=?";
    private static final String[] SELECTION_ARGS = new String[] {Zones.DISCHARGED_ZONE_UUID};

    @Override public String getSelectionString() {
        return SELECTION_STRING;
    }

    @Override public String[] getSelectionArgs(CharSequence constraint) {
        return SELECTION_ARGS;
    }

    @Override public String getDescription() {
        return App.getInstance().getString(R.string.present_filter_description);
    }
}
