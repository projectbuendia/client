package org.msf.records.net;

import android.app.Application;

import org.msf.records.inject.Qualifiers;
import org.msf.records.prefs.StringPreference;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module that provides bindings for user-related classes.
 */
@Module(
        complete = false,
        library = true
)
public class NetModule {

    @Provides @Singleton VolleySingleton provideVolleySingleton(Application app) {
        return VolleySingleton.getInstance(app);
    }

    @Provides @Singleton OpenMrsConnectionDetails provideOpenMrsConnectionDetails(
            VolleySingleton volley,
            @Qualifiers.OpenMrsRootUrl StringPreference openMrsRootUrl,
            @Qualifiers.OpenMrsUser StringPreference openMrsUser,
            @Qualifiers.OpenMrsPassword StringPreference openMrsPassword) {
        return new OpenMrsConnectionDetails(volley, openMrsRootUrl, openMrsUser, openMrsPassword);
    }

    @Provides @Singleton Server provideServer(
            OpenMrsConnectionDetails connectionDetails) {
        return new OpenMrsServer(connectionDetails);
    }
}
