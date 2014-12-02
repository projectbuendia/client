package org.msf.records.filter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.widget.FilterQueryProvider;

import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.PatientProviderContract;

/**
 * FilterQueryProviderFactory constructs a FilterQueryProvider based on a specified filter.
 */
public class FilterQueryProviderFactory {
    /**
     * Projection for querying the content provider.
     */
    private static final String[] PROJECTION = PatientProjection.getProjectionColumns();

    private Uri mUri = PatientProviderContract.CONTENT_URI;
    private String[] mProjection = PROJECTION;
    private String mSortClause =
            PatientProviderContract.PatientColumns.COLUMN_NAME_ADMISSION_TIMESTAMP + " desc";

    public FilterQueryProviderFactory() {
        // Intentionally blank.
    }

    public FilterQueryProviderFactory setUri(Uri uri) {
        mUri = uri;
        return this;
    }

    public FilterQueryProviderFactory setProjection(String[] projection) {
        mProjection = projection;
        return this;
    }

    public FilterQueryProviderFactory setSortClause(String sortClause) {
        mSortClause = sortClause;
        return this;
    }

    public CursorLoader getCursorLoader(
            final Context context, final SimpleSelectionFilter filter, CharSequence constraint) {
        return new CursorLoader(
                context,
                mUri,
                mProjection,
                filter.getSelectionString(),
                filter.getSelectionArgs(constraint),
                mSortClause);
    }

    public FilterQueryProvider getFilterQueryProvider(
            final Context context, final SimpleSelectionFilter filter) {
        return new FilterQueryProvider() {

            @Override
            public Cursor runQuery(CharSequence constraint) {
                return getCursorLoader(context, filter, constraint).loadInBackground();
            }
        };
    }
}
