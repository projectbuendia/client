package org.msf.records.data.app;

import org.msf.records.App;
import org.msf.records.data.app.converters.AppTypeConverterModule;
import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.data.app.tasks.AppAsyncTaskFactory;
import org.msf.records.data.app.tasks.AppAsyncTaskModule;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * A Dagger module that provides bindings for the app model.
 */
@Module(
        includes = {
                AppTypeConverterModule.class,
                AppAsyncTaskModule.class
        },
        complete = false,
        library = true)
public class AppModelModule {

    @Provides
    @Singleton
    AppModel provideAppModel(
            AppTypeConverters converters,
            AppAsyncTaskFactory taskFactory) {
        return new AppModel(App.getInstance().getContentResolver(), converters, taskFactory);
    }
}
