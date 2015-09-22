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

import org.projectbuendia.client.sync.Database;

import java.util.List;

/** A {@link ProviderDelegate} that provides query access to all localized locations. */
public class LocalizedLocationsDelegate implements ProviderDelegate<Database> {

    /**
     * Query that fetches localized location information for a given locale.
     * <p/>
     * <p>Parameters:
     * <ul>
     * <li>string, the locale in which the location information should be returned</li>
     * </ul>
     * <p/>
     * <p>Result Columns:
     * <ul>
     * <li>string location_uuid, the UUID of a location</li>
     * <li>string parent_uuid, the UUID of the location's parent</li>
     * <li>string name, the localized name of the location</li>
     * </ul>
     */
    private static final String QUERY = ""
        + " SELECT"
        + "     locations.uuid AS uuid,"
        + "     locations.parent_uuid AS parent_uuid,"
        + "     location_names.name AS name,"
        + "     COUNT(patients.location_uuid) AS patient_count"
        + " FROM locations"
        + "     INNER JOIN location_names"
        + "     ON locations.uuid = location_names.location_uuid"
        + "     LEFT JOIN patients"
        + "     ON locations.uuid = patients.location_uuid"
        + " WHERE location_names.locale = ?"
        + " GROUP BY locations.uuid";

    @Override public String getType() {
        return Contracts.LocalizedLocations.GROUP_CONTENT_TYPE;
    }

    @Override public Cursor query(
        Database dbHelper, ContentResolver contentResolver, Uri uri, String[] projection,
        String selection, String[] selectionArgs, String sortOrder) {
        // URI expected to be of form ../localized-locations/{locale}.
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 2) {
            throw new UnsupportedOperationException("URI '" + uri + "' is malformed.");
        }

        String locale = pathSegments.get(1);
        return dbHelper.getReadableDatabase().rawQuery(QUERY, new String[] {locale});
    }

    @Override public Uri insert(
        Database dbHelper, ContentResolver contentResolver, Uri uri,
        ContentValues values) {
        throw new UnsupportedOperationException("Insert is not supported for URI '" + uri + "'.");
    }

    @Override public int bulkInsert(
        Database dbHelper, ContentResolver contentResolver, Uri uri,
        ContentValues[] values) {
        throw new UnsupportedOperationException(
            "Bulk insert is not supported for URI '" + uri + "'.");
    }

    @Override public int delete(
        Database dbHelper, ContentResolver contentResolver, Uri uri, String selection,
        String[] selectionArgs) {
        throw new UnsupportedOperationException("Delete is not supported for URI '" + uri + "'.");
    }

    @Override public int update(
        Database dbHelper, ContentResolver contentResolver, Uri uri,
        ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update is not supported for URI '" + uri + "'.");
    }
}
