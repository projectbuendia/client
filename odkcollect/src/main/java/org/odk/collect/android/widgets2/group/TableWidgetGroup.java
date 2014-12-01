package org.odk.collect.android.widgets2.group;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;

import org.odk.collect.android.widgets2.common.TypedWidget;
import org.odk.collect.android.widgets2.selectone.BinarySelectOneWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link WidgetGroup} that displays widgets in a table with a configurable number of columns.
 */
public class TableWidgetGroup extends TableLayout implements WidgetGroup {

    private int mNumColumns = 2;

    private TableRow mLastTableRow = null;
    private List<TypedWidget<?>> mWidgets = new ArrayList<TypedWidget<?>>();

    public TableWidgetGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TableWidgetGroup setNumColumns(int numColumns) {
        mNumColumns = numColumns;

        return this;
    }

    @Override
    public void addView(View child) {
        if (mLastTableRow == null || mLastTableRow.getChildCount() == mNumColumns) {
            mLastTableRow = new TableRow(getContext());
            mLastTableRow.setWeightSum(mNumColumns);

            super.addView(mLastTableRow);
        }

        child.setLayoutParams(new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        mLastTableRow.addView(child);

        if (child instanceof TypedWidget<?>) {
            mWidgets.add((TypedWidget<?>) child);
        }
    }

    @Override
    public List<TypedWidget<?>> getWidgets() {
        return mWidgets;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If there are any columns, set all columns to be stretchable. This works around bug
        // https://code.google.com/p/android/issues/detail?id=19343.
        if (getChildCount() > 1 || (mLastTableRow != null && mLastTableRow.getChildCount() > 0)) {
            setStretchAllColumns(true);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
