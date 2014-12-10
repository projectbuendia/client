package org.msf.records;

import android.app.Application;

import org.msf.records.events.CrudEventBus;
import org.msf.records.events.mvcmodels.ModelReadyEvent;
import org.msf.records.mvcmodels.Models;
import org.msf.records.mvcmodels.PatientChartModel;
import org.msf.records.net.OpenMrsConnectionDetails;
import org.msf.records.net.OpenMrsServer;
import org.msf.records.net.OpenMrsXformsConnection;
import org.msf.records.net.Server;
import org.msf.records.user.UserManager;
import org.msf.records.utils.ActivityHierarchyServer;
import org.odk.collect.android.application.Collect;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.ObjectGraph;
import de.greenrobot.event.EventBus;

/**
 * Created by Gil on 08/10/2014.\
 */
public class App extends Application {

    private ObjectGraph mObjectGraph;

    /**
     * The current instance of the application.
     */
    private static App sInstance;

    private static UserManager sUserManager;

    private static Server mServer;
    private static OpenMrsXformsConnection mOpenMrsXformsConnection;

    private static OpenMrsConnectionDetails sConnectionDetails;

    @Inject Application mApplication;
    @Inject ActivityHierarchyServer mActivityHierarchyServer;
    @Inject UserManager mUserManager;
    @Inject OpenMrsConnectionDetails mOpenMrsConnectionDetails;

    // TODO(dxchen): Factor into individual fragment and activity classes when we introduce proper
    // scoped injection.
    @Inject public Provider<CrudEventBus> mCrudEventBusProvider;

    @Override
    public void onCreate() {
        Collect.onCreate(this);
        super.onCreate();

        buildObjectGraphAndInject();

        registerActivityLifecycleCallbacks(mActivityHierarchyServer);

        synchronized (App.class) {
            sInstance = this;

            sUserManager = mUserManager; // TODO(dxchen): Remove once fully migrated to Dagger
            sConnectionDetails = mOpenMrsConnectionDetails; // TODO(dxchen): Remove when Daggered.

            mServer = new OpenMrsServer(sConnectionDetails);
        }

        // TODO(dxchen): Refactor this into the model classes.
        EventBus.getDefault().postSticky(new ModelReadyEvent(Models.OBSERVATIONS));
        PatientChartModel.INSTANCE.init();
    }

    public void buildObjectGraphAndInject() {
        mObjectGraph = ObjectGraph.create(Modules.list(this));
        mObjectGraph.inject(this);
    }

    public void inject(Object o) {
        mObjectGraph.inject(o);
    }

    public static synchronized App getInstance() {
        return sInstance;
    }

    public static synchronized UserManager getUserManager() {
        return sUserManager;
    }

    public static synchronized Server getServer() {
        return mServer;
    }

    public static synchronized OpenMrsConnectionDetails getConnectionDetails() {
        return sConnectionDetails;
    }
}
