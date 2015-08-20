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
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.TypedCursor;
import org.projectbuendia.client.utils.PatientCountDisplay;

/** A patient list for a single location. */
public class SingleLocationActivity extends BaseSearchablePatientListActivity {
    private String mLocationName;
    private String mLocationUuid;
    private int mPatientCount;

    public static void start(Context caller, String locationUuid, String locationName, int patientCount) {
        Intent intent = new Intent(caller, SingleLocationActivity.class);
        intent.putExtra("uuid", locationUuid);
        intent.putExtra("name", locationName);
        intent.putExtra("count", patientCount);
        caller.startActivity(intent);
    }

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);

        mLocationUuid = getIntent().getStringExtra("uuid");
        mLocationName = getIntent().getStringExtra("name");
        mPatientCount = getIntent().getIntExtra("count", 0);
        setTitle(PatientCountDisplay.getPatientCountTitle(this, mPatientCount, mLocationName));
        setContentView(R.layout.activity_round);
        getSearchController().setLocationFilter(mLocationUuid);
    }

    @Override
    protected void setPatients(TypedCursor<AppPatient> patients) {
        mPatientCount = patients.getCount();
        setTitle(PatientCountDisplay.getPatientCountTitle(this, mPatientCount, mLocationName));
    }
}
