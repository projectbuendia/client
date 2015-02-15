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
import org.msf.records.events.actions.DownloadRequestedEvent;
import org.msf.records.events.actions.InstallationRequestedEvent;
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
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
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
                message.setText("Wifi is disabled");
                action.setText("Enable");
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
                message.setText("Wifi is disconnected");
                action.setText("Connect");
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        action.setEnabled(false);

                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                });
                break;
            case CHECK_SERVER_AUTH:
                message.setText("Server username/password may be incorrect");
                action.setText("Check");
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        action.setEnabled(false);

                        startActivity(new Intent(BaseActivity.this, SettingsActivity.class));
                    }
                });
                break;
            case CHECK_SERVER_CONFIGURATION:
                message.setText("Server address may be incorrect");
                action.setText("Check");
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        action.setEnabled(false);

                        startActivity(new Intent(BaseActivity.this, SettingsActivity.class));
                    }
                });
                break;
            case CHECK_SERVER_REACHABILITY:
                message.setText("Server unreachable");
                action.setText("More Info");
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        action.setEnabled(false);

                        // TODO(dxchen): Display the actual server URL that couldn't be reached in
                        // this message. This will require that injection be hooked up through to
                        // this inner class, which may be complicated.
                        new AlertDialog.Builder(BaseActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setTitle("Server unreachable")
                                .setMessage(
                                        "The server could not be reached. This may be because:\n"
                                                + "\n"
                                                + " • The wifi network is incorrect.\n"
                                                + " • The server URL is incorrect.\n"
                                                + " • The server is down.\n"
                                                + "\n"
                                                + "Please contact an administrator.")
                                .setNeutralButton("Ok", null)
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {

                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        action.setEnabled(true);
                                    }
                                })
                                .create().show();
                    }
                });
                break;
            case CHECK_SERVER_SETUP:
                message.setText("Server may be unstable");
                action.setText("More Info");
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        action.setEnabled(false);

                        // TODO(dxchen): Display the actual server URL that couldn't be reached in
                        // this message. This will require that injection be hooked up through to
                        // this inner class, which may be complicated.
                        // TODO(akalachman): Localize, along with all other strings in this class.
                        new AlertDialog.Builder(BaseActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setTitle("Server may be unstable")
                                .setMessage(
                                        "The server is currently responding with error code 500, "
                                                + "indicating that the server may be in an error "
                                                + "state.\n"
                                                + "Please contact an administrator.")
                                .setNeutralButton("Ok", null)
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {

                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        action.setEnabled(true);
                                    }
                                })
                                .create().show();
                    }
                });
                break;
            case CHECK_SERVER_STATUS:
                message.setText("Server not responding");
                action.setText("More Info");
                action.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        action.setEnabled(false);

                        // TODO(dxchen): Display the actual server URL that couldn't be reached in
                        // this message. This will require that injection be hooked up through to
                        // this inner class, which may be complicated.
                        // TODO(akalachman): Localize, along with all other strings in this class.
                        new AlertDialog.Builder(BaseActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setTitle("Server not responding")
                                .setMessage(
                                        "The server is currently not responding to requests. "
                                                + "This may be because:\n"
                                                + "\n"
                                                + " • The server is temporarily unavailable.\n"
                                                + " • The server is still starting up.\n"
                                                + "\n"
                                                + "Please contact an administrator.")
                                .setNeutralButton("Ok", null)
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {

                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        action.setEnabled(true);
                                    }
                                })
                                .create().show();
                    }
                });
                break;
            default:
                LOG.w("Troubleshooting action '%1$s' is unknown.");
                return;
        }

        setStatusView(view);
        setStatusVisibility(View.VISIBLE);
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

        UpdateNotificationController mController = null;
        final TextView mUpdateMessage;
        final TextView mUpdateAction;

        public UpdateNotificationUi() {
            View view = getLayoutInflater().inflate(R.layout.view_status_bar_default, null);
            setStatusView(view);
            mUpdateMessage = (TextView) view.findViewById(R.id.status_bar_default_message);
            mUpdateAction = (TextView) view.findViewById(R.id.status_bar_default_action);
        }

        public void setController(UpdateNotificationController controller) {
            mController = controller;
        }

        @Override
        public void showUpdateAvailableForDownload(AvailableUpdateInfo updateInfo) {
            setStatusVisibility(View.VISIBLE);
            mUpdateMessage.setText(R.string.snackbar_update_available);
            if (mController != null) {
                mUpdateAction.setText(R.string.snackbar_action_download);
                mUpdateAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setStatusVisibility(View.GONE);
                        EventBus.getDefault().post(
                                new DownloadRequestedEvent());
                    }
                });
            }

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
                    EventBus.getDefault().post(
                            new InstallationRequestedEvent());
                }
            });
        }

        @Override
        public void hideSoftwareUpdateNotifications() {
            setStatusVisibility(View.GONE);
        }
    }
}

