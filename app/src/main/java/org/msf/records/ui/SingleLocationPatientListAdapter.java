package org.msf.records.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.msf.records.R;
import org.msf.records.filter.SimpleSelectionFilter;

/**
 * SingleLocationPatientListAdapter is an ExpandablePatientListAdapter that hides its group
 * headings, as the group heading is expected to be reflected in the title bar or by other
 * information on the screen.
 */
public class SingleLocationPatientListAdapter extends ExpandablePatientListAdapter {
    private static final int SPACE_HEIGHT = 10;

    public SingleLocationPatientListAdapter(Cursor cursor, Context context, String queryFilterTerm,
                                            SimpleSelectionFilter filter) {
            super(cursor, context, queryFilterTerm, filter);
    }

    @Override
    protected View newGroupView(
            Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.patient_list_empty_group_header, null);
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
        // Intentionally blank.
    }
}