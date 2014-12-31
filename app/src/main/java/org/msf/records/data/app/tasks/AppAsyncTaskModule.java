package org.msf.records.data.app.tasks;

import android.content.ContentResolver;
import android.os.AsyncTask;

import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.net.Server;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module that provides bindings for the app model's {@link AsyncTask}s.
 */
@Module(
        complete = false,
        library = true)
public class AppAsyncTaskModule {

    @Provides
    @Singleton
    AppAsyncTaskFactory provideAppAsyncTaskFactory(
            Server server,
            ContentResolver contentResolver,
            AppTypeConverters converters) {
        return new AppAsyncTaskFactory(converters, server, contentResolver);
    }
}
