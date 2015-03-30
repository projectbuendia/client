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

package org.msf.records.prefs;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import org.msf.records.R;
import org.msf.records.inject.Qualifiers;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/** A Dagger module that provides bindings for preferences. */
@Module(complete = false,
        library = true)
public class PrefsModule {

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences(Application app) {
        return PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Provides
    @Singleton
    @Qualifiers.OpenMrsRootUrl
    StringPreference provideOpenMrsRootUrlStringPreference(
            SharedPreferences sharedPreferences, Resources resources) {
        return new StringPreference(
                sharedPreferences,
                "openmrs_root_url",
                resources.getString(R.string.openmrs_root_url_default));
    }

    @Provides
    @Singleton
    @Qualifiers.OpenMrsUser
    StringPreference provideOpenMrsUserStringPreference(
            SharedPreferences sharedPreferences, Resources resources) {
        return new StringPreference(
                sharedPreferences,
                "openmrs_user",
                resources.getString(R.string.openmrs_user_default));
    }

    @Provides
    @Singleton
    @Qualifiers.OpenMrsPassword
    StringPreference provideOpenMrsPasswordStringPreference(
            SharedPreferences sharedPreferences, Resources resources) {
        return new StringPreference(
                sharedPreferences,
                "openmrs_password",
                resources.getString(R.string.openmrs_password_default));
    }

    @Provides
    @Singleton
    @Qualifiers.PackageServerRootUrl
    StringPreference providePackageServerRootUrlStringPreference(
            SharedPreferences sharedPreferences, Resources resources) {
        return new StringPreference(
                sharedPreferences,
                "package_server_root_url",
                resources.getString(R.string.package_server_root_url_default));
    }

    @Provides
    @Singleton
    @Qualifiers.XformUpdateClientCache
    BooleanPreference provideXformUpdateClientCache(
            SharedPreferences sharedPreferences, Resources resources) {
        return new BooleanPreference(
                sharedPreferences,
                "xform_update_client_cache",
                resources.getBoolean(R.bool.xform_update_client_cache));
    }

    @Provides
    @Singleton
    @Qualifiers.IncrementalObservationUpdate
    BooleanPreference provideIncrementalObservationUpdate(
            SharedPreferences sharedPreferences, Resources resources) {
        return new BooleanPreference(
                sharedPreferences,
                "incremental_observation_update",
                resources.getBoolean(R.bool.xform_update_client_cache));
    }
}
