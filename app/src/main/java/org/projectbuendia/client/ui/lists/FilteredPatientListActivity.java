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

/** A list of patients with a choice of several filters in a dropdown menu. */
public class FilteredPatientListActivity extends BaseSearchablePatientListActivity {
    public static void start(Context caller) {
        caller.startActivity(new Intent(caller, FilteredPatientListActivity.class));
    }

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);
        setContentView(R.layout.activity_patient_list);
    }
}
