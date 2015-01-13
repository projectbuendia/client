package org.msf.records.ui.patientlist;

import org.msf.records.data.app.AppLocation;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;

import de.greenrobot.event.EventBus;

/**
 * Controller for non-inherited portions of {@link org.msf.records.ui.patientlist.RoundActivity}.
 */
public class RoundController {
    private final Ui mUi;
    private final String mLocationUuid;
    private final LocationEventSubscriber mSubscriber = new LocationEventSubscriber();

    public RoundController(Ui ui, String locationUuid) {
        mUi = ui;
        mLocationUuid = locationUuid;
    }

    public interface Ui {
        public void updateLocation(int patientCount, String name);
    }

    public void resume() {
        EventBus.getDefault().register(mSubscriber);
    }

    public void pause() {
        EventBus.getDefault().unregister(mSubscriber);
    }

    private class LocationEventSubscriber {
        // Keep title up-to-date with any location changes.
        public void onEventMainThread(AppLocationTreeFetchedEvent event) {
            AppLocation location = event.tree.findByUuid(mLocationUuid);
            if (location != null) {
                mUi.updateLocation(location.patientCount, location.name);
            }
        }
    }
}
