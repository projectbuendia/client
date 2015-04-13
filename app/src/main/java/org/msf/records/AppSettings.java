package org.msf.records;

import android.content.SharedPreferences;
import android.content.res.Resources;

/** Access to preference settings. */
public class AppSettings {
    SharedPreferences mSharedPreferences;
    Resources mResources;

    /**
     * The minimum period between checks for APK updates, in seconds.  Repeated calls to
     * UpdateManager.checkForUpdate() within this period will not check the server for new updates.
     */
    static final int APK_UPDATE_INTERVAL_DEFAULT = 90; // default to 1.5 minutes.

    public AppSettings(SharedPreferences sharedPreferences, Resources resources) {
        mSharedPreferences = sharedPreferences;
        mResources = resources;
    }

    public String getOpenmrsUrl() {
        return mSharedPreferences.getString("openmrs_root_url",
                mResources.getString(R.string.openmrs_root_url_default));
    }

    public String getOpenmrsUrl(String urlPath) {
        return getOpenmrsUrl().replaceAll("/*$", "") + urlPath;
    }

    public String getOpenmrsUser() {
        return mSharedPreferences.getString("openmrs_user",
                mResources.getString(R.string.openmrs_user_default));
    }

    public String getOpenmrsPassword() {
        return mSharedPreferences.getString("openmrs_password",
                mResources.getString(R.string.openmrs_password_default));
    }

    public String getPackageServerUrl() {
        return mSharedPreferences.getString("package_server_root_url",
                mResources.getString(R.string.package_server_root_url_default));
    }

    public String getPackageServerUrl(String urlPath) {
        return getPackageServerUrl().replaceAll("/*$", "") + urlPath;
    }

    public boolean getXformUpdateClientCache() {
        return mSharedPreferences.getBoolean("xform_update_client_cache",
                mResources.getBoolean(R.bool.xform_update_client_cache_default));
    }

    public boolean getIncrementalObservationUpdate() {
        return mSharedPreferences.getBoolean("incremental_observation_update",
                mResources.getBoolean(R.bool.incremental_observation_update_default));
    }

    public int getApkUpdateInterval() {
        return mSharedPreferences.getInt("apk_update_interval_secs", APK_UPDATE_INTERVAL_DEFAULT);
    }

    public boolean getKeepFormInstancesLocally() {
        return mSharedPreferences.getBoolean("keep_form_instances_locally",
                mResources.getBoolean(R.bool.keep_form_instances_locally_default));
    }

    public boolean getSetupComplete() {
        return mSharedPreferences.getBoolean("setup_complete", false);
    }

    public void setSetupComplete(boolean value) {
        mSharedPreferences.edit().putBoolean("setup_complete", value).commit();
    }
}
