package org.msf.records.prefs;

import android.content.SharedPreferences;

// TODO(dxchen): License (from U+2020).

public class BooleanPreference {

    private final SharedPreferences mPreferences;
    private final String mKey;
    private final boolean mDefaultValue;

    public BooleanPreference(SharedPreferences preferences, String key) {
        this(preferences, key, false);
    }

    public BooleanPreference(SharedPreferences preferences, String key, boolean defaultValue) {
        this.mPreferences = preferences;
        this.mKey = key;
        this.mDefaultValue = defaultValue;
    }

    public boolean get() {
        return mPreferences.getBoolean(mKey, mDefaultValue);
    }

    public boolean isSet() {
        return mPreferences.contains(mKey);
    }

    public void set(boolean value) {
        mPreferences.edit().putBoolean(mKey, value).apply();
    }

    public void delete() {
        mPreferences.edit().remove(mKey).apply();
    }
}