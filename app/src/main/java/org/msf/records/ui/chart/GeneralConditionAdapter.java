package org.msf.records.ui.chart;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Optional;

import org.msf.records.R;
import org.msf.records.data.app.AppLocation;
import org.msf.records.data.res.ResStatus;
import org.msf.records.data.res.ResZone;
import org.msf.records.model.Concept;
import org.msf.records.model.Zone;
import org.msf.records.utils.PatientCountDisplay;
import org.msf.records.widget.AutoResizeTextView;
import org.msf.records.widget.SubtitledButtonView;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by akalachman on 1/26/15.
 */
public class GeneralConditionAdapter extends ArrayAdapter<String> {
    private static final int VIEW_RESOURCE = R.layout.listview_cell_condition_selection;
    private String[] mConditions;
    private Optional<String> mSelectedConditionUuid;

    public Optional<String> getSelectedConditionUuid() {
        return mSelectedConditionUuid;
    }

    public void setSelectedConditionUuid(Optional<String> selectedConditionUuid) {
        mSelectedConditionUuid = selectedConditionUuid;
        notifyDataSetChanged();
    }

    public GeneralConditionAdapter(
            Context context, String[] conditions, Optional<String> selectedConditionUuid)  {
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

        // TODO: This is probably wrong.
        /*if (mSelectedConditionUuid.isPresent()
                && mSelectedConditionUuid.get().equals(conditionUuid)) {
            view.setBackgroundResource(R.color.zone_tent_selected_padding);
        } else {
            view.setBackgroundResource(R.drawable.tent_selector);
        }*/

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
