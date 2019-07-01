// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;

import org.projectbuendia.client.utils.Utils;

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

    private void init() {
        getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    public EditIntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditIntegerPreference(Context context) {
        super(context);
        init();
    }

    @Override public String getText() {
        return mInteger != null ? mInteger.toString() : null;
    }

    @Override public void setText(String text) {
        boolean wasBlocking = shouldDisableDependents();
        mInteger = Utils.toIntOrNull(text);
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
                newValue = defaultValue != null ? Utils.toIntOrNull(defaultValue.toString()) : null;
            }
        }
        this.setText(newValue != null ? newValue.toString() : null);
    }
}
