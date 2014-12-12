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

/**
 * Adapter for displaying a list of tents (locations).
 */
final class TentListAdapter extends ArrayAdapter<LocationSubtree> {
    private final Context context;
    private final Optional<String> selectedTentUuid;

    public TentListAdapter(
            Context context,
            List<LocationSubtree> tents,
            Optional<String> selectedTent) {
        super(context, R.layout.listview_cell_tent_selection, tents);
        this.context = context;
        this.selectedTentUuid = Preconditions.checkNotNull(selectedTent);
    }

    public Optional<String> getSelectedTentUuid() {
        return selectedTentUuid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view;
        if (convertView != null) {
        	view = convertView;
        } else {
        	view = inflater.inflate(
                R.layout.listview_cell_tent_selection, parent, false);
        }

        // TODO: Apply view holder pattern here to avoid calling findViewById.
        LocationSubtree tent = getItem(position);
        SubtitledButtonView button =
                (SubtitledButtonView)view.findViewById(R.id.tent_selection_tent);
        button.setTitle(tent.toString());
        button.setSubtitle(
                PatientCountDisplay.getPatientCountSubtitle(context, tent.getPatientCount()));
        button.setBackgroundResource(
                Zone.getBackgroundColorResource(tent.getLocation().parent_uuid));
        button.setTextColor(
                Zone.getForegroundColorResource(tent.getLocation().parent_uuid));

        if (selectedTentUuid.isPresent() &&
                selectedTentUuid.get() == tent.getLocation().uuid) {
            view.setBackgroundResource(R.color.zone_tent_selected_padding);
        }

        return view;
    }
}
