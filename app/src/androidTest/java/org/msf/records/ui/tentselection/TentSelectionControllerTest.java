package org.msf.records.ui.tentselection;

import static org.mockito.Mockito.verify;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.location.LocationManager;
import org.msf.records.ui.FakeEventBus;
import org.msf.records.ui.tentselection.TentSelectionController;

import android.test.AndroidTestCase;

/**
 * Tests for {@link TentSelectionController}.
 */
public final class TentSelectionControllerTest extends AndroidTestCase {
	
	private TentSelectionController mController;
	private FakeEventBus mFakeEventBus;
	@Mock private LocationManager mMockLocationManager;
	@Mock private TentSelectionController.Ui mMockUi;
	@Mock private TentSelectionController.TentFragmentUi mMockFragmentUi;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);
		
		// TODO: Create a fake event bus so we can check whether the controller
		// unregistered its event handler.
		mFakeEventBus = new FakeEventBus();
		mController = new TentSelectionController(
				mMockLocationManager,
				mMockUi,
				mFakeEventBus);
	}
	
	public void testInit_RequestsLoadLocations() {
		// GIVEN the controller hasn't previously fetched the location tree
		// WHEN the controller is initialized
		mController.init();
		// THEN the controller asks the location manager to provide the location tree
		verify(mMockLocationManager).loadLocations();
    }
	
	public void testSuspend_UnregistersFromEventBus() {
		// GIVEN an initialized controller
		mController.init();
		// WHEN the controller is suspended
		mController.suspend();
		// THEN the controller unregisters from the event bus
		assertEquals(0, mFakeEventBus.countRegisteredReceivers());
	}

	// TODO: LocationTree is effectively a singleton that can't be constructed
	// elsewhere. This prevents testing. Let's change it so it's nto a singleton.
}
