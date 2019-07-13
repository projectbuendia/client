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

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.joanzapata.iconify.Icon;
import com.joanzapata.iconify.IconDrawable;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.diagnostics.HealthIssue;
import org.projectbuendia.client.diagnostics.TroubleshootingAction;
import org.projectbuendia.client.events.diagnostics.TroubleshootingActionsChangedEvent;
import org.projectbuendia.client.updater.AvailableUpdateInfo;
import org.projectbuendia.client.updater.DownloadedUpdateInfo;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * An abstract {@link FragmentActivity} that is the base for all activities, providing a "content
 * view" that can be populated by implementing classes and a "status view" that can be used for
 * troubleshooting and status messages.
 */
public abstract class BaseActivity extends FragmentActivity {
    private static final Logger LOG = Logger.create();
    private static final double PHI = (Math.sqrt(5) + 1)/2; // golden ratio
    private static final double STEP_FACTOR = Math.pow(PHI, 0.25); // each step up/down scales this much
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
        startActivity(getIntent(), ActivityOptions.makeCustomAnimation(getApplicationContext(), 0, 0).toBundle());
    }

    public void setMenuBarIcon(MenuItem item, Icon icon) {
        item.setIcon(createIcon(icon, R.color.menubar_icon));
    }

    public IconDrawable createIcon(Icon icon, int colorRes) {
        int iconSizePx = (int) (36 * getResources().getDisplayMetrics().scaledDensity);
        return new IconDrawable(this, icon).color(getResources().getColor(colorRes)).sizePx(iconSizePx);
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

    /**
     * Adds a message to the SnackBar. Priority defaults to 999.
     * @see "SnackBar Documentation." {@link SnackBar#message(int)}
     */
    public void snackBar(@StringRes int message) {
        snackBar.message(message);
    }

    /**
     * Adds a message to the SnackBar with informed priority.
     * @see "SnackBar Documentation." {@link SnackBar#message(int, int)}
     */
    public void snackBar(@StringRes int message, int priority) {
        snackBar.message(message, priority);
    }

    /**
     * Adds a message to the SnackBar. Priority defaults to 999.
     * @see "SnackBar Documentation." {@link SnackBar#message(int, int, View.OnClickListener, int)}
     */
    public void snackBar(@StringRes int message, @StringRes int actionMessage, View
        .OnClickListener listener) {
        snackBar.message(message, actionMessage, listener, 999);
    }

    /**
     * Adds a message to the SnackBar with informed priority.
     * @see "SnackBar Documentation." {@link SnackBar#message(int, int, View.OnClickListener, int)}
     */
    public void snackBar(@StringRes int message, @StringRes int actionMessage, View
        .OnClickListener listener, int priority) {
        snackBar.message(message, actionMessage, listener, priority);
    }

    /**
     * Adds a message to the SnackBar with all parameters except for secondsToTimeout.
     * @see "SnackBar Documentation."
     * {@link SnackBar#message(int, int, View.OnClickListener, int, boolean, int)}
     */
    public void snackBar(@StringRes int message, @StringRes int actionMessage, View
        .OnClickListener actionOnClick, int priority, boolean isDismissible) {
        snackBar.message(message, actionMessage, actionOnClick, priority, isDismissible, 0);
    }

    /**
     * Adds a message to the SnackBar with all parameters.
     * @see "SnackBar Documentation."
     * {@link SnackBar#message(int, int, View.OnClickListener, int, boolean, int)}
     */
    public void snackBar(@StringRes int message, @StringRes int actionMessage, View
        .OnClickListener actionOnClick, int priority, boolean isDismissible, int secondsToTimeOut) {
        snackBar.message(message, actionMessage, actionOnClick, priority, isDismissible,
            secondsToTimeOut);
    }

    /**
     * Use it to programmatically dismiss a SnackBar message.
     * @param id The @StringRes for the message.
     */
    public void snackBarDismiss(@StringRes int id) {
        snackBar.dismiss(id);
    }

    /**
     * Programmatically dismiss multiple messages at once
     * @param id a @StringRes message Array
     */
    public void snackBarDismiss(@StringRes int[] id) {
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
        if (event.solvedIssue != null) {
            displayProblemSolvedMessage(event.solvedIssue);
        }

        if (event.actions.isEmpty()) {
            return;
        }

        for (TroubleshootingAction troubleshootingAction: event.actions) {
            switch (troubleshootingAction) {
                case ENABLE_WIFI:
                    snackBar(R.string.troubleshoot_wifi_disabled,
                        R.string.troubleshoot_wifi_disabled_action_enable,
                        new View.OnClickListener() {
                            @Override public void onClick(View view) {
                                ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE)).setWifiEnabled
                                    (true);
                            }
                        }, 995, false);
                    break;
                case CONNECT_WIFI:
                    snackBar(R.string.troubleshoot_wifi_disconnected,
                        R.string.troubleshoot_wifi_disconnected_action_connect,
                        new View.OnClickListener() {
                            @Override public void onClick(View view) {
                                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            }
                        }, 996, false);
                    break;
                case CHECK_SERVER_AUTH:
                    snackBar(R.string.troubleshoot_server_auth,
                        R.string.troubleshoot_server_auth_action_check,
                        new View.OnClickListener() {
                            @Override public void onClick(View view) {
                                SettingsActivity.start(BaseActivity.this);
                            }
                        }, 999, false);
                    break;
                case CHECK_SERVER_CONFIGURATION:
                    snackBar(R.string.troubleshoot_server_address,
                        R.string.troubleshoot_server_address_action_check,
                        new View.OnClickListener() {
                            @Override public void onClick(View view) {
                                SettingsActivity.start(BaseActivity.this);
                            }
                        }, 999, false);
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
                        }, 997, false);
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
                        }, 999, false);
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
                        }, 999, false);
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
                        }, 998, false);
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
                        }, 999, false);
                    break;
                default:
                    LOG.w("Troubleshooting action '%1$s' is unknown.", troubleshootingAction);
                    return;
            }
        }
    }

    private void displayProblemSolvedMessage(HealthIssue solvedIssue) {
        // The troubleShootingMessages Map have the issue as the key and the TroubleshootingMessage
        // object as it's value.
        Map<HealthIssue, TroubleshootingMessage> troubleshootingMessages = new HashMap<>();

        troubleshootingMessages.put(HealthIssue.WIFI_DISABLED,
            new TroubleshootingMessage(
                R.string.troubleshoot_wifi_disabled,
                R.string.troubleshoot_wifi_disabled_solved,
                10
        ));
        troubleshootingMessages.put(HealthIssue.WIFI_NOT_CONNECTED,
            new TroubleshootingMessage(
                R.string.troubleshoot_wifi_disconnected,
                R.string.troubleshoot_wifi_disconnected_solved,
                10
        ));
        troubleshootingMessages.put(HealthIssue.SERVER_AUTHENTICATION_ISSUE,
            new TroubleshootingMessage(
                R.string.troubleshoot_server_auth,
                R.string.troubleshoot_server_auth_solved,
                10
        ));
        troubleshootingMessages.put(HealthIssue.SERVER_CONFIGURATION_INVALID,
            new TroubleshootingMessage(
                R.string.troubleshoot_server_address,
                R.string.troubleshoot_server_address_solved,
                10
        ));
        troubleshootingMessages.put(HealthIssue.SERVER_HOST_UNREACHABLE,
            new TroubleshootingMessage(
                R.string.troubleshoot_server_unreachable,
                R.string.troubleshoot_server_unreachable_solved,
                10
        ));
        troubleshootingMessages.put(HealthIssue.SERVER_INTERNAL_ISSUE,
            new TroubleshootingMessage(
                R.string.troubleshoot_server_unstable,
                R.string.troubleshoot_server_unstable_solved,
                10
        ));
        troubleshootingMessages.put(HealthIssue.SERVER_NOT_RESPONDING,
            new TroubleshootingMessage(
                R.string.troubleshoot_server_not_responding,
                R.string.troubleshoot_server_not_responding_solved,
                10
        ));
        troubleshootingMessages.put(HealthIssue.PACKAGE_SERVER_HOST_UNREACHABLE,
            new TroubleshootingMessage(
                R.string.troubleshoot_package_server_unreachable,
                R.string.troubleshoot_package_server_unreachable_solved,
                5
        ));
        troubleshootingMessages.put(HealthIssue.PACKAGE_SERVER_INDEX_NOT_FOUND,
            new TroubleshootingMessage(
                R.string.troubleshoot_package_server_misconfigured,
                R.string.troubleshoot_package_server_misconfigured_solved,
                10
        ));

        TroubleshootingMessage messages = troubleshootingMessages.get(solvedIssue);

        if (messages != null) {
            SnackBar.Message snackBarMessage = snackBar.getMessage(messages.messageId);
            if (snackBarMessage != null) {
                snackBar.dismiss(snackBarMessage.key);
                snackBar.message(messages.resolvedMessageId, 0, null, 994, true, messages.timeout);
            }
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

        public UpdateNotificationUi() {}

        @Override public void showUpdateAvailableForDownload(AvailableUpdateInfo updateInfo) {
            snackBar(R.string.snackbar_update_available,
                R.string.snackbar_action_download,
                new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        Utils.logEvent("download_update_button_pressed");
                        //TODO: programatically dismiss the snackbar message
                        EventBus.getDefault().post(new DownloadRequestedEvent());
                    }
                });
        }

        @Override public void showUpdateReadyToInstall(DownloadedUpdateInfo updateInfo) {
            snackBar(R.string.snackbar_update_downloaded,
                R.string.snackbar_action_install,
                new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        Utils.logEvent("install_update_button_pressed");
                        //TODO: programatically dismiss the snackbar message
                        EventBus.getDefault().post(new InstallationRequestedEvent());
                    }
                });
        }

        @Override public void hideSoftwareUpdateNotifications() {
        }
    }

    /**
     * The TroubleshootingMessage object relates the error message with the solved message and
     * for how many seconds the solved message is displayed.
     */
    private static class TroubleshootingMessage {
        @StringRes public final int messageId;
        @StringRes public final int resolvedMessageId;
        public final int timeout;

        /**
         *
         * @param messageId The message string id triggered by the issue.
         * @param resolvedMessageId The new message string id (solved message).
         * @param timeout The timeout count (in seconds) for the solved message.
         */
        public TroubleshootingMessage(@StringRes int messageId, @StringRes int resolvedMessageId,
                                      int timeout) {
            this.messageId = messageId;
            this.resolvedMessageId = resolvedMessageId;
            this.timeout = timeout;
        }
    }
}

