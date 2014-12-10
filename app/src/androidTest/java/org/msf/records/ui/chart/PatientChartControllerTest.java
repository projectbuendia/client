package org.msf.records.ui.chart;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.test.AndroidTestCase;

import java.util.List;
import java.util.Map;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.ui.FakeEventBus;
import org.msf.records.ui.chart.PatientChartController.ObservationsProvider;
import org.msf.records.ui.chart.PatientChartController.OdkResultSender;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
	@Mock private OpenMrsChartServer mMockServer;
	@Mock private FakeEventBus mFakeCrudEventbus; // TODO: replace with a fake
	@Mock private PatientChartController.Ui mMockUi;
	@Mock private OdkResultSender mMockOdkResultSender;
	@Mock private ObservationsProvider mMockObservationsProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);

		mController = new PatientChartController(
				mMockAppModel,
				mMockServer,
				mFakeCrudEventbus,
				mMockUi,
				mMockOdkResultSender,
				mMockObservationsProvider,
				null);
	}

	public void testSuspend_UnregistersFromEventBus() {
		// GIVEN an initialized controller with patient set
		mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
		mController.init();
		// WHEN the controller is suspended
		mController.suspend();
		// THEN the controller unregisters from the event bus
		assertEquals(0, mFakeCrudEventbus.countRegisteredReceivers());
	}

	public void testInit_RequestsPatientDetails() {
		// GIVEN a patient was set
		mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
		// WHEN the controller is inited
		mController.init();
		// THEN it requests that patient's details be fetched
		mMockAppModel.fetchSinglePatient(mFakeCrudEventbus, PATIENT_UUID_1);
	}

	public void testPatientDetailsLoaded_SetsObservationsOnUi() {
		// GIVEN the observations provider is set up to return some dummy data
		List<LocalizedChartHelper.LocalizedObservation> allObservations =
				ImmutableList.of(OBSERVATION_A);
		Map<String, LocalizedChartHelper.LocalizedObservation> recentObservations =
				ImmutableMap.of("blah", OBSERVATION_A);
		when(mMockObservationsProvider.getObservations(PATIENT_UUID_1))
				.thenReturn(allObservations);
		when(mMockObservationsProvider.getMostRecentObservations(PATIENT_UUID_1))
				.thenReturn(recentObservations);
		// GIVEN patient is set and controller is initialized
		mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
		mController.init();
		// WHEN that patient's details are loaded
		AppPatient patient = new AppPatient();
		mFakeCrudEventbus.post(new SingleItemFetchedEvent<AppPatient>(patient));
		// THEN the controller puts observations on the UI
		verify(mMockUi).setObservationHistory(allObservations);
		verify(mMockUi).updatePatientVitalsUI(recentObservations);
	}

	public void testPatientDetailsLoaded_UpdatesUi() {
		// GIVEN patient is set and controller is initalized
		mController.setPatient(PATIENT_UUID_1, PATIENT_NAME_1, PATIENT_ID_1);
		mController.init();
		// WHEN that patient's details are loaded
		AppPatient patient = new AppPatient();
		mFakeCrudEventbus.post(new SingleItemFetchedEvent<AppPatient>(patient));
		// THEN the controller updates the UI
		verify(mMockUi).setPatient(patient);
	}
}
