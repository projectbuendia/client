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

package org.projectbuendia.client.providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.projectbuendia.client.sync.Database;

import java.util.List;

/**
 * A {@link ItemProviderDelegate} that supports insertion. Insertion operations will be treated as
 * upserts -- e.g. fields not explicitly overwritten from older entries will be retained.
 */
public class InsertableItemProviderDelegate extends ItemProviderDelegate {

    public InsertableItemProviderDelegate(String name, Contracts.Table table, String idColumn) {
        super(name, table, idColumn);
    }

    @Override public Uri insert(
        Database dbHelper, ContentResolver contentResolver, Uri uri,
        ContentValues values) {
        // Perform an upsert operation, updating only the columns specified in values.
        int done = 0;
        if (mIdColumn != null) {
            values.put(mIdColumn, uri.getLastPathSegment());
            done = dbHelper.getWritableDatabase().updateWithOnConflict(
                mTable.name, values, mIdColumn + "= ?", new String[] {uri.getLastPathSegment()},
                SQLiteDatabase.CONFLICT_IGNORE);
        }
        if (done == 0) {
            dbHelper.getWritableDatabase().insertWithOnConflict(
                mTable.name, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
        contentResolver.notifyChange(uri, null, false);
        return getPrefixUriBuilder(uri).appendPath(uri.getLastPathSegment()).build();
    }

    private static Uri.Builder getPrefixUriBuilder(Uri uri) {
        Uri.Builder prefixBuilder = uri.buildUpon()
            .path("");
        List<String> pathSegments = uri.getPathSegments();
        for (int i = 0; i < pathSegments.size() - 1; i++) {
            prefixBuilder.appendPath(pathSegments.get(i));
        }

        return prefixBuilder;
    }
}
