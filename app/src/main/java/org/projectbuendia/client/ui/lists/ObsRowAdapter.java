package org.projectbuendia.client.ui.lists;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.models.ObsRow;
import java.util.ArrayList;

public class ObsRowAdapter extends ArrayAdapter<ObsRow> {

    public ObsRowAdapter(Context context, ArrayList<ObsRow> observations) {
        super(context, 0, observations);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ObsRow obsrow = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_observation,parent, false);
        }
        TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
        TextView tvValue = (TextView) convertView.findViewById(R.id.tvValue);

        tvTitle.setText(obsrow.time + " - " + obsrow.conceptName);
        tvValue.setText(obsrow.valueName);
        return convertView;
    }
}

