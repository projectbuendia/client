package org.msf.records.ui.chart;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.base.Optional;

import org.msf.records.App;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.events.CreatePatientSucceededEvent;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.PatientUpdateFailedEvent;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.location.LocationManager;
import org.msf.records.model.Concept;
import org.msf.records.mvcmodels.PatientModel;
import org.msf.records.net.Constants;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.net.model.ChartStructure;
import org.msf.records.net.model.ConceptList;
import org.msf.records.net.model.PatientChart;
import org.msf.records.net.model.User;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.tentselection.AssignLocationDialog;
import org.msf.records.utils.EventBusWrapper;
import org.odk.collect.android.model.PrepopulatableFields;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.greenrobot.event.EventBus;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.msf.records.ui.tentselection.AssignLocationDialog.TentSelectedCallback;

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
    /** Maximum concurrent ODK forms assigned request codes. */
    private static final int MAX_ODK_REQUESTS = 10;
    private int nextIndex = 0;

    // TODO: Use a map for this instead of an array.
    private final String[] mPatientUuids;
    private AppPatient mPatient = AppPatient.builder().build();
    private long mLastObservation = Long.MIN_VALUE;
    private String mPatientUuid = "";

    public interface Ui {
    	/** Sets the activity title. */
    	void setTitle(String title);

    	/** Updates the UI showing current observation values for this patient. */
		void updatePatientVitalsUI(Map<String, LocalizedObservation> observations);

		/** Updates the UI showing the historic log of observation values for this patient. */
		void setObservationHistory(List<LocalizedObservation> observations);

		/** Shows the last time a user interacted with this patient. */
		void setLatestEncounter(long latestEncounterTimeMillis);

		/** Shows the patient's personal details. */
		void setPatient(AppPatient patient);

		/** Starts a new form activity to collect observations from the user. */
		void fetchAndShowXform(
	    		String formUuid,
	    		int requestCode,
	    		org.odk.collect.android.model.Patient patient,
	    		PrepopulatableFields fields);
    }

    private final OpenMrsChartServer mServer;
    private final EventBusWrapper mDefaultEventBus;
    private final CrudEventBus mCrudEventBus;
    private final OdkResultSender mOdkResultSender;
    private final Ui mUi;
    private final ObservationsProvider mObservationsProvider;
    private final AppModel mAppModel;
    private final EventSubscriber mEventBusSubscriber = new EventSubscriber();
    private final PatientModel mPatientModel;
    private final SyncManager mSyncManager;

    private AssignLocationDialog mAssignLocationDialog;

    /** Sends ODK form data. */
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
    		AppModel appModel,
    		OpenMrsChartServer server,
            EventBusWrapper defaultEventBus,
    		CrudEventBus crudEventBus,
    		Ui ui,
    		OdkResultSender odkResultSender,
    		ObservationsProvider observationsProvider,
    		@Nullable Bundle savedState,
    		PatientModel patientModel,
            SyncManager syncManager) {
    	mAppModel = appModel;
    	mServer = checkNotNull(server);
        mDefaultEventBus = defaultEventBus;
    	mCrudEventBus = crudEventBus;
    	mUi = ui;
    	mOdkResultSender = odkResultSender;
    	mObservationsProvider = observationsProvider;
    	mPatientModel = patientModel;
    	if (savedState != null) {
    		mPatientUuids = savedState.getStringArray(KEY_PENDING_UUIDS);
    	} else {
    		mPatientUuids = new String[MAX_ODK_REQUESTS];
    	}
        mSyncManager = syncManager;
    }

    /** Returns the state of the controller. This should be saved to preserve it over activity restarts. */
    public Bundle getState() {
    	Bundle bundle = new Bundle();
    	bundle.putStringArray("pendingUuids", mPatientUuids);
    	return bundle;
    }

    /** Sets the current patient. This should be called before init. */
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

    /** Initializes the controller, setting async operations going to collect data required by the UI. */
    public void init() {
        mDefaultEventBus.register(mEventBusSubscriber);
    	mCrudEventBus.register(mEventBusSubscriber);
    	prodServer();
    	mAppModel.fetchSinglePatient(mCrudEventBus, mPatientUuid);
    }

	/** Releases any resources used by the controller. */
    public void suspend() {
    	mCrudEventBus.unregister(mEventBusSubscriber);
        mDefaultEventBus.unregister(mEventBusSubscriber);
    }

    public void onXFormResult(int requestCode, int resultCode, Intent data) {
    	String patientUuid = getAndClearPatientUuidForRequestCode(requestCode);
        if (patientUuid == null) {
            Log.e(TAG, "Received unknown request code: " + requestCode);
            return;
        }

        // This will fire a CreatePatientSucceededEvent.
        mOdkResultSender.sendOdkResultToServer(patientUuid, resultCode, data);
    }

    /** Call when the user has indicated they want to add observation data. */
    public void onAddObservationPressed() {
        onAddObservationPressed(null);
    }

    /** Call when the user has indicated they want to add observation data. */
    public void onAddObservationPressed(String targetGroup) {
        PrepopulatableFields fields = new PrepopulatableFields();

        // TODO(dxchen): Re-enable this post v0.2.1.
//        fields.encounterTime = DateTime.now();
        fields.locationName = "Triage";

        User user = App.getUserManager().getActiveUser();
        if (user != null) {
            fields.clinicianName = user.getFullName();
        }

        Map<String, LocalizedChartHelper.LocalizedObservation> observations =
        		mObservationsProvider.getMostRecentObservations(mPatientUuid);

        if (observations.containsKey(Concept.PREGNANCY_UUID)
                && Concept.YES_UUID.equals(observations.get(Concept.PREGNANCY_UUID).value)) {
            fields.pregnant = PrepopulatableFields.YES;
        }

        if (observations.containsKey(Concept.IV_UUID)
                && Concept.YES_UUID.equals(observations.get(Concept.IV_UUID).value)) {
            fields.ivFitted = PrepopulatableFields.YES;
        }

        fields.targetGroup = targetGroup;

        mUi.fetchAndShowXform(
                Constants.ADD_OBSERVATION_UUID,
                savePatientUuidForRequestCode(mPatientUuid),
                mPatientModel.getOdkPatient(mPatientUuid),
                fields);
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
                        Log.i(TAG, "Response: " + Integer.toString(response.results.length));
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

    /** Gets the latest observation values and displays them on the UI. */
    private void updatePatientUI() {
        // Get the observations
        // TODO(dxchen,nfortescue): Background thread this, or make this call async-like.
        List<LocalizedObservation> observations = mObservationsProvider.getObservations(mPatientUuid);
        Map<String, LocalizedObservation> conceptsToLatestObservations =
        		new HashMap<>(mObservationsProvider.getMostRecentObservations(mPatientUuid));

        // Update timestamp
        for (LocalizedObservation observation : observations) {
        	// TODO(rjlothian): This looks odd. Why do we do this? I'd expect this to be set by getMostRecentObservations instead.
            conceptsToLatestObservations.put(observation.conceptUuid, observation);
            mLastObservation = Math.max(
            		mLastObservation,
            		observation.encounterTimeMillis);
        }

        if (DEBUG) {
        	Log.d(TAG, "Showing " + observations.size() + " observations, and "
        			+ conceptsToLatestObservations.size() + " latest observations");
        }

        mUi.setLatestEncounter(mLastObservation);
        mUi.updatePatientVitalsUI(conceptsToLatestObservations);
    	mUi.setObservationHistory(observations);
    }

    /**
     * Returns a requestCode that can be sent to ODK Xform activity representing the given UUID.
     */
    private int savePatientUuidForRequestCode(String patientUuid) {
        mPatientUuids[nextIndex] = patientUuid;
        int requestCode = BASE_ODK_REQUEST + nextIndex;
        nextIndex = (nextIndex + 1) % MAX_ODK_REQUESTS;
        return requestCode;
    }

    public void showAssignLocationDialog(
            Context context,
            LocationManager locationManager) {
        TentSelectedCallback callback =
                new TentSelectedCallback() {

                    @Override
                    public boolean onNewTentSelected(String newTentUuid) {
                        AppPatientDelta patientDelta = new AppPatientDelta();
                        patientDelta.assignedLocationUuid = Optional.of(newTentUuid);

                        mAppModel.updatePatient(mCrudEventBus, mPatient.uuid, patientDelta);
                        return false;
                    }
                };

        mAssignLocationDialog = new AssignLocationDialog(
                context,
                locationManager,
                new EventBusWrapper(EventBus.getDefault()),
                Optional.of(mPatient.locationUuid),
                callback);

        mAssignLocationDialog.show();
    }

    /**
     * Converts a requestCode that was previously sent to the ODK Xform activity back to a patient UUID.
     *
     * <p>Also removes details of that requestCode from the controller's state.
     */
    @Nullable private String getAndClearPatientUuidForRequestCode(int requestCode) {
        int index = requestCode - BASE_ODK_REQUEST;
        String patientUuid = mPatientUuids[index];
        mPatientUuids[index] = null;
        return patientUuid;
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class EventSubscriber {

        public void onEventMainThread(CreatePatientSucceededEvent event) {
            mSyncManager.forceSync();
        }

        public void onEventMainThread(SyncSucceededEvent event) {
            updatePatientUI();
        }

    	public void onEventMainThread(SingleItemFetchedEvent<AppPatient> event) {
    		mPatient = event.item;
    		mUi.setPatient(mPatient);

            if (mAssignLocationDialog != null) {
                mAssignLocationDialog.dismiss();
                mAssignLocationDialog = null;
            }

    		updatePatientUI();
    	}

        public void onEventMainThread(PatientUpdateFailedEvent event) {
            mAssignLocationDialog.onPatientUpdateFailed(event.reason);
        }
    }
}
