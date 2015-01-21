package org.msf.records.ui.chart;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import org.msf.records.App;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.FetchXformFailedEvent;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.events.data.PatientUpdateFailedEvent;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.model.Concept;
import org.msf.records.mvcmodels.PatientModel;
import org.msf.records.net.model.User;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.tentselection.AssignLocationDialog;
import org.msf.records.ui.tentselection.AssignLocationDialog.TentSelectedCallback;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.LocaleSelector;
import org.msf.records.utils.Logger;
import org.odk.collect.android.model.Patient;
import org.odk.collect.android.model.PrepopulatableFields;

import com.google.common.base.Optional;

import de.greenrobot.event.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Controller for {@link PatientChartActivity}.
 *
 * <p>Do not add untestable dependencies to this class.
 */
final class PatientChartController {

    private static final Logger LOG = Logger.create();

    private static final boolean DEBUG = true;

    private static final String KEY_PENDING_UUIDS = "pendingUuids";

    // The ODK code for filling in a form has no way of attaching metadata to it.
    // This means we can't pass which patient is currently being edited. Instead, we keep an array
    // of up to MAX_ODK_REQUESTS patientUuids. The array is persisted through activity restart in
    // the savedInstanceState.

    /** Maximum concurrent ODK forms assigned request codes. */
    private static final int MAX_ODK_REQUESTS = 10;
    private int nextIndex = 0;

    // TODO: Use a map for this instead of an array.
    private final String[] mPatientUuids;
    private AppPatient mPatient = AppPatient.builder().build();
    private AppLocationTree mLocationTree;
    private long mLastObservation = Long.MIN_VALUE;
    private String mPatientUuid = "";

    public interface Ui {
        /** Sets the activity title. */
        void setTitle(String title);

        /** Updates the UI showing current observation values for this patient. */
        void updatePatientVitalsUI(Map<String, LocalizedObservation> observations);

        /** Updates the UI with the patient's location. */
        void updatePatientLocationUi(AppLocationTree locationTree, AppPatient patient);

        /** Updates the UI showing the historic log of observation values for this patient. */
        void setObservationHistory(List<LocalizedObservation> observations);

        /** Shows the last time a user interacted with this patient. */
        void setLatestEncounter(long latestEncounterTimeMillis);

        /** Shows the patient's personal details. */
        void setPatient(AppPatient patient);

        /** Starts a new form activity to collect observations from the user. */
        void fetchAndShowXform(
                PatientChartActivity.XForm form,
                int code,
                Patient patient,
                PrepopulatableFields fields);

        /** Re-enables fetching. */
        void reEnableFetch();
    }

    private final EventBusRegistrationInterface mDefaultEventBus;
    private final CrudEventBus mCrudEventBus;
    private final OdkResultSender mOdkResultSender;
    private final Ui mUi;
    private final LocalizedChartHelper mObservationsProvider;
    private final AppModel mAppModel;
    private final EventSubscriber mEventBusSubscriber = new EventSubscriber();
    private final PatientModel mPatientModel;
    private final SyncManager mSyncManager;
    private final MinimalHandler mMainThreadHandler;

    private AssignLocationDialog mAssignLocationDialog;

    /** Sends ODK form data. */
    public interface OdkResultSender {
        void sendOdkResultToServer(
                @Nullable String patientUuid,
                int resultCode,
                Intent data);
    }

    public interface MinimalHandler {
        void post(Runnable runnable);
    }

    public PatientChartController(
            AppModel appModel,
            EventBusRegistrationInterface defaultEventBus,
            CrudEventBus crudEventBus,
            Ui ui,
            OdkResultSender odkResultSender,
            LocalizedChartHelper observationsProvider,
            @Nullable Bundle savedState,
            PatientModel patientModel,
            SyncManager syncManager,
            MinimalHandler mainThreadHandler) {
        mAppModel = appModel;
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
        mMainThreadHandler = mainThreadHandler;
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
        mAppModel.fetchSinglePatient(mCrudEventBus, mPatientUuid);
        mAppModel.fetchLocationTree(mCrudEventBus, LocaleSelector.getCurrentLocale().toString());
    }

    /** Releases any resources used by the controller. */
    public void suspend() {
        mCrudEventBus.unregister(mEventBusSubscriber);
        mDefaultEventBus.unregister(mEventBusSubscriber);
    }

    public void onXFormResult(int code, int resultCode, Intent data) {
        PatientChartActivity.RequestCode requestCode =
                new PatientChartActivity.RequestCode(code);

        String patientUuid = getAndClearPatientUuidForRequestCode(code);
        if (patientUuid == null) {
            LOG.e("Received unknown request code: " + code);
            return;
        }

        switch (requestCode.form) {
            case ADD_OBSERVATION:
                // This will fire a CreatePatientSucceededEvent.
                mOdkResultSender.sendOdkResultToServer(patientUuid, resultCode, data);
                break;
            case ADD_TEST_RESULTS:
                // This will fire a CreatePatientSucceededEvent.
                mOdkResultSender.sendOdkResultToServer(patientUuid, resultCode, data);
                break;
            default:
                LOG.e(
                        "Received an ODK result for a form that we do not know about: '%1$s'. This "
                                + "indicates programmer error.", requestCode.form.toString());
                break;
        }
    }

    /** Call when the user has indicated they want to add observation data. */
    public void onAddObservationPressed() {
        onAddObservationPressed(null);
    }

    /**
     * Call when the user has indicated they want to add observation data.
     * @param targetGroup the description of the corresponding group in the XForm. This corresponds
     *                    with the "description" field in OpenMRS.
     */
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
                PatientChartActivity.XForm.ADD_OBSERVATION,
                savePatientUuidForRequestCode(
                        PatientChartActivity.XForm.ADD_OBSERVATION, mPatientUuid),
                mPatientModel.getOdkPatient(mPatientUuid),
                fields);
    }

    public void onAddTestResultsPressed() {
        PrepopulatableFields fields = new PrepopulatableFields();

        // TODO(dxchen): Re-enable this post v0.2.1.
//        fields.encounterTime = DateTime.now();
        fields.locationName = "Triage";

        User user = App.getUserManager().getActiveUser();
        if (user != null) {
            fields.clinicianName = user.getFullName();
        }

        mUi.fetchAndShowXform(
                PatientChartActivity.XForm.ADD_TEST_RESULTS,
                savePatientUuidForRequestCode(
                        PatientChartActivity.XForm.ADD_TEST_RESULTS, mPatientUuid),
                mPatientModel.getOdkPatient(mPatientUuid),
                fields);
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
            LOG.d("Showing " + observations.size() + " observations, and "
                    + conceptsToLatestObservations.size() + " latest observations");
        }

        mUi.setLatestEncounter(mLastObservation);
        mUi.updatePatientVitalsUI(conceptsToLatestObservations);
        mUi.setObservationHistory(observations);
    }

    /**
     * Returns a requestCode that can be sent to ODK Xform activity representing the given UUID.
     */
    private int savePatientUuidForRequestCode(PatientChartActivity.XForm form, String patientUuid) {
        mPatientUuids[nextIndex] = patientUuid;
        int requestCode = new PatientChartActivity.RequestCode(form, nextIndex).getCode();
        nextIndex = (nextIndex + 1) % MAX_ODK_REQUESTS;
        return requestCode;
    }

    public void showAssignLocationDialog(
            Context context,
            final MenuItem menuItem) {
        TentSelectedCallback callback =
                new TentSelectedCallback() {

                    @Override
                    public boolean onNewTentSelected(String newTentUuid) {
                        AppPatientDelta patientDelta = new AppPatientDelta();
                        patientDelta.assignedLocationUuid = Optional.of(newTentUuid);

                        mAppModel.updatePatient(mCrudEventBus, mPatient, patientDelta);
                        return false;
                    }
                };

        Runnable reEnableButton = new Runnable() {
            @Override
            public void run() {
                menuItem.setEnabled(true);
            }
        };
        mAssignLocationDialog = new AssignLocationDialog(
                context,
                mAppModel,
                LocaleSelector.getCurrentLocale().getLanguage(),
                reEnableButton,
                mCrudEventBus,
                Optional.of(mPatient.locationUuid),
                callback);

        menuItem.setEnabled(false);
        mAssignLocationDialog.show();
    }

    /**
     * Converts a requestCode that was previously sent to the ODK Xform activity back to a patient UUID.
     *
     * <p>Also removes details of that requestCode from the controller's state.
     */
    @Nullable
    private String getAndClearPatientUuidForRequestCode(int code) {
        PatientChartActivity.RequestCode requestCode = new PatientChartActivity.RequestCode(code);
        String patientUuid = mPatientUuids[requestCode.requestIndex];
        mPatientUuids[requestCode.requestIndex] = null;
        return patientUuid;
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class EventSubscriber {

        public void onEventMainThread(AppLocationTreeFetchedEvent event) {
            mLocationTree = event.tree;
            updatePatientLocationUi();
        }

        public void onEventMainThread(SingleItemCreatedEvent<AppPatient> event) {
            mSyncManager.forceSync();
        }

        public void onEventMainThread(SyncSucceededEvent event) {
            updatePatientUI();
        }

        public void onEventMainThread(SingleItemFetchedEvent<AppPatient> event) {
            mPatient = event.item;
            mUi.setPatient(mPatient);
            updatePatientLocationUi();

            if (mAssignLocationDialog != null) {
                mAssignLocationDialog.dismiss();
                mAssignLocationDialog = null;
            }

            // TODO(dxchen): Displaying the observations part of the UI takes a lot of
            // main-thread time. This delays rendering of the rest of UI.
            // To allow the rest of the UI to be displayed before we attempt to populate
            // the observations, we delay the observation update slightly.
            // We need this hack because we load observations on the main thread. We should change
            // this to use a background thread. Either an async task or using CrudEventBus events.

            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    updatePatientUI();
                }
            });
        }

        public void onEventMainThread(PatientUpdateFailedEvent event) {
            mAssignLocationDialog.onPatientUpdateFailed(event.reason);
        }

        public void onEventMainThread(FetchXformFailedEvent event) {
            mUi.reEnableFetch();
        }
    }

    private synchronized void updatePatientLocationUi() {
        if (mLocationTree != null && mPatient != null && mPatient.locationUuid != null) {
            mUi.updatePatientLocationUi(mLocationTree, mPatient);
        }
    }
}
