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

/**
 * A {@link ProviderDelegate} that provides query access to the count of patients in each location.
 */
public class PatientCountsDelegate implements ProviderDelegate<Database> {

    @Override
    public String getType() {
        return Contracts.PatientCounts.GROUP_CONTENT_TYPE;
    }

    @Override
    public Cursor query(
            Database dbHelper, ContentResolver contentResolver, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        return new QueryBuilder(Database.PATIENTS_TABLE)
                .where(selection, selectionArgs)
                .where(Contracts.Patients.LOCATION_UUID + " is not null")
                .groupBy(Contracts.Patients.LOCATION_UUID)
                .orderBy(sortOrder)
                .select(dbHelper.getReadableDatabase(),
                        Contracts.Patients._ID,
                        Contracts.Patients.LOCATION_UUID,
                        "count(*) as " + Contracts.Patients._COUNT);
    }

    @Override
    public Uri insert(
            Database dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues values) {
        throw new UnsupportedOperationException("Insert is not supported for URI '" + uri + "'.");
    }

    @Override
    public int bulkInsert(
            Database dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues[] values) {
        throw new UnsupportedOperationException(
                "Bulk insert is not supported for URI '" + uri + "'.");
    }

    @Override
    public int delete(
            Database dbHelper, ContentResolver contentResolver, Uri uri, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException("Delete is not supported for URI '" + uri + "'.");
    }

    @Override
    public int update(
            Database dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update is not supported for URI '" + uri + "'.");
    }
}
