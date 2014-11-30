package org.msf.records.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * A compound layout that displays a table of data with fixed column and row headers.
 *
 * <p>This layout can only be instantiated programmatically; it is not intended to be used in XML.
 *
 * <p>In order to instantiate this layout, you must use a {@link DataGridView.Builder}, to which you
 * must provide a {@link DataGridAdapter}. The adapter's methods will be invoked synchronously on
 * construction and never again; therefore, the adapter must have ALL the data you wish to display
 * before you call {@link DataGridView.Builder#build}.
 *
 * <p>Based on https://www.codeofaninja.com/2013/08/android-scroll-table-fixed-header-column.html.
 */
public class DataGridView extends RelativeLayout {

    public static class Builder {

        private View mCornerView;
        private DataGridAdapter mDataGridAdapter;
        private boolean mHasDoubleWidthColumnHeaders = false;

        public Builder() {}

        /**
         * Sets the view in the top left corner of the grid.
         */
        public Builder setCornerView(View cornerView) {
            mCornerView = cornerView;
            return this;
        }

        public Builder setDataGridAdapter(DataGridAdapter dataGridAdapter) {
            mDataGridAdapter = dataGridAdapter;
            return this;
        }

        /**
         * Sets whether the column headers should be double width.
         *
         * <p>Double-width column headers span two columns each. When using double-width column
         * headers, {@link DataGridAdapter#getColumnHeader} will be called only for every other
         * column; for example, when called with column {@code 2}, the adapter should return the
         * column header for columns {@code 2} and {@code 3}.
         *
         * <p>If you use double-width column headers, the number of columns returned by
         * {@link DataGridAdapter#getColumnCount} must be a multiple of {@code 2}.
         */
        public Builder setDoubleWidthColumnHeaders(boolean hasDoubleWidthColumnHeaders) {
            mHasDoubleWidthColumnHeaders = hasDoubleWidthColumnHeaders;
            return this;
        }

        public DataGridView build(Context context) {
            if (mDataGridAdapter == null) {
                throw new IllegalStateException("Data grid adapter must be set.");
            }

            View cornerView = mCornerView != null ? mCornerView : new View(context);
            return new DataGridView(
                    context,
                    mDataGridAdapter,
                    cornerView,
                    mHasDoubleWidthColumnHeaders);
        }
    }

    private static final String TAG = DataGridView.class.getName();

    private Context mContext;
    private final DataGridAdapter mDataGridAdapter;
    private View mCornerView;
    private final boolean mHasDoubleWidthColumnHeaders;

    private TableLayout mColumnHeadersLayout;
    private TableLayout mRowHeadersLayout;
    private TableLayout mDataLayout;

    private Linkage<LinkableHorizontalScrollView> mHorizontalScrollViewLinkage;
    private HorizontalScrollView mColumnHeadersHorizontalScrollView;
    private HorizontalScrollView mDataHorizontalScrollView;

    private Linkage<LinkableScrollView> mVerticalScrollViewLinkage;
    private ScrollView mRowHeadersScrollView;
    private ScrollView mDataScrollView;

    @SuppressWarnings("ResourceType")
    private DataGridView(
            Context context,
            DataGridAdapter dataGridAdapter,
            View cornerView,
            boolean hasDoubleWidthColumnHeaders) {
        super(context);

        mContext = context;
        mDataGridAdapter = dataGridAdapter;
        mCornerView = cornerView;
        mHasDoubleWidthColumnHeaders = hasDoubleWidthColumnHeaders;

        if (mHasDoubleWidthColumnHeaders) {
            Preconditions.checkArgument(
                    mDataGridAdapter.getColumnCount() % 2 == 0,
                    "If using double-width column headers, the number of columns must be even.");
        }

        // Create all the main layout components.
        mColumnHeadersLayout = new TableLayout(mContext);
        mRowHeadersLayout = new TableLayout(mContext);
        mDataLayout = new TableLayout(mContext);

        mHorizontalScrollViewLinkage = new Linkage<LinkableHorizontalScrollView>();
        mColumnHeadersHorizontalScrollView =
                new LinkableHorizontalScrollView(mContext, mHorizontalScrollViewLinkage);
        mDataHorizontalScrollView =
                new LinkableHorizontalScrollView(mContext, mHorizontalScrollViewLinkage);
        mColumnHeadersHorizontalScrollView.setHorizontalScrollBarEnabled(false);
        mDataHorizontalScrollView.setHorizontalScrollBarEnabled(false);

        mVerticalScrollViewLinkage = new Linkage<LinkableScrollView>();
        mRowHeadersScrollView = new LinkableScrollView(mContext, mVerticalScrollViewLinkage);
        mDataScrollView = new LinkableScrollView(mContext, mVerticalScrollViewLinkage);
        mRowHeadersScrollView.setVerticalScrollBarEnabled(false);
        mDataScrollView.setVerticalScrollBarEnabled(false);

        // Set resource IDs so that they can be referenced by RelativeLayout.
        mCornerView.setId(1);
        mColumnHeadersHorizontalScrollView.setId(2);
        mRowHeadersScrollView.setId(3);
        mDataScrollView.setId(4);

        // Wrap the column headers in a horizontal scroll view.
        mColumnHeadersHorizontalScrollView.addView(mColumnHeadersLayout);

        // Wrap the row headers in a vertical scroll view.
        mRowHeadersScrollView.addView(mRowHeadersLayout);

        // Wrap the data grid in both horizontal and vertical scroll views.
        mDataScrollView.addView(mDataHorizontalScrollView);
        mDataHorizontalScrollView.addView(mDataLayout);

        // Add all the views to the main view.
        addView(mCornerView);

        RelativeLayout.LayoutParams columnHeadersParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        columnHeadersParams.addRule(RelativeLayout.RIGHT_OF, mCornerView.getId());
        addView(mColumnHeadersHorizontalScrollView, columnHeadersParams);

        RelativeLayout.LayoutParams mRowHeadersParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mRowHeadersParams.addRule(RelativeLayout.BELOW, mCornerView.getId());
        addView(mRowHeadersScrollView, mRowHeadersParams);

        RelativeLayout.LayoutParams mDataParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mDataParams.addRule(RelativeLayout.RIGHT_OF, mRowHeadersScrollView.getId());
        mDataParams.addRule(RelativeLayout.BELOW, mColumnHeadersHorizontalScrollView.getId());
        addView(mDataScrollView, mDataParams);

        // Add the column headers.
        mColumnHeadersLayout.addView(createColumnHeadersView());

        // Add the row headers.
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            TableRow row = new TableRow(mContext);
            View view = mDataGridAdapter.getRowHeader(i, null /*convertView*/, row);
            row.addView(view);

            mRowHeadersLayout.addView(row);
        }

        // Add the data cells.
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            TableRow row = new TableRow(mContext);
            for (int j = 0; j < mDataGridAdapter.getColumnCount(); j++) {
                View view = mDataGridAdapter.getCell(i, j, null /*convertView*/, row);

                row.addView(view);
            }

            mDataLayout.addView(row);
        }

        // Measure the entire layout!
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

        // Find the widest column header...
        int maxWidth = 0;
        if (!mHasDoubleWidthColumnHeaders) {
            for (int i = 0; i < mDataGridAdapter.getColumnCount(); i++) {
                int width = ((TableRow) mColumnHeadersLayout.getChildAt(0)).getChildAt(i)
                        .getMeasuredWidth();
                if (width > maxWidth) {
                    maxWidth = width;
                }
            }
        } else {
            for (int i = 0; i < mDataGridAdapter.getColumnCount() / 2; i++) {
                int width =
                        divideRoundUp(
                                ((TableRow) mColumnHeadersLayout.getChildAt(0)).getChildAt(i)
                                        .getMeasuredWidth(),
                                2);
                if (width > maxWidth) {
                    maxWidth = width;
                }
            }
        }

        // ... or data cell...
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            TableRow row = (TableRow) mDataLayout.getChildAt(i);
            for (int j = 0; j < mDataGridAdapter.getColumnCount(); j++) {
                int width = row.getChildAt(j).getMeasuredWidth();
                if (width > maxWidth) {
                    maxWidth = width;
                }
            }
        }

        // ... then set all of the column headers to that width...
        if (!mHasDoubleWidthColumnHeaders) {
            for (int i = 0; i < mDataGridAdapter.getColumnCount(); i++) {
                ViewGroup.LayoutParams newParams =
                        ((TableRow) mColumnHeadersLayout.getChildAt(0)).getChildAt(i)
                                .getLayoutParams();

                newParams.width = maxWidth;
            }
        } else {
            for (int i = 0; i < mDataGridAdapter.getColumnCount() / 2; i++) {
                ViewGroup.LayoutParams newParams =
                        ((TableRow) mColumnHeadersLayout.getChildAt(0)).getChildAt(i)
                                .getLayoutParams();

                newParams.width = maxWidth * 2;
            }
        }

        // ... then all of the data cells too.
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            TableRow row = (TableRow) mDataLayout.getChildAt(i);
            for (int j = 0; j < mDataGridAdapter.getColumnCount(); j++) {
                ViewGroup.LayoutParams newParams = row.getChildAt(j).getLayoutParams();

                newParams.width = maxWidth;
            }
        }

        // Measure again!
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

        // Find the tallest row header...
        int maxHeight = 0;
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            int height = ((TableRow) mRowHeadersLayout.getChildAt(i)).getChildAt(0)
                    .getMeasuredHeight();
            if (height > maxHeight) {
                maxHeight = height;
            }
        }

        // ... or data cell...
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            TableRow row = (TableRow) mDataLayout.getChildAt(i);
            for (int j = 0; j < mDataGridAdapter.getColumnCount(); j++) {
                int height = row.getChildAt(j).getMeasuredHeight();
                if (height > maxHeight) {
                    maxHeight = height;
                }
            }
        }

        // ... then set all of the row headers to that height...
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            ViewGroup.LayoutParams newParams =
                    ((TableRow) mRowHeadersLayout.getChildAt(i)).getChildAt(0).getLayoutParams();
            newParams.height = maxHeight;
        }

        // ... then all of the data cells too.
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            TableRow row = (TableRow) mDataLayout.getChildAt(i);
            for (int j = 0; j < mDataGridAdapter.getColumnCount(); j++) {
                ViewGroup.LayoutParams newParams = row.getChildAt(j).getLayoutParams();

                newParams.height = maxHeight;
            }
        }

        // Measure again (sigh)...
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

        // Find the tallest column header.
        int maxColumnHeaderHeight = 0;
        if (!mHasDoubleWidthColumnHeaders) {
            for (int i = 0; i < mDataGridAdapter.getColumnCount(); i++) {
                int height = ((TableRow) mColumnHeadersLayout.getChildAt(0)).getChildAt(i)
                        .getMeasuredHeight();
                if (height > maxColumnHeaderHeight) {
                    maxColumnHeaderHeight = height;
                }
            }
        } else {
            for (int i = 0; i < mDataGridAdapter.getColumnCount() / 2; i++) {
                int height = ((TableRow) mColumnHeadersLayout.getChildAt(0)).getChildAt(i)
                        .getMeasuredHeight();
                if (height > maxColumnHeaderHeight) {
                    maxColumnHeaderHeight = height;
                }
            }
        }

        // Find the widest row header.
        int maxRowHeaderWidth = 0;
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            int width = ((TableRow) mRowHeadersLayout.getChildAt(i)).getChildAt(0)
                    .getMeasuredWidth();
            if (width > maxRowHeaderWidth) {
                maxRowHeaderWidth = width;
            }
        }

        // Set the corner view's height and width.
        ViewGroup.LayoutParams cornerParams = mCornerView.getLayoutParams();
        cornerParams.height = maxColumnHeaderHeight;
        cornerParams.width = maxRowHeaderWidth;
    }

    private int divideRoundUp(int dividend, int divisor) {
        return (dividend + divisor - 1) / divisor;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mDataHorizontalScrollView.fullScroll(View.FOCUS_RIGHT);
    }

    private TableRow createColumnHeadersView() {
        TableRow columnHeadersTableRow = new TableRow(mContext);

        if (!mHasDoubleWidthColumnHeaders) {
            for (int i = 0; i < mDataGridAdapter.getColumnCount(); i++) {
                View view = mDataGridAdapter
                        .getColumnHeader(i, null /*convertView*/, columnHeadersTableRow);

                columnHeadersTableRow.addView(view);
            }
        } else {
            for (int i = 0; i < mDataGridAdapter.getColumnCount(); i += 2) {
                View view = mDataGridAdapter
                        .getColumnHeader(i, null /*convertView*/, columnHeadersTableRow);

                columnHeadersTableRow.addView(view);
            }
        }

        return columnHeadersTableRow;
    }

    private static class Linkage<T extends View> {

        Set<T> mLinkedViews = Sets.newHashSet();

        public void addLinkedView(T view) {
            mLinkedViews.add(view);
        }
    }

    /**
     * A {@link HorizontalScrollView} whose scrolling can be linked to other instances of this
     * class.
     */
    private static class LinkableHorizontalScrollView extends HorizontalScrollView {

        private Linkage<LinkableHorizontalScrollView> mLinkage;

        public LinkableHorizontalScrollView(
                Context context,
                Linkage<LinkableHorizontalScrollView> linkage) {
            super(context);

            mLinkage = linkage;

            linkage.addLinkedView(this);
        }

        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            for (LinkableHorizontalScrollView view : mLinkage.mLinkedViews) {
                if (view == this) {
                    continue;
                }

                view.scrollTo(l, 0);
            }
        }
    }

    /**
     * A {@link ScrollView} whose scrolling can be linked to other instances of this class.
     */
    private static class LinkableScrollView extends ScrollView {

        private Linkage<LinkableScrollView> mLinkage;

        public LinkableScrollView(
                Context context,
                Linkage<LinkableScrollView> linkage) {
            super(context);

            mLinkage = linkage;

            linkage.addLinkedView(this);
        }

        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            for (LinkableScrollView view : mLinkage.mLinkedViews) {
                if (view == this) {
                    continue;
                }

                view.scrollTo(0, t);
            }
        }
    }
}
