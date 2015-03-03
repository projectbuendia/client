package org.msf.records.ui.chart;

import android.test.AndroidTestCase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.msf.records.R;
import org.msf.records.data.app.AppEncounter;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.FetchXformFailedEvent;
import org.msf.records.events.FetchXformSucceededEvent;
import org.msf.records.events.SubmitXformFailedEvent;
import org.msf.records.events.SubmitXformSucceededEvent;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.model.Concepts;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.FakeEventBus;
import org.msf.records.ui.chart.PatientChartController.MinimalHandler;
import org.msf.records.ui.chart.PatientChartController.OdkResultSender;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PatientChartController}.
 */
public final class PatientChartControllerTest extends AndroidTestCase {

	private static final String PATIENT_UUID_1 = "uuid1";
	private static final String PATIENT_NAME_1 = "bob";
	private static final String PATIENT_ID_1 = "id1";

	private static final LocalizedChartHelper.LocalizedObservation OBSERVATION_A =
            new LocalizedChartHelper.LocalizedObservation(
                    0, 0, "g", "c", "c", "val", "localizedVal");

	private PatientChartController mController;

	@Mock private AppModel mMockAppModel;
	@Mock private PatientChartController.Ui mMockUi;
	@Mock private OdkResultSender mMockOdkResultSender;
	@Mock private LocalizedChartHelper mMockObservationsProvider;
	@Mock private SyncManager mMockSyncManager;
	private FakeEventBus mFakeCrudEventBus;
    private FakeEventBus mFakeGlobalEventBus;
    private FakeHandler mFakeHandler;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);

		mFakeCrudEventBus = new FakeEventBus();
        mFakeGlobalEventBus = new FakeEventBus();
		mFakeHandler = new FakeHandler();
		mController = new PatientChartController(
				mMockAppModel,
                mFakeGlobalEventBus,
				mFakeCrudEventBus,
				mMockUi,
				mMockOdkResultSender,
				mMockObservationsProvider,
				null,
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
        verify(mMockUi).setObservationHistory(allObservations, null);
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

    /** Tests that selecting a new general condition results in adding a new encounter. */
    public void testSetCondition_AddsEncounterForNewCondition() {
        // GIVEN patient is set and controller is initalized
        mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
        mController.init();
        // WHEN a new general condition is set from the dialog
        mController.setCondition(Concepts.GENERAL_CONDITION_PALLIATIVE_UUID);
        // THEN a new encounter is added
        verify(mMockAppModel).addEncounter(
                any(CrudEventBus.class),
                any(AppPatient.class),
                any(AppEncounter.class));
    }

    /** Tests that requesting an xform through clicking 'add observation' shows loading dialog. */
    public void testAddObservation_showsLoadingDialog() {
        // GIVEN patient is set, controller is initialized
        mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
        mController.init();
        // WHEN 'add observation' is pressed
        mController.onAddObservationPressed();
        // THEN the controller displays the loading dialog
        verify(mMockUi).showFormLoadingDialog(true);
    }

    /** Tests that requesting an xform through clicking on a vital shows loading dialog. */
    public void testVitalClick_showsLoadingDialog() {
        // GIVEN patient is set, controller is initialized
        mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
        mController.init();
        // WHEN a vital is pressed
        mController.onAddObservationPressed("foo");
        // THEN the controller displays the loading dialog
        verify(mMockUi).showFormLoadingDialog(true);
    }

    /** Tests that requesting an xform through clicking on test results shows loading dialog. */
    public void testTestResultsClick_showsLoadingDialog() {
        // GIVEN patient is set, controller is initialized
        mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
        mController.init();
        // WHEN test results are pressed
        mController.onAddTestResultsPressed();
        // THEN the controller displays the loading dialog
        verify(mMockUi).showFormLoadingDialog(true);
    }

    /** Tests that the xform can be fetched again if the first fetch fails. */
    public void testXformLoadFailed_ReenablesXformFetch() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform request fails
        mFakeGlobalEventBus.post(new FetchXformFailedEvent(FetchXformFailedEvent.Reason.UNKNOWN));
        // THEN the controller re-enables xform fetch
        verify(mMockUi).reEnableFetch();
    }

    /** Tests that an error message is displayed when the xform fails to load. */
    public void testXformLoadFailed_ShowsError() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform request fails
        mFakeGlobalEventBus.post(new FetchXformFailedEvent(FetchXformFailedEvent.Reason.UNKNOWN));
        // THEN the controller displays an error message
        verify(mMockUi).showError(R.string.fetch_xform_failed_unknown_reason);
    }

    /** Tests that a failed xform fetch hides the loading dialog. */
    public void testXformLoadFailed_HidesLoadingDialog() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform request fails
        mFakeGlobalEventBus.post(new FetchXformFailedEvent(FetchXformFailedEvent.Reason.UNKNOWN));
        // THEN the controller hides the loading dialog
        verify(mMockUi).showFormLoadingDialog(false);
    }

    /** Tests that the xform can be fetched again if the first fetch succeeds. */
    public void testXformLoadSucceeded_ReenablesXformFetch() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform request succeeds
        mFakeGlobalEventBus.post(new FetchXformSucceededEvent());
        // THEN the controller re-enables xform fetch
        verify(mMockUi).reEnableFetch();
    }

    /** Tests that a successful xform fetch hides the loading dialog. */
    public void testXformLoadSucceeded_HidesLoadingDialog() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform request succeeds
        mFakeGlobalEventBus.post(new FetchXformSucceededEvent());
        // THEN the controller hides the loading dialog
        verify(mMockUi).showFormLoadingDialog(false);
    }

    // TODO: Test that starting an xform submission shows the submission dialog.

    /** Tests that errors in xform submission are reported to the user. */
    public void testXformSubmitFailed_ShowsErrorMessage() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform fails to submit
        mFakeGlobalEventBus.post(new SubmitXformFailedEvent(SubmitXformFailedEvent.Reason.UNKNOWN));
        // THEN the controller shows an error
        verify(mMockUi).showError(R.string.submit_xform_failed_unknown_reason);
    }

    /** Tests that errors in xform submission hide the submission dialog. */
    public void testXformSubmitFailed_HidesSubmissionDialog() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform fails to submit
        mFakeGlobalEventBus.post(new SubmitXformFailedEvent(SubmitXformFailedEvent.Reason.UNKNOWN));
        // THEN the controller hides the submission dialog
        verify(mMockUi).showFormSubmissionDialog(false);
    }

    /** Tests that successful xform submission hides the submission dialog. */
    public void testXformSubmitSucceeded_EventuallyHidesSubmissionDialog() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform submits successfully
        mFakeGlobalEventBus.post(new SubmitXformSucceededEvent());
        // THEN the controller hides the submission dialog
        // TODO: When the handler UI updating hack in PatientChartController is removed, this can
        // also be removed.
        mFakeHandler.runUntilEmpty();
        verify(mMockUi).showFormSubmissionDialog(false);
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
