package org.msf.records.ui.tentselection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.msf.records.R;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Zone;
import org.msf.records.utils.PatientCountDisplay;
import org.msf.records.widget.SubtitledButtonView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Adapter for displaying a list of tents (locations).
 */
final class TentListAdapter extends ArrayAdapter<LocationSubtree> {

    private final Context context;
    private final Optional<String> selectedLocationUuid;
    private View mSelectedView;

    public TentListAdapter(
            Context context,
            List<LocationSubtree> tents,
            Optional<String> selectedTent) {
        super(context, R.layout.listview_cell_tent_selection, tents);
        this.context = context;
        this.selectedLocationUuid = Preconditions.checkNotNull(selectedTent);
    }

    public Optional<String> getSelectedLocationUuid() {
        return selectedLocationUuid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
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

        LocationSubtree tent = getItem(position);
        holder.mButton.setTitle(tent.toString());
        holder.mButton.setSubtitle(
                PatientCountDisplay.getPatientCountSubtitle(context, tent.getPatientCount()));
        holder.mButton.setBackgroundResource(
                Zone.getBackgroundColorResource(tent.getLocation().parent_uuid));
        holder.mButton.setTextColor(
                Zone.getForegroundColorResource(tent.getLocation().parent_uuid));

        if (selectedLocationUuid.isPresent() &&
                selectedLocationUuid.get().equals(tent.getLocation().uuid)) {
            setSelectedView( view );
        }

        return view;
    }

    public View getSelectedView() { return mSelectedView; }
    public void setSelectedView( View view )
    {
        if ( mSelectedView != null ) {
            mSelectedView.setBackgroundResource(R.drawable.tent_selector);
        }

        mSelectedView = view;

        if ( view != null )
        {
            view.setBackgroundResource(R.color.zone_tent_selected_padding);
        }
    }

    static class ViewHolder {
        
        @InjectView(R.id.tent_selection_tent) SubtitledButtonView mButton;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
