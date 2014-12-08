package org.msf.records;

import android.app.Application;

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
                UpdateModule.class,
                UserModule.class,
                UtilsModule.class
        },
        injects = {
                App.class
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
