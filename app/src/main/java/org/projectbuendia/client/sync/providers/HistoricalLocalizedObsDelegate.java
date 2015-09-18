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

/** A {@link ProviderDelegate} that provides access to all historical observations for a patient. */
public class HistoricalLocalizedObsDelegate implements ProviderDelegate<Database> {

    @Override public String getType() {
        return Contracts.HistoricalLocalizedObs.GROUP_CONTENT_TYPE;
    }

    @Override public Cursor query(
        Database dbHelper, ContentResolver contentResolver, Uri uri, String[] projection,
        String selection, String[] selectionArgs, String sortOrder) {
        // Decode the uri, expected:
        // content://org.projectbuendia.client/localizedchart/{chart_uuid}/{locale}/{patient_uuid}
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 4) {
            throw new UnsupportedOperationException("Unknown URI " + uri);
        }
        String patientUuid = pathSegments.get(pathSegments.size() - 1);
        String locale = pathSegments.get(pathSegments.size() - 2);
        @SuppressWarnings("UnusedAssignment") // May be used in the future.
            String chartUuid = pathSegments.get(pathSegments.size() - 3);

        // TODO/cleanup: Instead of doing this crazy complicated SQL query with all these table
        // joins, load the concept names into a small map in memory (there will probably only be
        // 100 entries or less) in a separate query; then eliminate this class, query directly
        // on the obs table, and look up concept names only as they're needed for display.

        // This scary SQL statement joins the observations with appropriate concept names to give
        // localized output in the correct order specified by a chart.
        String query = ""
            + " SELECT obs._id,"
            + "     obs.encounter_time,"
            + "     chart.concept_uuids as concept_uuid,"
            + "     names.name AS concept_name,"
            + "     concepts.concept_type,"
            // Localized value for concept values
            + "     obs.value,"
            + "     COALESCE(value_names.name, obs.value) AS localized_value"

            + " FROM chart_items AS chart"
            + "     INNER JOIN concept_names names"
            + "     ON chart.concept_uuids = names.concept_uuid"
            + "     INNER JOIN concepts"
            + "     ON chart.concept_uuids = concepts._id"
            + "     LEFT JOIN observations obs"
            + "     ON chart.concept_uuids = obs.concept_uuid AND "
            + "         (obs.patient_uuid = ? OR" // 1st selection arg
            + "          obs.patient_uuid is null)"

            // Some of the results are CODED so value is a concept UUID
            // Some are numeric so the value is fine.
            // To cope we will do a LEFT JOIN on the value AND the name
            + "     LEFT JOIN concept_names value_names"
            + "     ON obs.value = value_names.concept_uuid"
            + "         AND value_names.locale = ?" // 2nd selection arg

            + " WHERE names.locale = ?" // 3rd selection arg

            + " ORDER BY chart.weight, obs.encounter_time, obs._id";
        return dbHelper.getReadableDatabase()
            .rawQuery(query, new String[] {patientUuid, locale, locale});
    }

    @Override public Uri insert(
        Database dbHelper, ContentResolver contentResolver, Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Insert is not supported for URI '" + uri + "'.");
    }

    @Override public int bulkInsert(
        Database dbHelper, ContentResolver contentResolver, Uri uri, ContentValues[] values) {
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
