package org.msf.records.mvcmodels;

import org.msf.records.data.app.AppPatient;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.mvcmodels.ModelUpdatedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.sync.SyncManager;

import de.greenrobot.event.EventBus;

/**
 * A model for the patient chart.
 */
public class PatientChartModel {

    private final SyncManager mSyncManager;
    private final EventBus mEventBus;

    public PatientChartModel(EventBus eventBus, SyncManager syncManager) {
    	mEventBus = eventBus;
    	mSyncManager = syncManager;
    }

    public void init() {
        mEventBus.register(this);
    }

    public void onEvent(SingleItemCreatedEvent<AppPatient> event) {
        // When a new patient is created, sync from the server to get the latest info for
        // everything.
        mSyncManager.forceSync();
    }

    /**
     * Called when a sync finishes and fires a {@link ModelUpdatedEvent}.
     */
    public void onEvent(SyncFinishedEvent event) {
        EventBus.getDefault().post(new ModelUpdatedEvent(Models.OBSERVATIONS));
    }
}
