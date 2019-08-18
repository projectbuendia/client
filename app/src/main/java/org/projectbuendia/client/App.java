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
import android.content.ContentProviderClient;
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
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.user.UserManager;

import javax.inject.Inject;

import dagger.ObjectGraph;

/** An {@link Application} the represents the Android Client. */
public class App extends Application {

    // Global instances of all our singletons.
    private static App sInstance;
    private static AppModel sModel;
    private static AppSettings sSettings;
    private static ContentProviderClient sContentProviderClient;
    private static CrudEventBus sCrudEventBus;
    private static HealthMonitor sHealthMonitor;
    private static SyncManager sSyncManager;
    private static UserManager sUserManager;
    private static OpenMrsConnectionDetails sConnectionDetails;
    private static Server sServer;

    private ObjectGraph mObjectGraph;
    @Inject AppModel mModel;
    @Inject AppSettings mSettings;
    @Inject CrudEventBus mCrudEventBus;
    @Inject HealthMonitor mHealthMonitor;
    @Inject SyncManager mSyncManager;
    @Inject UserManager mUserManager;
    @Inject OpenMrsConnectionDetails mOpenMrsConnectionDetails;
    @Inject Server mServer;

    public static App getInstance() {
        return sInstance;
    }

    public static synchronized AppModel getModel() {
        return sModel;
    }

    public static synchronized AppSettings getSettings() {
        return sSettings;
    }

    public static synchronized ContentProviderClient getContentProviderClient() {
        return sContentProviderClient;
    }

    public static synchronized CrudEventBus getCrudEventBus() {
        return sCrudEventBus;
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

    @Override public void onCreate() {
        Collect.onCreate(this);
        super.onCreate();

        // Distinguish our Volley log messages from other apps that might use Volley
        VolleyLog.setTag("buendia/Volley");

        // Enable FontAwesome Icons
        Iconify.with(new FontAwesomeModule());

        // Enable Stetho, which lets you inspect the app's database, UI, and network activity
        // just by opening chrome://inspect in Chrome on a computer connected to the tablet.
        Stetho.initializeWithDefaults(this);

        sInstance = this;
        mObjectGraph = ObjectGraph.create(Modules.list(this));
        mObjectGraph.inject(this);
        mObjectGraph.injectStatics();

        // Ensure all unset preferences get initialized with default values.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        synchronized (App.class) {
            sModel = mModel;
            sSettings = mSettings;
            sContentProviderClient = getContentResolver().acquireContentProviderClient(Contracts.Users.URI);
            sCrudEventBus = mCrudEventBus;
            sHealthMonitor = mHealthMonitor;
            sSyncManager = mSyncManager;
            sUserManager = mUserManager; // TODO: Remove when Daggered.
            sConnectionDetails = mOpenMrsConnectionDetails; // TODO: Remove when Daggered.
            sServer = mServer; // TODO: Remove when Daggered.
            mHealthMonitor.start();
        }
    }

    public <T> T get(Class<T> type) {
        return mObjectGraph.get(type);
    }

    public void inject(Object obj) {
        mObjectGraph.inject(obj);
    }
}
