package org.msf.records.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by nfortescue on 11/25/14.
 */
public enum ConceptType {
    @SerializedName("numeric")
    NUMERIC,
    @SerializedName("coded")
    CODED,
    @SerializedName("none")
    NONE,
}
