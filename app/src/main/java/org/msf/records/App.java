package org.msf.records;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.squareup.otto.Bus;

import org.msf.records.events.MainThreadBus;
import org.msf.records.net.BuendiaServer;
import org.msf.records.net.OpenMrsServer;
import org.msf.records.net.OpenMrsXformsConnection;
import org.msf.records.net.Server;
import org.odk.collect.android.application.Collect;

/**
 * Created by Gil on 08/10/2014.
 */
public class App extends Application {

    private static Server mServer;
    private static OpenMrsXformsConnection mOpenMrsXformsConnection;

    /**
     * An event bus that posts events to any available thread.
     */
    private static Bus sBus;

    /**
     * An event bus that posts events specifically to the main thread.
     */
    private static MainThreadBus sMainThreadBus;

    @Override
    public void onCreate() {
        Collect.onCreate(this);
        super.onCreate();

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        synchronized (App.class) {
            sBus = new Bus();
            sMainThreadBus = new MainThreadBus(sBus);

            String rootUrl;
            if (preferences.getBoolean("use_openmrs", false)) {
                rootUrl = preferences.getString("openmrs_root_url", null);
                mServer = new OpenMrsServer(
                        getApplicationContext(), rootUrl,
                        preferences.getString("openmrs_user", null),
                        preferences.getString("openmrs_password", null));
            } else {
                rootUrl = preferences.getString("api_root_url", null);
                mServer = new BuendiaServer(getApplicationContext(), rootUrl);
            }
            mOpenMrsXformsConnection = new OpenMrsXformsConnection(
                    getApplicationContext(), rootUrl,
                    preferences.getString("openmrs_user", null),
                    preferences.getString("openmrs_password", null));
        }
    }

    public static synchronized MainThreadBus getMainThreadBus() {
        return sMainThreadBus;
    }

    public static synchronized Server getServer() {
        return mServer;
    }

    public static synchronized OpenMrsXformsConnection getmOpenMrsXformsConnection() {
        return mOpenMrsXformsConnection;
    }
}
