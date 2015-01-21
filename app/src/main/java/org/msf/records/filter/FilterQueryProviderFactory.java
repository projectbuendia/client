package org.msf.records.filter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.widget.FilterQueryProvider;

import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.providers.Contracts;

/**
 * FilterQueryProviderFactory constructs a FilterQueryProvider based on a specified filter.
 */
public class FilterQueryProviderFactory {
    /**
     * Projection for querying the content provider.
     */
    private static final String[] PROJECTION = PatientProjection.getProjectionColumns();

    private Uri mUri = Contracts.Patients.CONTENT_URI;
    private String[] mProjection = PROJECTION;
    private String mSortClause =
            Contracts.Patients.ADMISSION_TIMESTAMP + " desc";

    private final Context mContext;

    public FilterQueryProviderFactory(Context context) {
    	mContext = context.getApplicationContext();
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

    public CursorLoader getCursorLoader(final SimpleSelectionFilter filter, CharSequence constraint) {
        return new CursorLoader(
        		mContext,
                mUri,
                mProjection,
                filter.getSelectionString(),
                filter.getSelectionArgs(constraint),
                mSortClause);
    }

    public FilterQueryProvider getFilterQueryProvider(final SimpleSelectionFilter filter) {
        return new FilterQueryProvider() {

            @Override
            public Cursor runQuery(CharSequence constraint) {
                return getCursorLoader(filter, constraint).loadInBackground();
            }
        };
    }
}
