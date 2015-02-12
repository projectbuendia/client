package org.msf.records.data.app.tasks;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;

import com.android.volley.toolbox.RequestFuture;

import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.PatientAddFailedEvent;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.data.SingleItemFetchFailedEvent;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.filter.db.patient.UuidFilter;
import org.msf.records.net.Server;
import org.msf.records.net.model.Patient;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.utils.Logger;

import java.util.concurrent.ExecutionException;

/**
 * An {@link AsyncTask} that adds a patient to a server.
 */
public class AppAddPatientAsyncTask extends AsyncTask<Void, Void, PatientAddFailedEvent> {

    private static final Logger LOG = Logger.create();

    private static final SimpleSelectionFilter FILTER = new UuidFilter();

    private final AppAsyncTaskFactory mTaskFactory;
    private final AppTypeConverters mConverters;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final AppPatientDelta mPatientDelta;
    private final CrudEventBus mBus;

    private String mUuid;

    /**
     * Creates a new {@link AppAddPatientAsyncTask}.
     */
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

        mServer.addPatient(mPatientDelta, patientFuture, patientFuture);
        Patient patient;
        try {
            patient = patientFuture.get();
        } catch (InterruptedException e) {
            return new PatientAddFailedEvent(PatientAddFailedEvent.REASON_INTERRUPTED, e);
        } catch (ExecutionException e) {
            // TODO(dxchen): Parse the VolleyError to see exactly what kind of error was raised.
            return new PatientAddFailedEvent(PatientAddFailedEvent.REASON_NETWORK, e);
        }

        if (patient.uuid == null) {
            LOG.e(
                    "Although the server reported a patient successfully added, it did not return "
                            + "a UUID for that patient. This indicates a server error.");

            return new PatientAddFailedEvent(
                    PatientAddFailedEvent.REASON_SERVER, null /*exception*/);
        }

        AppPatient appPatient = AppPatient.fromNet(patient);
        Uri uri = mContentResolver.insert(
                Contracts.Patients.CONTENT_URI, appPatient.toContentValues());

        // Perform incremental observation sync so we get admission date.
        GenericAccountService.forceIncrementalObservationSync();

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
            LOG.e(
                    "Although a patient add ostensibly succeeded, no UUID was set for the newly-"
                            + "added patient. This indicates a programming error.");

            mBus.post(new PatientAddFailedEvent(
                    PatientAddFailedEvent.REASON_UNKNOWN, null /*exception*/));
            return;
        }

        // Otherwise, start a fetch task to fetch the patient from the database.
        mBus.register(new CreationEventSubscriber());
        FetchSingleAsyncTask<AppPatient> task = mTaskFactory.newFetchSingleAsyncTask(
                Contracts.Patients.CONTENT_URI,
                PatientProjection.getProjectionColumns(),
                new UuidFilter(),
                mUuid,
                mConverters.patient,
                mBus);
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
