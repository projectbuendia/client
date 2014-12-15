package org.msf.records.data.app.tasks;

import android.content.ContentResolver;
import android.os.AsyncTask;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.PatientUpdateFailedEvent;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.filter.UuidFilter;
import org.msf.records.net.Server;
import org.msf.records.net.model.Patient;
import org.msf.records.sync.PatientProviderContract;

import java.util.concurrent.ExecutionException;

/**
 * An {@link AsyncTask} that updates a patient.
 */
public class AppUpdatePatientAsyncTask extends AsyncTask<Void, Void, PatientUpdateFailedEvent> {

    private static final String TAG = AppUpdatePatientAsyncTask.class.getSimpleName();

    private static final SimpleSelectionFilter FILTER = new UuidFilter();

    private final AppAsyncTaskFactory mTaskFactory;
    private final AppTypeConverters mConverters;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final String mUuid;
    private final AppPatientDelta mPatientDelta;
    private final CrudEventBus mBus;

    public AppUpdatePatientAsyncTask(
            AppAsyncTaskFactory taskFactory,
            AppTypeConverters converters,
            Server server,
            ContentResolver contentResolver,
            String uuid,
            AppPatientDelta patientDelta,
            CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mConverters = converters;
        mServer = server;
        mContentResolver = contentResolver;
        mUuid = uuid;
        mPatientDelta = patientDelta;
        mBus = bus;
    }

    @Override
    protected PatientUpdateFailedEvent doInBackground(Void... params) {
        RequestFuture<Patient> patientFuture = RequestFuture.newFuture();

        mServer.updatePatient(mUuid, mPatientDelta, patientFuture, patientFuture, TAG);
        try {
            patientFuture.get();
        } catch (InterruptedException e) {
            return new PatientUpdateFailedEvent(PatientUpdateFailedEvent.REASON_INTERRUPTED, e);
        } catch (ExecutionException e) {
            // TODO(dxchen): Parse the VolleyError to see exactly what kind of error was raised.
            return new PatientUpdateFailedEvent(
                    PatientUpdateFailedEvent.REASON_NETWORK, (VolleyError) e.getCause());
        }

        int count = mContentResolver.update(
                PatientProviderContract.CONTENT_URI,
                mPatientDelta.toContentValues(),
                FILTER.getSelectionString(),
                FILTER.getSelectionArgs(mUuid));

        switch (count) {
            case 0:
                return new PatientUpdateFailedEvent(
                        PatientUpdateFailedEvent.REASON_NO_SUCH_PATIENT, null /*exception*/);
            case 1:
                return null;
            default:
                return new PatientUpdateFailedEvent(
                        PatientUpdateFailedEvent.REASON_SERVER, null /*exception*/);
        }
    }

    @Override
    protected void onPostExecute(PatientUpdateFailedEvent event) {
        // If an error occurred, post the error event.
        if (event != null) {
            mBus.post(event);
            return;
        }

        // Otherwise, start a fetch task to fetch the patient from the database.
        FetchSingleAsyncTask<AppPatient> task = mTaskFactory.newFetchSingleAsyncTask(
                new UuidFilter(), mUuid, mConverters.patient, mBus);
        task.execute();
    }
}