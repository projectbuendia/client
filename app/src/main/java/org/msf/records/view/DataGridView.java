package org.msf.records.view;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A compound layout that displays a table of data with fixed column and row headers.
 *
 * <p>This layout can only be instantiated programmatically; it is not intended to be used in XML.
 *
 * <p>Based on https://www.codeofaninja.com/2013/08/android-scroll-table-fixed-header-column.html.
 */
public class DataGridView extends RelativeLayout {

    public interface DataGridAdapter {

        int getColumnCount();

        int getRowCount();

        View getRowHeader(int row, View convertView, ViewGroup parent);

        View getColumnHeader(int column, View convertView, ViewGroup parent);

        View getCell(int row, int column, View convertView, ViewGroup parent);
    }

    public static class Builder {

        private View mCornerView;
        private DataGridAdapter mDataGridAdapter;

        private Builder() {}

        public Builder setCornerView(View cornerView) {
            mCornerView = cornerView;
            return this;
        }

        public Builder setDataGridAdapter(DataGridAdapter dataGridAdapter) {
            mDataGridAdapter = dataGridAdapter;
            return this;
        }

        public DataGridView build(Context context) {
            if (mDataGridAdapter == null) {
                throw new IllegalStateException("Data grid adapter must be set.");
            }

            View cornerView = mCornerView != null ? mCornerView : new View(context);
            return new DataGridView(context, mDataGridAdapter, cornerView);
        }
    }

    private static final String TAG = DataGridView.class.getName();
//
//    // set the header titles
//    String headers[] = {
//            "Header 1 \n multi-lines",
//            "Header 2",
//            "Header 3",
//            "Header 4",
//            "Header 5",
//            "Header 6",
//            "Header 7",
//            "Header 8",
//            "Header 9"
//    };

    private final DataGridAdapter mDataGridAdapter;

    /**
     * The view in the top-left corner of the table; can be empty.
     */
    private View mCornerView;
    private TableLayout mColumnHeadersLayout;
    private TableLayout mRowHeadersLayout;
    private TableLayout mDataLayout;

    private Linkage<LinkableHorizontalScrollView> mHorizontalScrollViewLinkage;
    private HorizontalScrollView mColumnHeadersHorizontalScrollView;
    private HorizontalScrollView mDataHorizontalScrollView;

    private Linkage<LinkableScrollView> mVerticalScrollViewLinkage;
    private ScrollView mRowHeadersScrollView;
    private ScrollView mDataScrollView;

    private Context mContext;

//    List<SampleObject> sampleObjects = sampleObjects();

//    int headerCellsWidth[] = new int[headers.length];

    @SuppressWarnings("ResourceType")
    public DataGridView(
            Context context,
            DataGridAdapter dataGridAdapter,
            View cornerView) {
        super(context);

        mContext = context;
        mDataGridAdapter = dataGridAdapter;
        mCornerView = cornerView;

        // Create all the main layout subcomponents.
        mColumnHeadersLayout = new TableLayout(mContext);
        mRowHeadersLayout = new TableLayout(mContext);
        mDataLayout = new TableLayout(mContext);

        mColumnHeadersHorizontalScrollView =
                new LinkableHorizontalScrollView(mContext, mHorizontalScrollViewLinkage);
        mDataHorizontalScrollView =
                new LinkableHorizontalScrollView(mContext, mHorizontalScrollViewLinkage);

        mRowHeadersScrollView = new LinkableScrollView(mContext, mVerticalScrollViewLinkage);
        mDataScrollView = new LinkableScrollView(mContext, mVerticalScrollViewLinkage);

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
        LayoutParams params =
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
//        params.setMargins(0, 2, 0, 0);

        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            TableRow row = new TableRow(mContext);
            View view = mDataGridAdapter.getRowHeader(i, null /*convertView*/, row);
            view.setLayoutParams(params);
            row.addView(view);

            mRowHeadersLayout.addView(row);
        }

        // Add the data cells.
        LayoutParams params2 =
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
//        params2.setMargins(2, 2, 0, 0);

        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            TableRow row = new TableRow(mContext);
            for (int j = 0; j < mDataGridAdapter.getColumnCount(); j++) {
                View view = mDataGridAdapter.getCell(i, j, null /*convertView*/, row);
                view.setLayoutParams(params);

                row.addView(view);
            }

            mDataLayout.addView(row);
        }

        // Measure the entire layout!
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

        // Find the widest column header...
        int maxWidth = 0;
        for (int i = 0; i < mDataGridAdapter.getColumnCount(); i++) {
            int width = ((TableRow) mColumnHeadersLayout.getChildAt(0)).getChildAt(i)
                    .getMeasuredWidth();
            if (width > maxWidth) {
                maxWidth = width;
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
        LayoutParams maxWidthLayoutParams = new LayoutParams(maxWidth, LayoutParams.MATCH_PARENT);
//        maxWidthLayoutParams.setMargins(2, 2, 0, 0);
        for (int i = 0; i < mDataGridAdapter.getColumnCount(); i++) {
            ((TableRow) mColumnHeadersLayout.getChildAt(0)).getChildAt(i)
                    .setLayoutParams(maxWidthLayoutParams);
        }

        // ... then all of the data cells too.
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            TableRow row = (TableRow) mDataLayout.getChildAt(i);
            for (int j = 0; j < mDataGridAdapter.getColumnCount(); j++) {
                row.getChildAt(j).setLayoutParams(maxWidthLayoutParams);
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
        LayoutParams maxHeightLayoutParams = new LayoutParams(maxWidth, maxHeight);
//        maxHeightLayoutParams.setMargins(2, 2, 0, 0);
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            ((TableRow) mRowHeadersLayout.getChildAt(i)).getChildAt(0)
                    .setLayoutParams(maxHeightLayoutParams);
        }

        // ... then all of the data cells too.
        for (int i = 0; i < mDataGridAdapter.getRowCount(); i++) {
            TableRow row = (TableRow) mDataLayout.getChildAt(i);
            for (int j = 0; j < mDataGridAdapter.getColumnCount(); j++) {
                row.getChildAt(j).setLayoutParams(maxHeightLayoutParams);
            }
        }

//        mRowHeadersLayout.addView(tableRowForTableC);
//        mDataLayout.addView(taleRowForTableD);

//        addTableRowToTableA();
//        addTableRowToTableB();

//        resizeHeaderHeight();
//
//        getTableRowHeaderCellWidth();

//        generateTableC_AndTable_B();

//        resizeBodyTableRowHeight();
    }
//
//    // this is just the sample data
//    List<SampleObject> sampleObjects(){
//
//        List<SampleObject> sampleObjects = new ArrayList<SampleObject>();
//
//        for(int x=1; x<=20; x++){
//
//            SampleObject sampleObject = new SampleObject(
//                    "Col 1, Row " + x,
//                    "Col 2, Row " + x + " - multi-lines",
//                    "Col 3, Row " + x,
//                    "Col 4, Row " + x,
//                    "Col 5, Row " + x,
//                    "Col 6, Row " + x,
//                    "Col 7, Row " + x,
//                    "Col 8, Row " + x,
//                    "Col 9, Row " + x
//            );
//
//            sampleObjects.add(sampleObject);
//        }
//
//        return sampleObjects;
//
//    }
//
//    // initalized components
//    private void initComponents(){
//        mColumnHeadersHorizontalScrollView.setBackgroundColor(Color.LTGRAY);
//    }
//
//    @SuppressWarnings("ResourceType")
//    private void setComponentsId(){
//    }
//
//    // set tags for some horizontal and vertical scroll view
//    private void setScrollViewAndHorizontalScrollViewTag(){
//
//        mColumnHeadersHorizontalScrollView.setTag("horizontal scroll view b");
//        mDataHorizontalScrollView.setTag("horizontal scroll view d");
//
//        mRowHeadersScrollView.setTag("scroll view c");
//        mDataScrollView.setTag("scroll view d");
//    }
//
//    private void addTableRowToTableA(){
//        cornerLayout.addView(componentATableRow());
//    }
//
//    private void addTableRowToTableB(){
//    }
//
//    // generate table row of table A
//    TableRow componentATableRow(){
//
//        TableRow componentATableRow = new TableRow(mContext);
//        TextView textView = headerTextView(headers[0]);
//        componentATableRow.addView(textView);
//
//        return componentATableRow;
//    }

    private TableRow createColumnHeadersView() {
        TableRow columnHeadersTableRow = new TableRow(mContext);

        LayoutParams params =
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
//        params.setMargins(2, 0, 0, 0);

        for (int i = 0; i < mDataGridAdapter.getColumnCount(); i++) {
            View view = mDataGridAdapter
                    .getColumnHeader(i, null /*convertView*/, columnHeadersTableRow);
            view.setLayoutParams(params);

            columnHeadersTableRow.addView(view);
        }

        return columnHeadersTableRow;
    }
//
//    // generate table row of table B
//    TableRow componentBTableRow(){
//
//        TableRow componentBTableRow = new TableRow(mContext);
//        int headerFieldCount = headers.length;
//
//        TableRow.LayoutParams params = new TableRow.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
//        params.setMargins(2, 0, 0, 0);
//
//        for(int x=0; x<(headerFieldCount-1); x++){
//            TextView textView = headerTextView(headers[x+1]);
//            textView.setLayoutParams(params);
//            componentBTableRow.addView(textView);
//        }
//
//        return componentBTableRow;
//    }
//
//    // generate table row of table C and table D
//    private void generateTableC_AndTable_B(){
//
//        // just seeing some header cell width
//        for(int x=0; x<headerCellsWidth.length; x++){
//            Log.v("TableMainLayout.java", headerCellsWidth[x]+"");
//        }
//
//        for(SampleObject sampleObject : sampleObjects){
//
//            TableRow tableRowForTableC = tableRowForTableC(sampleObject);
//            TableRow taleRowForTableD = taleRowForTableD(sampleObject);
//
//            tableRowForTableC.setBackgroundColor(Color.LTGRAY);
//            taleRowForTableD.setBackgroundColor(Color.LTGRAY);
//
//            mRowHeadersLayout.addView(tableRowForTableC);
//            mDataLayout.addView(taleRowForTableD);
//
//        }
//    }
//
//    // a TableRow for table C
//    TableRow tableRowForTableC(SampleObject sampleObject){
//
//        TableRow.LayoutParams params = new TableRow.LayoutParams( headerCellsWidth[0],LayoutParams.MATCH_PARENT);
//        params.setMargins(0, 2, 0, 0);
//
//        TableRow tableRowForTableC = new TableRow(mContext);
//        TextView textView = bodyTextView(sampleObject.header1);
//        tableRowForTableC.addView(textView,params);
//
//        return tableRowForTableC;
//    }
//
//    TableRow taleRowForTableD(SampleObject sampleObject){
//
//        TableRow taleRowForTableD = new TableRow(mContext);
//
//        int loopCount = ((TableRow)mColumnHeadersLayout.getChildAt(0)).getChildCount();
//        String info[] = {
//                sampleObject.header2,
//                sampleObject.header3,
//                sampleObject.header4,
//                sampleObject.header5,
//                sampleObject.header6,
//                sampleObject.header7,
//                sampleObject.header8,
//                sampleObject.header9
//        };
//
//        for(int x=0 ; x<loopCount; x++){
//            TableRow.LayoutParams params = new TableRow.LayoutParams( headerCellsWidth[x+1],LayoutParams.MATCH_PARENT);
//            params.setMargins(2, 2, 0, 0);
//
//            TextView textViewB = bodyTextView(info[x]);
//            taleRowForTableD.addView(textViewB,params);
//        }
//
//        return taleRowForTableD;
//
//    }
//
//    // table cell standard TextView
//    TextView bodyTextView(String label){
//
//        TextView bodyTextView = new TextView(mContext);
//        bodyTextView.setBackgroundColor(Color.WHITE);
//        bodyTextView.setText(label);
//        bodyTextView.setGravity(Gravity.CENTER);
//        bodyTextView.setPadding(5, 5, 5, 5);
//
//        return bodyTextView;
//    }
//
//    // header standard TextView
//    TextView headerTextView(String label){
//
//        TextView headerTextView = new TextView(mContext);
//        headerTextView.setBackgroundColor(Color.WHITE);
//        headerTextView.setText(label);
//        headerTextView.setGravity(Gravity.CENTER);
//        headerTextView.setPadding(5, 5, 5, 5);
//
//        return headerTextView;
//    }
//
//    // resizing TableRow height starts here
//    void resizeHeaderHeight() {
//
//        TableRow productNameHeaderTableRow = (TableRow) cornerLayout.getChildAt(0);
//        TableRow productInfoTableRow = (TableRow)  mColumnHeadersLayout.getChildAt(0);
//
//        int rowAHeight = viewHeight(productNameHeaderTableRow);
//        int rowBHeight = viewHeight(productInfoTableRow);
//
//        TableRow tableRow = rowAHeight < rowBHeight ? productNameHeaderTableRow : productInfoTableRow;
//        int finalHeight = rowAHeight > rowBHeight ? rowAHeight : rowBHeight;
//
//        matchLayoutHeight(tableRow, finalHeight);
//    }
//
//    void getTableRowHeaderCellWidth(){
//
//        int tableAChildCount = ((TableRow)cornerLayout.getChildAt(0)).getChildCount();
//        int tableBChildCount = ((TableRow)mColumnHeadersLayout.getChildAt(0)).getChildCount();;
//
//        for(int x=0; x<(tableAChildCount+tableBChildCount); x++){
//
//            if(x==0){
//                headerCellsWidth[x] = viewWidth(((TableRow) cornerLayout.getChildAt(0)).getChildAt(x));
//            }else{
//                headerCellsWidth[x] = viewWidth(((TableRow) mColumnHeadersLayout.getChildAt(0)).getChildAt(x - 1));
//            }
//
//        }
//    }
//
//    // resize body table row height
//    void resizeBodyTableRowHeight(){
//
//        int tableC_ChildCount = mRowHeadersLayout.getChildCount();
//
//        for(int x=0; x<tableC_ChildCount; x++){
//
//            TableRow productNameHeaderTableRow = (TableRow) mRowHeadersLayout.getChildAt(x);
//            TableRow productInfoTableRow = (TableRow)  mDataLayout.getChildAt(x);
//
//            int rowAHeight = viewHeight(productNameHeaderTableRow);
//            int rowBHeight = viewHeight(productInfoTableRow);
//
//            TableRow tableRow = rowAHeight < rowBHeight ? productNameHeaderTableRow : productInfoTableRow;
//            int finalHeight = rowAHeight > rowBHeight ? rowAHeight : rowBHeight;
//
//            matchLayoutHeight(tableRow, finalHeight);
//        }
//
//    }
//
//    // match all height in a table row
//    // to make a standard TableRow height
//    private void matchLayoutHeight(TableRow tableRow, int height) {
//
//        int tableRowChildCount = tableRow.getChildCount();
//
//        // if a TableRow has only 1 child
//        if(tableRow.getChildCount()==1){
//
//            View view = tableRow.getChildAt(0);
//            TableRow.LayoutParams params = (TableRow.LayoutParams) view.getLayoutParams();
//            params.height = height - (params.bottomMargin + params.topMargin);
//
//            return ;
//        }
//
//        // if a TableRow has more than 1 child
//        for (int x = 0; x < tableRowChildCount; x++) {
//
//            View view = tableRow.getChildAt(x);
//
//            TableRow.LayoutParams params = (TableRow.LayoutParams) view.getLayoutParams();
//
//            if (!isTheHeighestLayout(tableRow, x)) {
//                params.height = height - (params.bottomMargin + params.topMargin);
//                return;
//            }
//        }
//
//    }
//
//    // check if the view has the highest height in a TableRow
//    private boolean isTheHeighestLayout(TableRow tableRow, int layoutPosition) {
//
//        int tableRowChildCount = tableRow.getChildCount();
//        int heighestViewPosition = -1;
//        int viewHeight = 0;
//
//        for (int x = 0; x < tableRowChildCount; x++) {
//            View view = tableRow.getChildAt(x);
//            int height = viewHeight(view);
//
//            if (viewHeight < height) {
//                heighestViewPosition = x;
//                viewHeight = height;
//            }
//        }
//
//        return heighestViewPosition == layoutPosition;
//    }
//
//    // read a view's height
//    private int viewHeight(View view) {
//        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
//        return view.getMeasuredHeight();
//    }
//
//    // read a view's width
//    private int viewWidth(View view) {
//        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
//        return view.getMeasuredWidth();
//    }

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

                view.scrollTo(l, 0);
            }
        }
    }
//
//    public static class SampleObject {
//
//        String header1;
//        String header2;
//        String header3;
//        String header4;
//        String header5;
//        String header6;
//        String header7;
//        String header8;
//        String header9;
//
//        public SampleObject(String header1, String header2, String header3,
//                            String header4, String header5, String header6,
//                            String header7, String header8, String header9){
//
//            header1 = header1;
//            header2 = header2;
//            header3 = header3;
//            header4 = header4;
//            header5 = header5;
//            header6 = header6;
//            header7 = header7;
//            header8 = header8;
//            header9 = header9;
//
//        }
//    }
}
