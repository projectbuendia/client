package org.msf.records;

import android.app.Application;
import android.content.ContentResolver;
import android.content.res.Resources;

import org.msf.records.data.app.AppModelModule;
import org.msf.records.diagnostics.DiagnosticsModule;
import org.msf.records.events.EventsModule;
import org.msf.records.net.NetModule;
import org.msf.records.prefs.PrefsModule;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.chart.PatientChartActivity;
import org.msf.records.ui.patientcreation.PatientCreationActivity;
import org.msf.records.ui.patientlist.PatientListActivity;
import org.msf.records.ui.patientlist.PatientListFragment;
import org.msf.records.ui.patientlist.PatientSearchActivity;
import org.msf.records.ui.patientlist.RoundActivity;
import org.msf.records.ui.patientlist.RoundFragment;
import org.msf.records.ui.tentselection.TentSelectionActivity;
import org.msf.records.ui.userlogin.UserLoginFragment;
import org.msf.records.updater.UpdateModule;
import org.msf.records.user.UserModule;
import org.msf.records.utils.UtilsModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module that provides the top-level bindings for the app.
 */
@Module(
        includes = {
                AppModelModule.class,
                DiagnosticsModule.class,
                EventsModule.class,
                NetModule.class,
                PrefsModule.class,
                UpdateModule.class,
                UserModule.class,
                UtilsModule.class
        },
        injects = {
                App.class,

                // TODO(dxchen): Move these into activity-specific modules.
                // Activities
                PatientCreationActivity.class,
                BaseActivity.class,
                PatientChartActivity.class,
                PatientListActivity.class,
                PatientSearchActivity.class,
                RoundActivity.class,
                TentSelectionActivity.class,
                PatientListFragment.class,
                RoundFragment.class,
                UserLoginFragment.class
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
    ContentResolver provideContentResolver(Application app) {
        return app.getContentResolver();
    }

    @Provides
    @Singleton
    Resources provideResources(Application app) {
        return app.getResources();
    }

    @Provides
    @Singleton
    SyncManager provideSyncManager() {
        return new SyncManager();
    }

    @Provides
    @Singleton
    LocalizedChartHelper provideLocalizedChartHelper(ContentResolver contentResolver) {
        return new LocalizedChartHelper(contentResolver);
    }
}
