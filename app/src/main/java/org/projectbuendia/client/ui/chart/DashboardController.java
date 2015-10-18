// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui.chart;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.events.data.ItemFetchedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationTree;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.LocaleSelector;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Controller for {@link DashboardActivity}. */
public final class DashboardController {
    private static final Logger LOG = Logger.create();
    private static final int OBSERVATION_SYNC_PERIOD_MILLIS = 10000;

    private LocationTree mLocationTree;

    // This value is incremented whenever the controller is activated or suspended.
    // A "phase" is a period of time between such transition points.
    private int mCurrentPhaseId = 0;
    private final EventBusRegistrationInterface mDefaultEventBus;
    private final Ui mUi;
    private final CrudEventBus mCrudEventBus;
    private final ChartDataHelper mChartHelper;
    private final AppModel mAppModel;
    private final EventSubscriber mEventBusSubscriber;
    private final SyncManager mSyncManager;
    private final MinimalHandler mMainThreadHandler;
    private final ContentResolver mContentResolver;

    public interface Ui {
        /** U   pdates the dashboard UI. */
        void updateDashboard(LocationTree tree, Map<String, Stat> statsByLocationUuid);
    }

    public interface MinimalHandler {
        void post(Runnable runnable);
    }

    public class Stat {
        public int currentPatients;
        public int malariaPositivePatients;
        public List<Double> gPerKgPerDayValues = new ArrayList<>();
        public int gPerKgPerDayCount;
        public double gPerKgPerDayTotal;
        public int muacCount;
        public double muacMmPerDay;
        public String cp;
        public String mpp;
        public String gpkpd;
        public String mmpd;
    }

    public DashboardController(
        AppModel appModel,
        EventBusRegistrationInterface defaultEventBus,
        CrudEventBus crudEventBus,
        Ui ui,
        ChartDataHelper chartHelper,
        SyncManager syncManager,
        MinimalHandler mainThreadHandler,
        ContentResolver contentResolver) {
        mAppModel = appModel;
        mDefaultEventBus = defaultEventBus;
        mCrudEventBus = crudEventBus;
        mUi = ui;
        mChartHelper = chartHelper;
        mSyncManager = syncManager;
        mMainThreadHandler = mainThreadHandler;
        mEventBusSubscriber = new EventSubscriber();
        mContentResolver = contentResolver;
    }

    /**
     * Initializes the controller, setting async operations going to collect data required by the
     * UI.
     */
    public void init() {
        mCurrentPhaseId++;  // phase ID changes on every init() or suspend()

        mMainThreadHandler.post(new Runnable() {
            @Override public void run() {
                updateDashboard();
            }
        });

        mDefaultEventBus.register(mEventBusSubscriber);
        mCrudEventBus.register(mEventBusSubscriber);
        mAppModel.fetchLocationTree(mCrudEventBus, LocaleSelector.getCurrentLocale().toString());

        startObservationSync();
    }

    /** Starts syncing observations more frequently while the user is viewing the chart. */
    private void startObservationSync() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final int phaseId = mCurrentPhaseId;

        Runnable runnable = new Runnable() {
            @Override public void run() {
                // This runnable triggers itself in a cycle, each run calling postDelayed()
                // to schedule the next run.  Each such cycle belongs to a phase, identified
                // by phaseId; once the current phase is exited the cycle stops.  Thus, when the
                // controller is suspended the cycle stops; and also since mCurrentPhaseId can
                // only have one value, only one such cycle can be active at any given time.
                if (mCurrentPhaseId == phaseId) {
                    mSyncManager.startIncrementalObsSync();
                    handler.postDelayed(this, OBSERVATION_SYNC_PERIOD_MILLIS);
                }
            }
        };

        handler.postDelayed(runnable, 0);
    }

    /** Releases any resources used by the controller. */
    public void suspend() {
        mCurrentPhaseId++;  // phase ID changes on every init() or suspend()

        mDefaultEventBus.unregister(mEventBusSubscriber);
        if (mLocationTree != null) {
            mLocationTree.close();
        }
    }

    /** Gets the latest observation values and displays them on the UI. */
    private synchronized void updateDashboard() {
        if (mLocationTree == null) return;

        // Gather the stats.
        String weightId = Utils.expandUuid("5089");
        String muacId = Utils.expandUuid("777000102");
        String paracheckId = Utils.expandUuid("1643");
        String positiveId = Utils.expandUuid("703");

        Map<String, Stat> statsByLocationUuid = new HashMap<>();
        Map<String, Obs> earliestWeight = mChartHelper.getEarliestObservationsForConcept(weightId, "en");
        Map<String, Obs> latestWeight = mChartHelper.getLatestObservationsForConcept(weightId, "en");
        Map<String, Obs> earliestMuac = mChartHelper.getEarliestObservationsForConcept(muacId, "en");
        Map<String, Obs> latestMuac = mChartHelper.getLatestObservationsForConcept(muacId, "en");
        Map<String, Obs> latestParacheck = mChartHelper.getLatestObservationsForConcept(paracheckId, "en");

        for (Location loc : mLocationTree.getDescendantsAtDepth(2)) {
            statsByLocationUuid.put(loc.uuid, new Stat());
        }

        try (Cursor c = mContentResolver.query(
            Contracts.Patients.CONTENT_URI, null, null, null, null)) {
            while (c.moveToNext()){
                String uuid = Utils.getString(c, Contracts.Patients.UUID);
                String locationUuid = Utils.getString(c, Contracts.Patients.LOCATION_UUID);
                Stat stat = statsByLocationUuid.get(locationUuid);
                if (stat != null) {
                    stat.currentPatients += 1;
                    Obs obs = latestParacheck.get(uuid);
                    if (obs != null && positiveId.equals(obs.value)) {
                        stat.malariaPositivePatients += 1;
                    }

                    Obs obs1 = earliestWeight.get(uuid);
                    Double ew = (obs1 != null && obs1.getObsValue() != null) ? obs1.getObsValue().number : null;
                    Obs obs2 = latestWeight.get(uuid);
                    Double lw = (obs2 != null && obs2.getObsValue() != null) ? obs2.getObsValue().number : null;
                    if (ew != null && lw != null) {
                        double kg = ew;
                        double g = (lw - ew)*1000;
                        double days = (obs2.time.getMillis() - obs1.time.getMillis()) / (24*3600*1000);
                        if (days > 0) {
                            double gPerKgPerDay = g/kg/days;
                            stat.gPerKgPerDayTotal += gPerKgPerDay;
                            stat.gPerKgPerDayCount += 1;
                        }
                    }

                    obs1 = earliestMuac.get(uuid);
                    Double em = (obs1 != null && obs1.getObsValue() != null) ? obs1.getObsValue().number : null;
                    obs2 = latestMuac.get(uuid);
                    Double lm = (obs2 != null && obs2.getObsValue() != null) ? obs2.getObsValue().number : null;
                    if (em != null && lm != null) {
                        double days = (obs2.time.getMillis() - obs1.time.getMillis()) / (24*3600*1000);
                        if (days > 0) {
                            double muacMmPerDay = (lm - em)/days;
                            stat.muacMmPerDay += muacMmPerDay;
                            stat.muacCount += 1;
                        }
                    }
                }
            }
        }

        for (Location loc : mLocationTree.getDescendantsAtDepth(2)) {
            Stat stat = statsByLocationUuid.get(loc.uuid);
            if (stat != null) {
                if (stat.muacCount > 0) stat.muacMmPerDay /= stat.muacCount;
                if (stat.gPerKgPerDayCount > 0) stat.gPerKgPerDayTotal /= stat.gPerKgPerDayCount;
                loc.stat = stat;
                loc.pc = stat.currentPatients + "";
                loc.mpc = stat.malariaPositivePatients + "";
                loc.mpp = (stat.currentPatients > 0) ? ((stat.malariaPositivePatients * 100) / stat.currentPatients) + "%" : "";
                loc.kg = String.format("%.2f", stat.gPerKgPerDayTotal);
                loc.mm = String.format("%.2f", stat.muacMmPerDay);
            }
        }
        mUi.updateDashboard(mLocationTree, statsByLocationUuid);
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class EventSubscriber {

        public void onEventMainThread(AppLocationTreeFetchedEvent event) {
            if (mLocationTree != null) {
                mLocationTree.close();
            }
            mLocationTree = event.tree;
            updateDashboard();
        }

        public void onEventMainThread(SyncSucceededEvent event) {
            updateDashboard();
        }

        // We get a ItemFetchedEvent when the initial patient data is loaded
        // from SQLite or after an edit has been successfully posted to the server.
        public void onEventMainThread(ItemFetchedEvent event) {
            mMainThreadHandler.post(new Runnable() {
                @Override public void run() {
                    updateDashboard();
                }
            });
        }
    }
}
