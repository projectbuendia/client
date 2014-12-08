package org.msf.records;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by dxchen on 12/8/14.
 */
@Module(
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
