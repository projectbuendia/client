package org.msf.records;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.msf.records.events.mvcmodels.ModelReadyEvent;
import org.msf.records.mvcmodels.Models;
import org.msf.records.mvcmodels.PatientChartModel;
import org.msf.records.net.OpenMrsConnectionDetails;
import org.msf.records.net.OpenMrsServer;
import org.msf.records.net.OpenMrsXformsConnection;
import org.msf.records.net.Server;
import org.msf.records.updater.UpdateManager;
import org.msf.records.user.UserManager;
import org.odk.collect.android.application.Collect;

import de.greenrobot.event.EventBus;

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

    private static OpenMrsConnectionDetails mConnectionDetails;


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

            mConnectionDetails =
                    new OpenMrsConnectionDetails(
                            preferences.getString("openmrs_root_url", null),
                            preferences.getString("openmrs_user", null),
                            preferences.getString("openmrs_password", null),
                            getApplicationContext());
            mServer = new OpenMrsServer(mConnectionDetails);
        }

        // TODO(dxchen): Refactor this into the model classes.
        EventBus.getDefault().postSticky(new ModelReadyEvent(Models.OBSERVATIONS));
        PatientChartModel.INSTANCE.init();
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

    public static synchronized OpenMrsConnectionDetails getConnectionDetails() {
        return mConnectionDetails;
    }
}
