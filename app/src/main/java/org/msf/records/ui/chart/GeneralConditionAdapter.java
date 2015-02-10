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
import org.msf.records.model.Concept;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Shows a list of general conditions, with corresponding number, color, and name.
 */
public class GeneralConditionAdapter extends ArrayAdapter<String> {
    private static final int VIEW_RESOURCE = R.layout.listview_cell_condition_selection;
    private String[] mConditions;
    @Nullable private String mSelectedConditionUuid;

    @Nullable public String getSelectedConditionUuid() {
        return mSelectedConditionUuid;
    }

    public void setSelectedConditionUuid(@Nullable String selectedConditionUuid) {
        mSelectedConditionUuid = selectedConditionUuid;
        notifyDataSetChanged();
    }

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
                Concept.getResStatus(conditionUuid).resolve(getContext().getResources());

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
