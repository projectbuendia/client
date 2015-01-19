package org.msf.records.ui.chart;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.test.AndroidTestCase;

  import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.mvcmodels.PatientModel;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.FakeEventBus;
import org.msf.records.ui.chart.PatientChartController.MinimalHandler;
import org.msf.records.ui.chart.PatientChartController.OdkResultSender;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link PatientChartController}.
 */
public final class PatientChartControllerTest extends AndroidTestCase {

    private static final String PATIENT_UUID_1 = "uuid1";
    private static final String PATIENT_NAME_1 = "bob";
    private static final String PATIENT_ID_1 = "id1";

    private static final LocalizedChartHelper.LocalizedObservation OBSERVATION_A =
            new LocalizedChartHelper.LocalizedObservation(0, "g", "c", "c", "val", "localizedVal");

    private PatientChartController mController;

    @Mock private AppModel mMockAppModel;
    @Mock private PatientChartController.Ui mMockUi;
    @Mock private OdkResultSender mMockOdkResultSender;
    @Mock private LocalizedChartHelper mMockObservationsProvider;
    @Mock private PatientModel mMockPatientModel;
    @Mock private SyncManager mMockSyncManager;
    private FakeEventBus mFakeCrudEventBus;
    private FakeHandler mFakeHandler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mFakeCrudEventBus = new FakeEventBus();
        FakeEventBus fakeEventBus = new FakeEventBus();
        mFakeHandler = new FakeHandler();
        mController = new PatientChartController(
                mMockAppModel,
                fakeEventBus,
                mFakeCrudEventBus,
                mMockUi,
                mMockOdkResultSender,
                mMockObservationsProvider,
                null,
                mMockPatientModel,
                mMockSyncManager,
                mFakeHandler);
    }

    public void testSuspend_UnregistersFromEventBus() {
        // GIVEN an initialized controller with patient set
        mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
        mController.init();
        // WHEN the controller is suspended
        mController.suspend();
        // THEN the controller unregisters from the event bus
        assertEquals(0, mFakeCrudEventBus.countRegisteredReceivers());
    }

    public void testInit_RequestsPatientDetails() {
        // GIVEN a patient was set
        mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
        // WHEN the controller is inited
        mController.init();
        // THEN it requests that patient's details be fetched
        mMockAppModel.fetchSinglePatient(mFakeCrudEventBus, PATIENT_UUID_1);
    }

    public void testPatientDetailsLoaded_SetsObservationsOnUi() {
        // GIVEN the observations provider is set up to return some dummy data
        List<LocalizedChartHelper.LocalizedObservation> allObservations =
                ImmutableList.of(OBSERVATION_A);
        Map<String, LocalizedChartHelper.LocalizedObservation> recentObservations =
                ImmutableMap.of(OBSERVATION_A.conceptUuid, OBSERVATION_A);
        when(mMockObservationsProvider.getObservations(PATIENT_UUID_1))
                .thenReturn(allObservations);
        when(mMockObservationsProvider.getMostRecentObservations(PATIENT_UUID_1))
                .thenReturn(recentObservations);
        // GIVEN patient is set and controller is initialized
        mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
        mController.init();
        // WHEN that patient's details are loaded
        AppPatient patient = AppPatient.builder().build();
        mFakeCrudEventBus.post(new SingleItemFetchedEvent<>(patient));
        // TODO(rjlothian): When the handler UI updating hack in PatientChartController is
        // removed, this can also be removed.
        mFakeHandler.runUntilEmpty();
        // THEN the controller puts observations on the UI
        verify(mMockUi).setObservationHistory(allObservations);
        verify(mMockUi).updatePatientVitalsUI(recentObservations);
    }

    public void testPatientDetailsLoaded_UpdatesUi() {
        // GIVEN patient is set and controller is initalized
        mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
        mController.init();
        // WHEN that patient's details are loaded
        AppPatient patient = AppPatient.builder().build();
        mFakeCrudEventBus.post(new SingleItemFetchedEvent<>(patient));
        // THEN the controller updates the UI
        verify(mMockUi).setPatient(patient);
    }

    private final class FakeHandler implements MinimalHandler {
        private final ArrayDeque<Runnable> mTasks = new ArrayDeque<>();

        @Override
        public void post(Runnable runnable) {
            mTasks.add(runnable);
        }

        public void runUntilEmpty() {
            while (!mTasks.isEmpty()) {
                Runnable runnable = mTasks.pop();
                runnable.run();
            }
        }
    }
}
