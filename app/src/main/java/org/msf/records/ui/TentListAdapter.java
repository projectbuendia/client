package org.msf.records.ui;

import java.util.List;

import org.msf.records.R;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Zone;
import org.msf.records.utils.PatientCountDisplay;
import org.msf.records.widget.SubtitledButtonView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * Created by akalachman on 11/30/14.
 */
public class TentListAdapter extends ArrayAdapter<LocationSubtree> {
    private final Context context;

    public TentListAdapter(Context context, List<LocationSubtree> values) {
        super(context, R.layout.listview_cell_tent_selection, values);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(
                R.layout.listview_cell_tent_selection, parent, false);

        LocationSubtree tent = getItem(position);
        SubtitledButtonView button =
                (SubtitledButtonView)rowView.findViewById(R.id.tent_selection_tent);
        button.setTitle(tent.toString());
        button.setSubtitle(
                PatientCountDisplay.getPatientCountSubtitle(context, tent.getPatientCount()));
        button.setBackgroundResource(
                Zone.getBackgroundColorResource(tent.getLocation().parent_uuid));
        button.setTextColor(
                Zone.getForegroundColorResource(tent.getLocation().parent_uuid));

        return rowView;
    }
}
