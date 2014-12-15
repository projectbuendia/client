package org.msf.records;

import android.app.Application;
import android.content.ContentResolver;
import android.content.res.Resources;

import javax.inject.Singleton;

import org.msf.records.data.app.AppModelModule;
import org.msf.records.events.EventsModule;
import org.msf.records.location.LocationManager;
import org.msf.records.mvcmodels.PatientChartModel;
import org.msf.records.mvcmodels.PatientModel;
import org.msf.records.net.NetModule;
import org.msf.records.prefs.PrefsModule;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.PatientListActivity;
import org.msf.records.ui.PatientSearchActivity;
import org.msf.records.ui.RoundActivity;
import org.msf.records.ui.RoundFragment;
import org.msf.records.ui.chart.PatientChartActivity;
import org.msf.records.ui.patientcreation.PatientCreationActivity;
import org.msf.records.ui.patientlist.PatientListFragment;
import org.msf.records.ui.tentselection.TentSelectionActivity;
import org.msf.records.updater.UpdateModule;
import org.msf.records.user.UserModule;
import org.msf.records.utils.UtilsModule;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;

/**
 * A Dagger module that provides the top-level bindings for the app.
 */
@Module(
        includes = {
                AppModelModule.class,
                EventsModule.class,
                NetModule.class,
                PrefsModule.class,
                UpdateModule.class,
                UserModule.class,
                UtilsModule.class
        },
        injects = {
                App.class,

                // TODO(dxchen): Move these into activity-specific modules.
                // Activities
                PatientCreationActivity.class,
                BaseActivity.class,
                PatientChartActivity.class,
                PatientListActivity.class,
                PatientSearchActivity.class,
                RoundActivity.class,
                TentSelectionActivity.class,
                PatientListFragment.class,
                RoundFragment.class
        }
)
public final class AppModule {

    private final App mApp;

    public AppModule(App app) {
        mApp = app;
    }

    @Provides @Singleton Application provideApplication() {
        return mApp;
    }

    @Provides @Singleton ContentResolver provideContentResolver(Application app) {
        return app.getContentResolver();
    }

    @Provides @Singleton Resources provideResources(Application app) {
        return app.getResources();
    }

    @Provides @Singleton SyncManager provideSyncManager() {
    	return new SyncManager();
    }

    @Provides @Singleton PatientChartModel providePatientChartModel(SyncManager syncManager) {
    	PatientChartModel patientChartModel = new PatientChartModel(EventBus.getDefault(), syncManager);
    	patientChartModel.init();
    	return patientChartModel;
    }

    @Provides @Singleton PatientModel providePatientModel() {
    	return new PatientModel();
    }

    @Provides @Singleton LocationManager provideLocationManager(SyncManager syncManager) {
    	LocationManager locationManager = new LocationManager(
    			EventBus.getDefault(),
    			App.getInstance(),
    			syncManager);
    	locationManager.init();
    	return locationManager;
    }
}
