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

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.ui.SectionedSpinnerAdapter;
import org.projectbuendia.client.utils.Utils;

import java.util.List;

/** A list of patients with a choice of several filters in a dropdown menu. */
public class FilteredPatientListActivity extends BaseSearchablePatientListActivity {
    private static final String SELECTED_FILTER_KEY = "selected_filter";

    private PatientFilterController mFilterController;
    private int mSelectedFilter = 0;

    public static void start(Context caller) {
        caller.startActivity(new Intent(caller, FilteredPatientListActivity.class));
    }

    @Override protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);
        setContentView(R.layout.activity_patient_list);

        if (savedInstanceState != null) {
            mSelectedFilter = savedInstanceState.getInt(SELECTED_FILTER_KEY, 0);
        }

        mFilterController = new PatientFilterController(
            new FilterUi(),
            mAppModel,
            mLocale);

        App.getInstance().inject(this);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_FILTER_KEY, getActionBar().getSelectedNavigationIndex());
    }

    private final class FilterUi implements PatientFilterController.Ui {

        private int lastPosition = 0;

        @Override public void populateActionBar(List<SimpleSelectionFilter<?>> filters) {
            SectionedSpinnerAdapter<SimpleSelectionFilter<?>> adapter = new SectionedSpinnerAdapter<>(
                FilteredPatientListActivity.this,
                R.layout.patient_list_spinner_dropdown_item,
                R.layout.patient_list_spinner_expanded_dropdown_item,
                R.layout.patient_list_spinner_expanded_section_divider,
                filters);

            ActionBar.OnNavigationListener callback = (position, id) -> {
                if (position != lastPosition) {
                    SimpleSelectionFilter<?> filter = filters.get(position);
                    getSearchController().setFilter(filter);
                    Utils.logUserAction("filter_selected", "filter", filter.toString());
                    getSearchController().loadSearchResults();
                    lastPosition = position;
                }
                return true;
            };

            final ActionBar actionBar = getActionBar();
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(adapter, callback);
            actionBar.setSelectedNavigationItem(mSelectedFilter);

            getSearchController().setFilter(filters.get(mSelectedFilter));
        }
    }
}
