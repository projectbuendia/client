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

package org.projectbuendia.client.ui.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.diagnostics.Troubleshooter;
import org.projectbuendia.client.ui.BaseActivity;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.ui.SettingsActivity;
import org.projectbuendia.client.ui.SnackBar;
import org.projectbuendia.client.ui.dialogs.NewUserDialogFragment;
import org.projectbuendia.client.ui.lists.LocationListActivity;
import org.projectbuendia.client.utils.EventBusWrapper;
import org.projectbuendia.client.utils.Utils;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * {@link BaseActivity} where users log in by selecting their name from a list.
 * This is the starting activity for the app.
 */
public class LoginActivity extends BaseActivity {
    private LoginController mController;
    private AlertDialog mSyncFailedDialog;
    @Inject Troubleshooter mTroubleshooter;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);

        // This is the starting activity for the app, so show the app name and version.
        setTitle(getString(R.string.app_name) + " " + getString(R.string.app_version));

        setContentView(R.layout.activity_user_login);
        LoginFragment fragment = (LoginFragment)
            getSupportFragmentManager().findFragmentById(R.id.fragment_user_login);
        mController = new LoginController(
            App.getUserManager(),
            new EventBusWrapper(EventBus.getDefault()),
            mTroubleshooter,
            new Ui(),
            fragment.getFragmentUi());

        // TODO/cleanup: Consider factoring out some common code between here and tent selection.
        mSyncFailedDialog = new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getString(R.string.sync_failed_dialog_title))
            .setMessage(R.string.user_sync_failed_dialog_message)
            .setNegativeButton(R.string.sync_failed_settings, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    SettingsActivity.start(LoginActivity.this);
                }
            })
            .setPositiveButton(R.string.sync_failed_retry, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    mController.onSyncRetry();
                }
            })
            .create();
    }

    /**
     * Returns the {@link LoginController} used by this activity. After onCreate, this should
     * never be null.
     */
    public LoginController getUserLoginController() {
        return mController;
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login, menu);

        menu.findItem(R.id.action_new_user).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {

                @Override public boolean onMenuItemClick(MenuItem item) {
                    mController.onAddUserPressed();
                    return true;
                }
            }
        );

        menu.findItem(R.id.settings).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {

                @Override public boolean onMenuItemClick(MenuItem item) {
                    mController.onSettingsPressed();
                    return true;
                }
            }
        );

        // This code bellow is only for testing purposes and will be removed
        final Activity context = this;
        menu.findItem(R.id.snackbar_show).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {
                @Override public boolean onMenuItemClick(MenuItem item) {
                    snackBar("Simple Message");
                    return true;
                }
            }
        );

        menu.findItem(R.id.snackbar_hide).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {
                @Override public boolean onMenuItemClick(MenuItem item) {
                    snackBar("Wifi is disabled", "Enable", new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            Toast.makeText(
                                context,
                                "Enable WIFI NOW!",
                                Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                    return true;
                }
            }
        );
        menu.findItem(R.id.snackbar_priority).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {
                @Override public boolean onMenuItemClick(MenuItem item) {
                    snackBar("I have priority", "Selfish", new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            Toast.makeText(
                                context,
                                "Get out of my way! Me first!",
                                Toast.LENGTH_LONG
                            ).show();
                        }
                    }, 1);
                    return true;
                }
            }
        );
        menu.findItem(R.id.snackbar_dismiss).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {
                @Override public boolean onMenuItemClick(MenuItem item) {
                    snackBar("Programmatically Dismissible", "How?", new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            Toast.makeText(
                                context,
                                "Need to call dismiss() method.",
                                Toast.LENGTH_LONG
                            ).show();
                        }
                    }, 2, false);
                    return true;
                }
            }
        );
        menu.findItem(R.id.snackbar_timer).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {
                @Override public boolean onMenuItemClick(MenuItem item) {
                    snackBar("I'll be gone in 20 secs", "Cool", new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            Toast.makeText(
                                context,
                                "Yeah, Dude!",
                                Toast.LENGTH_LONG
                            ).show();
                        }
                    }, 3, false, 20);
                    return true;
                }
            }
        );
        return true;
    }

    @Override protected void onResume() {
        super.onResume();
        mController.init();
    }

    @Override protected void onPause() {
        mController.suspend();
        super.onPause();
    }

    private final class Ui implements LoginController.Ui {
        @Override public void showAddNewUserDialog() {
            NewUserDialogFragment.newInstance(mController.getDialogUi())
                .show(getSupportFragmentManager(), null);
        }

        @Override public void showSettings() {
            SettingsActivity.start(LoginActivity.this);
        }

        @Override public void showErrorToast(int stringResourceId) {
            BigToast.show(LoginActivity.this, getString(stringResourceId));
        }

        @Override public void showSyncFailedDialog(boolean show) {
            Utils.showDialogIf(mSyncFailedDialog, show);
        }

        @Override public void showTentSelectionScreen() {
            LocationListActivity.start(LoginActivity.this);
        }
    }
}
