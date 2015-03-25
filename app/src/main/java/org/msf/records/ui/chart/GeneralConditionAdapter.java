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

package org.msf.records.ui.chart;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.msf.records.R;
import org.msf.records.data.res.ResStatus;
import org.msf.records.model.Concepts;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Shows a list of general conditions, with corresponding number, color, and name.
 */
public class GeneralConditionAdapter extends ArrayAdapter<String> {
    private static final int VIEW_RESOURCE = R.layout.listview_cell_condition_selection;
    private String[] mConditions;
    @Nullable private String mSelectedConditionUuid;

    public void setSelectedConditionUuid(@Nullable String selectedConditionUuid) {
        mSelectedConditionUuid = selectedConditionUuid;
        notifyDataSetChanged();
    }

    /**
     * Creates a new adapter that displays conditions corresponding to the given UUID's.
     * @param context an activity context
     * @param conditions UUID's of the general conditions to display
     * @param selectedConditionUuid UUID of the current general condition, or null if the patient
     *                              has no prior known condition
     */
    public GeneralConditionAdapter(
            Context context, String[] conditions, @Nullable String selectedConditionUuid)  {
        super(context, VIEW_RESOURCE, conditions);
        mConditions = conditions;
        mSelectedConditionUuid = selectedConditionUuid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view;
        ViewHolder holder;
        if (convertView != null) {
            view = convertView;
            holder = (ViewHolder) convertView.getTag();
        } else {
            view = inflater.inflate(VIEW_RESOURCE, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        String conditionUuid = getItem(position);
        ResStatus.Resolved condition =
                Concepts.getResStatus(conditionUuid).resolve(getContext().getResources());

        holder.mConditionNumber.setText(condition.getShortDescription());
        holder.mConditionNumber.setTextColor(condition.getForegroundColor());
        holder.mConditionText.setText(condition.getMessage());
        holder.mConditionText.setTextColor(condition.getForegroundColor());
        holder.mConditionParent.setBackgroundColor(condition.getBackgroundColor());

        // TODO: Show currently-selected condition differently.

        return view;
    }

    static class ViewHolder {
        @InjectView(R.id.condition_selection_text) TextView mConditionText;
        @InjectView(R.id.condition_selection_number) TextView mConditionNumber;
        @InjectView(R.id.condition_selection_parent) LinearLayout mConditionParent;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
