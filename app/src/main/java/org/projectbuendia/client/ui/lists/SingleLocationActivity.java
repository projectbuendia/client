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

package org.projectbuendia.client.ui.lists;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.projectbuendia.client.R;
import org.projectbuendia.models.Location;
import org.projectbuendia.models.Patient;
import org.projectbuendia.models.TypedCursor;

/** A patient list for a single location. */
public class SingleLocationActivity extends PatientListActivity {
    private String mLocationUuid;
    private long mPatientCount;

    public static void start(Context caller, Location location) {
        caller.startActivity(
            new Intent(caller, SingleLocationActivity.class)
                .putExtra("uuid", location.uuid));
    }

    @Override protected boolean onCreateImpl(Bundle state) {
        if (!super.onCreateImpl(state)) return false;

        setTitle(R.string.title_single_location);
        mLocationUuid = getIntent().getStringExtra("uuid");
        setContentView(R.layout.activity_single_location);
        getSearchController().setLocationFilter(mLocationUuid);
        return true;
    }

    @Override protected void setPatients(TypedCursor<Patient> patients) {
        mPatientCount = patients.getCount();
        setTitle(u.formatLocationHeading(mLocationUuid, mPatientCount));
    }
}
