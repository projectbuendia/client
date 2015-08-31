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

package org.projectbuendia.client.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.sync.Database;

/** Custom Android preference widget that clears the database if new text is entered. */
public class EditAndClearDataPreference extends EditTextPreference {
    public EditAndClearDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPositiveButtonText(R.string.clear_data_button);
    }

    public void onDialogClosed(boolean positive) {
        super.onDialogClosed(positive);
        if (positive) {
            new Database(App.getInstance().getApplicationContext()).clear();
            App.getUserManager().reset();
        }
    }
}
