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
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.TypedCursor;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.ui.LoggedInActivity;
import org.projectbuendia.client.ui.ReadyState;
import org.projectbuendia.client.ui.chart.PatientChartActivity;
import org.projectbuendia.client.ui.dialogs.PatientDialogFragment;
import org.projectbuendia.client.utils.EventBusWrapper;
import org.projectbuendia.client.utils.Utils;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * A {@link LoggedInActivity} with a {@link SearchView} that filters a patient list.
 * Clicking on patients in the list displays details for that patient.
 */
public abstract class PatientListActivity extends LoggedInActivity {
    @Inject EventBus mEventBus;

    private PatientSearchController mSearchController;
    private SearchView mSearchView;
    private static boolean sSkippedPatientList;
    private final EventBusSubscriber mSubscriber = new EventBusSubscriber();

    public PatientSearchController getSearchController() {
        return mSearchController;
    }

    @Override public void onExtendOptionsMenu(Menu menu) {
        super.onExtendOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main, menu);

        menu.findItem(R.id.action_new_patient).setOnMenuItemClickListener(
            menuItem -> {
                Utils.logEvent("add_patient_pressed");
                return openDialog(PatientDialogFragment.create(null));
            }
        );

        MenuItem search = menu.findItem(R.id.action_search);
        setMenuBarIcon(search, FontAwesomeIcons.fa_filter);
        search.setVisible(getReadyState() == ReadyState.READY);

        MenuItem addPatient = menu.findItem(R.id.action_new_patient);
        setMenuBarIcon(addPatient, FontAwesomeIcons.fa_user_plus);
        addPatient.setVisible(getReadyState() == ReadyState.READY);

        mSearchView = (SearchView) search.getActionView();
        mSearchView.setIconifiedByDefault(false);

        InputMethodManager mgr =
            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                InputMethodManager mgr = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
                return true;
            }

            @Override public boolean onQueryTextChange(String newText) {
                mSearchController.onQuerySubmitted(newText);
                return true;
            }
        });
    }

    @Override protected boolean onCreateImpl(Bundle state) {
        if (!super.onCreateImpl(state)) return false;

        mSearchController = new PatientSearchController(
            new SearchUi(),
            App.getCrudEventBus(),
            new EventBusWrapper(mEventBus),
            App.getModel(),
            App.getSyncManager());

        // To facilitate chart development, there's a developer setting that
        // causes the app to go straight to a patient chart on startup.
        if (!sSkippedPatientList && settings.shouldSkipToPatientChart()) {
            try (Cursor cursor = getContentResolver().query(
                Patients.URI, null, Patients.ID + " = ?",
                new String[] {settings.getStartingPatientId()}, null)) {
                if (cursor.moveToNext()) {
                    sSkippedPatientList = true;
                    PatientChartActivity.start(this, Utils.getString(cursor, Patients.UUID, null));
                }
            }
        }
        return true;
    }

    @Override protected void onResumeImpl() {
        super.onResumeImpl();
        attemptInit();
    }

    protected void attemptInit() {
        if (App.getModel().isReady()) {
            mSearchController.init();
            mSearchController.loadSearchResults();
        } else {
            mEventBus.register(mSubscriber);
        }
    }

    @Override protected void onPauseImpl() {
        super.onPauseImpl();
        if (mEventBus.isRegistered(mSubscriber)) mEventBus.unregister(mSubscriber);
        mSearchController.suspend();
    }

    protected void setPatients(TypedCursor<Patient> patients) {
        // By default, do nothing.
    }

    private final class SearchUi implements PatientSearchController.Ui {
        @Override public void setPatients(TypedCursor<Patient> patients) {
            // Delegate to implementers.
            PatientListActivity.this.setPatients(patients);
        }

        @Override public void goToPatientChart(String patientUuid) {
            BigToast.show(R.string.patient_creation_success);
            PatientChartActivity.start(PatientListActivity.this, patientUuid);
        }
    }

    private final class EventBusSubscriber {
        public void onEventMainThread(SyncSucceededEvent event) {
            mEventBus.unregister(mSubscriber);
            attemptInit();
        }
    }
}
