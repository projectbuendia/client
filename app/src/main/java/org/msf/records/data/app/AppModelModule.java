package org.msf.records.data.app;

import android.app.Application;

import org.msf.records.data.app.converters.AppTypeConverterModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module that provides bindings for the app model.
 */
@Module(
        includes = {
                AppTypeConverterModule.class
        },
        complete = false,
        library = true
)
public class AppModelModule {

    @Provides @Singleton AppModel provideAppModel(Application app) {
        return new AppModel(app);
    }
}
