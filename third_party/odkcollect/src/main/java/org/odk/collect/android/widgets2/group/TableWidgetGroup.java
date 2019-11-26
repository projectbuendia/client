package org.odk.collect.android.widgets2.group;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;

import org.odk.collect.android.widgets2.common.TypedWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link WidgetGroup} that displays widgets in a table with a configurable number of columns.
 */
public class TableWidgetGroup extends TableLayout implements WidgetGroup {

    private int mNumColumns = 2;
    private int mColumnsFilled = 0;

    private TableRow mCurrentRow = null;
    private List<TypedWidget<?>> mWidgets = new ArrayList<TypedWidget<?>>();

    public TableWidgetGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TableWidgetGroup setNumColumns(int numColumns) {
        mNumColumns = numColumns;
        return this;
    }

    public void addCell(View child, int span) {
        if (mCurrentRow == null || mColumnsFilled + span > mNumColumns) {
            mCurrentRow = new TableRow(getContext());
            mColumnsFilled = 0;
            super.addView(mCurrentRow);
        }

        TableRow.LayoutParams params =
            new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT);
        params.span = span;
        child.setLayoutParams(params);

        mCurrentRow.addView(child);
        mColumnsFilled += span;

        if (child instanceof TypedWidget<?>) {
            mWidgets.add((TypedWidget<?>) child);
        }
    }

    @Override public void addView(View child) {
        addCell(child, 1);
    }

    void addRow(View child) {
        addCell(child, mNumColumns);
    }

    @Override public List<TypedWidget<?>> getWidgets() {
        return mWidgets;
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If there are any columns, set all columns to be stretchable. This works around bug
        // https://code.google.com/p/android/issues/detail?id=19343.
        if (getChildCount() > 1 || (mCurrentRow != null && mCurrentRow.getChildCount() > 0)) {
            setStretchAllColumns(true);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
