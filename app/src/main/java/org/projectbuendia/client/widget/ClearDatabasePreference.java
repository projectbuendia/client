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
import org.projectbuendia.client.sync.Database;

/** Custom Android preference widget for clearing the database. */
public class ClearDatabasePreference extends EditTextPreference {
    public ClearDatabasePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onDialogClosed(boolean positive) {
        if (positive) {
            new Database(App.getInstance().getApplicationContext()).clear();
        }
    }

    @Override
    public String getText() {
        return "Are you sure you want to clear the local database?";
    }
}
