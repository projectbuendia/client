package org.msf.records.data.app.converters;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module that provides bindings for the app model's {@link AppTypeConverters}.
 */
@Module(
        complete = false,
        library = true)
public class AppTypeConverterModule {

    @Provides
    @Singleton
    AppTypeConverters provideAppTypeConverters() {
        return new AppTypeConverters(
                new AppPatientConverter());
    }
}
