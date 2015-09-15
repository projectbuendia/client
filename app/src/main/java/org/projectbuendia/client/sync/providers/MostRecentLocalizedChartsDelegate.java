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

import java.util.List;

/** A {@link ProviderDelegate} that provides query access to all localized locations. */
public class MostRecentLocalizedChartsDelegate implements ProviderDelegate<Database> {

    @Override public String getType() {
        return Contracts.LocalizedCharts.GROUP_CONTENT_TYPE;
    }

    @Override public Cursor query(
        Database dbHelper, ContentResolver contentResolver, Uri uri, String[] projection,
        String selection, String[] selectionArgs, String sortOrder) {
        // Decode the uri, expected:
        // content://org.projectbuendia.client/mostrecent/{patient_uuid}/{locale}
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 3) {
            throw new UnsupportedOperationException("Unknown URI " + uri);
        }
        String locale = pathSegments.get(pathSegments.size() - 1);
        String patientUuid = pathSegments.get(pathSegments.size() - 2);

        // This scary SQL statement joins the observations a subselect for the latest for each
        // concept with appropriate concept names to give localized output.
        String query = ""
            + " SELECT obs._id,"
            + "     obs.encounter_time,"
            + "     obs.concept_uuid,"
            + "     names.name AS concept_name,"
            + "     concepts.concept_type,"
            // Localized value for concept values
            + "     obs.value,"
            + "     COALESCE(value_names.name, obs.value) AS localized_value"
            + " FROM observations AS obs"

            + " INNER JOIN ("
            + "     SELECT concept_uuid,"
            + "         max(encounter_time) AS maxtime"
            + "     FROM observations"
            + "     WHERE patient_uuid = ?" // 1st selection arg
            + "     GROUP BY concept_uuid"
            + " ) maxs"
            + " ON obs.encounter_time = maxs.maxtime AND"
            + "     obs.concept_uuid = maxs.concept_uuid"

            + " INNER JOIN concept_names names"
            + " ON obs.concept_uuid = names.concept_uuid"

            + " INNER JOIN concepts"
            + " ON obs.concept_uuid = concepts._id"

            // Some of the results are CODED so value is a concept UUID
            // Some are numeric so the value is fine.
            // To cope we will do a LEFT JOIN ON the value AND the name
            + " LEFT JOIN concept_names value_names"
            + " ON obs.value = value_names.concept_uuid"
            + "     AND value_names.locale = ?" // 2nd selection arg

            + " WHERE obs.patient_uuid = ? AND " // 3rd sel. arg
            + "     names.locale = ? " // 4th selection arg

            + " ORDER BY obs.concept_uuid, obs._id";

        return dbHelper.getReadableDatabase()
            .rawQuery(query, new String[] {patientUuid, locale, patientUuid, locale});
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
