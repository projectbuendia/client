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

package org.projectbuendia.client.ui.locationselection;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import javax.inject.Inject;
import javax.inject.Provider;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.data.app.AppLocation;
import org.projectbuendia.client.data.app.AppModel;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.net.Common;
import org.projectbuendia.client.sync.SyncAccountService;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.LoadingState;
import org.projectbuendia.client.ui.SettingsActivity;
import org.projectbuendia.client.ui.patientcreation.PatientCreationActivity;
import org.projectbuendia.client.ui.patientlist.PatientListFragment;
import org.projectbuendia.client.ui.patientlist.PatientSearchActivity;
import org.projectbuendia.client.ui.patientlist.RoundActivity;
import org.projectbuendia.client.utils.EventBusWrapper;
import org.projectbuendia.client.utils.Utils;

import de.greenrobot.event.EventBus;

/** Displays a list of locations and allows users to search through a list of patients. */
public final class LocationSelectionActivity extends PatientSearchActivity {

    private LocationSelectionController mController;
    private AlertDialog mSyncFailedDialog;

    @Inject AppModel mAppModel;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @Inject SyncManager mSyncManager;

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);
        App.getInstance().inject(this);

        if (Common.OFFLINE_SUPPORT) {
            // Create account, if needed
            SyncAccountService.initialize(this);
        }

        mController = new LocationSelectionController(
                mAppModel,
                mCrudEventBusProvider.get(),
                new Ui(),
                new EventBusWrapper(EventBus.getDefault()),
                mSyncManager,
                getSearchController());

        mSyncFailedDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.sync_failed_dialog_title))
                .setMessage(R.string.sync_failed_dialog_message)
                .setNegativeButton(
                        R.string.sync_failed_back, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Utils.logEvent("sync_failed_back_pressed");
                                finish();
                            }
                        })
                .setNeutralButton(
                        R.string.sync_failed_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Utils.logEvent("sync_failed_settings_pressed");
                                startActivity(new Intent(
                                        LocationSelectionActivity.this,SettingsActivity.class));
                            }
                        })
                .setPositiveButton(
                        R.string.sync_failed_retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Utils.logEvent("sync_failed_retry_pressed");
                                mController.onSyncRetry();
                            }
                        })
                .setCancelable(false)
                .create();

        setContentView(R.layout.activity_location_selection);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.location_selection_container, new LocationSelectionFragment())
                    .commit();
        }
    }

    LocationSelectionController getController() {
        return mController;
    }

    @Override
    public void onResumeImpl() {
        super.onResumeImpl();
        mController.init();
    }

    @Override
    public void onPauseImpl() {
        super.onPauseImpl();
        mController.suspend();
    }

    @Override
    public void onExtendOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        menu.findItem(R.id.action_add).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Utils.logEvent("add_patient_pressed");
                        startActivity(new Intent(
                                LocationSelectionActivity.this,
                                PatientCreationActivity.class));

                        return true;
                    }
                });

        super.onExtendOptionsMenu(menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mController.onSearchPressed();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mController.onSearchCancelled();
                return true;
            }
        });
    }

    private final class Ui implements LocationSelectionController.Ui {
        @Override
        public void switchToLocationSelectionScreen() {
            getSupportFragmentManager().popBackStack();
        }

        @Override
        public void switchToPatientListScreen() {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.location_selection_container, new PatientListFragment())
                    .addToBackStack(null)
                    .commit();
        }

        @Override
        public void showSyncFailedDialog(boolean show) {
            Utils.showDialogIf(mSyncFailedDialog, show);
        }

        @Override
        public void setLoadingState(LoadingState loadingState) {
            LocationSelectionActivity.this.setLoadingState(loadingState);
        }

        @Override
        public void finish() {
            LocationSelectionActivity.this.finish();
        }

        @Override
        public void launchActivityForLocation(AppLocation location) {
            Intent roundIntent =
                    new Intent(LocationSelectionActivity.this, RoundActivity.class);
            roundIntent.putExtra(RoundActivity.LOCATION_NAME_KEY, location.name);
            roundIntent.putExtra(RoundActivity.LOCATION_UUID_KEY, location.uuid);
            roundIntent.putExtra(RoundActivity.LOCATION_PATIENT_COUNT_KEY, location.patientCount);
            startActivity(roundIntent);
        }
    }
}
