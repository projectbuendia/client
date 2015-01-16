package org.msf.records.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.msf.records.R;

/**
 * A {@link PatientListTypedCursorAdapter} that hides its group headings, as the group heading is
 * expected to be reflected in the title bar or by other information on the screen.
 */
public class SingleLocationPatientListAdapter extends PatientListTypedCursorAdapter {
    public SingleLocationPatientListAdapter(Context context) {
        super(context);
    }

    @Override
    protected View newGroupView() {
        return LayoutInflater.from(mContext).inflate(
                R.layout.patient_list_empty_group_header, null);
    }

    @Override
    public View getGroupView(
            int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        return newGroupView();
    }
}