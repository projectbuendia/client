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
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
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

    private LinearLayout mWrapperView;
    private FrameLayout mInnerContent;
    private FrameLayout mStatusContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.getInstance().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        EventBus.getDefault().registerSticky(this);
        App.getInstance().getHealthMonitor().start();
        Utils.logEvent("resumed_activity", "class", this.getClass().getSimpleName());
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
        App.getInstance().getHealthMonitor().stop();
    }

    @Override
    public void setContentView(int layoutResId) {
        initializeWrapperView();

        mInnerContent.removeAllViews();
        getLayoutInflater().inflate(layoutResId, mInnerContent);
    }

    @Override
    public void setContentView(View view) {
        initializeWrapperView();

        mInnerContent.removeAllViews();
        mInnerContent.addView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        initializeWrapperView();

        mInnerContent.removeAllViews();
        mInnerContent.addView(view, params);
    }

    /**
     * Sets the view to be shown in the status bar.
     *
     * <p>The status bar is always a fixed height (80dp). Any view passed to this method should fit
     * that height.
     */
    public void setStatusView(View view) {
        initializeWrapperView();

        mStatusContent.removeAllViews();

        if (view != null) {
            mStatusContent.addView(view);
        }
    }

    /** Sets the visibility of the status bar. */
    public void setStatusVisibility(int visibility) {
        mStatusContent.setVisibility(visibility);
    }

    /** Gets the visibility of the status bar. */
    public int getStatusVisibility() {
        return mStatusContent.getVisibility();
    }

    /** Called when the set of troubleshooting actions changes. */
    public void onEventMainThread(TroubleshootingActionsChangedEvent event) {
        if (event.actions.isEmpty()) {
            setStatusView(null);
            setStatusVisibility(View.GONE);

            return;
        }

        TroubleshootingAction troubleshootingAction = event.actions.iterator().next();

        View view = getLayoutInflater().inflate(R.layout.view_status_bar_default, null);
        final TextView message = (TextView) view.findViewById(R.id.status_bar_default_message);
        final TextView action = (TextView) view.findViewById(R.id.status_bar_default_action);

        switch (troubleshootingAction) {
            case ENABLE_WIFI:
                message.setText(R.string.troubleshoot_wifi_disabled);
                action.setText(R.string.troubleshoot_wifi_disabled_action_enable);
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        action.setEnabled(false);
                        ((WifiManager) getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true);
                    }
                });
                break;
            case CONNECT_WIFI:
                message.setText(R.string.troubleshoot_wifi_disconnected);
                action.setText(R.string.troubleshoot_wifi_disconnected_action_connect);
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        action.setEnabled(false);
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                });
                break;
            case CHECK_SERVER_AUTH:
                message.setText(R.string.troubleshoot_server_auth);
                action.setText(R.string.troubleshoot_server_auth_action_check);
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        action.setEnabled(false);
                        SettingsActivity.start(BaseActivity.this);
                    }
                });
                break;
            case CHECK_SERVER_CONFIGURATION:
                message.setText(R.string.troubleshoot_server_address);
                action.setText(R.string.troubleshoot_server_address_action_check);
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        action.setEnabled(false);
                        SettingsActivity.start(BaseActivity.this);
                    }
                });
                break;
            case CHECK_SERVER_REACHABILITY:
                message.setText(R.string.troubleshoot_server_unreachable);
                action.setText(R.string.troubleshoot_action_more_info);
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO: Display the actual server URL that couldn't be reached in
                        // this message. This will require that injection be hooked up through to
                        // this inner class, which may be complicated.
                        showMoreInfoDialog(
                                action,
                                getString(R.string.troubleshoot_server_unreachable),
                                getString(R.string.troubleshoot_server_unreachable_details),
                                true);
                    }
                });
                break;
            case CHECK_SERVER_SETUP:
                message.setText(R.string.troubleshoot_server_unstable);
                action.setText(R.string.troubleshoot_action_more_info);
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO: Display the actual server URL that couldn't be reached in
                        // this message. This will require that injection be hooked up through to
                        // this inner class, which may be complicated.
                        showMoreInfoDialog(
                                action,
                                getString(R.string.troubleshoot_server_unstable),
                                getString(R.string.troubleshoot_server_unstable_details),
                                false);
                    }
                });
                break;
            case CHECK_SERVER_STATUS:
                message.setText(R.string.troubleshoot_server_not_responding);
                action.setText(R.string.troubleshoot_action_more_info);
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO: Display the actual server URL that couldn't be reached in
                        // this message. This will require that injection be hooked up through to
                        // this inner class, which may be complicated.
                        showMoreInfoDialog(
                                action,
                                getString(R.string.troubleshoot_server_not_responding),
                                getString(R.string.troubleshoot_server_not_responding_details),
                                false);
                    }
                });
                break;
            case CHECK_PACKAGE_SERVER_REACHABILITY:
                message.setText(R.string.troubleshoot_package_server_unreachable);
                action.setText(R.string.troubleshoot_action_more_info);
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMoreInfoDialog(
                                action,
                                getString(R.string.troubleshoot_package_server_unreachable),
                                getString(R.string.troubleshoot_update_server_unreachable_details),
                                true);
                    }
                });
                break;
            case CHECK_PACKAGE_SERVER_CONFIGURATION:
                message.setText(R.string.troubleshoot_package_server_misconfigured);
                action.setText(R.string.troubleshoot_action_more_info);
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMoreInfoDialog(
                                action,
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

        setStatusView(view);
        setStatusVisibility(View.VISIBLE);
    }

    private void showMoreInfoDialog(final View triggeringView, String title, String message,
                                    boolean includeSettingsButton) {
        triggeringView.setEnabled(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        triggeringView.setEnabled(true);
                    }
                });
        if (includeSettingsButton) {
            builder.setPositiveButton(R.string.troubleshoot_action_check_settings,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SettingsActivity.start(BaseActivity.this);
                        }
                    });
        }
        builder.show();
    }

    private void initializeWrapperView() {
        if (mWrapperView != null) {
            return;
        }

        mWrapperView =
                (LinearLayout) getLayoutInflater().inflate(R.layout.view_status_wrapper, null);
        super.setContentView(mWrapperView);

        mInnerContent =
                (FrameLayout) mWrapperView.findViewById(R.id.status_wrapper_inner_content);
        mStatusContent =
                (FrameLayout) mWrapperView.findViewById(R.id.status_wrapper_status_content);
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

        @Override
        public void showUpdateAvailableForDownload(AvailableUpdateInfo updateInfo) {
            mUpdateMessage.setText(R.string.snackbar_update_available);
            mUpdateAction.setText(R.string.snackbar_action_download);
            setStatusView(mStatusView);
            setStatusVisibility(View.VISIBLE);

            mUpdateAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.logEvent("download_update_button_pressed");
                    setStatusVisibility(View.GONE);
                    EventBus.getDefault().post(new DownloadRequestedEvent());
                }
            });
        }

        @Override
        public void showUpdateReadyToInstall(DownloadedUpdateInfo updateInfo) {
            mUpdateMessage.setText(R.string.snackbar_update_downloaded);
            mUpdateAction.setText(R.string.snackbar_action_install);
            setStatusView(mStatusView);
            setStatusVisibility(View.VISIBLE);

            mUpdateAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.logEvent("install_update_button_pressed");
                    setStatusVisibility(View.GONE);
                    EventBus.getDefault().post(new InstallationRequestedEvent());
                }
            });
        }

        @Override
        public void hideSoftwareUpdateNotifications() {
            setStatusVisibility(View.GONE);
        }
    }

    /** The user has requested a download of the last known available software update. */
    public static class DownloadRequestedEvent { }

    /** The user has requested installation of the last downloaded software update. */
    public static class InstallationRequestedEvent { }
}

