package org.msf.records.widget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * An adapter that provides {@link View}s for {@link DataGridView}.
 */
public interface DataGridAdapter {

    /**
     * Returns the number of rows in the data grid.
     */
    int getRowCount();

    /**
     * Returns the number of columns in the data grid.
     */
    int getColumnCount();

    /**
     * Fills {@code view} with the correct contents for the row header of given row. Can be
     * reused in RecyclerView without inflating new views.ß
     */
    void fillRowHeader(int row, View view, TextView textView);

    /**
     * Returns the row header view for a given row.
     */
    View getRowHeader(int row, View convertView, ViewGroup parent);

    /**
     * Returns the column header view for a given column.
     */
    View getColumnHeader(int column, View convertView, ViewGroup parent);

    /**
     * Fills {@code textView} with the correct contents for column {@code column}. Can be used
     * in RecyclerView to populate existing views with the correct content.
     */
    void fillColumnHeader(int column, TextView textView);

    /**
     * Returns the cell view for a given row and column.
     */
    View getCell(int row, int column, View convertView, ViewGroup parent);

    /**
     * Fills {@code view} with the correct contents for the cell at given row and column. Can be
     * reused in RecyclerView without inflating new views.
     */
    void fillCell(int row, int column, View view, TextView textView);
}
