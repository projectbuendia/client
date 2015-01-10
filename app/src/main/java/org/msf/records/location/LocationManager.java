package org.msf.records.location;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;

import org.msf.records.data.app.AppPatient;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.data.SingleItemUpdatedEvent;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.events.sync.SyncFailedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.sync.SyncManager;
import org.msf.records.utils.Logger;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import de.greenrobot.event.EventBus;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Loads the {@link LocationTree} from disk cache or from the network.
 *
 * <p>Caches the current {@link LocationTree}.
 *
 * <p>All classes that care about locations should handle the following bus events:
 * <ul>
 *     <li>{@link org.msf.records.events.location.LocationsLoadedEvent}</li>
 *     <li>{@link org.msf.records.events.location.LocationsLoadFailedEvent}</li>
 * </ul>
 *
 * <p>This class's public methods should only be called from the main thread.
 */
public class LocationManager {

    private static final Logger LOG = Logger.create();

    private static final boolean DEBUG = true;

    private final EventBus mEventBus;
    private final Context mContext;
    private final SyncManager mSyncManager;
    private Set<CrudEventBus> mSubscribedCrudEventBus = new HashSet<>();
    private final CrudEventBusSubscriber mCrudEventBusSubscriber = new CrudEventBusSubscriber();
    private final EventBusSubscriber mEventBusSubscriber = new EventBusSubscriber();

    @Nullable private LocationTree mLocationTree;

    public LocationManager(EventBus eventBus, Context context, SyncManager syncManager) {
        mEventBus = checkNotNull(eventBus);
        mContext = checkNotNull(context);
        mSyncManager = checkNotNull(syncManager);
    }

    public void init() {
        mEventBus.register(mEventBusSubscriber);
    }

    public void subscribe(CrudEventBus eventBus) {
        if (!mSubscribedCrudEventBus.contains(eventBus)) {
            eventBus.register(mCrudEventBusSubscriber);
            mSubscribedCrudEventBus.add(eventBus);
        }
    }

    /**
     * Loads the set of locations asynchronously, from memory, disk cache or over the network.
     *
     * <p>This method will post a {@link org.msf.records.events.location.LocationsLoadedEvent} if
     * the locations were successfully loaded and a
     * {@link org.msf.records.events.location.LocationsLoadFailedEvent} otherwise.
     *
     * <p>This method will only perform a local cache lookup once per application lifetime.
     */
    public void loadLocations() {
        if (mLocationTree != null) {
            LOG.d("Location tree already in memory");
            // Already loaded.
            mEventBus.post(new LocationsLoadedEvent(mLocationTree));
        } else {
            LOG.d("Location tree not in memory. Attempting to load from cache.");
            // Need to load from disk cache, or possible from the network.
            new LoadLocationsTask().execute();
        }
    }

    @SuppressWarnings("unused") // Called by reflection from CrudEventBus.
    private final class CrudEventBusSubscriber {
        public void onEventMainThread(SingleItemUpdatedEvent<AppPatient> event) {
            LocationTree.LocationSubtree oldLocation = null;
            LocationTree.LocationSubtree newLocation = null;

            // We don't know how to proceed if data is uninitialized, so don't bother.
            AppPatient oldPatient = event.originalItem;
            AppPatient newPatient = event.newItem;
            if (oldPatient == null || newPatient == null || mLocationTree == null) {
                return;
            }

            // Determine locations from any specified location UUID's.
            if (oldPatient.locationUuid != null) {
                oldLocation = mLocationTree.getLocationByUuid(oldPatient.locationUuid);
            }
            if (newPatient.locationUuid != null) {
                newLocation = mLocationTree.getLocationByUuid(newPatient.locationUuid);
            }

            if (oldLocation == newLocation) {
                // Nothing to do, location didn't change.
                return;
            }

            // "Move" the patient from one subtree to another.
            if (oldLocation != null) {
                oldLocation.decrementPatientCount();
            }
            if (newLocation != null) {
                newLocation.incrementPatientCount();
            }

            // Finally post the update to the event bus.
            mEventBus.post(new LocationsLoadedEvent(mLocationTree));
        }

        public void onEventMainThread(SingleItemCreatedEvent<AppPatient> event) {
            // If a patient was just created, we need to update the patient counts in the subtree
            // corresponding with that patient.
            AppPatient patient = event.item;
            if (patient == null) {
                return;
            }

            String locationUuid = patient.locationUuid;
            if (locationUuid == null) {
                return;
            }

            if (mLocationTree != null) {
                mLocationTree.getLocationByUuid(locationUuid).incrementPatientCount();

                // Treat any count update as a LocationsLoadedEvent for the purposes of updating
                // any relevant UI.
                mEventBus.post(new LocationsLoadedEvent(mLocationTree));
            }
        }
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class EventBusSubscriber {

        public void onEventMainThread(SyncSucceededEvent event) {
            new LoadLocationsTask().execute();
        }

        public void onEventMainThread(SyncFailedEvent event) {
            LOG.e("Failed to retrieve location data from server");
            mEventBus.post(
                    new LocationsLoadFailedEvent(LocationsLoadFailedEvent.REASON_SERVER_ERROR));
        }
    }

    /** Loads locations from disk cache. If not found, triggers loading from network. */
    private final class LoadLocationsTask extends AsyncTask<Void, Void, LocationTree> {
        @Override
        protected LocationTree doInBackground(Void... voids) {
            // Note: It's not possible to construct a CursorLoader on a background thread
            // without calling this.
            // TODO(rjlothian): Investigate and see if we can avoid this.
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }

            LocationTree loadedFromDiskCache = new LocationTreeFactory(mContext).build();
            if (loadedFromDiskCache != null) {
                if (DEBUG) {
                    LOG.d("Location tree successfully loaded from cache.");
                }
                return loadedFromDiskCache;
            }
            if (DEBUG) {
                LOG.d("Location tree not in cache. Attempting to load from network.");
            }
            if (!mSyncManager.isSyncing()) {
                mSyncManager.forceSync();
            } else {
                LOG.d("Already syncing");
            }
            return null;
        }

        @Override
        protected void onPostExecute(LocationTree result) {
            if (result != null) {
                mLocationTree = result;
                LocationTree.singletonInstance = result;
                mEventBus.post(new LocationsLoadedEvent(result));
            }
        }
    }
}
