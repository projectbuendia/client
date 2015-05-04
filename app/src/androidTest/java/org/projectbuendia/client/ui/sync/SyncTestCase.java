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

import android.content.Context;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import com.google.android.apps.common.testing.ui.espresso.Espresso;

import net.sqlcipher.database.SQLiteException;

import org.projectbuendia.client.App;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.sync.Database;
import org.projectbuendia.client.ui.FunctionalTestCase;
import org.projectbuendia.client.utils.Logger;

import java.sql.SQLException;
import java.util.UUID;

/**
 * A {@link FunctionalTestCase} that clears the application database as part of set up, allowing for
 * sync behavior to be tested more easily. This class does NOT currently clear ODK forms, which are
 * stored separately as flat files.
 *
 * <p>WARNING: Syncing may require the transfer of large quantities of data, so {@link SyncTestCase}
 * tests will almost always be very large tests.
 */
public class SyncTestCase extends FunctionalTestCase {
    private static final Logger LOG = Logger.create();
    // The database may still be holding a lock after a test, so clearing the database may not
    // be successful right away.
    private static final int MAX_DATABASE_CLEAR_RETRIES = 10;

    @Override
    public void setUp() throws Exception {
        // Clearing the database can be flaky if previous tests are temporarily holding a DB lock,
        // so try a few times before failing.
        boolean cleared = false;
        int retriesRemaining = MAX_DATABASE_CLEAR_RETRIES;
        while (!cleared && retriesRemaining > 0) {
            try {
                clearDatabase();
                clearPreferences();
                cleared = true;
            } catch (SQLiteException e) {
                Thread.sleep(500);
                retriesRemaining--;
            }
        }

        super.setUp();
    }

    /** Cleans up post-test wifi state. Won't work during tearDown(). */
    public void cleanupWifi() {
        setWifiEnabled(true);
        // Wait until wifi connection has been re-established.
        Espresso.registerIdlingResources(new WifiStateIdlingResource());
    }

    /** Clears all contents of the database (note: this does not include ODK forms or instances). */
    public void clearDatabase() throws SQLException {
        Database db = new Database(App.getInstance().getApplicationContext());
        db.onUpgrade(db.getWritableDatabase(), 0, 1);
        db.close();
    }

    /** Clears all shared preferences of the application. */
    public void clearPreferences() {
        PreferenceManager.getDefaultSharedPreferences(App.getInstance()).edit().clear().commit();
    }

    /** Turns wifi on or off. */
    protected void setWifiEnabled(boolean enabled) {
        LOG.i("Setting wifi state: %b", enabled);
        WifiManager wifiManager =
                (WifiManager)App.getInstance().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(enabled);
    }

    /** Delays all ViewActions until sync has failed once. */
    protected void waitForSyncFailure() {
        EventBusIdlingResource<SyncFailedEvent> syncFailedEventIdlingResource =
                new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);
        Espresso.registerIdlingResources(syncFailedEventIdlingResource);
    }
}
