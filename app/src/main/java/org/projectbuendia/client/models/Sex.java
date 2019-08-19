package org.projectbuendia.client.models;

import javax.annotation.Nonnull;

public enum Sex {
    UNKNOWN("U"),
    MALE("M"),
    FEMALE("F"),
    OTHER("O");

    public final String code;

    Sex(String code) {
        this.code = code;
    }

    public static @Nonnull Sex forCode(String code) {
        for (Sex sex : values()) {
            if (sex.code.equals(code)) return sex;
        }
        return UNKNOWN;
    }
}
