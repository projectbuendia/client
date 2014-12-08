package org.msf.records.prefs;

import android.content.SharedPreferences;

// TODO(dxchen): License (from U+2020).

public class StringPreference {

    private final SharedPreferences mPreferences;
    private final String mKey;
    private final String mDefaultValue;

    public StringPreference(SharedPreferences preferences, String key) {
        this(preferences, key, null);
    }

    public StringPreference(SharedPreferences preferences, String key, String defaultValue) {
        this.mPreferences = preferences;
        this.mKey = key;
        this.mDefaultValue = defaultValue;
    }

    public String get() {
        return mPreferences.getString(mKey, mDefaultValue);
    }

    public boolean isSet() {
        return mPreferences.contains(mKey);
    }

    public void set(String value) {
        mPreferences.edit().putString(mKey, value).apply();
    }

    public void delete() {
        mPreferences.edit().remove(mKey).apply();
    }
}