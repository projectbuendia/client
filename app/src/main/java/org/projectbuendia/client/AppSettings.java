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

import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Type-safe access to application settings. */
public class AppSettings {
    // The locale in which this app guarantees a definition for every string.
    public static final Locale APP_DEFAULT_LOCALE = new Locale("en");

    // The system default locale at app startup time.
    public static final Locale ORIGINAL_DEFAULT_LOCALE = Locale.getDefault();

    // Locales to offer as options to the user.
    public static final Locale[] AVAILABLE_LOCALES = new Locale[] {
        new Locale("en"), new Locale("fr")
    };

    private SharedPreferences prefs;
    private Resources resources;

    public AppSettings(SharedPreferences prefs, Resources resources) {
        this.prefs = prefs;
        this.resources = resources;
    }

    /** Constructs the URL for a given URL path under the OpenMRS root URL. */
    public String getOpenmrsUrl(String urlPath) {
        return getOpenmrsUrl().replaceAll("/*$", "") + urlPath;
    }

    /** Gets the root URL of the OpenMRS server providing the Buendia API. */
    public String getOpenmrsUrl() {
        return prefs.getString("openmrs_root_url",
            resources.getString(R.string.openmrs_root_url_default));
    }

    /** Gets the OpenMRS username. */
    public String getOpenmrsUser() {
        return prefs.getString("openmrs_user",
            resources.getString(R.string.openmrs_user_default));
    }

    /** Gets the OpenMRS password. */
    public String getOpenmrsPassword() {
        return prefs.getString("openmrs_password",
            resources.getString(R.string.openmrs_password_default));
    }

    /** Constructs the URL for a given URL path on the package server. */
    public String getPackageServerUrl(String urlPath) {
        return getPackageServerUrl().replaceAll("/*$", "") + urlPath;
    }

    /** Gets the root URL of the package server providing APK updates. */
    public String getPackageServerUrl() {
        return prefs.getString("package_server_root_url",
            resources.getString(R.string.package_server_root_url_default));
    }

    /** Gets the index of the preferred chart zoom level. */
    public int getChartZoomIndex() {
        return prefs.getInt("chart_zoom_index", 0);
    }

    /** Sets the preferred chart zoom level. */
    public void setChartZoomIndex(int zoom) {
        prefs.edit().putInt("chart_zoom_index", zoom).commit();
    }

    /**
     Gets the minimum period between checks for APK updates, in seconds.
     Repeated calls to UpdateManager.checkForUpdate() within this period
     will not check the package server for new updates.
     */
    public int getApkUpdateInterval() {
        return prefs.getInt("apk_update_interval",
            resources.getInteger(R.integer.apk_check_interval_default));
    }

    /** Returns true if the app should skip directly to a patient chart on startup. */
    public boolean shouldSkipToPatientChart() {
        return !getStartingPatientId().isEmpty();
    }

    /** Gets the patient ID of the chart to skip directly to on startup, or "". */
    public @NonNull String getStartingPatientId() {
        return prefs.getString("starting_patient_id",
            resources.getString(R.string.starting_patient_id_default)).trim();
    }

    /** Returns true if periodic sync has been disabled in the settings. */
    public boolean getPeriodicSyncDisabled() {
        return prefs.getBoolean("periodic_sync_disabled",
            resources.getBoolean(R.bool.periodic_sync_disabled_default));
    }

    /** Gets the setting for whether to retain filled-in forms after submission. */
    public boolean getformInstancesRetainedLocally() {
        return prefs.getBoolean("form_instances_retained",
            resources.getBoolean(R.bool.form_instances_retained_default));
    }

    /** Gets the setting for whether to use the unreliable SyncAdapter framework. */
    public boolean getSyncAdapterPreferred() {
        return prefs.getBoolean("sync_adapter_preferred",
            resources.getBoolean(R.bool.sync_adapter_preferred_default));
    }

    /** Gets the flag indicating whether the sync account has been initialized. */
    public boolean getSyncAccountInitialized() {
        return prefs.getBoolean("sync_account_initialized", false);
    }

    /** Sets the flag indicating whether the sync account has been initialized. */
    public void setSyncAccountInitialized(boolean value) {
        prefs.edit().putBoolean("sync_account_initialized", value).commit();
    }

    /** Gets the flag controlling whether to assume no wifi means no network. */
    public boolean getNonWifiAllowed() {
        return prefs.getBoolean("non_wifi_allowed",
            resources.getBoolean(R.bool.non_wifi_allowed_default));
    }

    /** Gets the currently selected locale. */
    public Locale getLocale() {
        String localeTag = Utils.toNonnull(prefs.getString("locale", ""));
        return !localeTag.isEmpty() ? new Locale(localeTag) : ORIGINAL_DEFAULT_LOCALE;
    }

    /** Sets the locale. */
    public void setLocale(String languageTag) {
        prefs.edit().putString("locale", languageTag).commit();
    }

    /** Gets the index of the currently selected locale in the getLocaleOptionValues() array. */
    public int getLocaleIndex() {
        String languageTag = Utils.toNonnull(prefs.getString("locale", ""));
        return Arrays.asList(getLocaleOptionValues()).indexOf(languageTag);
    }

    /** Gets the values for a menu of available locales. */
    public static String[] getLocaleOptionValues() {
        List<String> values = new ArrayList<>();
        for (Locale locale : AVAILABLE_LOCALES) {
            values.add(Utils.toLanguageTag(locale));
        }
        return values.toArray(new String[0]);
    }

    /** Gets the labels for a menu of available locales. */
    public static String[] getLocaleOptionLabels() {
        List<String> labels = new ArrayList<>();
        for (Locale locale : AVAILABLE_LOCALES) {
            labels.add(locale.getDisplayName(locale));
        }
        return labels.toArray(new String[0]);
    }

    /** Gets the interval for fast incremental syncs (patients, orders, observations). */
    // Syncs in this category should typically take less than 100 ms.
    public int getSmallSyncInterval() {
        return prefs.getInt("small_sync_interval",
            resources.getInteger(R.integer.small_sync_interval_default));
    }

    /** Gets the interval for syncs that are non-incremental but small (locations, users). */
    // This category is for syncs expected to take up to 500 ms, for data
    // that changes (on average) less than once an hour.
    public int getMediumSyncInterval() {
       return prefs.getInt("medium_sync_interval",
           resources.getInteger(R.integer.medium_sync_interval_default));
    }

    /** Gets the interval for syncs that are non-incremental and large (concepts, forms). */
    // This category is for syncs expected to take up to 2000 ms, for data
    // that changes (on average) less than once a day.
    public int getLargeSyncInterval() {
        return prefs.getInt("large_sync_interval",
            resources.getInteger(R.integer.large_sync_interval_default));
    }

    /** Gets the setting for whether to fabricate responses when the server fails. */
    public boolean getServerResponsesFabricated() {
        return prefs.getBoolean("server_responses_fabricated", false);
    }

    public String getLastIdPrefix() {
        return prefs.getString("last_id_prefix", "");
    }

    public void setLastIdPrefix(String prefix) {
        prefs.edit().putString("last_id_prefix", prefix).commit();
    }
}

