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
import android.view.MenuItem;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.data.app.AppEncounter;
import org.projectbuendia.client.data.app.AppLocationTree;
import org.projectbuendia.client.data.app.AppModel;
import org.projectbuendia.client.data.app.AppOrder;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.AppPatientDelta;
import org.projectbuendia.client.data.odk.OdkConverter;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.FetchXformFailedEvent;
import org.projectbuendia.client.events.FetchXformSucceededEvent;
import org.projectbuendia.client.events.OrderSaveRequestedEvent;
import org.projectbuendia.client.events.SubmitXformFailedEvent;
import org.projectbuendia.client.events.SubmitXformSucceededEvent;
import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.events.data.EncounterAddFailedEvent;
import org.projectbuendia.client.events.data.PatientUpdateFailedEvent;
import org.projectbuendia.client.events.data.SingleItemFetchedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.model.Concepts;
import org.projectbuendia.client.net.model.User;
import org.projectbuendia.client.sync.LocalizedChartHelper;
import org.projectbuendia.client.sync.LocalizedObs;
import org.projectbuendia.client.sync.Order;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.locationselection.AssignLocationDialog;
import org.projectbuendia.client.utils.Utils;
import org.projectbuendia.client.utils.date.Dates;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.LocaleSelector;
import org.projectbuendia.client.utils.Logger;
import org.odk.collect.android.model.Patient;
import org.odk.collect.android.model.PrepopulatableFields;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/** Controller for {@link PatientChartActivity}. */
final class PatientChartController {

    private static final Logger LOG = Logger.create();

    private static final boolean DEBUG = true;

    private static final String KEY_PENDING_UUIDS = "pendingUuids";

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
        void updatePatientVitalsUi(
                Map<String, LocalizedObs> observations,
                LocalDate admissionDate,
                LocalDate firstSymptomsDate);

        /** Updates the general condition UI with the patient's current condition. */
        void updatePatientGeneralConditionUi(String generalConditionUuid);

        /** Updates the UI with the patient's location. */
        void updatePatientLocationUi(AppLocationTree locationTree, AppPatient patient);

        /** Updates the UI showing the history of observations and orders for this patient. */
        void updateHistoryGrid(
                List<LocalizedObs> observations,
                List<Order> orders,
                LocalDate admissionDate,
                LocalDate firstSymptomsDate);

        /** Shows the last time a user interacted with this patient. */
        void setLatestEncounter(long latestEncounterTimeMillis);

        /** Shows the patient's personal details. */
        void setPatient(AppPatient patient);

        /** Displays an error message with the given resource id. */
        void showError(int errorMessageResource);

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

        /** Shows or hides the form loading dialog. */
        void showFormLoadingDialog(boolean show);

        /** Shows or hides the form submission dialog. */
        void showFormSubmissionDialog(boolean show);

        /** Shows the new order dialog. */
        void showNewOrderDialog(String patientUuid);
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
            OdkResultSender odkResultSender,
            LocalizedChartHelper chartHelper,
            @Nullable Bundle savedState,
            SyncManager syncManager,
            MinimalHandler mainThreadHandler) {
        mAppModel = appModel;
        mDefaultEventBus = defaultEventBus;
        mCrudEventBus = crudEventBus;
        mUi = ui;
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

    /**
     * Returns the state of the controller. This should be saved to preserve it over activity
     * restarts.
     */
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
                    mSyncManager.startIncrementalObsSync();
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
        String action = (resultCode == Activity.RESULT_CANCELED)
                ? "form_discard_pressed" : "form_save_pressed";
        switch (requestCode.form) {
            case ADD_OBSERVATION:
                Utils.logUserAction(action,
                        "form", "round",
                        "patient_uuid", patientUuid);
                // This will fire an SubmitXformSucceededEvent or a SubmitXformFailedEvent.
                mOdkResultSender.sendOdkResultToServer(patientUuid, resultCode, data);
                break;
            case ADD_TEST_RESULTS:
                Utils.logUserAction(action,
                        "form", "lab_test",
                        "patient_uuid", patientUuid);
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
        fields.locationName = "Triage";

        User user = App.getUserManager().getActiveUser();
        Utils.logUserAction("form_opener_pressed",
                "form", "round",
                "group", targetGroup);
        if (user != null) {
            fields.clinicianName = user.fullName;
        }

        Map<String, LocalizedObs> observations =
                mChartHelper.getMostRecentObservations(mPatientUuid);

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
        fields.locationName = "Triage";

        User user = App.getUserManager().getActiveUser();
        Utils.logUserAction("form_opener_pressed", "form", "lab_test");
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

    public void onNewOrderPressed() {
        mUi.showNewOrderDialog(mPatientUuid);
    }

    /** Retrieves the value of a date observation as a LocalDate. */
    private LocalDate getObservedDate(
            Map<String, LocalizedObs> observations, String conceptUuid) {
        LocalizedObs obs = observations.get(conceptUuid);
        return obs == null ? null : Dates.toLocalDate(obs.localizedValue);
    }

    /** Gets the latest observation values and displays them on the UI. */
    private synchronized void updatePatientUi() {
        // Get the observations and orders
        // TODO: Background thread this, or make this call async-like.
        List<LocalizedObs> observations =
                mChartHelper.getObservations(mPatientUuid);
        Map<String, LocalizedObs> conceptsToLatestObservations =
                new HashMap<>(mChartHelper.getMostRecentObservations(mPatientUuid));
        for (LocalizedObs obs : observations) {
            mLastObservation = Math.max(mLastObservation, obs.encounterTimeMillis);
        }
        List<Order> orders = mChartHelper.getOrders(mPatientUuid);

        LOG.d("Showing " + observations.size() + " observations and "
                + orders.size() + " orders");

        LocalDate admissionDate = getObservedDate(
                conceptsToLatestObservations, Concepts.ADMISSION_DATE_UUID);
        LocalDate firstSymptomsDate = getObservedDate(
                conceptsToLatestObservations, Concepts.FIRST_SYMPTOM_DATE_UUID);
        mUi.setLatestEncounter(mLastObservation);
        mUi.updatePatientVitalsUi(
                conceptsToLatestObservations, admissionDate, firstSymptomsDate);
        mUi.updateHistoryGrid(observations, orders, admissionDate, firstSymptomsDate);
    }

    /** Returns a requestCode that can be sent to ODK Xform activity representing the given UUID. */
    private int savePatientUuidForRequestCode(PatientChartActivity.XForm form, String patientUuid) {
        mPatientUuids[mNextIndex] = patientUuid;
        int requestCode = new PatientChartActivity.RequestCode(form, mNextIndex).getCode();
        mNextIndex = (mNextIndex + 1) % MAX_ODK_REQUESTS;
        return requestCode;
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
        AssignLocationDialog.LocationSelectedCallback callback =
                new AssignLocationDialog.LocationSelectedCallback() {

                    @Override
                    public boolean onNewTentSelected(String newTentUuid) {
                        AppPatientDelta patientDelta = new AppPatientDelta();
                        patientDelta.assignedLocationUuid = Optional.of(newTentUuid);

                        mAppModel.updatePatient(mCrudEventBus, mPatient, patientDelta);
                        Utils.logUserAction("location_assigned");
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
     * Converts a requestCode that was previously sent to the ODK Xform activity back to a patient
     * UUID.
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

            // TODO: Displaying the observations part of the UI takes a lot of main-thread time.
            // This delays rendering of the rest of UI. To allow the rest of the UI to be displayed
            // before we attempt to populate the observations, we delay the observation update
            // slightly. We need this hack because we load observations on the main thread. We
            // should change this to use a background thread. Either an async task or using
            // CrudEventBus events.
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

        public void onEventMainThread(OrderSaveRequestedEvent event) {
            DateTime start = DateTime.now();
            DateTime stop = null;

            if (event.stopDays != null) {
                LocalDate startDate = start.toLocalDate();
                if (start.getHourOfDay() >= 12) {
                    // Orders placed after noon start tomorrow.
                    startDate = startDate.plusDays(1);
                }
                LocalDate stopDate = startDate.plusDays(event.stopDays);
                start = startDate.toDateTimeAtStartOfDay();
                stop = stopDate.toDateTimeAtStartOfDay();
                // In OpenMRS, OrderServiceImpl.saveOrderInternal() forces the
                // order expiry (auuughhh!) to 23:59:59.999 on its specified date.
                // We have to shift it back a bit to prevent it from being
                // advanced almost an entire day, and even then this only works if
                // the client's time zone matches the server's time zone, because
                // the server's infidelity is time-zone-dependent (auggghh!!!)
                stop = stop.minusSeconds(1);
            }

            mAppModel.addOrder(mCrudEventBus, new AppOrder(
                    null, event.patientUuid, event.instructions, start, stop));
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
