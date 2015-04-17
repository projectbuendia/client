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

package org.projectbuendia.client.diagnostics;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

/** A {@link HealthCheck} that checks whether the current device is connected to a wifi network. */
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

    @Override
    public boolean isApiUnavailable() {
        // We will get an event that lets us update the set of active issues whenever
        // the wifi state changes, so we can be confident that the API is definitely
        // unavailable whenever either of the wifi-related issues is active.
        return mActiveIssues.contains(HealthIssue.WIFI_NOT_CONNECTED)
                || mActiveIssues.contains(HealthIssue.WIFI_DISABLED);
    }
}
