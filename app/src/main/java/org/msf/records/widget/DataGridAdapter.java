package org.msf.records.widget;

import android.view.View;
import android.view.ViewGroup;

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
     * Returns the row header view for a given row.
     */
    View getRowHeader(int row, View convertView, ViewGroup parent);

    /**
     * Returns the column header view for a given column.
     */
    View getColumnHeader(int column, View convertView, ViewGroup parent);

    /**
     * Returns the cell view for a given row and column.
     */
    View getCell(int row, int column, View convertView, ViewGroup parent);
}
