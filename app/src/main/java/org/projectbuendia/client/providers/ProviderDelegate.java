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
import android.database.Cursor;
import android.net.Uri;

import net.sqlcipher.database.SQLiteOpenHelper;

/** A delegate used to handle a single URI for {@link DelegatingProvider}. */
public interface ProviderDelegate<T extends SQLiteOpenHelper> {

    static final String TYPE_PACKAGE_PREFIX = "/vnd.projectbuendia.client.";

    /** Returns the MIME type this delegate provides. */
    String getType();

    Cursor query(
        T dbHelper, ContentResolver contentResolver, Uri uri, String[] projection,
        String selection, String[] selectionArgs, String sortOrder);

    Uri insert(T dbHelper, ContentResolver contentResolver, Uri uri, ContentValues values);

    int bulkInsert(T dbHelper, ContentResolver contentResolver, Uri uri, ContentValues[] values);

    int delete(
        T dbHelper, ContentResolver contentResolver, Uri uri, String selection,
        String[] selectionArgs);

    int update(
        T dbHelper, ContentResolver contentResolver, Uri uri, ContentValues values,
        String selection, String[] selectionArgs);
}
