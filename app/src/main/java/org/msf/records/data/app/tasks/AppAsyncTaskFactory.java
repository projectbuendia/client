package org.msf.records.data.app.tasks;

import android.content.ContentResolver;
import android.os.AsyncTask;

import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.data.app.AppTypeBase;
import org.msf.records.data.app.converters.AppTypeConverter;
import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.events.CrudEventBus;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.net.Server;

/**
 * An assisted injection factory that creates {@link AppModel} {@link AsyncTask}s.
 */
public class AppAsyncTaskFactory {

    private final AppTypeConverters mConverters;
    private final Server mServer;
    private final ContentResolver mContentResolver;

    public AppAsyncTaskFactory(
            AppTypeConverters converters, Server server, ContentResolver contentResolver) {
        mConverters = converters;
        mServer = server;
        mContentResolver = contentResolver;
    }

    public AppAddPatientAsyncTask newAddPatientAsyncTask(
            AppPatientDelta patientDelta, CrudEventBus bus) {
        return new AppAddPatientAsyncTask(
                this, mConverters, mServer, mContentResolver, patientDelta, bus);
    }

    public AppUpdatePatientAsyncTask newUpdatePatientAsyncTask(
            AppPatient originalPatient, AppPatientDelta patientDelta, CrudEventBus bus) {
        return new AppUpdatePatientAsyncTask(
                this, mConverters, mServer, mContentResolver, originalPatient, patientDelta, bus);
    }

    public <T extends AppTypeBase<?>> FetchSingleAsyncTask<T> newFetchSingleAsyncTask(
            SimpleSelectionFilter filter,
            String constraint,
            AppTypeConverter<T> converter,
            CrudEventBus bus) {
        return new FetchSingleAsyncTask<>(mContentResolver, filter, constraint, converter, bus);
    }
}
