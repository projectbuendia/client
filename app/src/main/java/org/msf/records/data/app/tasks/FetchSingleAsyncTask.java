package org.msf.records.data.app.tasks;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import org.msf.records.data.app.AppTypeBase;
import org.msf.records.data.app.converters.AppTypeConverter;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.SingleItemFetchFailedEvent;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.providers.Contracts;

/**
 * An {@link AsyncTask} that fetches a single item from the data store.
 */
public class FetchSingleAsyncTask<T extends AppTypeBase>
        extends AsyncTask<Void, Void, Object> {

    private final ContentResolver mContentResolver;
    private final Uri mContentUri;
    private final String[] mProjectionColumns;
    private final SimpleSelectionFilter<T> mFilter;
    private final String mConstraint;
    private final AppTypeConverter<T> mConverter;
    private final CrudEventBus mBus;

    FetchSingleAsyncTask(
            ContentResolver contentResolver,
            Uri contentUri,
            String[] projectionColumns,
            SimpleSelectionFilter filter,
            String constraint,
            AppTypeConverter<T> converter,
            CrudEventBus bus) {
        mContentResolver = contentResolver;
        mContentUri = contentUri;
        mProjectionColumns = projectionColumns;
        mFilter = filter;
        mConstraint = constraint;
        mConverter = converter;
        mBus = bus;
    }

    @Override
    protected Object doInBackground(Void... params) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(
                    mContentUri,
                    mProjectionColumns,
                    mFilter.getSelectionString(),
                    mFilter.getSelectionArgs(mConstraint),
                    null);

            if (cursor == null || !cursor.moveToFirst()) {
                return new SingleItemFetchFailedEvent("empty response from provider");
            }

            return new SingleItemFetchedEvent<>(mConverter.fromCursor(cursor));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void onPostExecute(Object result) {
        mBus.post(result);
    }
}