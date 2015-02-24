package org.msf.records.net.model;

import com.google.gson.annotations.SerializedName;

/**
 * An enumeration of concept types.
 */
public enum ConceptType {

    @SerializedName("numeric")
    NUMERIC,
    @SerializedName("coded")
    CODED,
    @SerializedName("text")
    TEXT,
    @SerializedName("date")
    DATE,
    @SerializedName("none")
    NONE,
}
