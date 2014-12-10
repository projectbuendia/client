package org.msf.records.ui.chart;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.msf.records.App;
import org.msf.records.events.mvcmodels.ModelReadyEvent;
import org.msf.records.events.mvcmodels.ModelUpdatedEvent;
import org.msf.records.filter.FilterQueryProviderFactory;
import org.msf.records.filter.UuidFilter;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Concept;
import org.msf.records.mvcmodels.Models;
import org.msf.records.mvcmodels.PatientModel;
import org.msf.records.net.Constants;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.net.model.ChartStructure;
import org.msf.records.net.model.ConceptList;
import org.msf.records.net.model.Patient;
import org.msf.records.net.model.PatientAge;
import org.msf.records.net.model.PatientChart;
import org.msf.records.net.model.User;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;
import org.msf.records.sync.PatientProjection;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.odk.collect.android.model.PrepopulatableFields;

import com.android.volley.Response;
import com.android.volley.VolleyError;

/**
 * Controller for {@link PatientChartActivity}.
 *
 * <p>Do not add untestable dependencies to this class.
 */
final class PatientChartController {

    private static final String TAG = PatientChartController.class.getName();
    private static final boolean DEBUG = true;

    private static final String KEY_PENDING_UUIDS = "pendingUuids";

    /*
     * The ODK code for filling in a form has no way of attaching metadata to it This means we can't
     * pass which patient is currently being edited. Instead, we keep an array of up to
     * MAX_ODK_REQUESTS patientUuids. We then send request code BASE_ODK_REQUEST + index, and roll
     * through the array. The array is persisted through activity restart in the savedInstanceState.
     */
    private static final int BASE_ODK_REQUEST = 100;
    // In reality we probably never need more than one request, but be safe.
    private static final int MAX_ODK_REQUESTS = 10;
    private int nextIndex = 0;

    // TODO: Use a map for this instead of an array.
    /** Pending requests. */
    private final String[] mPatientUuids;
    private Patient mPatient = new Patient();

    private long mLastObservation = Long.MIN_VALUE;
    private String mPatientUuid = "";

    public interface Ui {
    	void setTitle(String title);
		void updatePatientVitalsUI(Map<String, LocalizedObservation> observations);
		void setLatestEncounter(long latestEncounterTimeMillis);
		void setObservationHistory(List<LocalizedObservation> observations);
		void setPatient(Patient patient);
		void fetchAndShowXform(
	    		String formUuid,
	    		int requestCode,
	    		org.odk.collect.android.model.Patient patient,
	    		PrepopulatableFields fields);
    }

    public Bundle getState() {
    	Bundle bundle = new Bundle();
    	bundle.putStringArray("pendingUuids", mPatientUuids);
    	return bundle;
    }

    private final OpenMrsChartServer mServer;
    private final LoaderManager mLoaderManager;
    private final FilterQueryProviderFactory mFilterQueryProviderFactory;
    private final EventBusRegistrationInterface mEventBus;
    private final OdkResultSender mOdkResultSender;
    private final Ui mUi;
    private final ObservationsProvider mObservationsProvider;

    private final LoaderCallbacks mLoaderCallbacks = new LoaderCallbacks();
    private final EventSubscriber mEventBusSubscriber = new EventSubscriber();

    public interface OdkResultSender {
        void sendOdkResultToServer(
                @Nullable String patientUuid,
                int resultCode,
                Intent data);
    }

    public interface ObservationsProvider {
        /** Get all observations for a given patient from the local cache, localized to English.  */
        List<LocalizedObservation> getObservations(String patientUuid);
    	Map<String, LocalizedChartHelper.LocalizedObservation>
    			getMostRecentObservations(String patientUuid);
    }

    public PatientChartController(
    		OpenMrsChartServer server,
    		EventBusRegistrationInterface eventBus,
    		LoaderManager loaderManager,
    		FilterQueryProviderFactory filterQueryProviderFactory,
    		Ui ui,
    		OdkResultSender odkResultSender,
    		ObservationsProvider observationsProvider,
    		@Nullable Bundle savedState) {
    	mServer = server;
    	mEventBus = eventBus;
    	mLoaderManager = loaderManager;
    	mFilterQueryProviderFactory = filterQueryProviderFactory;
    	mUi = ui;
    	mOdkResultSender = odkResultSender;
    	mObservationsProvider = observationsProvider;
    	if (savedState != null) {
    		mPatientUuids = savedState.getStringArray(KEY_PENDING_UUIDS);
    	} else {
    		mPatientUuids = new String[MAX_ODK_REQUESTS];
    	}
    }

    public void setPatient(
    		String patientUuid,
    		@Nullable String patientName,
    		@Nullable String patientId) {
    	mPatientUuid = patientUuid;

    	if (patientName != null && patientId != null) {
            mUi.setTitle(patientName + " (" + patientId + ")");
        } else {
        	mUi.setTitle("");
        }
    }

    public void init() {
    	mEventBus.registerSticky(mEventBusSubscriber);
    	prodServer();
    	retrievePatientData();
    }

    private void retrievePatientData() {
        mLoaderManager.restartLoader(1, null, mLoaderCallbacks);
    }

    public void suspend() {
    	mEventBus.unregister(mEventBusSubscriber);
    }

    private void prodServer() {
    	// TODO(dxchen): This doesn't properly handle configuration changes. We should pass this
        // into the fragment arguments.
        // TODO(nfortescue): get proper caching, and the dictionary working.
    	// TODO: Document what this does!
    	mServer.getChart(mPatientUuid, new Response.Listener<PatientChart>() {
            @Override
            public void onResponse(PatientChart response) {
                Log.i(TAG, response.uuid + " " + Arrays.asList(response.encounters));
            }
        },
        new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Unexpected error on fetching chart", error);
            }
        });
    	mServer.getConcepts(
                new Response.Listener<ConceptList>() {
                    @Override
                    public void onResponse(ConceptList response) {
                        Log.i(TAG,  "Response: " + Integer.toString(response.results.length));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Unexpected error fetching concepts", error);
                    }
                });
    	mServer.getChartStructure("ea43f213-66fb-4af6-8a49-70fd6b9ce5d4",
                new Response.Listener<ChartStructure>() {
                    @Override
                    public void onResponse(ChartStructure response) {
                        Log.i(TAG, "Response: " + Arrays.asList(response.groups).toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Unexpected error fetching concepts", error);
                    }
                });
    }

    private void updatePatientUI() {
        // Get the observations
        // TODO(dxchen,nfortescue): Background thread this, or make this call async-like.
        List<LocalizedObservation> observations = mObservationsProvider.getObservations(mPatientUuid);
        Map<String, LocalizedObservation> conceptsToLatestObservations = mObservationsProvider.getMostRecentObservations(mPatientUuid);

        // Update timestamp
        for (LocalizedObservation observation : observations) {
            conceptsToLatestObservations.put(observation.conceptUuid, observation);
            mLastObservation = Math.max(
            		mLastObservation,
            		observation.encounterTimeMillis);
        }

        if (DEBUG) {
        	Log.d(TAG, "Showing " + observations.size() + " observations, and " + conceptsToLatestObservations.size() + " latest observations");
        }

        mUi.setLatestEncounter(mLastObservation);
        mUi.updatePatientVitalsUI(conceptsToLatestObservations);
    	mUi.setObservationHistory(observations);
    }

    /** Call when the user has indicated they want to add observation data. */
    public void onAddObservationPressed() {
        onAddObservationPressed(null);
    }

    /** Call when the user has indicated they want to add observation data. */
    public void onAddObservationPressed(String targetGroup) {
        PrepopulatableFields fields = new PrepopulatableFields();

        fields.mEncounterTime = DateTime.now();
        fields.mLocationName = "Triage";

        User user = App.getUserManager().getActiveUser();
        if (user != null) {
            fields.mClinicianName = user.getFullName();
        }

        Map<String, LocalizedChartHelper.LocalizedObservation> observations =
        		mObservationsProvider.getMostRecentObservations(mPatientUuid);

        if (observations.containsKey(Concept.PREGNANCY_UUID)
                && Concept.YES_UUID.equals(observations.get(Concept.PREGNANCY_UUID).value)) {
            fields.mPregnant = PrepopulatableFields.YES;
        }

        if (observations.containsKey(Concept.IV_UUID)
                && Concept.YES_UUID.equals(observations.get(Concept.IV_UUID).value)) {
            fields.mIvFitted = PrepopulatableFields.YES;
        }

        fields.mTargetGroup = targetGroup;

        mUi.fetchAndShowXform(
                Constants.ADD_OBSERVATION_UUID,
                savePatientUuidForRequestCode(mPatientUuid),
                PatientModel.INSTANCE.getOdkPatient(mPatientUuid),
                fields);
    }

    private int savePatientUuidForRequestCode(String patientUuid) {
        synchronized (mPatientUuids) {
            mPatientUuids[nextIndex] = patientUuid;
            int requestCode = BASE_ODK_REQUEST + nextIndex;
            nextIndex = (nextIndex + 1) % MAX_ODK_REQUESTS;
            return requestCode;
        }
    }

    @Nullable public String getAndClearPatientUuidForRequestCode(int requestCode) {
        int index = requestCode - BASE_ODK_REQUEST;
        String patientUuid = mPatientUuids[index];
        mPatientUuids[index] = null;
        return patientUuid;
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class EventSubscriber {
    	// TODO(dxchen): Replace the below two when https://github.com/greenrobot/EventBus/issues/135 is
        // resolved.
        public void onEventMainThread(ModelReadyEvent event) {
            if (event.shouldRead(Models.OBSERVATIONS)) {
                retrievePatientData();
                updatePatientUI();
            }
        }

        public void onEventMainThread(ModelUpdatedEvent event) {
            if (event.shouldRead(Models.OBSERVATIONS)) {
                retrievePatientData();
                updatePatientUI();
            }
        }
    }

    private final class LoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
	    @Override
	    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
	    	return mFilterQueryProviderFactory.getCursorLoader(new UuidFilter(), mPatientUuid);
	    }

	    @Override
	    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
	    	try {
		        data.moveToFirst();

		        mPatient.uuid = mPatientUuid;
		        mPatient.given_name = data.getString(PatientProjection.COLUMN_GIVEN_NAME);
		        mPatient.family_name = data.getString(PatientProjection.COLUMN_FAMILY_NAME);
		        mPatient.gender = data.getString(PatientProjection.COLUMN_GENDER);

		        PatientAge age = new PatientAge();
		        age.years = data.getInt(PatientProjection.COLUMN_AGE_YEARS);
		        age.months = data.getInt(PatientProjection.COLUMN_AGE_MONTHS);
		        if (age.years > 0) {
		            age.type = "years";
		        }
		        mPatient.age = age;
		        mPatient.id = data.getString(PatientProjection.COLUMN_ID);
		        LocationSubtree location = LocationTree.SINGLETON_INSTANCE.getLocationByUuid(
		                data.getString(PatientProjection.COLUMN_LOCATION_UUID));
		        mPatient.assigned_location = (location == null) ? null : location.getLocation();
		        mPatient.admission_timestamp = data.getLong(PatientProjection.COLUMN_ADMISSION_TIMESTAMP);

		        mLastObservation = Math.max(
		        		mLastObservation,
		        		mPatient.admission_timestamp * 1000);
		        mUi.setLatestEncounter(mLastObservation);

		        mUi.setPatient(mPatient);
	    	} finally {
	    		data.close();
	    	}
	    }

	    @Override
	    public void onLoaderReset(Loader<Cursor> loader) {

	    }
    }
}
