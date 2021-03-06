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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.models.Location;
import org.projectbuendia.client.ui.ReadyState;
import org.projectbuendia.client.ui.SettingsActivity;
import org.projectbuendia.client.utils.EventBusWrapper;
import org.projectbuendia.client.utils.Utils;

import de.greenrobot.event.EventBus;

/** Displays a list of locations and allows users to search through a list of patients. */
public final class LocationListActivity extends PatientListActivity {

    private LocationListController mController;
    private AlertDialog mSyncFailedDialog;

    public static void start(Context caller) {
        caller.startActivity(new Intent(caller, LocationListActivity.class));
    }

    @Override public void onExtendOptionsMenu(Menu menu) {
        super.onExtendOptionsMenu(menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override public boolean onMenuItemActionExpand(MenuItem item) {
                mController.onSearchPressed();
                return true;
            }

            @Override public boolean onMenuItemActionCollapse(MenuItem item) {
                mController.onSearchCancelled();
                return true;
            }
        });
    }

    @Override protected boolean onCreateImpl(Bundle state) {
        if (!super.onCreateImpl(state)) return false;
        setTitle(R.string.title_location_list);

        mController = new LocationListController(
            App.getModel(),
            settings,
            App.getCrudEventBus(),
            new Ui(),
            new EventBusWrapper(EventBus.getDefault()),
            App.getSyncManager(),
            getSearchController());

        mSyncFailedDialog = new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getString(R.string.sync_failed_dialog_title))
            .setMessage(R.string.sync_failed_dialog_message)
            .setNegativeButton(
                R.string.sync_failed_back, (dialog, which) -> {
                    Utils.logEvent("sync_failed_back_pressed");
                    finish();
                })
            .setNeutralButton(
                R.string.sync_failed_settings, (dialog, which) -> {
                    Utils.logEvent("sync_failed_settings_pressed");
                    SettingsActivity.start(LocationListActivity.this);
                })
            .setPositiveButton(
                R.string.sync_failed_retry, (dialog, which) -> {
                    Utils.logEvent("sync_failed_retry_pressed");
                    mController.startInitialSync();
                })
            .setCancelable(false)
            .create();

        setContentView(R.layout.activity_location_selection);
        if (state == null) {
            getSupportFragmentManager().beginTransaction()
                .add(R.id.location_selection_container, new LocationListFragment())
                .commit();
        }
        return true;
    }

    LocationListController getController() {
        return mController;
    }

    private final class Ui implements LocationListController.Ui {
        @Override public void switchToLocationList() {
            getSupportFragmentManager().popBackStack();
        }

        @Override public void switchToPatientList() {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.location_selection_container, new PatientListFragment())
                .addToBackStack(null)
                .commit();
        }

        @Override public void showSyncFailedDialog(boolean show) {
            Utils.showDialogIf(mSyncFailedDialog, show);
        }

        @Override public void setReadyState(ReadyState state) {
            LocationListActivity.this.setReadyState(state);
        }

        @Override public void finish() {
            LocationListActivity.this.finish();
        }

        @Override public void openSingleLocation(Location location) {
            SingleLocationActivity.start(LocationListActivity.this, location);
        }
    }
}
