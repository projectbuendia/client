package org.msf.records.prefs;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.msf.records.App;
import org.msf.records.inject.Qualifiers;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.UserLoginFragment;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module that provides bindings for utilities.
 */
@Module(
        complete = false,
        library = true
)
public class PrefsModule {

    @Provides @Singleton SharedPreferences provideSharedPreferences(Application app) {
        return PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Provides @Singleton @Qualifiers.OpenMrsRootUrl
    StringPreference provideOpenMrsRootUrlStringPreference(SharedPreferences sharedPreferences) {
        return new StringPreference(sharedPreferences, "openmrs_root_url");
    }

    @Provides @Singleton @Qualifiers.OpenMrsUser
    StringPreference provideOpenMrsUserStringPreference(SharedPreferences sharedPreferences) {
        return new StringPreference(sharedPreferences, "openmrs_user");
    }

    @Provides @Singleton @Qualifiers.OpenMrsPassword
    StringPreference provideOpenMrsPasswordStringPreference(SharedPreferences sharedPreferences) {
        return new StringPreference(sharedPreferences, "openmrs_password");
    }
}
