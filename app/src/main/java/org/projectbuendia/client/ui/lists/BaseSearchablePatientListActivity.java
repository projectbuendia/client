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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.actions.SyncCancelRequestedEvent;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.TypedCursor;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.BaseLoggedInActivity;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.ui.LoadingState;
import org.projectbuendia.client.ui.UpdateNotificationController;
import org.projectbuendia.client.ui.chart.PatientChartActivity;
import org.projectbuendia.client.ui.dialogs.EditPatientDialogFragment;
import org.projectbuendia.client.ui.dialogs.GoToPatientDialogFragment;
import org.projectbuendia.client.utils.EventBusWrapper;
import org.projectbuendia.client.utils.Utils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * A {@link BaseLoggedInActivity} with a {@link SearchView} that filters a patient list.
 * Clicking on patients in the list displays details for that patient.
 */
public abstract class BaseSearchablePatientListActivity extends BaseLoggedInActivity {

    @Inject AppModel mAppModel;
    @Inject EventBus mEventBus;
    @Inject CrudEventBus mCrudEventBus;
    @Inject SyncManager mSyncManager;

    private PatientSearchController mSearchController;
    private SearchView mSearchView;
    private CancelButtonListener mCancelListener = new CancelButtonListener();

    // TODO/i18n: Populate properly.
    protected final String mLocale = "en";

    public PatientSearchController getSearchController() {
        return mSearchController;
    }

    @Override public void onExtendOptionsMenu(Menu menu) {
        super.onExtendOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        menu.findItem(R.id.action_new_patient).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {

                @Override public boolean onMenuItemClick(MenuItem menuItem) {
                    Utils.logEvent("add_patient_pressed");
                    EditPatientDialogFragment.newInstance(null)
                        .show(getSupportFragmentManager(), null);
                    return true;
                }
            });

        /*
        menu.findItem(R.id.action_go_to).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {

                @Override public boolean onMenuItemClick(MenuItem menuItem) {
                    Utils.logUserAction("go_to_patient_pressed");
                    GoToPatientDialogFragment.newInstance()
                        .show(getSupportFragmentManager(), null);
                    return true;
                }
            });
        */

        MenuItem search = menu.findItem(R.id.action_search);
        search.setIcon(
            new IconDrawable(this, Iconify.IconValue.fa_search)
                .color(0xCCFFFFFF)
                .sizeDp(36));
        search.setVisible(getLoadingState() == LoadingState.LOADED);

        MenuItem addPatient = menu.findItem(R.id.action_new_patient);
        addPatient.setIcon(
            new IconDrawable(this, Iconify.IconValue.fa_plus)
                .color(0xCCFFFFFF)
                .sizeDp(36));
        addPatient.setVisible(getLoadingState() == LoadingState.LOADED);

        mSearchView = (SearchView) search.getActionView();
        mSearchView.setIconifiedByDefault(false);

        MenuItem cancel = menu.findItem(R.id.action_cancel);
        cancel.setIcon(
            new IconDrawable(this, Iconify.IconValue.fa_close)
                .color(0xCCFFFFFF)
                .sizeDp(36));
        cancel.setOnMenuItemClickListener(mCancelListener);
        cancel.setVisible(getLoadingState() == LoadingState.SYNCING);

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

    @Override protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);

        App.getInstance().inject(this);
        mSearchController = new PatientSearchController(
            new SearchUi(),
            mCrudEventBus,
            new EventBusWrapper(mEventBus),
            mAppModel,
            mSyncManager,
            mLocale);

        mUpdateNotificationController = new UpdateNotificationController(
            new UpdateNotificationUi()
        );

        ButterKnife.inject(this);
    }

    @Override protected void onResumeImpl() {
        super.onResumeImpl();
        mSearchController.init();
        mSearchController.loadSearchResults();
    }

    @Override protected void onPauseImpl() {
        super.onPauseImpl();
        mSearchController.suspend();
    }

    protected void setPatients(TypedCursor<Patient> patients) {
        // By default, do nothing.
    }

    private final class SearchUi implements PatientSearchController.Ui {
        @Override public void setPatients(TypedCursor<Patient> patients) {
            // Delegate to implementers.
            BaseSearchablePatientListActivity.this.setPatients(patients);
        }

        @Override public void goToPatientChart(String patientUuid) {
            BigToast.show(BaseSearchablePatientListActivity.this, R.string.patient_creation_success);
            PatientChartActivity.start(BaseSearchablePatientListActivity.this, patientUuid);
        }
    }

    private class CancelButtonListener implements MenuItem.OnMenuItemClickListener {
        @Override public boolean onMenuItemClick(MenuItem item) {
            mEventBus.post(new SyncCancelRequestedEvent());
            return true;
        }
    }
}
