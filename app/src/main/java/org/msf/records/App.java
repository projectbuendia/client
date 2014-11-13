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
import org.msf.records.updater.UpdateManager;
import org.odk.collect.android.application.Collect;

/**
 * Created by Gil on 08/10/2014.
 */
public class App extends Application {

    /**
     * The current instance of the application.
     */
    private static App sInstance;

    /**
     * An event bus that posts events to any available thread.
     */
    private static Bus sBus;

    /**
     * An event bus that posts events specifically to the main thread.
     */
    private static MainThreadBus sMainThreadBus;

    private static UpdateManager mUpdateManager;

    private static Server mServer;
    private static OpenMrsXformsConnection mOpenMrsXformsConnection;

    @Override
    public void onCreate() {
        Collect.onCreate(this);
        super.onCreate();

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        synchronized (App.class) {
            sInstance = this;
            sBus = new Bus();
            sMainThreadBus = new MainThreadBus(sBus);

            if (preferences.getBoolean("use_openmrs", false)) {
                String openmrsRootUrl = preferences.getString("openmrs_root_url", null);
                mServer = new OpenMrsServer(getApplicationContext(), openmrsRootUrl);
            } else {
                String apiRootUrl = preferences.getString("api_root_url", null);
                mServer = new BuendiaServer(getApplicationContext(), apiRootUrl);
            }
            mOpenMrsXformsConnection = new OpenMrsXformsConnection(getApplicationContext());
        }
    }

    public static synchronized App getInstance() {
        return sInstance;
    }

    public static synchronized Bus getBus() {
        return sBus;
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
