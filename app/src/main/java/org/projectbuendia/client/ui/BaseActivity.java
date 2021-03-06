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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.joanzapata.iconify.Icon;
import com.joanzapata.iconify.IconDrawable;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.diagnostics.HealthIssue;
import org.projectbuendia.client.diagnostics.TroubleshootingAction;
import org.projectbuendia.client.events.diagnostics.TroubleshootingActionsChangedEvent;
import org.projectbuendia.client.receivers.BatteryWatcher;
import org.projectbuendia.client.ui.chart.ChartRenderer;
import org.projectbuendia.client.updater.AvailableUpdateInfo;
import org.projectbuendia.client.updater.DownloadedUpdateInfo;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.utils.Utils.eq;

/**
 * An abstract {@link FragmentActivity} that is the base for all activities
 * except SettingsActivity, providing a "content view" that is populated by
 * subclasses and a SnackBar for troubleshooting and status messages.
 */
public abstract class BaseActivity extends FragmentActivity {
    private static final Logger LOG = Logger.create();
    private static final double PHI = (Math.sqrt(5) + 1)/2; // golden ratio
    private static final double STEP_FACTOR = Math.sqrt(Math.sqrt(PHI)); // each step up/down scales this much
    private static final long MIN_STEP = -4;
    private static final long MAX_STEP = 2;

    protected ContextUtils u;
    protected AppSettings settings;
    protected Instant idleStartTime = null;
    protected Handler tickHandler = new Handler();
    protected Runnable tick = () -> {
        onTick();
        tickHandler.postDelayed(this.tick, 1000);
    };
    protected final BatteryWatcher batteryWatcher = new BatteryWatcher();
    protected boolean mIsCreated = false; // activity creation completed successfully

    private static long sScaleStep = 0; // app-wide scale step, selected by user
    private Long pausedScaleStep = null; // this activity's scale step when last paused
    private LinearLayout mWrapperView;
    private FrameLayout mInnerContent;
    private SnackBar snackBar;
    private Locale initialLocale; // for restarting when locale has changed
    private Set<String> openDialogTypes;
    protected UpdateCheckController updateCheckController = null;

    // NOTE: Don't override this method; override onCreateImpl() instead.
    @Override protected final void onCreate(Bundle state) {
        super.onCreate(state);
        mIsCreated = onCreateImpl(state);
    }

    /**
     * Performs setup operations for this activity and returns true if creation
     * should continue.  All overrides of this method should start with:
     *     if (!super.onCreateImpl(state)) return false;
     */
    protected boolean onCreateImpl(Bundle state) {
        settings = App.getSettings();
        initialLocale = Locale.getDefault();
        openDialogTypes = new HashSet<>();
        updateCheckController =
            new UpdateCheckController(new UpdateNotificationUi());

        if (!settings.isAuthorized() && !(this instanceof AuthorizationActivity)) {
            Utils.jumpToActivity(this, AuthorizationActivity.class);
            return false;
        }
        App.inject(this);
        ChartRenderer.backgroundCompileTemplate();
        return true;
    }

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(App.applyLocaleSetting(base));
        u = ContextUtils.from(this);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!eq(Locale.getDefault(), initialLocale)) Utils.restartActivity(this);
        initializeSnackBar();
        if (pausedScaleStep != null && sScaleStep != pausedScaleStep) {
            // If the font scale was changed while this activity was paused, force a refresh.
            restartWithFontScale(sScaleStep);
        }
        EventBus.getDefault().registerSticky(this);
        registerReceiver(batteryWatcher, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        App.getHealthMonitor().start();
        App.getSyncManager().applyPeriodicSyncSettings();
        App.getHealthEventBus().post(
            App.getSettings().getPeriodicSyncDisabled() ?
                HealthIssue.PERIODIC_SYNC_DISABLED.discovered :
                HealthIssue.PERIODIC_SYNC_DISABLED.resolved
        );
        Utils.logEvent("resumed_activity", "class", this.getClass().getSimpleName());
        if (updateCheckController != null) {
            updateCheckController.init();
        }
        idleStartTime = Instant.now();
        tick.run();
    }

    @Override protected void onPause() {
        if (updateCheckController != null) {
            updateCheckController.suspend();
        }
        unregisterReceiver(batteryWatcher);
        EventBus.getDefault().unregister(this);
        App.getHealthMonitor().stop();
        pausedScaleStep = sScaleStep;
        tickHandler.removeCallbacks(tick);
        super.onPause();
    }

    /** Invoked once every second. */
    protected void onTick() { }

    @Override public void onUserInteraction() {
        idleStartTime = Instant.now();
    }

    protected @Nonnull Duration getIdleDuration() {
        return idleStartTime != null
            ? new Duration(idleStartTime, Instant.now())
            : new Duration(0);
    }

    /** Opens the dialog and returns true, unless a dialog of this type is already open. */
    public boolean openDialog(DialogFragment fragment) {
        String type = fragment.getClass().getName();
        if (!openDialogTypes.contains(type)) {
            fragment.show(getSupportFragmentManager(), null);
            return true;
        }
        return false;
    }

    public void onDialogOpened(DialogFragment fragment) {
        openDialogTypes.add(fragment.getClass().getName());
    }

    public void onDialogClosed(DialogFragment fragment) {
        openDialogTypes.remove(fragment.getClass().getName());
    }

    /** Intercepts volume-up/volume-down presses and changes the UI scale step. */
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
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        sScaleStep = newScaleStep;
        Utils.restartActivity(this);
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
        ButterKnife.inject(this);
    }

    private void initializeWrapperView() {
        if (mWrapperView != null) return;

        mWrapperView =
            (LinearLayout) getLayoutInflater().inflate(R.layout.view_status_wrapper, null);
        super.setContentView(mWrapperView);

        mInnerContent =
            mWrapperView.findViewById(R.id.status_wrapper_inner_content);
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
        initializeSnackBar();
        snackBar.message(message);
    }

    /**
     * Adds a message to the SnackBar with informed priority.
     * @see "SnackBar Documentation." {@link SnackBar#message(int, int)}
     */
    public void snackBar(@StringRes int message, int priority) {
        initializeSnackBar();
        snackBar.message(message, priority);
    }

    /**
     * Adds a message to the SnackBar. Priority defaults to 999.
     * @see "SnackBar Documentation." {@link SnackBar#message(int, int, View.OnClickListener, int)}
     */
    public void snackBar(@StringRes int message, @StringRes int actionMessage, View.OnClickListener listener) {
        initializeSnackBar();
        snackBar.message(message, actionMessage, listener, 999);
    }

    /**
     * Adds a message to the SnackBar with informed priority.
     * @see "SnackBar Documentation." {@link SnackBar#message(int, int, View.OnClickListener, int)}
     */
    public void snackBar(@StringRes int message, @StringRes int actionMessage,
                         View.OnClickListener listener, int priority) {
        initializeSnackBar();
        snackBar.message(message, actionMessage, listener, priority);
    }

    /**
     * Adds a message to the SnackBar with all parameters except for secondsToTimeout.
     * @see "SnackBar Documentation."
     * {@link SnackBar#message(int, int, View.OnClickListener, int, boolean, int)}
     */
    public void snackBar(@StringRes int message, @StringRes int actionMessage,
                         View.OnClickListener actionOnClick, int priority, boolean isDismissible) {
        initializeSnackBar();
        snackBar.message(message, actionMessage, actionOnClick, priority, isDismissible, 0);
    }

    /**
     * Adds a message to the SnackBar with all parameters.
     * @see "SnackBar Documentation."
     * {@link SnackBar#message(int, int, View.OnClickListener, int, boolean, int)}
     */
    public void snackBar(@StringRes int message, @StringRes int actionMessage,
                         View.OnClickListener actionOnClick, int priority,
                         boolean isDismissible, int secondsToTimeOut) {
        initializeSnackBar();
        snackBar.message(message, actionMessage, actionOnClick, priority, isDismissible,
            secondsToTimeOut);
    }

    /**
     * Use it to programmatically dismiss a SnackBar message.
     * @param id The @StringRes for the message.
     */
    public void snackBarDismiss(@StringRes int id) {
        if (snackBar != null) snackBar.dismiss(id);
    }

    /**
     * Programmatically dismiss multiple messages at once
     * @param id a @StringRes message Array
     */
    public void snackBarDismiss(@StringRes int[] ids) {
        if (snackBar != null) snackBar.dismiss(ids);
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

        for (TroubleshootingAction action : event.actions) {
            if (action != null) switch (action) {
                case ENABLE_WIFI:
                    snackBar(R.string.troubleshoot_wifi_disabled,
                        R.string.troubleshoot_wifi_disabled_action_enable,
                        view -> ((WifiManager) getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true),
                        10, false);
                    break;
                case CONNECT_WIFI:
                    snackBar(R.string.troubleshoot_wifi_disconnected,
                        R.string.troubleshoot_wifi_disconnected_action_connect,
                        view -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)),
                        20, false);
                    break;
                case CHECK_SERVER_CONFIGURATION:
                    snackBar(R.string.troubleshoot_server_address,
                        R.string.troubleshoot_server_address_action_check,
                        view -> SettingsActivity.start(BaseActivity.this),
                        30, false);
                    break;
                case CHECK_SERVER_REACHABILITY:
                    snackBar(R.string.troubleshoot_server_unreachable,
                        R.string.troubleshoot_action_more_info,
                        view -> showMoreInfoDialog(
                            // TODO: Display the actual server URL that couldn't be reached in
                            // this message. This will require that injection be hooked up
                            // through to this inner class, which may be complicated.
                            getString(R.string.troubleshoot_server_unreachable),
                            getString(R.string.troubleshoot_server_unreachable_details),
                            true
                        ), 40, false);
                    break;
                case CHECK_SERVER_AUTH:
                    if (settings.isAuthorized()) {
                        snackBar(R.string.troubleshoot_server_auth,
                            R.string.troubleshoot_server_auth_action_check,
                            view -> SettingsActivity.start(BaseActivity.this),
                            50, false);
                    }
                    break;
                case CHECK_SERVER_PERMISSIONS:
                    if (settings.isAuthorized()) {
                        snackBar(R.string.troubleshoot_server_permission,
                            R.string.troubleshoot_server_auth_action_check,
                            view -> SettingsActivity.start(BaseActivity.this),
                            50, false);
                    }
                    break;
                case CHECK_SERVER_SETUP:  // server is returning 500
                    snackBar(R.string.troubleshoot_server_unstable,
                        R.string.troubleshoot_action_more_info,
                        view -> showMoreInfoDialog(
                            // TODO: Display the actual server URL that couldn't be reached in
                            // this message. This will require that injection be hooked up
                            // through to this inner class, which may be complicated.
                            getString(R.string.troubleshoot_server_unstable),
                            getString(R.string.troubleshoot_server_unstable_details),
                            false
                        ), 60, false);
                    break;
                case CHECK_SERVER_STATUS:  // server is not responding
                    snackBar(R.string.troubleshoot_server_not_responding,
                        R.string.troubleshoot_action_more_info,
                        view -> showMoreInfoDialog(
                            // TODO: Display the actual server URL that couldn't be reached in
                            // this message. This will require that injection be hooked up
                            // through to this inner class, which may be complicated.
                            getString(R.string.troubleshoot_server_not_responding),
                            getString(R.string.troubleshoot_server_not_responding_details),
                            false
                        ), 60, false);
                    break;
                case CHECK_PERIODIC_SYNC_SETTINGS:
                    snackBar(R.string.troubleshoot_periodic_sync_disabled,
                        R.string.troubleshoot_action_check_settings,
                        view -> SettingsActivity.start(BaseActivity.this),
                        70, false);
                    break;
                case CHECK_PACKAGE_SERVER_REACHABILITY:
                    snackBar(R.string.troubleshoot_package_server_unreachable,
                        R.string.troubleshoot_action_more_info,
                        view -> showMoreInfoDialog(
                            getString(R.string.troubleshoot_package_server_unreachable),
                            getString(R.string.troubleshoot_update_server_unreachable_details),
                            true
                        ), 80, false);
                    break;
                case CHECK_PACKAGE_SERVER_CONFIGURATION:
                    snackBar(R.string.troubleshoot_package_server_misconfigured,
                        R.string.troubleshoot_action_more_info,
                        view -> showMoreInfoDialog(
                            getString(R.string.troubleshoot_package_server_misconfigured),
                            getString(R.string.troubleshoot_update_server_misconfigured_details),
                            true
                        ), 90, false);
                    break;
                default:
                    LOG.w("Troubleshooting action '%1$s' is unknown.", action);
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
        troubleshootingMessages.put(HealthIssue.SERVER_PERMISSION_ISSUE,
            new TroubleshootingMessage(
                R.string.troubleshoot_server_permission,
                R.string.troubleshoot_server_permission_solved,
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
        troubleshootingMessages.put(HealthIssue.PERIODIC_SYNC_DISABLED,
            new TroubleshootingMessage(
                R.string.troubleshoot_periodic_sync_disabled,
                R.string.troubleshoot_periodic_sync_disabled_solved,
                10
            ));

        TroubleshootingMessage message = troubleshootingMessages.get(solvedIssue);
        if (message != null) {
            initializeSnackBar();
            SnackBar.Message snackBarMessage = snackBar.getMessage(message.messageId);
            if (snackBarMessage != null) {
                snackBar.dismiss(snackBarMessage.key);
                snackBar.message(message.resolvedMessageId, 0, null, 994, true, message.timeout);
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
                (dialog, which) -> SettingsActivity.start(BaseActivity.this));
        }
        builder.show();
    }

    /** The user has requested a download of the last known available software update. */
    public static class DownloadRequestedEvent {
    }

    /** The user has requested installation of the last downloaded software update. */
    public static class InstallationRequestedEvent {
    }

    protected class UpdateNotificationUi implements UpdateCheckController.Ui {

        public UpdateNotificationUi() {}

        @Override public void showUpdateAvailableForDownload(AvailableUpdateInfo updateInfo) {
            snackBar(R.string.snackbar_update_available,
                R.string.snackbar_action_download,
                view -> {
                    Utils.logEvent("download_update_button_pressed");
                    //TODO: programatically dismiss the snackbar message
                    EventBus.getDefault().post(new DownloadRequestedEvent());
                }, 35, false);
        }

        @Override public void showUpdateReadyToInstall(DownloadedUpdateInfo updateInfo) {
            snackBar(R.string.snackbar_update_downloaded,
                R.string.snackbar_action_install,
                view -> {
                    Utils.logEvent("install_update_button_pressed");
                    //TODO: programatically dismiss the snackbar message
                    EventBus.getDefault().post(new InstallationRequestedEvent());
                }, 35, false);
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

