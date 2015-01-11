package org.msf.records.prefs;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import org.msf.records.BuildConfig;
import org.msf.records.R;
import org.msf.records.inject.Qualifiers;

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
        String fromBuildConfig = BuildConfig.OPENMRS_ROOT_URL;
        String fromResources = resources.getString(R.string.openmrs_root_url_default);
        return new StringPreference(
                sharedPreferences,
                "openmrs_root_url",
                (fromBuildConfig != null) ? fromBuildConfig : fromResources);
    }

    @Provides @Singleton @Qualifiers.OpenMrsUser
    StringPreference provideOpenMrsUserStringPreference(
            SharedPreferences sharedPreferences, Resources resources) {
        String fromBuildConfig = BuildConfig.OPENMRS_USER;
        String fromResources = resources.getString(R.string.openmrs_user_default);
        return new StringPreference(
                sharedPreferences,
                "openmrs_user",
                (fromBuildConfig != null) ? fromBuildConfig : fromResources);
    }

    @Provides @Singleton @Qualifiers.OpenMrsPassword
    StringPreference provideOpenMrsPasswordStringPreference(
            SharedPreferences sharedPreferences, Resources resources) {
        String fromBuildConfig = BuildConfig.OPENMRS_PASSWORD;
        String fromResources = resources.getString(R.string.openmrs_password_default);
        return new StringPreference(
                sharedPreferences,
                "openmrs_password",
                (fromBuildConfig != null) ? fromBuildConfig : fromResources);
    }

    @Provides @Singleton @Qualifiers.XformUpdateClientCache
    BooleanPreference provideXformUpdateClientCache(
            SharedPreferences sharedPreferences, Resources resources) {
        return new BooleanPreference(
                sharedPreferences,
                "xform_update_client_cache",
                resources.getBoolean(R.bool.xform_update_client_cache));
    }
}
