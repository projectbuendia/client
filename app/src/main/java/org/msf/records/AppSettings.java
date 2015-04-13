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

package org.msf.records;

import android.content.SharedPreferences;
import android.content.res.Resources;

/** Type-safe access to application settings. */
public class AppSettings {
    SharedPreferences mSharedPreferences;
    Resources mResources;

    static final int APK_UPDATE_INTERVAL_DEFAULT = 90; // default to 1.5 minutes.

    public AppSettings(SharedPreferences sharedPreferences, Resources resources) {
        mSharedPreferences = sharedPreferences;
        mResources = resources;
    }

    /** Gets the root URL of the OpenMRS server providing the Buendia API. */
    public String getOpenmrsUrl() {
        return mSharedPreferences.getString("openmrs_root_url",
                mResources.getString(R.string.openmrs_root_url_default));
    }

    /** Constructs the URL for a given URL path under the OpenMRS root URL. */
    public String getOpenmrsUrl(String urlPath) {
        return getOpenmrsUrl().replaceAll("/*$", "") + urlPath;
    }

    /** Gets the OpenMRS username. */
    public String getOpenmrsUser() {
        return mSharedPreferences.getString("openmrs_user",
                mResources.getString(R.string.openmrs_user_default));
    }

    /** Gets the OpenMRS password. */
    public String getOpenmrsPassword() {
        return mSharedPreferences.getString("openmrs_password",
                mResources.getString(R.string.openmrs_password_default));
    }

    /** Gets the root URL of the package server providing APK updates. */
    public String getPackageServerUrl() {
        return mSharedPreferences.getString("package_server_root_url",
                mResources.getString(R.string.package_server_root_url_default));
    }

    /** Constructs the URL for a given URL path on the package server. */
    public String getPackageServerUrl(String urlPath) {
        return getPackageServerUrl().replaceAll("/*$", "") + urlPath;
    }

    /**
     * Gets the flag that controls whether to immediately apply edits
     * to the local database when an XForm is submitted to the server.
     */
    public boolean getXformUpdateClientCache() {
        return mSharedPreferences.getBoolean("xform_update_client_cache",
                mResources.getBoolean(R.bool.xform_update_client_cache_default));
    }

    /**
     * Gets the flag that controls whether to update observations with
     * an incremental fetch instead of fetching all the observations.
     */
    public boolean getIncrementalObservationUpdate() {
        return mSharedPreferences.getBoolean("incremental_observation_update",
                mResources.getBoolean(R.bool.incremental_observation_update_default));
    }

    /**
     * Gets the minimum period between checks for APK updates, in seconds.
     * Repeated calls to UpdateManager.checkForUpdate() within this period
     * will not check the package server for new updates.
     */
    public int getApkUpdateInterval() {
        return mSharedPreferences.getInt("apk_update_interval_secs", APK_UPDATE_INTERVAL_DEFAULT);
    }

    /** Gets the flag for whether to save filled-in forms locally. */
    public boolean getKeepFormInstancesLocally() {
        return mSharedPreferences.getBoolean("keep_form_instances_locally",
                mResources.getBoolean(R.bool.keep_form_instances_locally_default));
    }

    /** Gets the flag indicating whether sync account setup is complete. */
    public boolean getSetupComplete() {
        return mSharedPreferences.getBoolean("setup_complete", false);
    }

    /** Sets the flag indicating whether sync account setup is complete. */
    public void setSetupComplete(boolean value) {
        mSharedPreferences.edit().putBoolean("setup_complete", value).commit();
    }
}
