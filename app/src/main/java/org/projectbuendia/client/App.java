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

package org.projectbuendia.client;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.android.volley.VolleyLog;
import com.facebook.stetho.Stetho;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

import org.odk.collect.android.application.Collect;
import org.projectbuendia.client.diagnostics.HealthMonitor;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.net.OpenMrsConnectionDetails;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.sync.ConceptService;
import org.projectbuendia.client.sync.Database;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.updater.UpdateManager;
import org.projectbuendia.client.user.UserManager;
import org.projectbuendia.client.utils.Loc;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.io.File;
import java.util.Locale;

import javax.inject.Inject;

import dagger.ObjectGraph;
import de.greenrobot.event.EventBus;

/** An {@link Application} the represents the Android Client. */
public class App extends Application {
    private static final Logger LOG = Logger.create();

    // Global instances of all our singletons.
    private static App sInstance;
    private static Resources sResources;
    private static AppModel sModel;
    private static AppSettings sSettings;
    private static ChartDataHelper sChartDataHelper;
    private static CrudEventBus sCrudEventBus;
    private static EventBus sHealthEventBus;
    private static HealthMonitor sHealthMonitor;
    private static SyncManager sSyncManager;
    private static UpdateManager sUpdateManager;
    private static UserManager sUserManager;
    private static OpenMrsConnectionDetails sConnectionDetails;
    private static Server sServer;
    private static ConceptService sConceptService;

    private ObjectGraph mObjectGraph;
    @Inject AppModel mModel;
    @Inject AppSettings mSettings;
    @Inject ChartDataHelper mChartDataHelper;
    @Inject CrudEventBus mCrudEventBus;
    @Inject EventBus mHealthEventBus;
    @Inject HealthMonitor mHealthMonitor;
    @Inject SyncManager mSyncManager;
    @Inject UpdateManager mUpdateManager;
    @Inject UserManager mUserManager;
    @Inject OpenMrsConnectionDetails mOpenMrsConnectionDetails;
    @Inject Server mServer;

    public static App getInstance() {
        return sInstance;
    }

    public static Context getContext() {
        return sInstance.getApplicationContext();
    }

    /** This should be called in every Activity's attachBaseContext method. */
    public static Context applyLocaleSetting(Context base) {
        Locale locale = getSettings().getLocale();
        Locale.setDefault(locale);
        Resources resources = base.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        Context context = base.createConfigurationContext(config);
        sResources = context.getResources();
        return context;
    }

    public static String str(int id, Object... args) {
        return sResources.getString(id, args);
    }

    public static String localize(String packed) {
        return new Loc(packed).get(getSettings().getLocale());
    }

    public static void inject(Object obj) {
        sInstance.mObjectGraph.inject(obj);
    }

    public static synchronized ContentResolver getResolver() {
        return sInstance.getContentResolver();
    }

    public static synchronized SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    public static synchronized AppModel getModel() {
        return sModel;
    }

    public static synchronized AppSettings getSettings() {
        return sSettings;
    }

    public static synchronized ChartDataHelper getChartDataHelper() {
        return sChartDataHelper;
    }

    public static synchronized CrudEventBus getCrudEventBus() {
        return sCrudEventBus;
    }

    public static synchronized EventBus getHealthEventBus() {
        return sHealthEventBus;
    }

    public static synchronized HealthMonitor getHealthMonitor() {
        return sHealthMonitor;
    }

    public static synchronized SyncManager getSyncManager() {
        return sSyncManager;
    }

    public static synchronized UserManager getUserManager() {
        return sUserManager;
    }

    public static synchronized OpenMrsConnectionDetails getConnectionDetails() {
        return sConnectionDetails;
    }

    public static synchronized Server getServer() {
        return sServer;
    }

    public static synchronized ConceptService getConceptService() {
        return sConceptService;
    }

    @Override public void onCreate() {
        sInstance = this;
        sResources = this.getResources();
        Collect.onCreate(this);
        super.onCreate();

        // Distinguish our Volley log messages from other apps that might use Volley
        VolleyLog.setTag("buendia/Volley");

        // Enable FontAwesome Icons
        Iconify.with(new FontAwesomeModule());

        // Enable Stetho, which lets you inspect the app's database, UI, and network activity
        // just by opening chrome://inspect in Chrome on a computer connected to the tablet.
        Stetho.initializeWithDefaults(this);

        mObjectGraph = ObjectGraph.create(Modules.list(this));
        mObjectGraph.inject(this);
        mObjectGraph.injectStatics();

        // Ensure all unset preferences get initialized with default values.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        synchronized (App.class) {
            sModel = mModel;
            sSettings = mSettings;
            sChartDataHelper = mChartDataHelper;
            sCrudEventBus = mCrudEventBus;
            sHealthEventBus = mHealthEventBus;
            sHealthMonitor = mHealthMonitor;
            sSyncManager = mSyncManager;
            sUpdateManager = mUpdateManager;
            sUserManager = mUserManager;
            sConnectionDetails = mOpenMrsConnectionDetails;
            sServer = mServer;
            sConceptService = new ConceptService(getContentResolver());
            mHealthMonitor.start();
        }
    }

    public static synchronized void reset(Runnable callback) {
        sSyncManager.setNewSyncsSuppressed(true);
        LOG.i("reset(): Waiting for syncs to stop...");
        sSyncManager.stopSyncing(() -> {
            try {
                clearDatabase();
                clearMemoryState();
                clearOdkFiles();
                LOG.i("reset(): Completed");
                if (callback != null) callback.run();
            } finally {
                sSyncManager.setNewSyncsSuppressed(false);
            }
        });
    }

    private static void clearDatabase() {
        LOG.i("Clearing local database");
        try {
            Database db = new Database(getContext());
            db.clear();
            db.close();
        } catch (Throwable t) {
            LOG.e(t, "Failed to clear database");
        }
    }

    private static synchronized void clearMemoryState() {
        LOG.i("Clearing memory state");
        try {
            sUserManager.reset();
            sModel.reset();
        } catch (Throwable t) {
            LOG.e(t, "Failed to clear memory state");
        }
    }

    private static void clearOdkFiles() {
        LOG.i("Clearing ODK files");
        try {
            File filesDir = getContext().getFilesDir();
            File odkDir = new File(filesDir, "odk");
            File odkTempDir = new File(filesDir, "odk-deleted." + System.currentTimeMillis());
            odkDir.renameTo(odkTempDir);
            Utils.recursivelyDelete(odkTempDir);
        } catch (Throwable t) {
            LOG.e(t, "Failed to clear ODK state");
        }
    }
}
