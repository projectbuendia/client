package org.msf.records.updater;

import android.app.Application;
import android.content.SharedPreferences;

import org.msf.records.inject.Qualifiers;
import org.msf.records.net.VolleySingleton;
import org.msf.records.prefs.StringPreference;
import org.msf.records.ui.patientlist.PatientSearchActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module that provides bindings for update-related classes.
 */
@Module(
        injects = {
                PatientSearchActivity.class
        },
        complete = false,
        library = true)
public class UpdateModule {

    @Provides
    @Singleton
    UpdateServer providePackageServer(
            Application application,
            @Qualifiers.PackageServerRootUrl StringPreference rootUrl) {
        return new UpdateServer(VolleySingleton.getInstance(application), rootUrl);
    }

    @Provides
    @Singleton
    UpdateManager provideUpdateManager(Application application, UpdateServer updateServer,
                                       SharedPreferences sharedPreferences) {
        return new UpdateManager(application, updateServer, sharedPreferences);
    }
}
