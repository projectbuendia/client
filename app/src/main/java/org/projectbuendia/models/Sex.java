package org.projectbuendia.models;

import com.google.gson.annotations.SerializedName;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum Sex {
    @SerializedName("F") FEMALE,
    @SerializedName("M") MALE,
    @SerializedName("O") OTHER;

    public static @Nullable Sex nullableValueOf(@Nullable String name) {
        return name != null ? Enum.valueOf(Sex.class, name) : null;
    }

    public static @Nullable String nullableNameOf(@Nullable Sex sex) {
        return sex != null ? sex.name() : null;
    }

    /** Converts a Sex value to a code for JSON communication with the server. */
    public static @Nullable String serialize(@Nullable Sex sex) {
        if (sex == null) return null;
        try {
            return Sex.class.getField(sex.name()).getAnnotation(SerializedName.class).value();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /** Gets the localized abbreviation for a Sex value. */
    public static @Nonnull String getAbbreviation(@Nullable Sex sex) {
        // Java switch is not safe to use because it stupidly crashes on null.
        if (sex == FEMALE) return App.str(R.string.sex_female_abbreviation);
        if (sex == MALE) return App.str(R.string.sex_male_abbreviation);
        if (sex == OTHER) return App.str(R.string.sex_other_abbreviation);
        return "";
    }
}
