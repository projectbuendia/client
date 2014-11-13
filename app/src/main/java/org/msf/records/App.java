package org.msf.records;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.msf.records.net.BuendiaServer;
import org.msf.records.net.OpenMrsServer;
import org.msf.records.net.OpenMrsXforms;
import org.msf.records.net.Server;
import org.odk.collect.android.application.Collect;

/**
 * Created by Gil on 08/10/2014.
 */
public class App extends Application {

    private static Server mServer;
    private static OpenMrsXforms mOpenMrsXforms;

    @Override
    public void onCreate() {
        Collect.onCreate(this);
        super.onCreate();

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        synchronized (App.class) {
            if (preferences.getBoolean("use_openmrs", false)) {
                String openmrsRootUrl = preferences.getString("openmrs_root_url", null);
                mServer = new OpenMrsServer(getApplicationContext(), openmrsRootUrl);
            } else {
                String apiRootUrl = preferences.getString("api_root_url", null);
                mServer = new BuendiaServer(getApplicationContext(), apiRootUrl);
            }
            mOpenMrsXforms = new OpenMrsXforms(getApplicationContext());
        }
    }

    public static synchronized Server getServer() {
        return mServer;
    }

    public static synchronized OpenMrsXforms getmOpenMrsXforms() {
        return mOpenMrsXforms;
    }
}
