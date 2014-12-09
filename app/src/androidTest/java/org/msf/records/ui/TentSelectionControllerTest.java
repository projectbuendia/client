package org.msf.records.ui;

import static org.mockito.Mockito.verify;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.location.LocationManager;

import android.test.AndroidTestCase;
import de.greenrobot.event.EventBus;

/**
 * Tests for {@link TentSelectionController}.
 */
public class TentSelectionControllerTest extends AndroidTestCase {
	
	private TentSelectionController mController;
	private EventBus mEventBus;
	@Mock private LocationManager mMockLocationManager;
	@Mock private TentSelectionController.Ui mMockUi;
	@Mock private TentSelectionController.TentFragmentUi mMockFragmentUi;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);
		
		// TODO: Create a fake event bus so we can check whether the controller
		// unregistered its event handler.
		mEventBus = new EventBus();
		mController = new TentSelectionController(
				mMockLocationManager,
				mMockUi,
				mEventBus);
	}
	
	public void testInit_RequestsLoadLocations() {
		// GIVEN the controller hasn't previously fetched the location tree
		// WHEN initialized
		mController.init();
		// THEN the controller asks the location manager to provide the location tree
		verify(mMockLocationManager).loadLocations();
    }

	// TODO: LocationTree is effectively a singleton that can't be constructed
	// elsewhere. This prevents testing. Let's change it so it's nto a singleton.
}
