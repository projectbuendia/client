package org.msf.records.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.msf.records.R;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.view.SubtitledButtonView;

/**
 * Created by akalachman on 11/30/14.
 */
// TODO(akalachman): Replace with retrieval of real data.
public class TentListAdapter extends ArrayAdapter<SimpleSelectionFilter> {
    private final Context context;
    private final SimpleSelectionFilter[] values;

    private final String DEFAULT_SUBTITLE = "34 patients";

    public TentListAdapter(Context context, SimpleSelectionFilter[] values) {
        super(context, R.layout.listview_cell_tent_selection, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(
                R.layout.listview_cell_tent_selection, parent, false);

        SubtitledButtonView button =
                (SubtitledButtonView)rowView.findViewById(R.id.tent_selection_tent);
        button.setTitle(values[position].toString());
        button.setSubtitle(DEFAULT_SUBTITLE);

        return rowView;
    }
}
