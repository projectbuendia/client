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
import android.support.annotation.VisibleForTesting;
import android.webkit.JavascriptInterface;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.odk.collect.android.model.Preset;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.FetchXformFailedEvent;
import org.projectbuendia.client.events.FetchXformFailedEvent.Reason;
import org.projectbuendia.client.events.FetchXformSucceededEvent;
import org.projectbuendia.client.events.SubmitXformFailedEvent;
import org.projectbuendia.client.events.SubmitXformSucceededEvent;
import org.projectbuendia.client.events.actions.ObsDeleteRequestedEvent;
import org.projectbuendia.client.events.actions.OrderAddRequestedEvent;
import org.projectbuendia.client.events.actions.OrderDeleteRequestedEvent;
import org.projectbuendia.client.events.actions.OrderExecutionAddRequestedEvent;
import org.projectbuendia.client.events.actions.OrderStopRequestedEvent;
import org.projectbuendia.client.events.data.EncounterAddFailedEvent;
import org.projectbuendia.client.events.data.ItemDeletedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.events.data.PatientUpdateFailedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.json.ConceptType;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Chart;
import org.projectbuendia.client.models.ChartItem;
import org.projectbuendia.client.models.ChartSection;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Encounter;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.sync.ConceptService;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static org.projectbuendia.client.utils.Utils.eq;

/** Controller for {@link PatientChartActivity}. */
public final class PatientChartController implements ChartRenderer.JsInterface {

    private static final Logger LOG = Logger.create();
    private static final boolean DEBUG = true;

    @VisibleForTesting public static String currentPatientUuid;

    // Form UUIDs specific to Ebola deployments.
    static final String EBOLA_LAB_TEST_FORM_UUID = "buendia_form_ebola_lab_test";

    /**
     * Period between observation syncs while the chart view is active.
     * It would be nice to make this very short, but note that the grid
     * scroll position resets every time the sync causes data to change.
     */
    private static final int PATIENT_UPDATE_PERIOD_MILLIS = 10000;

    private Patient mPatient = null;
    private String mPatientUuid = "";
    private Map<String, Order> mOrdersByUuid;
    private List<Obs> mObservations;

    private final EventBusRegistrationInterface mDefaultEventBus;
    private final CrudEventBus mCrudEventBus;
    private final OdkResultSender mOdkResultSender;
    private final Ui mUi;
    private final ChartDataHelper mChartHelper;
    private final AppModel mModel;
    private final EventSubscriber mEventBusSubscriber = new EventSubscriber();
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

        /** Updates the UI showing the history of observations and orders for this patient. */
        void updateTilesAndGrid(Chart chart, Map<String, Obs> latestObservations,
            List<Obs> observations, List<Order> orders);

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
        void showDateObsDialog(String title, String conceptUuid, LocalDate date);
        void showOrderDialog(String patientUuid, Order order, List<Obs> executions);
        void showOrderExecutionDialog(Order order, Interval interval, List<Obs> executions);
        void showEditPatientDialog(Patient patient);
        void showObsDetailDialog(Interval interval, String[] conceptUuids, List<ObsRow> obsRows, List<String> orderedConceptUuids);
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
        EventBusRegistrationInterface defaultEventBus,
        CrudEventBus crudEventBus,
        Ui ui,
        String patientUuid,
        OdkResultSender odkResultSender,
        ChartDataHelper chartHelper,
        MinimalHandler mainThreadHandler) {
        mModel = App.getModel();
        mDefaultEventBus = defaultEventBus;
        mCrudEventBus = crudEventBus;
        mUi = ui;
        currentPatientUuid = mPatientUuid = patientUuid;
        mOdkResultSender = odkResultSender;
        mChartHelper = chartHelper;
        mMainThreadHandler = mainThreadHandler;
        mLastScrollPosition = new Point(Integer.MAX_VALUE, 0);
        mCharts = mChartHelper.getCharts();
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
        currentPatientUuid = mPatientUuid = uuid;
        mModel.loadSinglePatient(mCrudEventBus, mPatientUuid);
    }

    /** Sets async operations going to collect data required by the UI. */
    public void init() {
        mDefaultEventBus.register(mEventBusSubscriber);
        mCrudEventBus.register(mEventBusSubscriber);
        mModel.loadSinglePatient(mCrudEventBus, mPatientUuid);
    }

    /** Releases any resources used by the controller. */
    public void suspend() {
        mActivePatientUpdater = null;  // clearing this stops the patient update loop

        mCrudEventBus.unregister(mEventBusSubscriber);
        mDefaultEventBus.unregister(mEventBusSubscriber);
    }

    public void onXFormResult(int requestCode, int resultCode, Intent data) {
        App.getSyncManager().setNewSyncsSuppressed(false);
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

    public void onEbolaTestResultsPressed() {
        String[] conceptUuids = new String[] {ConceptUuids.PCR_GP_UUID, ConceptUuids.PCR_NP_UUID};
        List<ObsRow> obsRows = mChartHelper.getPatientObservations(mPatientUuid, conceptUuids, null, null);
        if (!obsRows.isEmpty()) {
            mUi.showObsDetailDialog(null, conceptUuids, obsRows, Arrays.asList(conceptUuids));
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
        if (mModel.getDefaultLocation() == null) {
            mUi.showError(R.string.no_location);
            return;
        }

        // Preset the provider and location so they don't appear as questions in the form.
        Preset preset = new Preset();
        preset.providerUuid = user.getUuid();
        preset.locationUuid = mPatient.locationUuid;
        if (preset.locationUuid == null) {
            if (mModel.getDefaultLocation() != null) {
                preset.locationUuid = mModel.getDefaultLocation().uuid;
            }
        }
        Map<String, Obs> observations = mChartHelper.getLatestObservations(mPatientUuid);
        if (mPatient.pregnancy) {
            preset.pregnancy = Preset.YES;
        }
        if (ConceptUuids.isYes(observations.get(ConceptUuids.IV_UUID))) {
            preset.ivAccess = Preset.YES;
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

    @JavascriptInterface public void showObsDialog(String conceptUuids) {
        if (!conceptUuids.contains(",")) {
            String uuid = conceptUuids;
            if (eq(uuid, ConceptUuids.PLACEMENT_UUID)) {
                mUi.showPatientLocationDialog(mPatient);
                return;
            }
            Map<String, Obs> latest = mChartHelper.getLatestObservations(mPatientUuid);
            ConceptService concepts = App.getConceptService();
            if (concepts.getType(uuid) == ConceptType.DATE) {
                String title = concepts.getName(uuid, App.getSettings().getLocale());
                Obs obs = latest.get(uuid);
                mUi.showDateObsDialog(title, uuid, obs != null ? Utils.toLocalDate(obs.value) : null);
                return;
            }
        }
        mUi.showObsDetailDialog(
            null,
            conceptUuids.split(","),
            mChartHelper.getPatientObservations(mPatientUuid, conceptUuids.split(","), null, null),
            getConceptUuidsInChartOrder(getCurrentChart())
        );
    }

    @JavascriptInterface public void showObsDialog(long startMillis, long stopMillis) {
        Interval interval = new Interval(startMillis, stopMillis);
        mUi.showObsDetailDialog(
            interval,
            null,
            mChartHelper.getPatientObservations(mPatientUuid, null, startMillis, stopMillis),
            getConceptUuidsInChartOrder(getCurrentChart())
        );
    }

    @JavascriptInterface public void showObsDialog(String conceptUuids, long startMillis, long stopMillis) {
        Interval interval = new Interval(startMillis, stopMillis);
        mUi.showObsDetailDialog(
            interval,
            conceptUuids.split(","),
            mChartHelper.getPatientObservations(mPatientUuid, conceptUuids.split(","), startMillis, stopMillis),
            getConceptUuidsInChartOrder(getCurrentChart())
        );
    }

    @JavascriptInterface public void onNewOrderPressed() {
        mUi.showOrderDialog(mPatientUuid, null, null);
    }

    @JavascriptInterface public void onOrderHeadingPressed(String orderUuid) {
        mUi.showOrderDialog(mPatientUuid, mOrdersByUuid.get(orderUuid), getExecutions(orderUuid));
    }

    @JavascriptInterface public void onOrderCellPressed(String orderUuid, long startMillis) {
        Order order = mOrdersByUuid.get(orderUuid);
        DateTime start = new DateTime(startMillis);
        Interval interval = new Interval(start, start.plusDays(1));
        mUi.showOrderExecutionDialog(order, interval, getExecutions(orderUuid));
    }

    private List<Obs> getExecutions(String orderUuid) {
        List<Obs> executions = new ArrayList<>();
        for (Obs obs : mObservations) {
            if (eq(obs.conceptUuid, ConceptUuids.ORDER_EXECUTED_UUID) &&
                eq(orderUuid, obs.value)) {
                executions.add(obs);
            }
        }
        return executions;
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

    public void submitDateObservation(String conceptUuid, LocalDate date) {
        mUi.showWaitDialog(R.string.title_updating_patient);
        mModel.addObservationEncounter(mCrudEventBus, mPatientUuid, new Obs(
            null, mPatientUuid, DateTime.now(),
            conceptUuid, ConceptType.DATE, date.toString(), null
        ));
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
        mModel.addObservationEncounter(mCrudEventBus, mPatientUuid, new Obs(
            null, mPatientUuid, DateTime.now(),
            ConceptUuids.GENERAL_CONDITION_UUID, ConceptType.CODED, newConditionUuid, null
        ));
    }

    public void showAssignLocationDialog(Context context) {
        if (mPatient != null) {
            mUi.showPatientLocationDialog(mPatient);
        }
    }

    public void setZoomIndex(int index) {
        App.getSettings().setChartZoomIndex(index);
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

        if (!mCharts.isEmpty()) {
            mUi.updateTilesAndGrid(
                mCharts.get(mChartIndex),
                latestObservations, mObservations, orders);
        }

        LOG.finish("updatePatientObsUi");
    }

    public List<Chart> getCharts(){
        return mCharts;
    }

    private Chart getCurrentChart() {
        return mCharts.get(mChartIndex);
    }

    private ArrayList<String> getConceptUuidsInChartOrder(Chart chart) {
        ArrayList<String> conceptUuids = new ArrayList<>();
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
            mModel.loadSinglePatient(mCrudEventBus, mPatientUuid); // if the sync touched this patient
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
            // Java switch is not safe to use because it stupidly crashes on null.
            int messageId =
                event.reason == SubmitXformFailedEvent.Reason.SERVER_AUTH ? R.string.submit_xform_failed_server_auth
                : event.reason == SubmitXformFailedEvent.Reason.SERVER_TIMEOUT ? R.string.submit_xform_failed_server_timeout
                : R.string.submit_xform_failed_unknown_reason;
            mUi.showError(messageId);
        }

        public void onEventMainThread(FetchXformSucceededEvent event) {
            mUi.showFormLoadingDialog(false);
        }

        public void onEventMainThread(FetchXformFailedEvent event) {
            // Java switch is not safe to use because it stupidly crashes on null.
            int messageId =
                event.reason == Reason.NO_FORMS_FOUND ? R.string.fetch_xform_failed_no_forms_found
                : event.reason == Reason.SERVER_AUTH ? R.string.fetch_xform_failed_server_auth
                : event.reason == Reason.SERVER_BAD_ENDPOINT ? R.string.fetch_xform_failed_server_bad_endpoint
                : event.reason == Reason.SERVER_FAILED_TO_FETCH ? R.string.fetch_xform_failed_server_failed_to_fetch
                : event.reason == Reason.SERVER_UNKNOWN ? R.string.fetch_xform_failed_server_unknown
                : R.string.fetch_xform_failed_unknown_reason;
            mFormPending = false;
            mUi.showFormLoadingDialog(false);
            mUi.showError(messageId);
        }

        public void onEventMainThread(OrderAddRequestedEvent event) {
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
            mModel.addOrder(mCrudEventBus, new Order(
                event.orderUuid, event.patientUuid, event.instructions, start, stop));
        }

        public void onEventMainThread(OrderStopRequestedEvent event) {
            Order order = mOrdersByUuid.get(event.orderUuid);
            DateTime newStop = DateTime.now();
            if (order.isSeries() && (order.stop == null || newStop.isBefore(order.stop))) {
                LOG.i("Stopping order: %s", order.instructions);
                mModel.addOrder(mCrudEventBus, new Order(
                    event.orderUuid, order.patientUuid, order.instructions, order.start, newStop
                ));
            }
        }

        public void onEventMainThread(OrderDeleteRequestedEvent event) {
            mModel.deleteOrder(mCrudEventBus, event.orderUuid);
        }

        public void onEventMainThread(ObsDeleteRequestedEvent event) {
            for (Obs obs : event.observations) {
                mModel.deleteObs(mCrudEventBus, obs);
            }
            updatePatientObsUi();
        }

        public void onEventMainThread(OrderExecutionAddRequestedEvent event) {
            Order order = mOrdersByUuid.get(event.orderUuid);
            if (order != null) {
                mModel.addOrderExecutionEncounter(
                    mCrudEventBus, mPatient.uuid, order.uuid, event.executionTime);
            }
        }
    }
}
