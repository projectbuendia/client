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

package org.msf.records.ui.userlogin;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.diagnostics.Troubleshooter;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.BigToast;
import org.msf.records.ui.SettingsActivity;
import org.msf.records.ui.dialogs.AddNewUserDialogFragment;
import org.msf.records.ui.locationselection.LocationSelectionActivity;
import org.msf.records.utils.EventBusWrapper;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * {@link BaseActivity} where users log in by selecting their name from a list.
 * This is the starting activity for the app.
 */
public class UserLoginActivity extends BaseActivity {

    private UserLoginController mController;
    private AlertDialog mSyncFailedDialog;
    @Inject Troubleshooter mTroubleshooter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);

        // This is the starting activity for the app, so show the app name and version.
        setTitle(getString(R.string.app_name) + " " + getString(R.string.app_version));

        setContentView(R.layout.activity_user_login);
        UserLoginFragment fragment =
                (UserLoginFragment)getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_user_login);
        mController = new UserLoginController(
                App.getUserManager(),
                new EventBusWrapper(EventBus.getDefault()),
                mTroubleshooter,
                new MyUi(),
                fragment.getFragmentUi());

        // TODO/refactor: Consider refactoring out some common code between here and tent selection.
        mSyncFailedDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.sync_failed_dialog_title))
                .setMessage(R.string.user_sync_failed_dialog_message)
                .setNegativeButton(
                        R.string.sync_failed_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(
                                        UserLoginActivity.this,SettingsActivity.class));
                            }
                        })
                .setPositiveButton(
                        R.string.sync_failed_retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mController.onSyncRetry();
                            }
                        })
                .create();
    }

    /**
     * Returns the {@link UserLoginController} used by this activity. After onCreate, this should
     * never be null.
     */
    public UserLoginController getUserLoginController() {
        return mController;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login, menu);

        menu.findItem(R.id.action_add_user).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mController.onAddUserPressed();
                        return true;
                    }
                }
        );

        menu.findItem(R.id.settings).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mController.onSettingsPressed();
                        return true;
                    }
                }
        );

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mController.init();
    }

    @Override
    protected void onPause() {
        mController.suspend();
        super.onPause();
    }

    private final class MyUi implements UserLoginController.Ui {
        @Override
        public void showAddNewUserDialog() {
            FragmentManager fm = getSupportFragmentManager();
            AddNewUserDialogFragment dialogFragment =
                    AddNewUserDialogFragment.newInstance(mController.getDialogUi());
            dialogFragment.show(fm, null);
        }

        @Override
        public void showSettings() {
            Intent settingsIntent =
                    new Intent(UserLoginActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        @Override
        public void showErrorToast(int stringResourceId) {
            BigToast.show(UserLoginActivity.this, getString(stringResourceId));
        }

        @Override
        public void showSyncFailedDialog(boolean show) {
            if (mSyncFailedDialog == null) {
                return;
            }

            if (mSyncFailedDialog.isShowing() != show) {
                if (show) {
                    mSyncFailedDialog.show();
                } else {
                    mSyncFailedDialog.hide();
                }
            }
        }

        @Override
        public void showTentSelectionScreen() {
            startActivity(new Intent(UserLoginActivity.this, LocationSelectionActivity.class));
        }
    }
}
