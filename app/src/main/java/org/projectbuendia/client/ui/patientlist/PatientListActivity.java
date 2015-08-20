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

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.ui.OdkActivityLauncher;
import org.projectbuendia.client.ui.SectionedSpinnerAdapter;
import org.projectbuendia.client.ui.patientcreation.PatientCreationActivity;
import org.projectbuendia.client.utils.Utils;

import javax.inject.Inject;

/**
 * A {@link PatientSearchActivity} showing a filterable list of patients via a
 * {@link PatientListFragment}.
 */
public class PatientListActivity extends PatientSearchActivity {

    private static final int ODK_ACTIVITY_REQUEST = 1;
    private static final String SELECTED_FILTER_KEY = "selected_filter";

    private PatientListFilterController mFilterController;
    private int mSelectedFilter = 0;
    @Inject AppSettings mSettings;

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);
        setContentView(R.layout.activity_patient_list);

        if (savedInstanceState != null) {
            mSelectedFilter = savedInstanceState.getInt(SELECTED_FILTER_KEY, 0);
        }

        mFilterController = new PatientListFilterController(
            new FilterUi(),
            mCrudEventBusProvider.get(),
            mAppModel,
            mLocale);
        mFilterController.setupActionBarAsync();

        App.getInstance().inject(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_FILTER_KEY, getActionBar().getSelectedNavigationIndex());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != ODK_ACTIVITY_REQUEST) {
            return;
        }
        OdkActivityLauncher.sendOdkResultToServer(
                this, mSettings, /* create a new patient */ null,
                false, resultCode, data);
    }

    private void startActivity(Class<?> activityClass) {
        Intent intent = new Intent(PatientListActivity.this, activityClass);
        startActivity(intent);
    }

    private final class FilterUi implements PatientListFilterController.Ui {

        @Override
        public void populateActionBar(final SimpleSelectionFilter[] filters) {
            SectionedSpinnerAdapter<SimpleSelectionFilter> adapter = new SectionedSpinnerAdapter<>(
                    PatientListActivity.this,
                    R.layout.patient_list_spinner_dropdown_item,
                    R.layout.patient_list_spinner_expanded_dropdown_item,
                    R.layout.patient_list_spinner_expanded_section_divider,
                    filters);

            ActionBar.OnNavigationListener callback = new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int position, long id) {
                    getSearchController().setFilter(filters[position]);
                    Utils.logUserAction("filter_selected",
                            "filter", filters[position].toString());
                    getSearchController().loadSearchResults();
                    return true;
                }
            };

            final ActionBar actionBar = getActionBar();
            actionBar.setLogo(R.drawable.ic_launcher);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(adapter, callback);
            actionBar.setSelectedNavigationItem(mSelectedFilter);

            getSearchController().setFilter(filters[mSelectedFilter]);
        }
    }
}
