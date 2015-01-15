package org.msf.records.diagnostics;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

/**
 * A {@link HealthCheck} that checks wifi state.
 */
public class WifiHealthCheck extends HealthCheck {

    private static final IntentFilter sWifiStateChangedIntentFilter =
            getIntentFilter();

    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;
    private final WifiChangeBroadcastReceiver mWifiStateChangedReceiver;

    protected WifiHealthCheck(Application application) {
        super(application);

        mWifiManager = (WifiManager) application.getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager =
                (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiStateChangedReceiver = new WifiChangeBroadcastReceiver();
    }

    @Override
    protected void startImpl() {
        mApplication.registerReceiver(mWifiStateChangedReceiver, sWifiStateChangedIntentFilter);

        checkWifiState();
    }

    @Override
    protected void stopImpl() {
        mApplication.unregisterReceiver(mWifiStateChangedReceiver);
    }

    private static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        return intentFilter;
    }

    private void checkWifiState() {
        int wifiState = mWifiManager.getWifiState();
        if (wifiState != WifiManager.WIFI_STATE_ENABLING
                && wifiState != WifiManager.WIFI_STATE_ENABLED) {
            reportIssue(HealthIssue.WIFI_DISABLED);
        } else {
            resolveIssue(HealthIssue.WIFI_DISABLED);
            NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
                reportIssue(HealthIssue.WIFI_NOT_CONNECTED);
            } else {
                resolveAllIssues();
            }
        }
    }

    private class WifiChangeBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            checkWifiState();
        }
    }
}
