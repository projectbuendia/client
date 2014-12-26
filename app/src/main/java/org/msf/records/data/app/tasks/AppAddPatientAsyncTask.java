package org.msf.records.data.app.tasks;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.PatientAddFailedEvent;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.data.SingleItemFetchFailedEvent;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.filter.UuidFilter;
import org.msf.records.net.Server;
import org.msf.records.net.model.Patient;
import org.msf.records.sync.PatientProviderContract;

import java.util.concurrent.ExecutionException;

/**
 * An {@link AsyncTask} that adds a patient to a server.
 */
public class AppAddPatientAsyncTask extends AsyncTask<Void, Void, PatientAddFailedEvent> {

    private static final String TAG = AppAddPatientAsyncTask.class.getSimpleName();

    private static final SimpleSelectionFilter FILTER = new UuidFilter();

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
        RequestFuture<Patient> patientFuture = RequestFuture.newFuture();

        mServer.addPatient(mPatientDelta, patientFuture, patientFuture, TAG);
        Patient patient;
        try {
            patient = patientFuture.get();
        } catch (InterruptedException e) {
            return new PatientAddFailedEvent(PatientAddFailedEvent.REASON_INTERRUPTED, e);
        } catch (ExecutionException e) {
            // TODO(dxchen): Parse the VolleyError to see exactly what kind of error was raised.
            return new PatientAddFailedEvent(
                    PatientAddFailedEvent.REASON_NETWORK, (VolleyError) e.getCause());
        }

        if (patient.uuid == null) {
            Log.e(
                    TAG,
                    "Although the server reported a patient successfully added, it did not return "
                            + "a UUID for that patient. This indicates a server error.");

            return new PatientAddFailedEvent(
                    PatientAddFailedEvent.REASON_SERVER, null /*exception*/);
        }

        AppPatient appPatient = AppPatient.fromNet(patient);
        Uri uri = mContentResolver.insert(
                PatientProviderContract.CONTENT_URI, appPatient.toContentValues());

        if (uri == null || uri.equals(Uri.EMPTY)) {
            return new PatientAddFailedEvent(
                    PatientAddFailedEvent.REASON_CLIENT, null /*exception*/);
        }

        mUuid = patient.uuid;

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
            return;
        }

        // Otherwise, start a fetch task to fetch the patient from the database.
        mBus.register(new CreationEventSubscriber());
        FetchSingleAsyncTask<AppPatient> task = mTaskFactory.newFetchSingleAsyncTask(
                new UuidFilter(), mUuid, mConverters.patient, mBus);
        task.execute();
    }

    // After updating a patient, we fetch the patient from the database. The result of the fetch
    // determines if adding a patient was truly successful and propagates a new event to report
    // success/failure.
    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class CreationEventSubscriber {
        public void onEventMainThread(SingleItemFetchedEvent<AppPatient> event) {
            mBus.post(new SingleItemCreatedEvent<>(event.item));
            mBus.unregister(this);
        }

        public void onEventMainThread(SingleItemFetchFailedEvent event) {
            mBus.post(new PatientAddFailedEvent(
                    PatientAddFailedEvent.REASON_CLIENT, new Exception(event.error)));
            mBus.unregister(this);
        }
    }
}
