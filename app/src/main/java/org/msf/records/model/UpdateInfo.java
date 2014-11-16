package org.msf.records.model;

import android.support.annotation.Nullable;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * A Gson object that represents update info responses from the update server.
 */
public class UpdateInfo implements Serializable {

    @SerializedName("androidclient")
    public ComponentUpdateInfo androidClient;

    /**
     * A Gson object that represents update info for a specific component.
     */
    public static class ComponentUpdateInfo implements Serializable {

        /**
         * The latest version available on the server, as a string.
         */
        @SerializedName("latest_version")
        String latestVersion;

        /**
         * The first time of day at which the update can be automatically installed, in 24-hour
         * format.
         */
        @SerializedName("install_window_hours_min")
        public int installWindowHoursMin;

        /**
         * The last time of day at which the update can be automatically installed, in 24-hour
         * format.
         */
        @SerializedName("install_window_hours_max")
        public int installWindowHoursMax;

        /**
         * A list of commands to be run in order to update a component.
         *
         * <p>The format of each entry is component-specific.
         */
        public List<String> run;

        /**
         * Returns the latest semantic version available on the server or {@code null} if no latest
         * version is specified.
         */
        @Nullable
        public Version getLatestVersion() {
            try {
                return latestVersion == null ? null : Version.valueOf(latestVersion);
            } catch (ParseException e) {
                return null;
            }
        }
    }
}
