package org.msf.records.ui.chart;

import android.test.AndroidTestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.data.app.AppModel;
import org.msf.records.events.CrudEventBus;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.ui.FakeEventBus;
import org.msf.records.ui.chart.PatientChartController.ObservationsProvider;
import org.msf.records.ui.chart.PatientChartController.OdkResultSender;

/**
 * Tests for {@link PatientChartController}.
 */
public class PatientChartControllerTest extends AndroidTestCase {

	private PatientChartController mController;
	@Mock private AppModel mMockAppModel;
	@Mock private OpenMrsChartServer mMockServer;
	@Mock private CrudEventBus mMockCrudEventBus; // TODO: replace with a fake
	@Mock private PatientChartController.Ui mMockUi;
	@Mock private OdkResultSender mMockOdkResultSender;
	@Mock private ObservationsProvider mMockObservationsProvider;
	private FakeEventBus mFakeEventBus;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);

		mFakeEventBus = new FakeEventBus();

		mController = new PatientChartController(
				mMockAppModel,
				mMockServer,
				mFakeEventBus,
				mMockCrudEventBus,
				mMockUi,
				mMockOdkResultSender,
				mMockObservationsProvider,
				null);
	}


	public void testSuspend_UnregistersFromEventBus() {
		// GIVEN an initialized controller
		mController.init();
		// WHEN the controller is suspended
		mController.suspend();
		// THEN the controller unregisters from the event bus
		assertEquals(0, mFakeEventBus.countRegisteredReceivers());
	}

}

