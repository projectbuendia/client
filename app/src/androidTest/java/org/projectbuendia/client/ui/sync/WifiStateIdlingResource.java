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

package org.projectbuendia.client.ui.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.google.android.apps.common.testing.ui.espresso.IdlingResource;

import org.projectbuendia.client.App;

import java.util.UUID;

/**
 * {@link IdlingResource} that is busy when wifi is disabled or disconnected.
 *
 * <p>Note: until tests are migrated to Espresso 2.0, this resource will be configured to idle
 * forever after its first idle transition, to maintain test isolation.
 */
public class WifiStateIdlingResource implements IdlingResource {

    private final WifiManager mWifiManager =
            (WifiManager) App.getInstance().getSystemService(Context.WIFI_SERVICE);
    private final ConnectivityManager mConnectivityManager =
            (ConnectivityManager) App.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
    private final WifiChangeBroadcastReceiver mWifiStateChangedReceiver =
            new WifiChangeBroadcastReceiver();
    private final String mName = UUID.randomUUID().toString();

    private ResourceCallback mResourceCallback;

    // Because you can't unregister idling resources in Espresso 1.1, each WifiStateIdlingResource
    // needs to fire its state transition only once and then be idle forever. Once we switch to
    // Espresso 2.0, this is no longer necesssary.
    private static final boolean ONE_TIME_ONLY = true;
    private boolean mIdleTransitionOccurred = false;

    public WifiStateIdlingResource() {
        App.getInstance().registerReceiver(mWifiStateChangedReceiver, getIntentFilter());
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public boolean isIdleNow() {
        if (ONE_TIME_ONLY && mIdleTransitionOccurred) {
            return true;
        }

        int wifiState = mWifiManager.getWifiState();
        if (wifiState != WifiManager.WIFI_STATE_ENABLING
                && wifiState != WifiManager.WIFI_STATE_ENABLED) {
            return false;
        }
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
        if (isIdleNow()) {
            onIdleTransition();
        }
    }

    private void onIdleTransition() {
        if (!ONE_TIME_ONLY || !mIdleTransitionOccurred) {
            mIdleTransitionOccurred = true;
            if (ONE_TIME_ONLY) {
                App.getInstance().unregisterReceiver(mWifiStateChangedReceiver);
            }
            if (mResourceCallback != null) {
                mResourceCallback.onTransitionToIdle();
            }
        }
    }

    private static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        return intentFilter;
    }

    private class WifiChangeBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isIdleNow()) {
                onIdleTransition();
            }
        }
    }
}
