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

package org.projectbuendia.client.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.diagnostics.TroubleshootingAction;
import org.projectbuendia.client.events.diagnostics.TroubleshootingActionsChangedEvent;
import org.projectbuendia.client.updater.AvailableUpdateInfo;
import org.projectbuendia.client.updater.DownloadedUpdateInfo;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import de.greenrobot.event.EventBus;

/**
 * An abstract {@link FragmentActivity} that is the base for all activities, providing a "content
 * view" that can be populated by implementing classes and a "status view" that can be used for
 * troubleshooting and status messages.
 */
public abstract class BaseActivity extends FragmentActivity {
    private static final Logger LOG = Logger.create();
    private static final double PHI = (Math.sqrt(5) + 1)/2; // golden ratio
    private static final double STEP_FACTOR = Math.sqrt(PHI); // each step up/down scales this much
    private static final long MIN_STEP = -2;
    private static final long MAX_STEP = 2;

    // TODO: Store sScaleStep in an app preference.
    private static long sScaleStep = 0; // app-wide scale step, selected by user
    private Long pausedScaleStep = null; // this activity's scale step when last paused
    private LinearLayout mWrapperView;
    private FrameLayout mInnerContent;
    private SnackBar snackBar;

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    adjustFontScale(1);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    adjustFontScale(-1);
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public void adjustFontScale(int delta) {
        long newScaleStep = Math.max(MIN_STEP, Math.min(MAX_STEP, sScaleStep + delta));
        if (newScaleStep != sScaleStep) {
            restartWithFontScale(newScaleStep);
        }
    }

    public void restartWithFontScale(long newScaleStep) {
        Configuration config = getResources().getConfiguration();
        config.fontScale = (float) Math.pow(STEP_FACTOR, newScaleStep);
        sScaleStep = newScaleStep;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        finish();
        startActivity(getIntent());
    }

    @Override public void setContentView(int layoutResId) {
        initializeWrapperView();

        mInnerContent.removeAllViews();
        getLayoutInflater().inflate(layoutResId, mInnerContent);
    }

    private void initializeWrapperView() {
        if (mWrapperView != null) return;

        mWrapperView =
            (LinearLayout) getLayoutInflater().inflate(R.layout.view_status_wrapper, null);
        super.setContentView(mWrapperView);

        mInnerContent =
            (FrameLayout) mWrapperView.findViewById(R.id.status_wrapper_inner_content);
    }

    private void initializeSnackBar() {
        if ((mWrapperView != null) && (snackBar == null)) {
            snackBar = new SnackBar(mWrapperView);
        }
    }

    // Methods for Displaying SnackBar Messages
    public int snackBar(String message) {
        return snackBar.message(message);
    }

    public int snackBar(String message, int priority) {
        return snackBar.message(message, priority);
    }

    public int snackBar(String message, String actionMessage, View.OnClickListener listener) {
        return snackBar.message(message, actionMessage, listener, 999);
    }

    public int snackBar(int resourceIdMessage, int resourceIdActionMessage, View.OnClickListener
        listener) {
        return snackBar.message(getResources().getString(resourceIdMessage), getResources()
            .getString(resourceIdActionMessage), listener, 999);
    }

    public int snackBar(String message, String actionMessage, View.OnClickListener listener,
                        int priority) {
        return snackBar.message(message, actionMessage, listener, priority);
    }

    public int snackBar(String message, String actionMessage, View.OnClickListener actionOnClick,
                       int priority, boolean isDismissible){
        return snackBar.message(message, actionMessage, actionOnClick, priority, isDismissible, 0);
    }

    public int snackBar(String message, String actionMessage, View.OnClickListener actionOnClick,
                       int priority, boolean isDismissible, int secondsToTimeOut){
        return snackBar.message(message, actionMessage, actionOnClick, priority,
            isDismissible, secondsToTimeOut);
    }

    // Use it to programmatically dismiss a SnackBar message.
    public void snackBarDismiss(int id) {
        snackBar.dismiss(id);
    }

    @Override public void setContentView(View view) {
        initializeWrapperView();

        mInnerContent.removeAllViews();
        mInnerContent.addView(view);
    }

    @Override public void setContentView(View view, ViewGroup.LayoutParams params) {
        initializeWrapperView();

        mInnerContent.removeAllViews();
        mInnerContent.addView(view, params);
    }

    /** Called when the set of troubleshooting actions changes. */
    public void onEventMainThread(TroubleshootingActionsChangedEvent event) {
        if (event.actions.isEmpty()) {
            return;
        }

        TroubleshootingAction troubleshootingAction = event.actions.iterator().next();

        switch (troubleshootingAction) {
            case ENABLE_WIFI:
                snackBar(R.string.troubleshoot_wifi_disabled,
                    R.string.troubleshoot_wifi_disabled_action_enable,
                    new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            ((WifiManager) getSystemService(Context.WIFI_SERVICE)).setWifiEnabled
                                (true);
                        }
                    });
                break;
            case CONNECT_WIFI:
                snackBar(R.string.troubleshoot_wifi_disconnected,
                    R.string.troubleshoot_wifi_disconnected_action_connect,
                    new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    });
                break;
            case CHECK_SERVER_AUTH:
                snackBar(R.string.troubleshoot_server_auth,
                    R.string.troubleshoot_server_auth_action_check,
                    new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            SettingsActivity.start(BaseActivity.this);
                        }
                    });
                break;
            case CHECK_SERVER_CONFIGURATION:
                snackBar(R.string.troubleshoot_server_address,
                    R.string.troubleshoot_server_address_action_check,
                    new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            SettingsActivity.start(BaseActivity.this);
                        }
                    });
                break;
            case CHECK_SERVER_REACHABILITY:
                snackBar(R.string.troubleshoot_server_unreachable,
                    R.string.troubleshoot_action_more_info,
                    new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            // TODO: Display the actual server URL that couldn't be reached in
                            // this message. This will require that injection be hooked up
                            // through to
                            // this inner class, which may be complicated.
                            showMoreInfoDialog(
                                getString(R.string.troubleshoot_server_unreachable),
                                getString(R.string.troubleshoot_server_unreachable_details),
                                true);
                        }
                    });
                break;
            case CHECK_SERVER_SETUP:
                snackBar(R.string.troubleshoot_server_unstable,
                    R.string.troubleshoot_action_more_info,
                    new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            // TODO: Display the actual server URL that couldn't be reached in
                            // this message. This will require that injection be hooked up
                            // through to
                            // this inner class, which may be complicated.
                            showMoreInfoDialog(
                                getString(R.string.troubleshoot_server_unstable),
                                getString(R.string.troubleshoot_server_unstable_details),
                                false);
                        }
                    });
                break;
            case CHECK_SERVER_STATUS:
                snackBar(R.string.troubleshoot_server_not_responding,
                    R.string.troubleshoot_action_more_info,
                    new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            // TODO: Display the actual server URL that couldn't be reached in
                            // this message. This will require that injection be hooked up
                            // through to
                            // this inner class, which may be complicated.
                            showMoreInfoDialog(
                                getString(R.string.troubleshoot_server_not_responding),
                                getString(R.string.troubleshoot_server_not_responding_details),
                                false);
                        }
                    });
                break;
            case CHECK_PACKAGE_SERVER_REACHABILITY:
                snackBar(R.string.troubleshoot_package_server_unreachable,
                    R.string.troubleshoot_action_more_info,
                    new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            showMoreInfoDialog(
                                getString(R.string.troubleshoot_package_server_unreachable),
                                getString(R.string.troubleshoot_update_server_unreachable_details),
                                true);
                        }
                    });
                break;
            case CHECK_PACKAGE_SERVER_CONFIGURATION:
                snackBar(R.string.troubleshoot_package_server_misconfigured,
                    R.string.troubleshoot_action_more_info,
                    new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            showMoreInfoDialog(
                                getString(R.string.troubleshoot_package_server_misconfigured),
                                getString(
                                    R.string.troubleshoot_update_server_misconfigured_details),
                                true);
                        }
                    });
                break;
            default:
                LOG.w("Troubleshooting action '%1$s' is unknown.", troubleshootingAction);
                return;
        }
    }

    private void showMoreInfoDialog(String title, String message,
                                    boolean includeSettingsButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(title)
            .setMessage(message)
            .setNeutralButton(android.R.string.ok, null);
        if (includeSettingsButton) {
            builder.setPositiveButton(R.string.troubleshoot_action_check_settings,
                new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        SettingsActivity.start(BaseActivity.this);
                    }
                });
        }
        builder.show();
    }

    /** The user has requested a download of the last known available software update. */
    public static class DownloadRequestedEvent {
    }

    /** The user has requested installation of the last downloaded software update. */
    public static class InstallationRequestedEvent {
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.getInstance().inject(this);
    }

    @Override protected void onResume() {
        super.onResume();
        initializeSnackBar();
        if (pausedScaleStep != null && sScaleStep != pausedScaleStep) {
            // If the font scale was changed while this activity was paused, force a refresh.
            restartWithFontScale(sScaleStep);
        }
        EventBus.getDefault().registerSticky(this);
        App.getInstance().getHealthMonitor().start();
        Utils.logEvent("resumed_activity", "class", this.getClass().getSimpleName());
    }

    @Override protected void onPause() {
        EventBus.getDefault().unregister(this);
        App.getInstance().getHealthMonitor().stop();
        pausedScaleStep = sScaleStep;

        super.onPause();
    }

    protected class UpdateNotificationUi implements UpdateNotificationController.Ui {

        final View mStatusView;
        final TextView mUpdateMessage;
        final TextView mUpdateAction;

        public UpdateNotificationUi() {
            mStatusView = getLayoutInflater().inflate(R.layout.view_status_bar_default, null);
            mUpdateMessage = (TextView) mStatusView.findViewById(R.id.status_bar_default_message);
            mUpdateAction = (TextView) mStatusView.findViewById(R.id.status_bar_default_action);
        }

        @Override public void showUpdateAvailableForDownload(AvailableUpdateInfo updateInfo) {
            mUpdateMessage.setText(R.string.snackbar_update_available);
            mUpdateAction.setText(R.string.snackbar_action_download);
            //setStatusView(mStatusView);
            //setStatusVisibility(View.VISIBLE);

            mUpdateAction.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    Utils.logEvent("download_update_button_pressed");
                    //setStatusVisibility(View.GONE);
                    EventBus.getDefault().post(new DownloadRequestedEvent());
                }
            });
        }

        @Override public void showUpdateReadyToInstall(DownloadedUpdateInfo updateInfo) {
            mUpdateMessage.setText(R.string.snackbar_update_downloaded);
            mUpdateAction.setText(R.string.snackbar_action_install);
            //setStatusView(mStatusView);
            //setStatusVisibility(View.VISIBLE);

            mUpdateAction.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    Utils.logEvent("install_update_button_pressed");
                    //setStatusVisibility(View.GONE);
                    EventBus.getDefault().post(new InstallationRequestedEvent());
                }
            });
        }

        @Override public void hideSoftwareUpdateNotifications() {
            //setStatusVisibility(View.GONE);
        }
    }
}

