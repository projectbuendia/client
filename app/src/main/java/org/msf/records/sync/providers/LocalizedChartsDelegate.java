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

package org.msf.records.sync.providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.msf.records.sync.PatientDatabase;

import java.util.List;

/** A {@link ProviderDelegate} that provides query access to all localized locations. */
public class LocalizedChartsDelegate implements ProviderDelegate<PatientDatabase> {

    @Override
    public String getType() {
        return Contracts.LocalizedCharts.GROUP_CONTENT_TYPE;
    }

    @Override
    public Cursor query(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        // Decode the uri, expected:
        // content://org.msf.records/localizedchart/{chart_uuid}/{locale}/{patient_uuid}
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 4) {
            throw new UnsupportedOperationException("Unknown URI " + uri);
        }
        String patientUuid = pathSegments.get(pathSegments.size() - 1);
        String locale = pathSegments.get(pathSegments.size() - 2);
        @SuppressWarnings("UnusedAssignment") // May be used in the future.
        String chartUuid = pathSegments.get(pathSegments.size() - 3);

        // This scary SQL statement joins the observations with appropriate concept names to give
        // localized output in the correct order specified by a chart.
        String query = "SELECT obs." + Contracts.Observations._ID
                + ",obs.encounter_time,"
                + "group_names." + Contracts.ConceptNames.LOCALIZED_NAME + " AS group_name,"
                + "chart." + Contracts.Observations.CONCEPT_UUID + ","
                + "names." + Contracts.ConceptNames.LOCALIZED_NAME + " AS concept_name,"
                // Localized value for concept values
                + "obs." + Contracts.Observations.VALUE
                + ",coalesce(value_names." + Contracts.ConceptNames.LOCALIZED_NAME + ", obs."
                + Contracts.Observations.VALUE + ") "
                + "AS localized_value"

                + " FROM "
                + PatientDatabase.CHARTS_TABLE_NAME + " chart "

                + " INNER JOIN " + PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " names "
                + "ON chart." + Contracts.Charts.CONCEPT_UUID + "="
                + "names." + Contracts.Charts.CONCEPT_UUID

                + " INNER JOIN "
                + PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " group_names "
                + "ON chart." + Contracts.Charts.GROUP_UUID + "="
                + "group_names." + Contracts.Charts.CONCEPT_UUID

                + " LEFT JOIN "
                + PatientDatabase.OBSERVATIONS_TABLE_NAME + " obs "
                + "ON chart." + Contracts.Charts.CONCEPT_UUID + "="
                + "obs." + Contracts.Observations.CONCEPT_UUID + " AND "
                + "(obs." + Contracts.Observations.PATIENT_UUID + "=? OR " // 2nd selection arg
                + "obs." + Contracts.Observations.PATIENT_UUID + " IS NULL)"

                // Some of the results are CODED so value is a concept UUID
                // Some are numeric so the value is fine.
                // To cope we will do a left join on the value and the name
                + " LEFT JOIN " + PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " value_names "
                + "ON obs." + Contracts.Observations.VALUE + "= "
                + "value_names." + Contracts.Charts.CONCEPT_UUID
                + " AND value_names." + Contracts.ConceptNames.LOCALE + "=?" // 1st selection arg

//              + " WHERE chart." + ChartColumns.CHART_UUID + "=? AND "
                + " WHERE "
                + "names." + Contracts.ConceptNames.LOCALE + "=? AND " // 3rd selection arg
                + "group_names." + Contracts.ConceptNames.LOCALE + "=?" // 4th selection arg

                + " ORDER BY chart." + Contracts.Charts.CHART_ROW
                + ", obs." + Contracts.Observations.ENCOUNTER_TIME
                + ", obs." + Contracts.Observations._ID
                ;

        return dbHelper.getReadableDatabase()
                .rawQuery(query, new String[]{patientUuid, locale, locale, locale});
    }

    @Override
    public Uri insert(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues values) {
        throw new UnsupportedOperationException("Insert is not supported for URI '" + uri + "'.");
    }

    @Override
    public int bulkInsert(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues[] values) {
        throw new UnsupportedOperationException(
                "Bulk insert is not supported for URI '" + uri + "'.");
    }

    @Override
    public int delete(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException("Delete is not supported for URI '" + uri + "'.");
    }

    @Override
    public int update(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update is not supported for URI '" + uri + "'.");
    }
}
