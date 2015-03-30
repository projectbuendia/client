// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.msf.records.widget;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;

import javax.annotation.Nullable;

/** An adapter that provides {@link View}s for {@link FastDataGridView}. */
public interface DataGridAdapter {

    /** Returns the number of rows in the data grid. */
    int getRowCount();

    /** Returns the number of columns in the data grid. */
    int getColumnCount();

    /**
     * Fills {@code view} with the correct contents for the row header of given row. Can be
     * reused in RecyclerView without inflating new views.ß
     */
    void fillRowHeader(int row, View view, TextView textView);

    /** Returns the row header view for a given row. */
    View getRowHeader(int row, View convertView, ViewGroup parent);

    /**
     * Sets a background for view {@code view}, depending on the {@code viewType} and
     * {@code rowType}.
     * @param viewType an identifier for the type of cell (e.g. cell vs. row header vs. column
     *                 header) which may be used to visualize different sections of the grid
     *                 differently
     * @param rowType 0 or 1, usually the row index modulo 2 -- used to alternate between colors
     */
    void setCellBackgroundForViewType(View view, CellType viewType, int rowType);

    /** Returns the column header view for a given column. */
    View getColumnHeader(int column, View convertView, ViewGroup parent);

    /**
     * Fills {@code textView} with the correct contents for column {@code column}. Can be used
     * in RecyclerView to populate existing views with the correct content.
     */
    void fillColumnHeader(int column, TextView textView);

    /** Returns the cell view for a given row and column. */
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
