package org.msf.records.ui.tentselection;

import static org.mockito.Mockito.verify;

import android.test.AndroidTestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.FakeAppLocationTreeFactory;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.ui.FakeEventBus;

/**
 * Tests for {@link TentSelectionController}.
 */
public final class TentSelectionControllerTest extends AndroidTestCase {

	private TentSelectionController mController;
	private FakeEventBus mFakeEventBus;
    @Mock private AppModel mMockAppModel;
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
                mMockAppModel,
                mFakeEventBus,
                mMockUi,
                mFakeEventBus);
	}

	public void testInit_RequestsLoadLocations() {
		// GIVEN the controller hasn't previously fetched the location tree
		// WHEN the controller is initialized
		mController.init();
		// THEN the controller asks the location manager to provide the location tree
		verify(mMockAppModel).fetchLocationTree(mFakeEventBus, "en");
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
		AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
		mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
		// THEN the controller hides the progress spinner
		verify(mMockFragmentUi).showSpinner(false);
	}
}
