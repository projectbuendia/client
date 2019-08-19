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

package org.projectbuendia.client.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.projectbuendia.client.R;

/**
 * A {@link PatientListTypedCursorAdapter} that hides its group headings, as the group heading is
 * expected to be reflected in the title bar or by other information on the screen.
 */
public class SingleLocationPatientListAdapter extends PatientListTypedCursorAdapter {
    public SingleLocationPatientListAdapter(Context context) {
        super(context);
    }

    @Override public View getGroupView(
        int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        return newGroupView();
    }

    @Override protected View newGroupView() {
        return LayoutInflater.from(mContext).inflate(
            R.layout.patient_list_empty_group_header, null);
    }
}
