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

package org.msf.records;

import android.app.Application;

import net.sqlcipher.database.SQLiteDatabase;

import org.msf.records.diagnostics.HealthMonitor;
import org.msf.records.net.OpenMrsConnectionDetails;
import org.msf.records.net.Server;
import org.msf.records.user.UserManager;
import org.odk.collect.android.application.Collect;

import javax.inject.Inject;

import dagger.ObjectGraph;

/** An {@link Application} the represents the Android Client. */
public class App extends Application {

    private ObjectGraph mObjectGraph;

    /** The current instance of the application. */
    private static App sInstance;

    private static UserManager sUserManager;

    private static Server sServer;

    private static OpenMrsConnectionDetails sConnectionDetails;

    @Inject UserManager mUserManager;
    @Inject OpenMrsConnectionDetails mOpenMrsConnectionDetails;
    @Inject Server mServer;
    @Inject HealthMonitor mHealthMonitor;

    @Override
    public void onCreate() {
        Collect.onCreate(this);
        super.onCreate();

        initializeSqlCipher();

        buildObjectGraphAndInject();

        synchronized (App.class) {
            sInstance = this;
            sUserManager = mUserManager; // TODO: Remove when Daggered.
            sConnectionDetails = mOpenMrsConnectionDetails; // TODO: Remove when Daggered.
            sServer = mServer; // TODO: Remove when Daggered.
        }

        mHealthMonitor.start();
    }

    private void initializeSqlCipher() {
        SQLiteDatabase.loadLibs(this);
    }

    public void buildObjectGraphAndInject() {
        mObjectGraph = ObjectGraph.create(Modules.list(this));
        mObjectGraph.inject(this);
    }

    public void inject(Object obj) {
        mObjectGraph.inject(obj);
    }

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

    public HealthMonitor getHealthMonitor() {
        return mHealthMonitor;
    }
}
