package org.msf.records.ui.tentselection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.msf.records.R;
import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.res.ResZone;
import org.msf.records.model.Zone;
import org.msf.records.utils.PatientCountDisplay;
import org.msf.records.widget.SubtitledButtonView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Adapter for displaying a list of tents (locations).
 */
final class TentListAdapter extends ArrayAdapter<AppLocation> {

    private final Context mContext;
    private final AppLocationTree mLocationTree;
    private Optional<String> mSelectedLocationUuid;
    private View mSelectedView;

    public TentListAdapter(
            Context context,
            List<AppLocation> tents,
            AppLocationTree locationTree,
            Optional<String> selectedTent) {
        super(context, R.layout.listview_cell_tent_selection, tents);
        mContext = context;
        mLocationTree = locationTree;
        mSelectedLocationUuid = Preconditions.checkNotNull(selectedTent);
    }

    public Optional<String> getmSelectedLocationUuid() {
        return mSelectedLocationUuid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view;
        ViewHolder holder;
        if (convertView != null) {
        	view = convertView;
            holder = (ViewHolder) convertView.getTag();
        } else {
        	view = inflater.inflate(
                R.layout.listview_cell_tent_selection, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        AppLocation location = getItem(position);
        ResZone.Resolved zone = Zone.getResZone(
                location.parentUuid).resolve(mContext.getResources());

        holder.mButton.setTitle(location.toString());
        holder.mButton.setSubtitle(
                PatientCountDisplay.getPatientCountSubtitle(
                        mContext, mLocationTree.getTotalPatientCount(location)));
        holder.mButton.setBackgroundColor(zone.getBackgroundColor());
        holder.mButton.setTextColor(zone.getForegroundColor());

        if (mSelectedLocationUuid.isPresent()
                && mSelectedLocationUuid.get().equals(location.uuid)) {
            view.setBackgroundResource(R.color.zone_tent_selected_padding);
        } else {
            view.setBackgroundResource(R.drawable.tent_selector);
        }

        return view;
    }

    public void setmSelectedLocationUuid(Optional<String> locationUuid) {
        mSelectedLocationUuid = locationUuid;

        notifyDataSetChanged();
    }

    static class ViewHolder {

        @InjectView(R.id.tent_selection_tent) SubtitledButtonView mButton;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
