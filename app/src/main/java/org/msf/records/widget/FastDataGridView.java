package org.msf.records.widget;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import org.msf.records.R;

/**
 * A compound layout that displays a table of data with fixed column and row headers. Since the main
 * layout is defined in XML, call {@code #createView} to trigger the actual inflation.
 */
public class FastDataGridView {
    private final Context context;
    private final DataGridAdapter dataGridAdapter;
    private final LayoutInflater layoutInflater;

    public FastDataGridView(
            Context context, DataGridAdapter dataGridAdapter, LayoutInflater layoutInflater) {
        this.context = checkNotNull(context);
        this.dataGridAdapter = checkNotNull(dataGridAdapter);
        this.layoutInflater = checkNotNull(layoutInflater);
        Preconditions.checkArgument(
                dataGridAdapter.getColumnCount() % 2 == 0,
                "If using double-width column headers, the number of columns must be even.");
    }

    public View createView() {
        // Assemble top-level views.
        TableLayout topLayout =
                (TableLayout) layoutInflater.inflate(R.layout.tableview_chart_view, null);
        final LinkableRecyclerView rowHeaders =
                (LinkableRecyclerView) topLayout.findViewById(R.id.chart_row_headers);
        final LinkableRecyclerView columnHeaders =
                (LinkableRecyclerView) topLayout.findViewById(R.id.chart_col_headers);
        final LinkableRecyclerView cells =
                (LinkableRecyclerView) topLayout.findViewById(R.id.chart_cells);
        final LinkableScrollView verticalScrollview =
                (LinkableScrollView) topLayout.findViewById(R.id.chart_grid_vertical_scrollview);

        // Initialize rowHeaders.
        LinearLayoutManager rowHeaderLayoutManager = new WrapContentLinearLayoutManager(
                context, LinearLayoutManager.VERTICAL, false /* reverseLayout */);
        rowHeaders.setAdapter(new RowHeaderAdapter(
                dataGridAdapter, layoutInflater));
        rowHeaders.setLayoutManager(rowHeaderLayoutManager);
        rowHeaders.setInternalScrollListener(new OnInternalScrollListener() {
            @Override
            public void onInternalScrollBy(int dx, int dy) {
                verticalScrollview.scrollBy(0, dy);
            }
        });

        // Initialize columnHeaders.
        LinearLayoutManager columnHeaderLayoutManager = new WrapContentLinearLayoutManager(
                context, LinearLayoutManager.HORIZONTAL, false);
        columnHeaderLayoutManager.setReverseLayout(true);
        columnHeaders.setAdapter(new ColumnHeaderAdapter(dataGridAdapter, layoutInflater));
        columnHeaders.setLayoutManager(columnHeaderLayoutManager);
        columnHeaders.setInternalScrollListener(new OnInternalScrollListener() {
            @Override
            public void onInternalScrollBy(int dx, int dy) {
                cells.scrollBy(dx, 0);
            }
        });

        // Initialize cells.
        CellAdapter cellAdapter = new CellAdapter(
                dataGridAdapter, layoutInflater);
        RecyclerView.LayoutManager layoutManager = new WrapContentGridLayoutManager(
                context, dataGridAdapter.getRowCount(), LinearLayoutManager.HORIZONTAL,
                true /* reverseLayout */);
        cells.setHasFixedSize(true);
        cells.setLayoutManager(layoutManager);
        cells.setAdapter(cellAdapter);
        cells.setInternalScrollListener(new OnInternalScrollListener() {
            @Override
            public void onInternalScrollBy(int dx, int dy) {
                columnHeaders.scrollBy(dx, 0);
            }
        });
        verticalScrollview.setInternalScrollListener(new OnInternalScrollListener() {
            @Override
            public void onInternalScrollBy(int dx, int dy) {
                rowHeaders.scrollBy(0, dy);
            }
        });

        return topLayout;
    }

    /**
     * ViewHolder class for RowHeaders, ColumnHeaders and Cells.
     */
    private static final class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textView;
        public ViewHolder(View itemView, TextView textView) {
            super(itemView);
            this.textView = textView;
        }
    }

    private static final class CellViewHolder extends RecyclerView.ViewHolder {
        public TextView textView = null;
        public ViewStub viewStub;
        public CellViewHolder(View itemView, ViewStub viewStub) {
            super(itemView);
            this.viewStub = viewStub;
        }
    }

    /**
     * A {@link RecyclerView.Adapter} for patient observation cells.
     *
     * <p>This class wraps {@link DataGridAdapter} but reverses the order in which columns are
     * populated so that the most recent day with observations is shown on screen first.
     */
    private static final class CellAdapter extends RecyclerView.Adapter {
        private final DataGridAdapter mDataGridAdapter;
        private final LayoutInflater mLayoutInflater;


        public CellAdapter(
                DataGridAdapter dataGridAdapter, LayoutInflater layoutInflater) {
            this.mDataGridAdapter = checkNotNull(dataGridAdapter);
            this.mLayoutInflater = checkNotNull(layoutInflater);
        }

        @Override
        public int getItemViewType(int position) {
            // Setting row backgrounds is expensive, so only reuse item views with the right
            // background color.
            int rowIndex = position % mDataGridAdapter.getRowCount();
            return rowIndex % 2;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(
                    R.layout.data_grid_cell_chart_text, parent, false /* attachToRoot */);
            ViewStub viewStub = (ViewStub) view.findViewById(R.id.data_grid_cell_chart_viewstub);
            mDataGridAdapter.setCellBackgroundForViewType(view, CellType.CELL, viewType);
            return new CellViewHolder(view, viewStub);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int columnIndex = position / mDataGridAdapter.getRowCount();
            int rowIndex = position % mDataGridAdapter.getRowCount();
            CellViewHolder viewHolder = (CellViewHolder) holder;
            viewHolder.textView = mDataGridAdapter.fillCell(
                    rowIndex,
                    (mDataGridAdapter.getColumnCount() - columnIndex - 1),
                    viewHolder.itemView,
                    viewHolder.viewStub,
                    viewHolder.textView);
            if (viewHolder.textView != null) {
                // After inflation, the viewStub is no longer part of the view hierarchy, so let it
                // be garbage collected.
                viewHolder.viewStub = null;
            }
        }

        @Override
        public int getItemCount() {
            return mDataGridAdapter.getColumnCount() * mDataGridAdapter.getRowCount();
        }
    }

    private static final class RowHeaderAdapter extends RecyclerView.Adapter {
        private final DataGridAdapter mDataGridAdapter;
        private final LayoutInflater mLayoutInflater;

        public RowHeaderAdapter(DataGridAdapter dataGridAdapter, LayoutInflater layoutInflater) {
            this.mDataGridAdapter = checkNotNull(dataGridAdapter);
            this.mLayoutInflater = checkNotNull(layoutInflater);
        }

        @Override
        public int getItemViewType(int position) {
            // Setting row backgrounds is expensive, so only reuse item views with the right
            // background color.
            return position % 2;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(
                    R.layout.data_grid_row_header_chart, parent, false /* attachToRoot */);
            TextView textView = (TextView) view.findViewById(R.id.data_grid_header_text);
            mDataGridAdapter.setCellBackgroundForViewType(view, CellType.ROW_HEADER, viewType);
            return new ViewHolder(view, textView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int rowIndex = position;
            mDataGridAdapter.fillRowHeader(
                    rowIndex, holder.itemView, ((ViewHolder)holder).textView);
        }

        @Override
        public int getItemCount() {
            return mDataGridAdapter.getRowCount();
        }
    }

    private static final class ColumnHeaderAdapter extends RecyclerView.Adapter {
        private final DataGridAdapter mDataGridAdapter;
        private final LayoutInflater mLayoutInflater;

        public ColumnHeaderAdapter(DataGridAdapter dataGridAdapter, LayoutInflater layoutInflater) {
            this.mDataGridAdapter = checkNotNull(dataGridAdapter);
            this.mLayoutInflater = (layoutInflater);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(
                    R.layout.data_grid_column_header_chart, parent, false /* attachToRoot */);
            TextView textView = (TextView) view.findViewById(R.id.data_grid_header_text);
            return new ViewHolder(view, textView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            mDataGridAdapter.fillColumnHeader(
                    (mDataGridAdapter.getColumnCount() - (position * 2) - 2),
                    ((ViewHolder) holder).textView);
        }

        @Override
        public int getItemCount() {
            return mDataGridAdapter.getColumnCount() / 2;
        }
    }

    /**
     * Interface to notify clients of internal scroll events of a View. These events have been
     * generated by a user scrolling the view itself, rather than by programmatically scrolling it.
     */
    private interface OnInternalScrollListener {
        void onInternalScrollBy(int dx, int dy);
    }

    private static final OnInternalScrollListener DO_NOTHING_LISTENER =
            new OnInternalScrollListener() {

                @Override
                public void onInternalScrollBy(int dx, int dy) {}
            };

    /**
     * A {@link android.widget.ScrollView} whose scrolling can be linked to other instances of this class.
     */
    public static class LinkableScrollView extends ScrollView {

        private OnInternalScrollListener internalScrollListener = DO_NOTHING_LISTENER;
        /** Whether the current scrolling operation was caused by a linked view. */
        boolean externallyScrolled = false;
        int lastX = 0;
        int lastY = 0;

        public LinkableScrollView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void setInternalScrollListener(OnInternalScrollListener l) {
            internalScrollListener = Preconditions.checkNotNull(l);
        }

        @Override
        public void scrollBy(int x, int y) {
            externallyScrolled = true;
            super.scrollBy(x, y);
        }

        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            super.onScrollChanged(l, t, oldl, oldt);
            // Due to overscrolling, some onScrollChanged events might be sent twice. Hence we cannot
            // rely on the diff "l - oldl" and instead do the tracking ourselves.
            if (!externallyScrolled) {
                int dx = l - lastX;
                int dy = t - lastY;
                if (dx != 0 || dy != 0) {
                    internalScrollListener.onInternalScrollBy(l - lastX, t - lastY);
                }
            } else {
                externallyScrolled = false;
            }
            lastX = l;
            lastY = t;
        }
    }

    public static class LinkableRecyclerView extends RecyclerView {
        private OnInternalScrollListener internalScrollListener = DO_NOTHING_LISTENER;
        boolean externallyScrolled = false;

        public LinkableRecyclerView(Context context, AttributeSet attrs) {
            super(context, attrs);
            setOnScrollListener(new OnScrollListener() {

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (!externallyScrolled) {
                        internalScrollListener.onInternalScrollBy(dx, dy);
                    } else {
                        externallyScrolled = false;
                    }
                }
            });
        }

        @Override
        public void scrollBy(int x, int y) {
            externallyScrolled = true;
            super.scrollBy(x, y);
        }

        public void setInternalScrollListener(OnInternalScrollListener l) {
            internalScrollListener = Preconditions.checkNotNull(l);
        }
    }
}
