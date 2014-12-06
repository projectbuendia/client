package org.msf.records.location;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.msf.records.App;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.events.location.LocationsSyncFailedEvent;
import org.msf.records.events.location.LocationsSyncedEvent;
import org.msf.records.events.sync.SyncFailedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.events.user.ActiveUserSetEvent;
import org.msf.records.events.user.ActiveUserUnsetEvent;
import org.msf.records.events.user.KnownUsersLoadFailedEvent;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.events.user.KnownUsersSyncFailedEvent;
import org.msf.records.events.user.KnownUsersSyncedEvent;
import org.msf.records.events.user.UserAddFailedEvent;
import org.msf.records.events.user.UserAddedEvent;
import org.msf.records.events.user.UserDeleteFailedEvent;
import org.msf.records.events.user.UserDeletedEvent;
import org.msf.records.net.model.NewUser;
import org.msf.records.net.model.User;
import org.msf.records.sync.SyncManager;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * An object that manages patient locations.
 *
 * <p>All classes that care about locations should handle the following bus events:
 * <ul>
 *     <li>{@link org.msf.records.events.location.LocationsLoadedEvent}</li>
 *     <li>{@link org.msf.records.events.location.LocationsLoadFailedEvent}</li>
 *     <li>{@link org.msf.records.events.location.LocationsSyncedEvent}</li>
 *     <li>{@link org.msf.records.events.location.LocationsSyncFailedEvent}</li>
 * </ul>
 *
 * <p>This class is thread-safe.
 */
public class LocationManager {

    private static final String TAG = "LocationManager";
    /**
     * A lock object for the set of locations.
     */
    private final Object mLocationsLock = new Object();

    private static LocationTree ROOT = null;
    private static LocationTreeFactory FACTORY = null;

    static void rebuild() {
        FACTORY = new LocationTreeFactory(App.getInstance());
        ROOT = FACTORY.build();
    }

    public LocationManager() {
        EventBus.getDefault().register(this);
    }

    /**
     * Loads the set of locations from local cache.
     *
     * <p>This method will post a {@link org.msf.records.events.location.LocationsLoadedEvent} if
     * the locations were successfully loaded and a
     * {@link org.msf.records.events.location.LocationsLoadFailedEvent} otherwise.
     *
     * <p>This method will only perform a local cache lookup once per application lifetime.
     */
    public synchronized void loadLocations() {
        synchronized (mLocationsLock) {
            if (ROOT == null) {
                new LoadLocationsTask().execute();
            } else {
                EventBus.getDefault().post(new LocationsLoadedEvent(ROOT));
            }
        }
    }

    /**
     * Syncs the set of locations known to the application with the server.
     *
     * <p>Server synchronization will periodically happen automatically, but this method allows for
     * the sync to be forced. This actually syncs ALL server data to avoid consistency issues.
     *
     * <p>This method will post a {@link org.msf.records.events.location.LocationsSyncedEvent} if
     * the sync succeeded and a {@link org.msf.records.events.location.LocationsSyncFailedEvent}
     * otherwise.
     */
    public synchronized void syncLocations() {
        new SyncLocationsTask().execute();
    }

    public synchronized void onEvent(LocationsSyncedEvent event) {
        // Build the tree.
        Log.i(TAG, "Building location tree from server data");
        rebuild();

        // If ROOT is null, give up.
        if (ROOT == null) {
            Log.e(TAG, "Could not construct the tree from server data");
            EventBus.getDefault().post(
                    new LocationsLoadFailedEvent(
                            LocationsLoadFailedEvent.REASON_TREE_CONSTRUCTION_ERROR));
        } else {
            // If tree is built, we're done.
            Log.i(TAG, "Location tree constructed from server data");
            EventBus.getDefault().post(new LocationsLoadedEvent(ROOT));
        }

        // Unregister from any further events.
        EventBus.getDefault().unregister(this);
    }

    public synchronized void onEvent(LocationsSyncFailedEvent event) {
        // Forward as a LocationsLoadFailedEvent.
        Log.e(TAG, "Failed to retrieve location data from server");
        EventBus.getDefault().post(
                new LocationsLoadFailedEvent(LocationsLoadFailedEvent.REASON_SERVER_ERROR));

        // Unregister from any further events.
        EventBus.getDefault().unregister(this);
    }


    public synchronized void onEvent(SyncSucceededEvent event) {
        EventBus.getDefault().post(new LocationsSyncedEvent());
    }

    public synchronized void onEvent(SyncFailedEvent event) {
        EventBus.getDefault().post(
                new LocationsSyncFailedEvent(LocationsLoadFailedEvent.REASON_SERVER_ERROR));
    }

    private class LoadLocationsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Looper.prepare();
                rebuild();

                // If ROOT is null, then we need fresh data from the server.
                if (ROOT == null) {
                    Log.i(TAG, "No locations found in database, retrieving from server");
                    syncLocations();
                } else {
                    synchronized (mLocationsLock) {
                        Log.i(TAG, "Location tree built from local cache");
                        EventBus.getDefault().post(new LocationsLoadedEvent(ROOT));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Load locations task failed", e);
                EventBus.getDefault().post(
                        new LocationsLoadFailedEvent(LocationsLoadFailedEvent.REASON_DB_ERROR));

                return null;
            }

            return null;
        }
    }

    private class SyncLocationsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                SyncManager syncManager = new SyncManager();

                synchronized(this) {
                    if (!syncManager.isSyncing()) {
                        syncManager.forceSync();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Location sync failed", e);
                EventBus.getDefault().post(
                        new LocationsSyncFailedEvent(
                                LocationsSyncFailedEvent.REASON_SERVER_ERROR));

                return null;
            }

            return null;
        }
    }
}
