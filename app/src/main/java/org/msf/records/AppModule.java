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
import android.content.ContentResolver;
import android.preference.PreferenceManager;

import org.msf.records.data.app.AppModelModule;
import org.msf.records.diagnostics.DiagnosticsModule;
import org.msf.records.events.EventsModule;
import org.msf.records.net.NetModule;
import org.msf.records.sync.SyncAccountService;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.UpdateNotificationController;
import org.msf.records.ui.chart.PatientChartActivity;
import org.msf.records.ui.locationselection.LocationSelectionActivity;
import org.msf.records.ui.patientcreation.PatientCreationActivity;
import org.msf.records.ui.patientlist.PatientListActivity;
import org.msf.records.ui.patientlist.PatientListController;
import org.msf.records.ui.patientlist.PatientListFragment;
import org.msf.records.ui.patientlist.PatientSearchActivity;
import org.msf.records.ui.patientlist.RoundActivity;
import org.msf.records.ui.patientlist.RoundFragment;
import org.msf.records.ui.userlogin.UserLoginActivity;
import org.msf.records.ui.userlogin.UserLoginFragment;
import org.msf.records.updater.UpdateModule;
import org.msf.records.user.UserModule;
import org.msf.records.utils.UtilsModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/** A Dagger module that provides the top-level bindings for the app. */
@Module(
        includes = {
                AppModelModule.class,
                DiagnosticsModule.class,
                EventsModule.class,
                NetModule.class,
                UpdateModule.class,
                UserModule.class,
                UtilsModule.class
        },
        injects = {
                App.class,

                // TODO: Move these into activity-specific modules.
                // Activities
                PatientCreationActivity.class,
                BaseActivity.class,
                PatientChartActivity.class,
                PatientListActivity.class,
                PatientListController.class,
                PatientSearchActivity.class,
                RoundActivity.class,
                LocationSelectionActivity.class,
                PatientListFragment.class,
                RoundFragment.class,
                UpdateNotificationController.class,
                UserLoginActivity.class,
                UserLoginFragment.class
        },
        staticInjections = {
                SyncAccountService.class
        })
public final class AppModule {

    private final App mApp;

    public AppModule(App app) {
        mApp = app;
    }

    @Provides
    @Singleton
    Application provideApplication() {
        return mApp;
    }

    @Provides
    @Singleton
    AppSettings provideAppSettings(Application app) {
        return new AppSettings(
                PreferenceManager.getDefaultSharedPreferences(app), app.getResources());
    }

    @Provides
    @Singleton
    ContentResolver provideContentResolver(Application app) {
        return app.getContentResolver();
    }

    @Provides
    @Singleton
    SyncManager provideSyncManager(AppSettings settings) {
        return new SyncManager(settings);
    }

    @Provides
    @Singleton
    LocalizedChartHelper provideLocalizedChartHelper(ContentResolver contentResolver) {
        return new LocalizedChartHelper(contentResolver);
    }
}
