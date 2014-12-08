package org.msf.records;

import android.app.Application;

import org.msf.records.net.NetModule;
import org.msf.records.prefs.PrefsModule;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.PatientChartActivity;
import org.msf.records.ui.PatientListActivity;
import org.msf.records.ui.PatientSearchActivity;
import org.msf.records.ui.RoundActivity;
import org.msf.records.ui.TentSelectionActivity;
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
                BaseActivity.class,
                PatientChartActivity.class,
                PatientListActivity.class,
                PatientSearchActivity.class,
                RoundActivity.class,
                TentSelectionActivity.class
        }
)
public final class AppModule {

    private final App mApp;

    public AppModule(App app) {
        mApp = app;
    }

    @Provides @Singleton Application provideApplication() {
        return mApp;
    }
}
