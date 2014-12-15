package org.msf.records.data.app.tasks;

import android.content.ContentResolver;
import android.os.AsyncTask;
import android.util.Log;

import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.PatientAddFailedEvent;
import org.msf.records.filter.UuidFilter;
import org.msf.records.net.Server;

/**
 * An {@link AsyncTask} that adds a patient to a server.
 */
public class AppAddPatientAsyncTask extends AsyncTask<Void, Void, PatientAddFailedEvent> {

    private static final String TAG = AppAddPatientAsyncTask.class.getSimpleName();

    private final AppAsyncTaskFactory mTaskFactory;
    private final AppTypeConverters mConverters;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final AppPatientDelta mPatientDelta;
    private final CrudEventBus mBus;

    private String mUuid;

    public AppAddPatientAsyncTask(
            AppAsyncTaskFactory taskFactory,
            AppTypeConverters converters,
            Server server,
            ContentResolver contentResolver,
            AppPatientDelta patientDelta,
            CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mConverters = converters;
        mServer = server;
        mContentResolver = contentResolver;
        mPatientDelta = patientDelta;
        mBus = bus;
    }

    @Override
    protected PatientAddFailedEvent doInBackground(Void... params) {
        return null;
    }

    @Override
    protected void onPostExecute(PatientAddFailedEvent event) {
        // If an error occurred, post the error event.
        if (event != null) {
            mBus.post(event);
            return;
        }

        // If the UUID was not set, a programming error occurred. Log and post an error event.
        if (mUuid == null) {
            Log.wtf(
                    TAG,
                    "Although a patient add ostensibly succeeded, no UUID was set for the newly-"
                            + "added patient. This indicates a programming error.");

            mBus.post(new PatientAddFailedEvent(
                    PatientAddFailedEvent.REASON_UNKNOWN, null /*exception*/));
        }

        // Otherwise, start a fetch task to fetch the patient from the database.
        FetchSingleAsyncTask<AppPatient> task = mTaskFactory.newFetchSingleAsyncTask(
                new UuidFilter(), mUuid, mConverters.patient, mBus);
        task.execute();
    }
}
