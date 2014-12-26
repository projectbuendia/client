package org.msf.records.widget;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;

import javax.annotation.Nullable;

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
     * Sets a light or dark background for view {@code view}, depending on the {@code viewType}.
     * @param viewType 0 or 1, usually the row index modulo 2
     */
    void setCellBackgroundForViewType(View view, int viewType);

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
     *
     * <p>Either {@code viewStub} or {@code textView} must be non-null. If {@code textView} is null
     * and we need the textView to represent the cell, it will be inflated from {@code viewStub} and
     * returned by the method.
     */
    @Nullable TextView fillCell(int row, int column, View view, @Nullable ViewStub viewStub,
                      @Nullable TextView textView);
}
