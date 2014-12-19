package org.msf.records.net;

import android.app.Application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.joda.time.LocalDate;
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

    @Provides @Singleton RequestConfigurator provideRequestConfigurator() {
        return new RequestConfigurator(5000 /*timeoutMs*/);
    }

    @Provides @Singleton RequestFactory provideRequestFactory(RequestConfigurator configurator) {
        return new RequestFactory(configurator);
    }

    @Provides @Singleton Gson provideGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
                .create();
    }

    @Provides @Singleton Server provideServer(
            OpenMrsConnectionDetails connectionDetails,
            RequestFactory requestFactory,
            Gson gson) {
        return new OpenMrsServer(connectionDetails, requestFactory, gson);
    }
}
