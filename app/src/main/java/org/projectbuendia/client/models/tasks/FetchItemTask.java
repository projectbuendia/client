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

package org.projectbuendia.client.models.tasks;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemFetchFailedEvent;
import org.projectbuendia.client.events.data.ItemFetchedEvent;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.models.Base;
import org.projectbuendia.client.models.converters.Converter;

/**
 * An {@link AsyncTask} that fetches a single item from the data store.
 * <p/>
 * <p>If the operation succeeds, a {@link ItemFetchedEvent} is posted on the given
 * {@link CrudEventBus} with the retrieved item. If the operation fails, a
 * {@link ItemFetchFailedEvent} is posted instead.
 */
public class FetchItemTask<T extends Base> extends AsyncTask<Void, Void, Object> {
    private final ContentResolver mContentResolver;
    private final Uri mContentUri;
    private final String[] mProjectionColumns;
    private final SimpleSelectionFilter<T> mFilter;
    private final String mConstraint;
    private final Converter<T> mConverter;
    private final CrudEventBus mBus;

    FetchItemTask(
        ContentResolver contentResolver,
        Uri contentUri,
        String[] projectionColumns,
        SimpleSelectionFilter filter,
        String constraint,
        Converter<T> converter,
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
                return new ItemFetchFailedEvent("no results");
            }

            return new ItemFetchedEvent<>(mConverter.fromCursor(cursor));
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
