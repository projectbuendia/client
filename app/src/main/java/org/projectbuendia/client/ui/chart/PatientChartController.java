// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui.chart;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.odk.collect.android.model.Preset;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.FetchXformFailedEvent;
import org.projectbuendia.client.events.FetchXformSucceededEvent;
import org.projectbuendia.client.events.SubmitXformFailedEvent;
import org.projectbuendia.client.events.SubmitXformSucceededEvent;
import org.projectbuendia.client.events.actions.OrderExecutionSaveRequestedEvent;
import org.projectbuendia.client.events.actions.OrderSaveRequestedEvent;
import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.events.data.EncounterAddFailedEvent;
import org.projectbuendia.client.events.data.ItemFetchedEvent;
import org.projectbuendia.client.events.data.PatientUpdateFailedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Concepts;
import org.projectbuendia.client.models.Encounter;
import org.projectbuendia.client.models.Encounter.Observation;
import org.projectbuendia.client.models.LocationTree;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.PatientDelta;
import org.projectbuendia.client.net.json.JsonUser;
import org.projectbuendia.client.sync.LocalizedChartHelper;
import org.projectbuendia.client.sync.LocalizedObs;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.dialogs.AssignLocationDialog;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.LocaleSelector;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/** Controller for {@link PatientChartActivity}. */
final class PatientChartController implements GridRenderer.GridJsInterface {

    private static final Logger LOG = Logger.create();
    private static final boolean DEBUG = true;
    private static final String KEY_PENDING_UUIDS = "pendingUuids";
    static final String OBSERVATION_FORM_UUID = "buendia-form-clinical_observation";
    static final String EBOLA_LAB_TEST_FORM_UUID = "buendia-form-ebola_lab_test";


    /**
     * Period between observation syncs while the chart view is active.  It would be nice for
     * this to be even shorter (20 s? 10 s?) but currently the table scroll position resets on
     * each sync, if any data has changed.
     * TODO: Try reducing this period to improve responsiveness, but be wary of the table scrolling
     * whenever data is refreshed.
     */
    private static final int OBSERVATION_SYNC_PERIOD_MILLIS = 60000;

    // The ODK code for filling in a form has no way of attaching metadata to it.
    // This means we can't pass which patient is currently being edited. Instead, we keep an array
    // of up to MAX_ODK_REQUESTS patientUuids. The array is persisted through activity restart in
    // the savedInstanceState.

    /** Maximum concurrent ODK forms assigned request codes. */
    private static final int MAX_ODK_REQUESTS = 10;
    private int mNextIndex = 0;

    // TODO: Use a map for this instead of an array.
    private final String[] mPatientUuids;
    private Patient mPatient = Patient.builder().build();
    private LocationTree mLocationTree;
    private DateTime mLastObsTime = null;
    private String mPatientUuid = "";
    private Map<String, org.projectbuendia.client.sync.Order> mOrdersByUuid;
    private List<LocalizedObs> mObservations;

    // This value is incremented whenever the controller is activated or suspended.
    // A "phase" is a period of time between such transition points.
    private int mCurrentPhaseId = 0;

    public interface Ui {
        /** Sets the activity title. */
        void setTitle(String title);

        /** Updates the UI showing current observation values for this patient. */
        void updatePatientVitalsUi(
                Map<String, LocalizedObs> observations,
                LocalDate admissionDate,
                LocalDate firstSymptomsDate);

        /** Updates the general condition UI with the patient's current condition. */
        void updatePatientConditionUi(String generalConditionUuid);

        /** Updates the UI with the patient's location. */
        void updatePatientLocationUi(LocationTree locationTree, Patient patient);

        /** Updates the UI showing the history of observations and orders for this patient. */
        void updatePatientHistoryUi(
                List<Pair<String, String>> tileConceptUuidsAndNames,
                Map<String, LocalizedObs> latestObservations,
                List<Pair<String, String>> gridConceptUuidsAndNames,
                List<LocalizedObs> observations,
                List<org.projectbuendia.client.sync.Order> orders,
                LocalDate admissionDate,
                LocalDate firstSymptomsDate);

        /** Updates the indicator of this patient's latest encounter time. */
        void updateLastObsTimeUi(DateTime lastObsTime);

        /** Updates the UI with the patient's personal details (name, gender, etc.). */
        void updatePatientDetailsUi(Patient patient);

        /** Displays an error message with the given resource id. */
        void showError(int errorMessageResource);

        /** Displays an error with the given resource and optional substitution args. */
        void showError(int errorResource, Object... args);

        /** Starts a new form activity to collect observations from the user. */
        void fetchAndShowXform(
                int requestCode, String formUuid, org.odk.collect.android.model.Patient patient,
                Preset preset);

        void reEnableFetch();
        void showFormLoadingDialog(boolean show);
        void showFormSubmissionDialog(boolean show);
        void showNewOrderDialog(String patientUuid);
        void showOrderExecutionDialog(org.projectbuendia.client.sync.Order order, Interval interval, List<DateTime> executionTimes);
    }

    private final EventBusRegistrationInterface mDefaultEventBus;
    private final CrudEventBus mCrudEventBus;
    private final OdkResultSender mOdkResultSender;
    private final Ui mUi;
    private final LocalizedChartHelper mChartHelper;
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
            String patientUuid,
            OdkResultSender odkResultSender,
            LocalizedChartHelper chartHelper,
            @Nullable Bundle savedState,
            SyncManager syncManager,
            MinimalHandler mainThreadHandler) {
        mAppModel = appModel;
        mDefaultEventBus = defaultEventBus;
        mCrudEventBus = crudEventBus;
        mUi = ui;
        mPatientUuid = patientUuid;
        mOdkResultSender = odkResultSender;
        mChartHelper = chartHelper;
        if (savedState != null) {
            mPatientUuids = savedState.getStringArray(KEY_PENDING_UUIDS);
        } else {
            mPatientUuids = new String[MAX_ODK_REQUESTS];
        }
        mSyncManager = syncManager;
        mMainThreadHandler = mainThreadHandler;
    }

    /** Represents an instance of a form being opened by the user. */
    class FormRequest {
        public final String formUuid;
        public final String patientUuid;
        public final int requestIndex;

        public FormRequest(String formUuid, String patientUuid, int index) {
            this.formUuid = formUuid;
            this.patientUuid = patientUuid;
            this.requestIndex = index;
        }
    }

    // Every form request made by this controller is kept in this list until
    // the form is closed.
    List<FormRequest> mFormRequests = new ArrayList<>();

    FormRequest newFormRequest(String formUuid, String patientUuid) {
        // Find an empty slot in the array of all existing form requests.
        int requestIndex = 0;
        while (requestIndex < mFormRequests.size() && mFormRequests.get(requestIndex) != null) {
            requestIndex++;
        }
        if (requestIndex >= mFormRequests.size()) {
            mFormRequests.add(null);
        }
        FormRequest request = new FormRequest(formUuid, patientUuid, requestIndex);
        mFormRequests.set(requestIndex, request);
        return request;
    }

    FormRequest popFormRequest(int requestIndex) {
        FormRequest request = mFormRequests.get(requestIndex);
        mFormRequests.set(requestIndex, null);
        return request;
    }

    /**
     * Returns the state of the controller. This should be saved to preserve it over activity
     * restarts.
     */
    public Bundle getState() {
        Bundle bundle = new Bundle();
        bundle.putStringArray("pendingUuids", mPatientUuids);
        return bundle;
    }

    /**
     * Initializes the controller, setting async operations going to collect data required by the
     * UI.
     */
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
        final Handler handler = new Handler(Looper.getMainLooper());
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
                    mSyncManager.startIncrementalObsSync();
                    handler.postDelayed(this, OBSERVATION_SYNC_PERIOD_MILLIS);
                }
            }
        };

        handler.postDelayed(runnable, OBSERVATION_SYNC_PERIOD_MILLIS);
    }

    public void onXFormResult(int requestCode, int resultCode, Intent data) {
        FormRequest request = popFormRequest(requestCode);
        if (request == null) {
            LOG.e("Unknown form request code: " + requestCode);
            return;
        }

        boolean shouldShowSubmissionDialog = (resultCode != Activity.RESULT_CANCELED);
        String action = (resultCode == Activity.RESULT_CANCELED)
                ? "form_discard_pressed" : "form_save_pressed";
        Utils.logUserAction(action,
                "form", request.formUuid,
                "patient_uuid", request.patientUuid);
        mOdkResultSender.sendOdkResultToServer(request.patientUuid, resultCode, data);
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

        Preset preset = new Preset();
        preset.locationName = "Triage";

        JsonUser user = App.getUserManager().getActiveUser();
        Utils.logUserAction("form_opener_pressed",
                "form", "round",
                "group", targetGroup);
        if (user != null) {
            preset.clinicianName = user.fullName;
        }

        Map<String, LocalizedObs> observations =
                mChartHelper.getMostRecentObservations(mPatientUuid);

        if (observations.containsKey(Concepts.PREGNANCY_UUID)
                && Concepts.YES_UUID.equals(observations.get(Concepts.PREGNANCY_UUID).value)) {
            preset.pregnant = Preset.YES;
        }

        if (observations.containsKey(Concepts.IV_UUID)
                && Concepts.YES_UUID.equals(observations.get(Concepts.IV_UUID).value)) {
            preset.ivFitted = Preset.YES;
        }

        preset.targetGroup = targetGroup;

        mUi.showFormLoadingDialog(true);
        FormRequest request = newFormRequest(OBSERVATION_FORM_UUID, mPatientUuid);
        mUi.fetchAndShowXform(
            request.requestIndex, request.formUuid,
            mPatient.toOdkPatient(), preset);
    }

    public void onAddTestResultsPressed() {
        Preset preset = new Preset();
        preset.locationName = "Triage";

        JsonUser user = App.getUserManager().getActiveUser();
        Utils.logUserAction("form_opener_pressed", "form", "lab_test");
        if (user != null) {
            preset.clinicianName = user.fullName;
        }

        mUi.showFormLoadingDialog(true);
        FormRequest request = newFormRequest(EBOLA_LAB_TEST_FORM_UUID, mPatientUuid);
        mUi.fetchAndShowXform(
            request.requestIndex, request.formUuid,
            mPatient.toOdkPatient(), preset);
    }

    public void onOpenFormPressed(String formUuid) {
        Preset preset = new Preset();
        preset.locationName = "Triage";

        JsonUser user = App.getUserManager().getActiveUser();
        if (user != null) {
            preset.clinicianName = user.fullName;
        }

        Utils.logUserAction("form_opener_pressed", "form", formUuid);
        mUi.showFormLoadingDialog(true);
        FormRequest request = newFormRequest(formUuid, mPatientUuid);
        mUi.fetchAndShowXform(
            request.requestIndex, request.formUuid,
            mPatient.toOdkPatient(), preset);
    }

    @android.webkit.JavascriptInterface
    public void onNewOrderPressed() {
        mUi.showNewOrderDialog(mPatientUuid);
    }

    @android.webkit.JavascriptInterface
    public void onOrderCellPressed(String orderUuid, long startMillis) {
        org.projectbuendia.client.sync.Order order = mOrdersByUuid.get(orderUuid);
        DateTime start = new DateTime(startMillis);
        Interval interval = new Interval(start, start.plusDays(1));
        List<DateTime> executionTimes = new ArrayList<>();
        for (LocalizedObs obs : mObservations) {
            if (AppModel.ORDER_EXECUTED_CONCEPT_UUID.equals(obs.conceptUuid) &&
                    order.uuid.equals(obs.value)) {
                executionTimes.add(obs.encounterTime);
            }
        }
        mUi.showOrderExecutionDialog(order, interval, executionTimes);
    }

    /** Retrieves the value of a date observation as a LocalDate. */
    private LocalDate getObservedDate(
            Map<String, LocalizedObs> observations, String conceptUuid) {
        LocalizedObs obs = observations.get(conceptUuid);
        return obs == null ? null : Utils.toLocalDate(obs.localizedValue);
    }

    /** Gets the latest observation values and displays them on the UI. */
    private synchronized void updatePatientObsUi() {
        // Get the observations and orders
        // TODO: Background thread this, or make this call async-like.
        mObservations = mChartHelper.getObservations(mPatientUuid);
        Map<String, LocalizedObs> conceptsToLatestObservations =
                new HashMap<>(mChartHelper.getMostRecentObservations(mPatientUuid));
        for (LocalizedObs obs : mObservations) {
            mLastObsTime = Utils.max(mLastObsTime, obs.encounterTime);
        }
        List<org.projectbuendia.client.sync.Order> orders = mChartHelper.getOrders(mPatientUuid);
        mOrdersByUuid = new HashMap<>();
        for (org.projectbuendia.client.sync.Order order : orders) {
            mOrdersByUuid.put(order.uuid, order);
        }
        LOG.d("Showing " + mObservations.size() + " observations and "
                + orders.size() + " orders");

        LocalDate admissionDate = getObservedDate(
                conceptsToLatestObservations, Concepts.ADMISSION_DATE_UUID);
        LocalDate firstSymptomsDate = getObservedDate(
                conceptsToLatestObservations, Concepts.FIRST_SYMPTOM_DATE_UUID);
        mUi.updateLastObsTimeUi(mLastObsTime);
        mUi.updatePatientVitalsUi(
                conceptsToLatestObservations, admissionDate, firstSymptomsDate);
        mUi.updatePatientHistoryUi(
                mChartHelper.getTileConcepts(), conceptsToLatestObservations,
                mChartHelper.getGridRowConcepts(), mObservations, orders,
                admissionDate, firstSymptomsDate);
    }

    public void showAssignGeneralConditionDialog(
            Context context, final String generalConditionUuid) {
        AssignGeneralConditionDialog.ConditionSelectedCallback callback =
                new AssignGeneralConditionDialog.ConditionSelectedCallback() {

                    @Override
                    public boolean onNewConditionSelected(String newConditionUuid) {
                        setCondition(newConditionUuid);
                        Utils.logUserAction("condition_assigned");
                        return false;
                    }
                };
        mAssignGeneralConditionDialog = new AssignGeneralConditionDialog(
                context, generalConditionUuid, callback);

        mAssignGeneralConditionDialog.show();
    }

    public void setCondition(String newConditionUuid) {
        LOG.v("Assigning general condition: %s", newConditionUuid);
        Encounter encounter = new Encounter(
                mPatientUuid,
                null, // encounter UUID, which the server will generate
                DateTime.now(),
                new Observation[] {
                        new Observation(
                                Concepts.GENERAL_CONDITION_UUID,
                                newConditionUuid,
                                Observation.Type.NON_DATE)
                }, null);
        mAppModel.addEncounter(mCrudEventBus, mPatient, encounter);
    }

    public void showAssignLocationDialog(Context context) {
        if (mAssignLocationDialog != null) return;

        AssignLocationDialog.LocationSelectedCallback callback =
                new AssignLocationDialog.LocationSelectedCallback() {
                    @Override
                    public boolean onLocationSelected(String locationUuid) {
                        PatientDelta delta = new PatientDelta();
                        delta.assignedLocationUuid = Optional.of(locationUuid);
                        mAppModel.updatePatient(mCrudEventBus, mPatient, delta);
                        Utils.logUserAction("location_assigned");
                        return false;
                    }
                };

        Runnable onDismiss = new Runnable() {
            @Override
            public void run() {
                mAssignLocationDialog = null;
            }
        };

        mAssignLocationDialog = new AssignLocationDialog(
                context,
                mAppModel,
                LocaleSelector.getCurrentLocale().getLanguage(),
                onDismiss,
                mCrudEventBus,
                Optional.of(mPatient.locationUuid),
                callback);
        mAssignLocationDialog.show();
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
            updatePatientObsUi();
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

        // We get a ItemFetchedEvent when the initial patient data is loaded
        // from SQLite or after an edit has been successfully posted to the server.
        public void onEventMainThread(ItemFetchedEvent event) {
            if (event.item instanceof Patient) {
                // When the patient's location is changed, the location dialog stays
                // open while we wait for the patient edit to be posted to the server.
                // Now that the patient has been posted, close the dialog.
                if (mAssignLocationDialog != null) {
                    mAssignLocationDialog.dismiss();
                    mAssignLocationDialog = null;
                }

                // Update the parts of the UI that use data in the Patient.
                mPatient = (Patient) event.item;
                mUi.updatePatientDetailsUi(mPatient);
                updatePatientLocationUi();
            } else if (event.item instanceof Encounter) {
                // When the patient's condition is changed, the condition dialog stays
                // open while we wait for the observation to be posted to the server.
                // Now that the encounter has been posted, close the dialog.
                if (mAssignGeneralConditionDialog != null) {
                    mAssignGeneralConditionDialog.dismiss();
                    mAssignGeneralConditionDialog = null;
                }

                // We don't need to update the UI here because updatePatientObsUi()
                // below updates all the parts of the UI that use observation data.
            }

            // TODO: Displaying the observations part of the UI takes a lot of main-thread time.
            // This delays rendering of the rest of UI. To allow the rest of the UI to be displayed
            // before we attempt to populate the observations, we delay the observation update
            // slightly. We need this hack because we load observations on the main thread. We
            // should change this to use a background thread. Either an async task or using
            // CrudEventBus events.
            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    updatePatientObsUi();
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
                    updatePatientObsUi();
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

        public void onEventMainThread(OrderSaveRequestedEvent event) {
            DateTime start = DateTime.now();
            DateTime stop = null;

            if (event.durationDays != null) {
                LocalDate stopDate = start.toLocalDate().plusDays(event.durationDays);
                // In OpenMRS, OrderServiceImpl.saveOrderInternal() forces the
                // order expiry (auuughhh!) to 23:59:59.999 on its specified date.
                // We have to shift it back a bit to prevent it from being
                // advanced almost an entire day, and even then this only works if
                // the client's time zone matches the server's time zone, because
                // the server's infidelity is time-zone-dependent (auggghh!!!)
                stop = stopDate.toDateTimeAtStartOfDay().minusSeconds(1);
            }

            mAppModel.addOrder(mCrudEventBus, new Order(
                    null, event.patientUuid, event.instructions, start, stop));
        }

        public void onEventMainThread(OrderExecutionSaveRequestedEvent event) {
            org.projectbuendia.client.sync.Order order = mOrdersByUuid.get(event.orderUuid);
            if (order != null) {
                mAppModel.addOrderExecutedEncounter(mCrudEventBus, mPatient, order.uuid);
            }
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
