package org.msf.records.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.msf.records.R;
import org.msf.records.data.app.AppModel;
import org.msf.records.events.CrudEventBus;
import org.msf.records.filter.SimpleSelectionFilter;

/**
 * A {@link org.msf.records.ui.PatientListTypedCursorAdapter} that hides its group headings, as the
 * group heading is expected to be reflected in the title bar or by other information on the screen.
 */
public class SingleLocationPatientListAdapter extends PatientListTypedCursorAdapter {
    public SingleLocationPatientListAdapter(
            AppModel appModel, Context context, CrudEventBus eventBus) {
        super(appModel, context, eventBus);
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