package org.msf.records.ui.tentselection;

import static org.mockito.Mockito.verify;

import android.test.AndroidTestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.model.Zone;
import org.msf.records.net.model.Location;
import org.msf.records.ui.FakeEventBus;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
                null,
                null,
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

	public void testLoadLocations_HidesSpinner() {
		// GIVEN an initialized controller with a fragment attached
		mController.init();
		mController.attachFragmentUi(mMockFragmentUi);
		// WHEN the location tree is loaded
		LocationTree locationTree = newLocationTree();
		mFakeEventBus.post(new LocationsLoadedEvent(locationTree));
		// THEN the controller hides the progress spinner
		verify(mMockFragmentUi).showSpinner(false);
	}

	private LocationTree newLocationTree() {
		ImmutableList<Location> locations = ImmutableList.of(
				newLocation("root", "", null),
				newLocation("Probable Zone", Zone.PROBABLE_ZONE_UUID, ""));
		ImmutableMap<String, Integer> patientCountByUuid = ImmutableMap.of(
				Zone.PROBABLE_ZONE_UUID, 3);
		// THEN it should succeed
		return  new LocationTree(
		getContext().getResources(),
		locations,
		patientCountByUuid);
	}

	private Location newLocation(String name, String uuid, String parentUuid) {
		Location location = new Location();
		location.names = ImmutableMap.of("en", name);
		location.uuid = uuid;
		location.parent_uuid = parentUuid;
		return location;
	}
}
