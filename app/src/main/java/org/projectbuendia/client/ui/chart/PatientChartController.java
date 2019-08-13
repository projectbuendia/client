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
import android.graphics.Point;
import android.os.Bundle;
import android.webkit.JavascriptInterface;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.odk.collect.android.model.Preset;
import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.FetchXformFailedEvent;
import org.projectbuendia.client.events.FetchXformSucceededEvent;
import org.projectbuendia.client.events.SubmitXformFailedEvent;
import org.projectbuendia.client.events.SubmitXformSucceededEvent;
import org.projectbuendia.client.events.actions.OrderDeleteRequestedEvent;
import org.projectbuendia.client.events.actions.OrderExecutionSaveRequestedEvent;
import org.projectbuendia.client.events.actions.OrderSaveRequestedEvent;
import org.projectbuendia.client.events.actions.VoidObservationsRequestEvent;
import org.projectbuendia.client.events.data.EncounterAddFailedEvent;
import org.projectbuendia.client.events.data.ItemDeletedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.events.data.PatientUpdateFailedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Chart;
import org.projectbuendia.client.models.ChartItem;
import org.projectbuendia.client.models.ChartSection;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Encounter;
import org.projectbuendia.client.models.Encounter.Observation;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.VoidObs;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/** Controller for {@link PatientChartActivity}. */
final class PatientChartController implements ChartRenderer.JsInterface {

    private static final Logger LOG = Logger.create();
    private static final boolean DEBUG = true;

    // Form UUIDs specific to Ebola deployments.
    static final String EBOLA_LAB_TEST_FORM_UUID = "buendia-form-ebola_lab_test";

    /**
     * Period between observation syncs while the chart view is active.
     * It would be nice to make this very short, but note that the grid
     * scroll position resets every time the sync causes data to change.
     */
    private static final int PATIENT_UPDATE_PERIOD_MILLIS = 10000;

    private Patient mPatient = Patient.builder().build();
    private String mPatientUuid = "";
    private Map<String, Order> mOrdersByUuid;
    private List<Obs> mObservations;

    private final EventBusRegistrationInterface mDefaultEventBus;
    private final CrudEventBus mCrudEventBus;
    private final OdkResultSender mOdkResultSender;
    private final Ui mUi;
    private final ChartDataHelper mChartHelper;
    private final AppModel mAppModel;
    private final AppSettings mSettings;
    private final EventSubscriber mEventBusSubscriber = new EventSubscriber();
    private final SyncManager mSyncManager;
    private final MinimalHandler mMainThreadHandler;
    private AssignGeneralConditionDialog mAssignGeneralConditionDialog;
    private Runnable mActivePatientUpdater;

    /** The user has requested a form, and it's either open or about to be opened. */
    private boolean mFormPending = false;

    private List<Chart> mCharts;
    private int mChartIndex = 0;  // the currently selected tab (chart number)

    // Every form request made by this controller is kept in this list until
    // the form is closed.
    List<FormRequest> mFormRequests = new ArrayList<>();

    // Store chart's last scroll position
    private Point mLastScrollPosition;
    public Point getLastScrollPosition() {
        return mLastScrollPosition;
    }

    public interface Ui {
        /** Sets the activity title. */
        void setTitle(String title);

        /** Updates the UI showing the admission date and first symptoms date for this patient. */
        void updateAdmissionDateAndFirstSymptomsDateUi(
            @Nullable LocalDate admissionDate,
            @Nullable LocalDate firstSymptomsDate);

        /** Updates the UI showing Ebola PCR lab test results for this patient. */
        void updateEbolaPcrTestResultUi(Map<String, Obs> observations);

        /** Updates the UI showing the pregnancy status and IV status for this patient. */
        void updatePregnancyAndIvStatusUi(Map<String, Obs> observations);

        /** Updates the general condition UI with the patient's current condition. */
        void updatePatientConditionUi(String generalConditionUuid);

        /** Updates the UI with the patient's location. */
        void updatePatientLocationUi(LocationForest forest, Patient patient);

        /** Updates the UI showing the history of observations and orders for this patient. */
        void updateTilesAndGrid(
            Chart chart,
            Map<String, Obs> latestObservations,
            List<Obs> observations,
            List<Order> orders,
            LocalDate admissionDate,
            LocalDate firstSymptomsDate);

        /** Updates the UI with the patient's personal details (name, sex, etc.) */
        void updatePatientDetailsUi(Patient patient);

        /** Shows a progress dialog with an indeterminate spinner in it. */
        void showWaitDialog(int titleId);

        void hideWaitDialog();

        /** Displays an error message with the given resource id. */
        void showError(int errorMessageResource);

        /** Displays an error with the given resource and optional substitution args. */
        void showError(int errorResource, Object... args);

        /** Starts a new form activity to collect observations from the user. */
        void fetchAndShowXform(
            int requestCode, String formUuid, org.odk.collect.android.model.Patient patient,
            Preset preset);

        void showFormLoadingDialog(boolean show);
        void showFormSubmissionDialog(boolean show);
        void showOrderDialog(String patientUuid, Order order);
        void showOrderExecutionDialog(Order order, Interval interval, List<DateTime> executionTimes);
        void showEditPatientDialog(Patient patient);
        void showObsDetailDialog(List<ObsRow> obsRows, List<String> orderedConceptUuids);
        void showPatientLocationDialog(Patient patient);
        void showPatientUpdateFailed(int reason);
    }

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
        AppSettings settings,
        EventBusRegistrationInterface defaultEventBus,
        CrudEventBus crudEventBus,
        Ui ui,
        String patientUuid,
        OdkResultSender odkResultSender,
        ChartDataHelper chartHelper,
        @Nullable Bundle savedState,
        SyncManager syncManager,
        MinimalHandler mainThreadHandler) {
        mAppModel = appModel;
        mSettings = settings;
        mDefaultEventBus = defaultEventBus;
        mCrudEventBus = crudEventBus;
        mUi = ui;
        mPatientUuid = patientUuid;
        mOdkResultSender = odkResultSender;
        mChartHelper = chartHelper;
        mSyncManager = syncManager;
        mMainThreadHandler = mainThreadHandler;
        mLastScrollPosition = new Point(Integer.MAX_VALUE, 0);
        mCharts = mChartHelper.getCharts(AppModel.CHART_UUID);
    }

    public void setPatient(String uuid) {
        // Clear all patient-specific state.
        mPatient = null;
        mOrdersByUuid = null;
        mObservations = null;
        if (mAssignGeneralConditionDialog != null) {
            mAssignGeneralConditionDialog.dismiss();
            mAssignGeneralConditionDialog = null;
        }

        // Load a new patient, which will trigger UI updates.
        mPatientUuid = uuid;
        mAppModel.loadSinglePatient(mCrudEventBus, mPatientUuid);
    }

    /** Sets async operations going to collect data required by the UI. */
    public void init() {
        mDefaultEventBus.register(mEventBusSubscriber);
        mCrudEventBus.register(mEventBusSubscriber);
        mAppModel.loadSinglePatient(mCrudEventBus, mPatientUuid);
        updatePatientLocationUi();
    }

    /** Releases any resources used by the controller. */
    public void suspend() {
        mActivePatientUpdater = null;  // clearing this stops the patient update loop

        mCrudEventBus.unregister(mEventBusSubscriber);
        mDefaultEventBus.unregister(mEventBusSubscriber);
    }

    public void onXFormResult(int requestCode, int resultCode, Intent data) {
        mSyncManager.setNewSyncsSuppressed(false);
        mFormPending = false;

        FormRequest request = popFormRequest(requestCode);
        if (request == null) {
            LOG.e("Unknown form request code: " + requestCode);
            return;
        }

        boolean cancelled = (resultCode == Activity.RESULT_CANCELED);
        String action = cancelled ? "form_discard_pressed" : "form_save_pressed";
        Utils.logUserAction(action,
            "form", request.formUuid,
            "patient_uuid", request.patientUuid);
        mUi.showFormSubmissionDialog(!cancelled);
        if (!cancelled) {
            mOdkResultSender.sendOdkResultToServer(request.patientUuid, resultCode, data);
        }
    }

    FormRequest popFormRequest(int requestIndex) {
        FormRequest request = mFormRequests.get(requestIndex);
        mFormRequests.set(requestIndex, null);
        return request;
    }

    public void onEditPatientPressed() {
        Utils.logUserAction("edit_patient_pressed", "uuid", mPatientUuid);
        mUi.showEditPatientDialog(mPatient);
    }

    private boolean dialogShowing() {
        return (mAssignGeneralConditionDialog != null && mAssignGeneralConditionDialog.isShowing());
    }

    FormRequest createFormRequest(String formUuid, String patientUuid, Preset preset) {
        // Find an empty slot in the array of all existing form requests.
        int requestIndex = 0;
        while (requestIndex < mFormRequests.size() && mFormRequests.get(requestIndex) != null) {
            requestIndex++;
        }
        if (requestIndex >= mFormRequests.size()) {
            mFormRequests.add(null);
        }
        FormRequest request = new FormRequest(formUuid, patientUuid, preset, requestIndex);
        mFormRequests.set(requestIndex, request);
        return request;
    }

    public void onPcrResultsPressed() {
        String[] conceptUuids = new String[] {ConceptUuids.PCR_GP_UUID, ConceptUuids.PCR_NP_UUID};
        List<ObsRow> obsRows = mChartHelper.getPatientObservationsByConcept(mPatientUuid, conceptUuids);
        if (!obsRows.isEmpty()) {
            mUi.showObsDetailDialog(obsRows, Arrays.asList(conceptUuids));
        }
    }

    public void onFormRequested(String formUuid) {
        if (!dialogShowing()) requestForm(formUuid, null);
    }

    public void requestForm(String formUuid, String targetGroup) {
        Utils.logUserAction("form_opener_pressed", "form", "round", "group", targetGroup);
        if (mFormPending) {
            LOG.w("Form request is already pending; not opening another form");
            return;
        }

        JsonUser user = App.getUserManager().getActiveUser();
        if (user == null) {
            mUi.showError(R.string.no_user);
            return;
        }
        if (mAppModel.getDefaultLocation() == null) {
            mUi.showError(R.string.no_location);
            return;
        }

        // Preset the provider and location so they don't appear as questions in the form.
        Preset preset = new Preset();
        preset.providerUuid = user.uuid;
        preset.locationUuid = mPatient.locationUuid;
        if (preset.locationUuid == null) {
            if (mAppModel.getDefaultLocation() != null) {
                preset.locationUuid = mAppModel.getDefaultLocation().uuid;
            }
        }
        Map<String, Obs> observations = mChartHelper.getLatestObservations(mPatientUuid);
        if (ConceptUuids.isYes(observations.get(ConceptUuids.PREGNANCY_UUID))) {
            preset.pregnant = Preset.YES;
        }
        if (ConceptUuids.isYes(observations.get(ConceptUuids.IV_UUID))) {
            preset.ivFitted = Preset.YES;
        }
        preset.targetGroup = targetGroup;

        mFormPending = true;
        mUi.showFormLoadingDialog(true);
        openForm(createFormRequest(formUuid, mPatientUuid, preset));
    }

    private void openForm(FormRequest request) {
        LOG.i("Fetching and showing form: %s", request.formUuid);
        mUi.fetchAndShowXform(
            request.requestIndex,
            request.formUuid,
            mPatient.toOdkPatient(),
            request.preset
        );
    }

    @JavascriptInterface public void onObsDialog(String conceptUuid, String startMillis, String stopMillis) {
        ArrayList<ObsRow> obsRows = null;
        if (!conceptUuid.isEmpty()){
            if (!startMillis.isEmpty()){
                obsRows = mChartHelper.getPatientObservationsByConceptMillis(mPatientUuid, conceptUuid, startMillis, stopMillis);
            } else {
                obsRows = mChartHelper.getPatientObservationsByConcept(mPatientUuid, conceptUuid);
            }
        }
        else if (!startMillis.isEmpty()){
            obsRows = mChartHelper.getPatientObservationsByMillis(mPatientUuid, startMillis, stopMillis);
        }
        if (obsRows != null && !obsRows.isEmpty()) {
            mUi.showObsDetailDialog(obsRows, getCurrentChartRowItemConceptUuids());
        }
    }

    @JavascriptInterface public void onNewOrderPressed() {
        mUi.showOrderDialog(mPatientUuid, null);
    }

    @JavascriptInterface public void onOrderHeadingPressed(String orderUuid) {
        mUi.showOrderDialog(mPatientUuid, mOrdersByUuid.get(orderUuid));
    }

    @JavascriptInterface public void onOrderCellPressed(String orderUuid, long startMillis) {
        Order order = mOrdersByUuid.get(orderUuid);
        DateTime start = new DateTime(startMillis);
        Interval interval = new Interval(start, start.plusDays(1));
        List<DateTime> executionTimes = new ArrayList<>();
        for (Obs obs : mObservations) {
            if (AppModel.ORDER_EXECUTED_CONCEPT_UUID.equals(obs.conceptUuid) &&
                order.uuid.equals(obs.value)) {
                executionTimes.add(obs.time);
            }
        }
        mUi.showOrderExecutionDialog(order, interval, executionTimes);
    }

    @JavascriptInterface public void onPageUnload(int scrollX, int scrollY) {
        mLastScrollPosition.set(scrollX, scrollY);
    }

    @JavascriptInterface public void log(String message) {
        LOG.elapsed("ChartJS", message);
    }

    @JavascriptInterface public void finish() {
        LOG.finish("ChartJS");
    }

    public void setDate(String conceptUuid, LocalDate date) {
        mUi.showWaitDialog(R.string.title_updating_patient);
        Encounter encounter = new Encounter(
            mPatientUuid,
            null, // encounter UUID, which the server will generate
            DateTime.now(),
            new Observation[] {
                new Observation(conceptUuid, date.toString(), Observation.Type.DATE)
            }, null);
        mAppModel.addEncounter(mCrudEventBus, mPatient, encounter);
    }

    public void showAssignGeneralConditionDialog(
        Context context, final String generalConditionUuid) {
        AssignGeneralConditionDialog.ConditionSelectedCallback callback =
            newConditionUuid -> {
                mUi.showWaitDialog(R.string.title_updating_patient);
                setCondition(newConditionUuid);
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
                    ConceptUuids.GENERAL_CONDITION_UUID,
                    newConditionUuid,
                    Observation.Type.NON_DATE)
            }, null);
        mAppModel.addEncounter(mCrudEventBus, mPatient, encounter);
    }

    public void showAssignLocationDialog(Context context) {
        if (mPatient != null) {
            mUi.showPatientLocationDialog(mPatient);
        }
    }

    public void setZoomIndex(int index) {
        mSettings.setChartZoomIndex(index);
        updatePatientObsUi();
    }

    public void setChartIndex(int chartIndex) {
        mChartIndex = chartIndex;
        updatePatientObsUi();
    }

    /** Gets the latest observation values and displays them on the UI. */
    public synchronized void updatePatientObsUi() {
        // Get the observations and orders
        // TODO: Background thread this, or make this call async-like.
        String patientId = mPatient != null ? mPatient.id : "(unknown)";
        LOG.start("updatePatientObsUi", "patientId = %s", patientId);

        mObservations = mChartHelper.getObservations(mPatientUuid);
        Map<String, Obs> latestObservations =
            new HashMap<>(mChartHelper.getLatestObservations(mPatientUuid));
        List<Order> orders = mChartHelper.getOrders(mPatientUuid);
        mOrdersByUuid = new HashMap<>();
        for (Order order : orders) {
            mOrdersByUuid.put(order.uuid, order);
        }
        LOG.elapsed("updatePatientObsUi", "%d obs, %d orders", mObservations.size(), orders.size());

        LocalDate admissionDate = getObservedDate(
            latestObservations, ConceptUuids.ADMISSION_DATE_UUID);
        LocalDate firstSymptomsDate = getObservedDate(
            latestObservations, ConceptUuids.FIRST_SYMPTOM_DATE_UUID);
        mUi.updateAdmissionDateAndFirstSymptomsDateUi(admissionDate, firstSymptomsDate);
        mUi.updateEbolaPcrTestResultUi(latestObservations);
        mUi.updatePregnancyAndIvStatusUi(latestObservations);
        if (!mCharts.isEmpty()) {
            mUi.updateTilesAndGrid(
                mCharts.get(mChartIndex),
                latestObservations, mObservations, orders,
                admissionDate, firstSymptomsDate);
        }

        LOG.finish("updatePatientObsUi");
    }

    public List<Chart> getCharts(){
        return mCharts;
    }

    private Chart getCurrentChart() {
        return mCharts.get(mChartIndex);
    }

    private ArrayList<String> getCurrentChartRowItemConceptUuids() {
        ArrayList<String> conceptUuids = new ArrayList<>();
        Chart chart = getCurrentChart();
        for (ChartSection chartSection : chart.rowGroups) {
            for (ChartItem chartItem : chartSection.items) {
                conceptUuids.addAll(Arrays.asList(chartItem.conceptUuids));
            }
        }
        return conceptUuids;
    }

    /** Retrieves the value of a date observation as a LocalDate. */
    private @Nullable LocalDate getObservedDate(
        Map<String, Obs> observations, String conceptUuid) {
        Obs obs = observations.get(conceptUuid);
        return obs != null ? Utils.toLocalDate(obs.valueName) : null;
    }

    private synchronized void updatePatientLocationUi() {
        if (mPatient != null && mPatient.locationUuid != null) {
            mUi.updatePatientLocationUi(mAppModel.getForest(), mPatient);
        }
    }

    /** Represents an instance of a form being opened by the user. */
    class FormRequest {
        public final String formUuid;
        public final String patientUuid;
        public final Preset preset;
        public final int requestIndex;

        public FormRequest(String formUuid, String patientUuid, Preset preset, int index) {
            this.formUuid = formUuid;
            this.patientUuid = patientUuid;
            this.preset = preset;
            this.requestIndex = index;
        }
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class EventSubscriber {

        public void onEventMainThread(SyncSucceededEvent event) {
            updatePatientObsUi(); // if the sync fetched observations
            mAppModel.loadSinglePatient(mCrudEventBus, mPatientUuid); // if the sync touched this patient
        }

        public void onEventMainThread(EncounterAddFailedEvent event) {
            LOG.e(event.exception, "Encounter add failed.");
            mUi.hideWaitDialog();
            if (mAssignGeneralConditionDialog != null) {
                mAssignGeneralConditionDialog.onEncounterAddFailed(event);
            }
        }

        // We get a ItemLoadedEvent when the initial patient data is loaded
        // from SQLite or after an edit has been successfully posted to the server.
        public void onEventMainThread(ItemLoadedEvent<?> event) {
            if (event.item instanceof Patient) {
                mUi.hideWaitDialog();

                // Update the parts of the UI that use data in the Patient.
                Patient patient = (Patient) event.item;
                if (patient.uuid.equals(mPatientUuid)) {
                    mPatient = patient;
                    mUi.updatePatientDetailsUi(mPatient);
                    updatePatientLocationUi();
                }
            } else if (event.item instanceof Encounter) {
                mUi.hideWaitDialog();

                // We don't need to update the UI here because updatePatientObsUi()
                // below updates all the parts of the UI that use observation data.
            }

            // TODO: Displaying the observations part of the UI takes a lot of main-thread time.
            // This delays rendering of the rest of UI. To allow the rest of the UI to be displayed
            // before we attempt to populate the observations, we delay the observation update
            // slightly. We need this hack because we load observations on the main thread. We
            // should change this to use a background thread. Either an async task or using
            // CrudEventBus events.
            mMainThreadHandler.post(PatientChartController.this::updatePatientObsUi);
        }

        public void onEventMainThread(ItemDeletedEvent event) {
            mMainThreadHandler.post(PatientChartController.this::updatePatientObsUi);
        }

        public void onEventMainThread(PatientUpdateFailedEvent event) {
            LOG.e(event.exception, "Patient update failed.");
            mUi.hideWaitDialog();
            mUi.showPatientUpdateFailed(event.reason);
        }

        public void onEventMainThread(SubmitXformSucceededEvent event) {
            mMainThreadHandler.post(() -> {
                updatePatientObsUi();
                mUi.showFormSubmissionDialog(false);
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
            mFormPending = false;
            mUi.showFormLoadingDialog(false);
            mUi.showError(errorMessageResource);
        }

        public void onEventMainThread(OrderSaveRequestedEvent event) {
            DateTime start = event.start;
            DateTime stop = null;

            if (event.durationDays != null) {
                stop = start.plusDays(event.durationDays);
                // In OpenMRS, OrderServiceImpl.saveOrderInternal() has a crazy
                // special case that changes an expiry time at 00:00:00.000 on
                // any date to 23:59:59.999 on that date.  To prevent such an
                // expiry time from being advanced almost an entire day, we have
                // to detect this special case and shift the expiry time a bit.
                // Because we can't be sure that the client's time zone matches
                // the server's time zone, we have to do this for any time that
                // might be at 00:00:00.000 in any time zone.  Conservatively,
                // we treat any time with a whole number of minutes this way.
                if (stop.getSecondOfMinute() == 0 && stop.getMillisOfSecond() == 0) {
                    stop = stop.withMillisOfSecond(1);
                }
            }

            LOG.i("Saving order: %s", event.instructions);
            mAppModel.saveOrder(mCrudEventBus, new Order(
                event.orderUuid, event.patientUuid, event.instructions, start, stop));
        }

        public void onEventMainThread(OrderDeleteRequestedEvent event) {
            mAppModel.deleteOrder(mCrudEventBus, event.orderUuid);
        }

        public void onEventMainThread(VoidObservationsRequestEvent event) {
            for (String uuid : event.Uuids) {
                mAppModel.VoidObservation(mCrudEventBus, new VoidObs(uuid));
            }
            updatePatientObsUi();
        }

        public void onEventMainThread(OrderExecutionSaveRequestedEvent event) {
            Order order = mOrdersByUuid.get(event.orderUuid);
            if (order != null) {
                mAppModel.addOrderExecutedEncounter(mCrudEventBus, mPatient, order.uuid);
            }
        }
    }
}
