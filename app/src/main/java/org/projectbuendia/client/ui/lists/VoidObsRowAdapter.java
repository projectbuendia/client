package org.projectbuendia.client.ui.lists;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.models.ObsRow;

import java.util.ArrayList;

public class VoidObsRowAdapter  extends ArrayAdapter<ObsRow> {

    public ArrayList<String> mCheckedItems;

    public VoidObsRowAdapter (Context context, ArrayList<ObsRow> observations) {
        super(context, 0, observations);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ObsRow obsrow = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_void_observation,parent, false);
        }

        TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
        TextView tvValue = (TextView) convertView.findViewById(R.id.tvValue);

        tvTitle.setText(obsrow.time + " - " + obsrow.conceptName);
        tvValue.setText(obsrow.valueName);

        CheckBox cbVoid = (CheckBox) convertView.findViewById(R.id.cbVoid);
        cbVoid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                String uuid = obsrow.uuid;

                if (isChecked) {
                    //mCheckedItems.add(uuid);
                } else {
                    //mCheckedItems.remove(uuid);
                }
            }
        });

        return convertView;
    }
}

