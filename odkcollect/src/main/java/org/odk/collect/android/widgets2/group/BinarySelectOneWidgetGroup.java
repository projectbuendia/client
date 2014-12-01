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
 * A {@link WidgetGroup} that contains a number of {@link BinarySelectOneWidget}s.
 */
public class BinarySelectOneWidgetGroup extends TableLayout implements WidgetGroup {

    private TableRow mLastTableRow = null;
    private List<TypedWidget<?>> mWidgets = new ArrayList<TypedWidget<?>>();

    public BinarySelectOneWidgetGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void addView(View child) {
        if (mLastTableRow == null || mLastTableRow.getChildCount() == 2) {
            mLastTableRow = new TableRow(getContext());
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

        // If the last table row only has one item, add another to have it take up the appropriate
        // amount of space.
        if (mLastTableRow != null && mLastTableRow.getChildCount() == 1) {
            View view = new View(getContext());
            view.setLayoutParams(new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

            mLastTableRow.addView(view);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
