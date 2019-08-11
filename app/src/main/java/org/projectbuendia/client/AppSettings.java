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

package org.projectbuendia.client;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;

/** Type-safe access to application settings. */
public class AppSettings {
    SharedPreferences mSharedPreferences;
    Resources mResources;

    public AppSettings(SharedPreferences sharedPreferences, Resources resources) {
        mSharedPreferences = sharedPreferences;
        mResources = resources;
    }

    /** Constructs the URL for a given URL path under the OpenMRS root URL. */
    public String getOpenmrsUrl(String urlPath) {
        return getOpenmrsUrl().replaceAll("/*$", "") + urlPath;
    }

    /** Gets the root URL of the OpenMRS server providing the Buendia API. */
    public String getOpenmrsUrl() {
        return mSharedPreferences.getString("openmrs_root_url",
            mResources.getString(R.string.openmrs_root_url_default));
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

    /** Constructs the URL for a given URL path on the package server. */
    public String getPackageServerUrl(String urlPath) {
        return getPackageServerUrl().replaceAll("/*$", "") + urlPath;
    }

    /** Gets the root URL of the package server providing APK updates. */
    public String getPackageServerUrl() {
        return mSharedPreferences.getString("package_server_root_url",
            mResources.getString(R.string.package_server_root_url_default));
    }

    /** Gets the index of the preferred chart zoom level. */
    public int getChartZoomIndex() {
        return mSharedPreferences.getInt("chart_zoom_index", 0);
    }

    /** Sets the preferred chart zoom level. */
    public void setChartZoomIndex(int zoom) {
        mSharedPreferences.edit().putInt("chart_zoom_index", zoom).commit();
    }

    /**
     Gets the minimum period between checks for APK updates, in seconds.
     Repeated calls to UpdateManager.checkForUpdate() within this period
     will not check the package server for new updates.
     */
    public int getApkUpdateInterval() {
        return mSharedPreferences.getInt("apk_update_interval",
            mResources.getInteger(R.integer.apk_check_interval_default));
    }

    /** Returns true if the app should skip directly to a patient chart on startup. */
    public boolean shouldSkipToPatientChart() {
        return !getStartingPatientId().isEmpty();
    }

    /** Gets the patient ID of the chart to skip directly to on startup, or "". */
    public @NonNull String getStartingPatientId() {
        return mSharedPreferences.getString("starting_patient_id",
            mResources.getString(R.string.starting_patient_id_default)).trim();
    }

    /** Returns true if sync has been disabled in the settings. */
    public boolean getSyncDisabled() {
        return mSharedPreferences.getBoolean("sync_disabled",
            mResources.getBoolean(R.bool.sync_disabled_default));
    }

    /** Gets the setting for whether to retain filled-in forms after submission. */
    public boolean getformInstancesRetainedLocally() {
        return mSharedPreferences.getBoolean("form_instances_retained",
            mResources.getBoolean(R.bool.form_instances_retained_default));
    }

    /** Gets the setting for whether to use the unreliable SyncAdapter framework. */
    public boolean getSyncAdapterPreferred() {
        return mSharedPreferences.getBoolean("sync_adapter_preferred",
            mResources.getBoolean(R.bool.sync_adapter_preferred_default));
    }

    /** Gets the flag indicating whether the sync account has been initialized. */
    public boolean getSyncAccountInitialized() {
        return mSharedPreferences.getBoolean("sync_account_initialized", false);
    }

    /** Sets the flag indicating whether the sync account has been initialized. */
    public void setSyncAccountInitialized(boolean value) {
        mSharedPreferences.edit().putBoolean("sync_account_initialized", value).commit();
    }

    /** Gets the flag controlling whether to assume no wifi means no network. */
    public boolean getNonWifiAllowed() {
        return mSharedPreferences.getBoolean("non_wifi_allowed",
            mResources.getBoolean(R.bool.non_wifi_allowed_default));
    }

    /** Gets the currently selected locale as a BCP 47 tag. */
    public String getLocaleTag() {
        return "en";
    }

    /** Gets the interval for fast incremental syncs (patients, orders, observations). */
    // Syncs in this category should typically take less than 100 ms.
    public int getSmallSyncInterval() {
        return mSharedPreferences.getInt("small_sync_interval",
            mResources.getInteger(R.integer.small_sync_interval_default));
    }

    /** Gets the interval for syncs that are non-incremental but small (locations, users). */
    // This category is for syncs expected to take up to 500 ms, for data
    // that changes (on average) less than once an hour.
    public int getMediumSyncInterval() {
       return mSharedPreferences.getInt("medium_sync_interval",
           mResources.getInteger(R.integer.medium_sync_interval_default));
    }

    /** Gets the interval for syncs that are non-incremental and large (concepts, forms). */
    // This category is for syncs expected to take up to 2000 ms, for data
    // that changes (on average) less than once a day.
    public int getLargeSyncInterval() {
        return mSharedPreferences.getInt("large_sync_interval",
            mResources.getInteger(R.integer.large_sync_interval_default));
    }
}

