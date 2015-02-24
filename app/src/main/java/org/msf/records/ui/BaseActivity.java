package org.msf.records.ui;

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

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.diagnostics.TroubleshootingAction;
import org.msf.records.events.diagnostics.TroubleshootingActionsChangedEvent;
import org.msf.records.updater.AvailableUpdateInfo;
import org.msf.records.updater.DownloadedUpdateInfo;
import org.msf.records.utils.Logger;

import de.greenrobot.event.EventBus;

/**
 * An abstract {@link FragmentActivity} that is the base for all activities.
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

    /**
     * Sets the visibility of the status bar.
     */
    public void setStatusVisibility(int visibility) {
        mStatusContent.setVisibility(visibility);
    }

    /**
     * Gets the visibility of the status bar.
     */
    public int getStatusVisibility() {
        return mStatusContent.getVisibility();
    }

    /**
     * Called when the set of troubleshooting actions changes.
     */
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

                        ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                                .setWifiEnabled(true);
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

                        startActivity(new Intent(BaseActivity.this, SettingsActivity.class));
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

                        startActivity(new Intent(BaseActivity.this, SettingsActivity.class));
                    }
                });
                break;
            case CHECK_SERVER_REACHABILITY:
                message.setText(R.string.troubleshoot_server_unreachable);
                action.setText(R.string.troubleshoot_action_more_info);
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO(dxchen): Display the actual server URL that couldn't be reached in
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
                        // TODO(dxchen): Display the actual server URL that couldn't be reached in
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
                        // TODO(dxchen): Display the actual server URL that couldn't be reached in
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
            case CHECK_UPDATE_SERVER_REACHABILITY:
                message.setText(R.string.troubleshoot_update_server_unreachable);
                action.setText(R.string.troubleshoot_action_more_info);
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMoreInfoDialog(
                                action,
                                getString(R.string.troubleshoot_update_server_unreachable),
                                getString(R.string.troubleshoot_update_server_unreachable_details),
                                true);
                    }
                });
                break;
            case CHECK_UPDATE_SERVER_CONFIGURATION:
                message.setText(R.string.troubleshoot_update_server_misconfigured);
                action.setText(R.string.troubleshoot_action_more_info);
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMoreInfoDialog(
                                action,
                                getString(R.string.troubleshoot_update_server_misconfigured),
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
                                    boolean includeSettingsLink) {
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
        if (includeSettingsLink) {
            builder.setPositiveButton(R.string.troubleshoot_action_check_settings,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(BaseActivity.this, SettingsActivity.class));
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

        final TextView mUpdateMessage;
        final TextView mUpdateAction;

        public UpdateNotificationUi() {
            View view = getLayoutInflater().inflate(R.layout.view_status_bar_default, null);
            setStatusView(view);
            mUpdateMessage = (TextView) view.findViewById(R.id.status_bar_default_message);
            mUpdateAction = (TextView) view.findViewById(R.id.status_bar_default_action);
        }

        @Override
        public void showUpdateAvailableForDownload(AvailableUpdateInfo updateInfo) {
            setStatusVisibility(View.VISIBLE);
            mUpdateMessage.setText(R.string.snackbar_update_available);
            mUpdateAction.setText(R.string.snackbar_action_download);
            mUpdateAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setStatusVisibility(View.GONE);
                    EventBus.getDefault().post(new DownloadRequestedEvent());
                }
            });
        }

        @Override
        public void showUpdateReadyToInstall(DownloadedUpdateInfo updateInfo) {
            setStatusVisibility(View.VISIBLE);
            mUpdateMessage.setText(R.string.snackbar_update_downloaded);
            mUpdateAction.setText(R.string.snackbar_action_install);
            mUpdateAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
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

