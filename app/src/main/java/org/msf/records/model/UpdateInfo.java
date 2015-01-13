package org.msf.records.model;

import com.google.gson.annotations.SerializedName;

import org.msf.records.utils.LexicographicVersion;

/**
 * A Gson object that represents an available update.
 */
public class UpdateInfo {

    @SerializedName("src")
    public String source;

    @SerializedName("version")
    public String version;

    /**
     * Returns the parsed {@link LexicographicVersion} or {@code null} if the version is
     * malformed.
     */
    public LexicographicVersion getParsedVersion() {
        try {
            return version == null ? null : LexicographicVersion.parse(version);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
