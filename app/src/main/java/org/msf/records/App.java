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

            //Turned off preferences as it is impossible to access them from login screen
            mServer = new OpenMrsServer(
                    getApplicationContext(),
                    Constants.API_URL,
                    Constants.API_ADMIN_USERNAME,
                    Constants.API_ADMIN_PASSWORD);

            mOpenMrsXformsConnection = new OpenMrsXformsConnection(
                    getApplicationContext(),
                    Constants.API_URL,
                    Constants.API_ADMIN_USERNAME,
                    Constants.API_ADMIN_PASSWORD);
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
