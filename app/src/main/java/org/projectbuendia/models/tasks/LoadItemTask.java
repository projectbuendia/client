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

package org.projectbuendia.models.tasks;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemLoadFailedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.models.Model;
import org.projectbuendia.models.CursorLoader;

/**
 * An {@link AsyncTask} that loads a single item from the data store.
 * <p/>
 * <p>If the operation succeeds, a {@link ItemLoadedEvent} is posted on the given
 * {@link CrudEventBus} with the retrieved item. If the operation fails, a
 * {@link ItemLoadFailedEvent} is posted instead.
 */
public class LoadItemTask<T extends Model> extends AsyncTask<Void, Void, Object> {
    private final ContentResolver mContentResolver;
    private final Uri mContentUri;
    private final String[] mProjectionColumns;
    private final SimpleSelectionFilter<T> mFilter;
    private final String mConstraint;
    private final CursorLoader<T> mLoader;
    private final CrudEventBus mBus;

    LoadItemTask(
        ContentResolver contentResolver,
        Uri contentUri,
        String[] projectionColumns,
        SimpleSelectionFilter filter,
        String constraint,
        CursorLoader<T> loader,
        CrudEventBus bus) {
        mContentResolver = contentResolver;
        mContentUri = contentUri;
        mProjectionColumns = projectionColumns;
        mFilter = filter;
        mConstraint = constraint;
        mLoader = loader;
        mBus = bus;
    }

    @Override protected Object doInBackground(Void... params) {
        try (Cursor cursor = mContentResolver.query(
            mContentUri, mProjectionColumns,
            mFilter.getSelectionString(), mFilter.getSelectionArgs(mConstraint), null
        )) {
            if (cursor == null || !cursor.moveToFirst()) {
                return new ItemLoadFailedEvent("no results");
            }
            return new ItemLoadedEvent<>(mLoader.load(cursor));
        }
    }

    @Override protected void onPostExecute(Object result) {
        mBus.post(result);
    }
}
