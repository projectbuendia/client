package org.msf.records.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;

/**
 * Custom Android preference widget for editing an Integer value. The majority of code modifies
 * the storage type to be an Integer rather than a String.
 */
public class EditIntegerPreference extends EditTextPreference {

    private Integer mInteger;

    public EditIntegerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public EditIntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditIntegerPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        Integer newValue;
        if (restoreValue) {
            if (!shouldPersist()) {
                newValue = mInteger;
            } else {
                PreferenceManager preferenceManager = getPreferenceManager();
                SharedPreferences sharedPreferences = preferenceManager.getSharedPreferences();
                String key = getKey();
                // We have to do the contains check, as we can't do getInt(x, null) to get null.
                newValue = sharedPreferences.contains(key)
                        ? sharedPreferences.getInt(key, 0) : mInteger;
            }
        } else {
            if (defaultValue instanceof Integer) {
                newValue = (Integer) defaultValue;
            } else {
                newValue = defaultValue == null ? null : parseIntOrNull(defaultValue.toString());
            }
        }
        this.setText(newValue == null ? null : newValue.toString());
    }

    @Override
    public String getText() {
        return mInteger != null ? mInteger.toString() : null;
    }

    @Override
    public void setText(String text) {
        boolean wasBlocking = shouldDisableDependents();
        mInteger = parseIntOrNull(text);
        if (mInteger == null) {
            SharedPreferences.Editor editor = getEditor();
            editor.remove(getKey());
            editor.commit();
        } else {
            persistInt(mInteger);
        }
        boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    private Integer parseIntOrNull(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
