package org.msf.records.data.app;

import android.content.ContentResolver;

import org.msf.records.data.app.converters.AppTypeConverterModule;
import org.msf.records.data.app.converters.AppTypeConverters;

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

    @Provides @Singleton AppModel provideAppModel(
            ContentResolver contentResolver, AppTypeConverters converters) {
        return new AppModel(contentResolver, converters);
    }
}
