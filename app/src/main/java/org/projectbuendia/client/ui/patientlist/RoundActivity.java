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

package org.projectbuendia.client.ui.patientlist;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.projectbuendia.client.R;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.TypedCursor;
import org.projectbuendia.client.ui.dialogs.GoToPatientDialogFragment;
import org.projectbuendia.client.ui.patientcreation.PatientCreationActivity;
import org.projectbuendia.client.utils.PatientCountDisplay;
import org.projectbuendia.client.utils.Utils;

/**
 * A {@link PatientSearchActivity} that limits the patients listed to a single location using a
 * {@link RoundFragment}. The UI is adjusted accordingly.
 */
public class RoundActivity extends PatientSearchActivity {
    private String mLocationName;
    private String mLocationUuid;
    private int mLocationPatientCount;

    public static final String LOCATION_NAME_KEY = "location_name";
    public static final String LOCATION_PATIENT_COUNT_KEY = "location_patient_count";
    public static final String LOCATION_UUID_KEY = "location_uuid";

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);

        mLocationName = getIntent().getStringExtra(LOCATION_NAME_KEY);
        mLocationPatientCount = getIntent().getIntExtra(LOCATION_PATIENT_COUNT_KEY, 0);
        mLocationUuid = getIntent().getStringExtra(LOCATION_UUID_KEY);

        setTitle(PatientCountDisplay.getPatientCountTitle(
                this, mLocationPatientCount, mLocationName));
        setContentView(R.layout.activity_round);

        getSearchController().setLocationFilter(mLocationUuid);
    }

    @Override
    public void onExtendOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        menu.findItem(R.id.action_add).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Utils.logUserAction("add_patient_pressed");
                        startActivity(
                                new Intent(RoundActivity.this, PatientCreationActivity.class));

                        return true;
                    }
                });

        menu.findItem(R.id.action_go_to).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Utils.logUserAction("go_to_patient_pressed");
                        GoToPatientDialogFragment.newInstance()
                                .show(getSupportFragmentManager(), null);
                        return true;
                    }
                });

        super.onExtendOptionsMenu(menu);
    }

    @Override
    protected void setPatients(TypedCursor<AppPatient> patients) {
        mLocationPatientCount = patients.getCount();
        setTitle(PatientCountDisplay.getPatientCountTitle(
                RoundActivity.this, mLocationPatientCount, mLocationName));
    }
}
