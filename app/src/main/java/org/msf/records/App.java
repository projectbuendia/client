package org.msf.records;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.msf.records.net.BuendiaServer;
import org.msf.records.net.Constants;
import org.msf.records.net.OpenMrsServer;
import org.msf.records.net.OpenMrsXformsConnection;
import org.msf.records.net.Server;
import org.msf.records.updater.UpdateManager;
import org.msf.records.user.UserManager;
import org.odk.collect.android.application.Collect;

/**
 * Created by Gil on 08/10/2014.
 */
public class App extends Application {

    /**
     * The current instance of the application.
     */
    private static App sInstance;

    private static UserManager sUserManager;
    private static UpdateManager sUpdateManager;

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

            sUserManager = new UserManager();
            sUpdateManager = new UpdateManager();

            String rootUrl;
            if (preferences.getBoolean("use_openmrs", true)) {
                rootUrl = Constants.API_URL;//preferences.getString("openmrs_root_url", null);
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

    public static synchronized App getInstance() {
        return sInstance;
    }

    public static synchronized UserManager getUserManager() {
        return sUserManager;
    }

    public static synchronized UpdateManager getUpdateManager() {
        return sUpdateManager;
    }

    public static synchronized Server getServer() {
        return mServer;
    }

    public static synchronized OpenMrsXformsConnection getmOpenMrsXformsConnection() {
        return mOpenMrsXformsConnection;
    }
}
