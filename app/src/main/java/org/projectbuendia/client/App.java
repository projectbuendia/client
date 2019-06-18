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
import android.preference.PreferenceManager;

import com.facebook.stetho.Stetho;

import org.odk.collect.android.application.Collect;
import org.projectbuendia.client.diagnostics.HealthMonitor;
import org.projectbuendia.client.net.OpenMrsConnectionDetails;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.user.UserManager;

import javax.inject.Inject;

import dagger.ObjectGraph;

/** An {@link Application} the represents the Android Client. */
public class App extends Application {

    /** The current instance of the application. */
    private static App sInstance;
    private static UserManager sUserManager;
    private static Server sServer;
    private static OpenMrsConnectionDetails sConnectionDetails;
    private ObjectGraph mObjectGraph;
    @Inject UserManager mUserManager;
    @Inject OpenMrsConnectionDetails mOpenMrsConnectionDetails;
    @Inject Server mServer;
    @Inject HealthMonitor mHealthMonitor;

    public static synchronized App getInstance() {
        return sInstance;
    }

    public static synchronized UserManager getUserManager() {
        return sUserManager;
    }

    public static synchronized Server getServer() {
        return sServer;
    }

    public static synchronized OpenMrsConnectionDetails getConnectionDetails() {
        return sConnectionDetails;
    }

    @Override public void onCreate() {
        Collect.onCreate(this);
        super.onCreate();

        // Enable Stetho, which lets you inspect the app's database, UI, and network activity
        // just by opening chrome://inspect in Chrome on a computer connected to the tablet.
        Stetho.initializeWithDefaults(this);

        mObjectGraph = ObjectGraph.create(Modules.list(this));
        mObjectGraph.inject(this);
        mObjectGraph.injectStatics();

        // Ensure all unset preferences get initialized with default values.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        synchronized (App.class) {
            sInstance = this;
            sUserManager = mUserManager; // TODO: Remove when Daggered.
            sConnectionDetails = mOpenMrsConnectionDetails; // TODO: Remove when Daggered.
            sServer = mServer; // TODO: Remove when Daggered.
        }

        mHealthMonitor.start();
    }

    public <T> T get(Class<T> type) {
        return mObjectGraph.get(type);
    }

    public void inject(Object obj) {
        mObjectGraph.inject(obj);
    }

    public HealthMonitor getHealthMonitor() {
        return mHealthMonitor;
    }
}
