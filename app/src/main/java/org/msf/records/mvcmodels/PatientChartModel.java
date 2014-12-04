package org.msf.records.mvcmodels;

import org.msf.records.events.CreatePatientSucceededEvent;
import org.msf.records.events.mvcmodels.ModelUpdatedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.sync.SyncManager;

import de.greenrobot.event.EventBus;

/**
 * A model for the patient chart.
 */
public class PatientChartModel {

    // TODO(dxchen): Dagger this!
    public static final PatientChartModel INSTANCE = new PatientChartModel();

    public void init() {
        EventBus.getDefault().register(this);
    }

    public synchronized void onEvent(CreatePatientSucceededEvent event) {
        // When a new patient is created, sync from the server to get the latest info for
        // everything.
        SyncManager.INSTANCE.forceSync();
    }

    /**
     * Called when a sync finishes and fires a {@link ModelUpdatedEvent}.
     */
    public synchronized void onEvent(SyncFinishedEvent event) {
        EventBus.getDefault().post(new ModelUpdatedEvent(Models.OBSERVATIONS));
    }
}
