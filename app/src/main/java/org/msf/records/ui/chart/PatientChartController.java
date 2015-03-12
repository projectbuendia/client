package org.msf.records.ui.chart;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppEncounter;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.data.odk.OdkConverter;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.FetchXformFailedEvent;
import org.msf.records.events.FetchXformSucceededEvent;
import org.msf.records.events.SubmitXformFailedEvent;
import org.msf.records.events.SubmitXformSucceededEvent;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.events.data.EncounterAddFailedEvent;
import org.msf.records.events.data.PatientUpdateFailedEvent;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.model.Concepts;
import org.msf.records.net.model.User;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.tentselection.AssignLocationDialog;
import org.msf.records.ui.tentselection.AssignLocationDialog.TentSelectedCallback;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.LocaleSelector;
import org.msf.records.utils.Logger;
import org.msf.records.utils.Utils;
import org.odk.collect.android.model.Patient;
import org.odk.collect.android.model.PrepopulatableFields;

import java.util.HashMap;
import java.util.List;
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

    /**
     * Period between observation syncs while the chart view is active.  It would be nice for
     * this to be even shorter (20 s? 10 s?) but currently the table scroll position resets on
     * each sync.  TODO: Try reducing this period to improve responsiveness, but only after
     * we're able to prevent the table from scrolling to the top on sync, or when we're able to
     * skip re-rendering for syncs that pull down no new patient or observation data.
     */
    private static final int OBSERVATION_SYNC_PERIOD_MILLIS = 60000;

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

    // This value is incremented whenever the controller is activated or suspended.
    // A "phase" is a period of time between such transition points.
    private int mCurrentPhaseId = 0;

    public interface Ui {
        /** Sets the activity title. */
        void setTitle(String title);

        /** Updates the UI showing current observation values for this patient. */
        void updatePatientVitalsUI(Map<String, LocalizedObservation> observations);

        /** Updates the general condition UI with the patient's current condition. */
        void updatePatientGeneralConditionUi(String generalConditionUuid);

        /** Updates the UI with the patient's location. */
        void updatePatientLocationUi(AppLocationTree locationTree, AppPatient patient);

        /** Updates the UI showing the historic log of observation values for this patient. */
        void setObservationHistory(
                List<LocalizedObservation> observations, LocalDate admissionDate);

        /** Shows the last time a user interacted with this patient. */
        void setLatestEncounter(long latestEncounterTimeMillis);

        /** Shows the patient's personal details. */
        void setPatient(AppPatient patient);

        /** Displays an error with the given resource and optional substitution args. */
        void showError(int errorResource, Object... args);

        /** Starts a new form activity to collect observations from the user. */
        void fetchAndShowXform(
                PatientChartActivity.XForm form,
                int code,
                Patient patient,
                PrepopulatableFields fields);

        /** Re-enables fetching. */
        void reEnableFetch();

        /** Displays an error message with the given resource id. */
        void showError(int errorMessageResource);

        /** Shows or hides the form loading dialog. */
        void showFormLoadingDialog(boolean show);

        /** Shows or hides the form submission dialog. */
        void showFormSubmissionDialog(boolean show);
    }

    private final EventBusRegistrationInterface mDefaultEventBus;
    private final CrudEventBus mCrudEventBus;
    private final OdkResultSender mOdkResultSender;
    private final Ui mUi;
    private final LocalizedChartHelper mObservationsProvider;
    private final AppModel mAppModel;
    private final EventSubscriber mEventBusSubscriber = new EventSubscriber();
    private final SyncManager mSyncManager;
    private final MinimalHandler mMainThreadHandler;

    private AssignLocationDialog mAssignLocationDialog;
    private AssignGeneralConditionDialog mAssignGeneralConditionDialog;

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
            SyncManager syncManager,
            MinimalHandler mainThreadHandler) {
        mAppModel = appModel;
        mDefaultEventBus = defaultEventBus;
        mCrudEventBus = crudEventBus;
        mUi = ui;
        mOdkResultSender = odkResultSender;
        mObservationsProvider = observationsProvider;
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
        mCurrentPhaseId++;  // phase ID changes on every init() or suspend()

        mDefaultEventBus.register(mEventBusSubscriber);
        mCrudEventBus.register(mEventBusSubscriber);
        mAppModel.fetchSinglePatient(mCrudEventBus, mPatientUuid);
        mAppModel.fetchLocationTree(mCrudEventBus, LocaleSelector.getCurrentLocale().toString());

        startObservationSync();
    }

    /** Releases any resources used by the controller. */
    public void suspend() {
        mCurrentPhaseId++;  // phase ID changes on every init() or suspend()

        mCrudEventBus.unregister(mEventBusSubscriber);
        mDefaultEventBus.unregister(mEventBusSubscriber);
        if (mLocationTree != null) {
            mLocationTree.close();
        }
    }

    /** Starts syncing observations more frequently while the user is viewing the chart. */
    private void startObservationSync() {
        final Handler handler = new Handler();
        final int phaseId = mCurrentPhaseId;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // This runnable triggers itself in a cycle, each run calling postDelayed()
                // to schedule the next run.  Each such cycle belongs to a phase, identified
                // by phaseId; once the current phase is exited the cycle stops.  Thus, when the
                // controller is suspended the cycle stops; and also since mCurrentPhaseId can
                // only have one value, only one such cycle can be active at any given time.
                if (mCurrentPhaseId == phaseId) {
                    mSyncManager.incrementalObservationSync();
                    handler.postDelayed(this, OBSERVATION_SYNC_PERIOD_MILLIS);
                }
            }
        };

        handler.postDelayed(runnable, OBSERVATION_SYNC_PERIOD_MILLIS);
    }

    public void onXFormResult(int code, int resultCode, Intent data) {
        PatientChartActivity.RequestCode requestCode =
                new PatientChartActivity.RequestCode(code);

        String patientUuid = getAndClearPatientUuidForRequestCode(code);
        if (patientUuid == null) {
            LOG.e("Received unknown request code: " + code);
            return;
        }

        boolean shouldShowSubmissionDialog = (resultCode != Activity.RESULT_CANCELED);
        switch (requestCode.form) {
            case ADD_OBSERVATION:
                // This will fire an SubmitXformSucceededEvent or a SubmitXformFailedEvent.
                mOdkResultSender.sendOdkResultToServer(patientUuid, resultCode, data);
                break;
            case ADD_TEST_RESULTS:
                // This will fire an SubmitXformSucceededEvent or a SubmitXformFailedEvent.
                mOdkResultSender.sendOdkResultToServer(patientUuid, resultCode, data);
                break;
            default:
                LOG.e(
                        "Received an ODK result for a form that we do not know about: '%1$s'. This "
                                + "indicates programmer error.", requestCode.form.toString());
                shouldShowSubmissionDialog = false;
                break;
        }
        mUi.showFormSubmissionDialog(shouldShowSubmissionDialog);
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
        // Don't acknowledge this action if a dialog is showing
        if (dialogShowing()) {
            return;
        }

        PrepopulatableFields fields = new PrepopulatableFields();

        // TODO(dxchen): Re-enable this post v0.2.1.
//        fields.encounterTime = DateTime.now();
        fields.locationName = "Triage";

        User user = App.getUserManager().getActiveUser();
        if (user != null) {
            fields.clinicianName = user.fullName;
        }

        Map<String, LocalizedChartHelper.LocalizedObservation> observations =
                mObservationsProvider.getMostRecentObservations(mPatientUuid);

        if (observations.containsKey(Concepts.PREGNANCY_UUID)
                && Concepts.YES_UUID.equals(observations.get(Concepts.PREGNANCY_UUID).value)) {
            fields.pregnant = PrepopulatableFields.YES;
        }

        if (observations.containsKey(Concepts.IV_UUID)
                && Concepts.YES_UUID.equals(observations.get(Concepts.IV_UUID).value)) {
            fields.ivFitted = PrepopulatableFields.YES;
        }

        fields.targetGroup = targetGroup;

        mUi.showFormLoadingDialog(true);
        mUi.fetchAndShowXform(
                PatientChartActivity.XForm.ADD_OBSERVATION,
                savePatientUuidForRequestCode(
                        PatientChartActivity.XForm.ADD_OBSERVATION, mPatientUuid),
                OdkConverter.toOdkPatient(mPatient),
                fields);
    }

    public void onAddTestResultsPressed() {
        PrepopulatableFields fields = new PrepopulatableFields();

        // TODO(dxchen): Re-enable this post v0.2.1.
//        fields.encounterTime = DateTime.now();
        fields.locationName = "Triage";

        User user = App.getUserManager().getActiveUser();
        if (user != null) {
            fields.clinicianName = user.fullName;
        }

        mUi.showFormLoadingDialog(true);
        mUi.fetchAndShowXform(
                PatientChartActivity.XForm.ADD_TEST_RESULTS,
                savePatientUuidForRequestCode(
                        PatientChartActivity.XForm.ADD_TEST_RESULTS, mPatientUuid),
                OdkConverter.toOdkPatient(mPatient),
                fields);
    }

    /** Gets the latest observation values and displays them on the UI. */
    private synchronized void updatePatientUi() {
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

        LocalDate admissionDate = null;
        if (conceptsToLatestObservations.containsKey(Concepts.ADMISSION_DATE_UUID)) {
            LocalizedObservation admissionDateObservation =
                    conceptsToLatestObservations.get(Concepts.ADMISSION_DATE_UUID);
            String admissionDateString = admissionDateObservation.localizedValue;
            if (admissionDateString != null) {
                admissionDate = Utils.stringToLocalDate(admissionDateString);
            }
        }
        mUi.setObservationHistory(observations, admissionDate);
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

    public void showAssignGeneralConditionDialog(
            Context context, final String generalConditionUuid) {
        AssignGeneralConditionDialog.ConditionSelectedCallback callback =
                new AssignGeneralConditionDialog.ConditionSelectedCallback() {

                    @Override
                    public boolean onNewConditionSelected(String newConditionUuid) {
                        setCondition(newConditionUuid);
                        return false;
                    }
                };
        mAssignGeneralConditionDialog = new AssignGeneralConditionDialog(
                context, generalConditionUuid, callback);

        mAssignGeneralConditionDialog.show();
    }

    public void setCondition(String newConditionUuid) {
        LOG.v("Assigning general condition: %s", newConditionUuid);
        AppEncounter appEncounter = new AppEncounter(
                mPatientUuid,
                null, // encounter UUID, which the server will generate
                DateTime.now(),
                new AppEncounter.AppObservation[] {
                        new AppEncounter.AppObservation(
                                Concepts.GENERAL_CONDITION_UUID,
                                newConditionUuid,
                                AppEncounter.AppObservation.Type.UUID)
                });
        mAppModel.addEncounter(mCrudEventBus, mPatient, appEncounter);
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
            if (mLocationTree != null) {
                mLocationTree.close();
            }
            mLocationTree = event.tree;
            updatePatientLocationUi();
        }

        public void onEventMainThread(SyncSucceededEvent event) {
            updatePatientUi();
        }

        public void onEventMainThread(EncounterAddFailedEvent event) {
            if (mAssignGeneralConditionDialog != null) {
                mAssignGeneralConditionDialog.dismiss();
                mAssignGeneralConditionDialog = null;
            }

            int messageResource;
            String exceptionMessage = event.exception.getMessage();
            switch (event.reason) {
                case FAILED_TO_AUTHENTICATE:
                    messageResource = R.string.encounter_add_failed_to_authenticate;
                    break;
                case FAILED_TO_FETCH_SAVED_OBSERVATION:
                    messageResource = R.string.encounter_add_failed_to_fetch_saved;
                    break;
                case FAILED_TO_SAVE_ON_SERVER:
                    messageResource = R.string.encounter_add_failed_to_saved_on_server;
                    break;
                case FAILED_TO_VALIDATE:
                    messageResource = R.string.encounter_add_failed_invalid_encounter;
                    // Validation reason typically starts after the message below.
                    exceptionMessage = exceptionMessage.replaceFirst(
                            ".*failed to validate with reason: .*: ", "");
                    break;
                case INTERRUPTED:
                    messageResource = R.string.encounter_add_failed_interrupted;
                    break;
                case INVALID_NUMBER_OF_OBSERVATIONS_SAVED: // Hard to communicate to the user.
                case UNKNOWN_SERVER_ERROR:
                    messageResource = R.string.encounter_add_failed_unknown_server_error;
                    break;
                case UNKNOWN:
                default:
                    messageResource = R.string.encounter_add_failed_unknown_reason;
            }
            mUi.showError(messageResource, exceptionMessage);
        }

        public void onEventMainThread(SingleItemFetchedEvent event) {
            if (event.item instanceof AppPatient) {
                mPatient = (AppPatient)event.item;
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
                // We need this hack because we load observations on the main thread. We should
                // change this to use a background thread. Either an async task or using
                // CrudEventBus events.

            } else if (event.item instanceof AppEncounter) {
                AppEncounter.AppObservation[] observations =
                        ((AppEncounter)event.item).observations;
                for (AppEncounter.AppObservation observation : observations) {
                    if (observation.conceptUuid.equals(Concepts.GENERAL_CONDITION_UUID)) {
                        mUi.updatePatientGeneralConditionUi(observation.value);
                        LOG.v("Setting general condition in UI: %s", observation.value);
                    }
                }

                if (mAssignGeneralConditionDialog != null) {
                    mAssignGeneralConditionDialog.dismiss();
                    mAssignGeneralConditionDialog = null;
                }
            }

            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    updatePatientUi();
                }
            });
        }

        public void onEventMainThread(PatientUpdateFailedEvent event) {
            mAssignLocationDialog.onPatientUpdateFailed(event.reason);
            LOG.e(event.exception, "Patient update failed.");
        }

        public void onEventMainThread(SubmitXformSucceededEvent event) {
            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    updatePatientUi();
                    mUi.showFormSubmissionDialog(false);
                }
            });
        }

        public void onEventMainThread(SubmitXformFailedEvent event) {
            mUi.showFormSubmissionDialog(false);
            int errorMessageResource;
            switch (event.reason) {
                case SERVER_AUTH:
                    errorMessageResource = R.string.submit_xform_failed_server_auth;
                    break;
                case SERVER_TIMEOUT:
                    errorMessageResource = R.string.submit_xform_failed_server_timeout;
                    break;
                default:
                    errorMessageResource = R.string.submit_xform_failed_unknown_reason;
            }
            mUi.showError(errorMessageResource);
        }

        public void onEventMainThread(FetchXformSucceededEvent event) {
            mUi.showFormLoadingDialog(false);
            mUi.reEnableFetch();
        }

        public void onEventMainThread(FetchXformFailedEvent event) {
            int errorMessageResource = R.string.fetch_xform_failed_unknown_reason;
            switch (event.reason) {
                case NO_FORMS_FOUND:
                    errorMessageResource = R.string.fetch_xform_failed_no_forms_found;
                    break;
                case SERVER_AUTH:
                    errorMessageResource = R.string.fetch_xform_failed_server_auth;
                    break;
                case SERVER_BAD_ENDPOINT:
                    errorMessageResource = R.string.fetch_xform_failed_server_bad_endpoint;
                    break;
                case SERVER_FAILED_TO_FETCH:
                    errorMessageResource = R.string.fetch_xform_failed_server_failed_to_fetch;
                    break;
                case SERVER_UNKNOWN:
                    errorMessageResource = R.string.fetch_xform_failed_server_unknown;
                    break;
                case UNKNOWN:
                default:
                    // Intentionally blank.
            }
            mUi.showError(errorMessageResource);
            mUi.showFormLoadingDialog(false);
            mUi.reEnableFetch();
        }
    }

    private synchronized void updatePatientLocationUi() {
        if (mLocationTree != null && mPatient != null && mPatient.locationUuid != null) {
            mUi.updatePatientLocationUi(mLocationTree, mPatient);
        }
    }

    private boolean dialogShowing() {
        return (mAssignGeneralConditionDialog != null && mAssignGeneralConditionDialog.isShowing())
                || (mAssignLocationDialog != null && mAssignLocationDialog.isShowing());
    }
}
