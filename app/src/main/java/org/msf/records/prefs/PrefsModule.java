package org.msf.records.prefs;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import org.msf.records.R;
import org.msf.records.inject.Qualifiers;
import org.msf.records.net.Constants;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module that provides bindings for preferences.
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
    StringPreference provideOpenMrsRootUrlStringPreference(
            SharedPreferences sharedPreferences, Resources resources) {
        return new StringPreference(
                sharedPreferences,
                "openmrs_root_url",
                resources.getString(R.string.openmrs_root_url_default));
    }

    @Provides @Singleton @Qualifiers.OpenMrsUser
    StringPreference provideOpenMrsUserStringPreference(
            SharedPreferences sharedPreferences, Resources resources) {
        return new StringPreference(
                sharedPreferences,
                "openmrs_user",
                resources.getString(R.string.openmrs_user_default));
    }

    @Provides @Singleton @Qualifiers.OpenMrsPassword
    StringPreference provideOpenMrsPasswordStringPreference(
            SharedPreferences sharedPreferences, Resources resources) {
        return new StringPreference(
                sharedPreferences,
                "openmrs_password",
                resources.getString(R.string.openmrs_password_default));
    }
}
