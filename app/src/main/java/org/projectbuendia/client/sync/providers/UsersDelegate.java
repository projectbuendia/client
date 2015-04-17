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

package org.projectbuendia.client.sync.providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.projectbuendia.client.sync.Database;
import org.projectbuendia.client.sync.QueryBuilder;

/** A {@link ProviderDelegate} that provides read-write access to users. */
class UsersDelegate implements ProviderDelegate<Database> {

    public static final String NAME = "users";

    public static final String TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + NAME;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Cursor query(
            Database dbHelper, ContentResolver contentResolver, Uri uri,
            String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = new QueryBuilder(Database.USERS_TABLE)
                .where(selection, selectionArgs)
                .orderBy(sortOrder)
                .select(dbHelper.getReadableDatabase(), projection);
        cursor.setNotificationUri(contentResolver, uri);
        return cursor;
    }

    @Override
    public Uri insert(
            Database dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues values) {
        long id = dbHelper.getWritableDatabase()
                .replaceOrThrow(Database.USERS_TABLE, null, values);
        contentResolver.notifyChange(uri, null, false);
        return uri.buildUpon().appendPath(Long.toString(id)).build();
    }

    @Override
    public int bulkInsert(
            Database dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues[] values) {
        // TODO: optimise this.
        for (ContentValues value : values) {
            insert(dbHelper, contentResolver, uri, value);
        }
        return values.length;
    }

    @Override
    public int delete(
            Database dbHelper, ContentResolver contentResolver, Uri uri,
            String selection, String[] selectionArgs) {
        int count = new QueryBuilder(Database.USERS_TABLE)
                .where(selection, selectionArgs)
                .delete(dbHelper.getWritableDatabase());
        contentResolver.notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(
            Database dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues values, String selection, String[] selectionArgs) {
        int count = new QueryBuilder(Database.USERS_TABLE)
                .where(selection, selectionArgs)
                .update(dbHelper.getWritableDatabase(), values);
        contentResolver.notifyChange(uri, null, false);
        return count;
    }
}
