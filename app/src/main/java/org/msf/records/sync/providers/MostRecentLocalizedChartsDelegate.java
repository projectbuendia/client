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
public class MostRecentLocalizedChartsDelegate implements ProviderDelegate<PatientDatabase> {

    @Override
    public String getType() {
        return Contracts.LocalizedCharts.GROUP_CONTENT_TYPE;
    }

    @Override
    public Cursor query(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        // Decode the uri, expected:
        // content://org.msf.records/mostrecent/{patient_uuid}/{locale}
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 3) {
            throw new UnsupportedOperationException("Unknown URI " + uri);
        }
        String locale = pathSegments.get(pathSegments.size() - 1);
        String patientUuid = pathSegments.get(pathSegments.size() - 2);

        // This scary SQL statement joins the observations a subselect for the latest for each
        // concept with appropriate concept names to give localized output.
        String query = "SELECT obs." + Contracts.Observations._ID
                + ", obs.encounter_time,"
                + "obs.concept_uuid,names."
                + Contracts.ConceptNames.LOCALIZED_NAME + " AS concept_name,"
                // Localized value for concept values
                + "obs." + Contracts.Observations.VALUE
                + ",coalesce(value_names." + Contracts.ConceptNames.LOCALIZED_NAME
                + ", obs." + Contracts.Observations.VALUE + ") "
                + "AS localized_value"

                + " FROM "
                + "observations" + " obs "

                + " INNER JOIN "

                + "(SELECT " + Contracts.Charts.CONCEPT_UUID
                + ", MAX(" + Contracts.Observations.ENCOUNTER_TIME + ") AS maxtime "
                + "FROM " + "observations"
                + " WHERE " + Contracts.Observations.PATIENT_UUID + "=? " // 1st selection arg
                + "GROUP BY " + Contracts.Observations.CONCEPT_UUID + ") maxs "

                + "ON obs." + Contracts.Observations.ENCOUNTER_TIME + " = maxs.maxtime AND "
                + "obs." + Contracts.Observations.CONCEPT_UUID
                + "=maxs." + Contracts.Observations.CONCEPT_UUID

                + " INNER JOIN "
                + "concept_names" + " names "
                + "ON obs." + Contracts.Observations.CONCEPT_UUID + "="
                + "names." + Contracts.ConceptNames.CONCEPT_UUID

                // Some of the results are CODED so value is a concept UUID
                // Some are numeric so the value is fine.
                // To cope we will do a left join on the value and the name
                + " LEFT JOIN " + "concept_names" + " value_names "
                + "ON obs." + Contracts.Observations.VALUE + "="
                + "value_names." + Contracts.Charts.CONCEPT_UUID
                + " AND value_names." + Contracts.ConceptNames.LOCALE + "=?" // 2nd selection arg

                + " WHERE obs." + Contracts.Observations.PATIENT_UUID + "=? AND " // 3rd sel. arg
                + "names." + Contracts.ConceptNames.LOCALE + "=? " // 4th selection arg

                + " ORDER BY obs." + Contracts.Charts.CONCEPT_UUID
                + ", obs." + Contracts.Observations._ID;

        return dbHelper.getReadableDatabase()
                .rawQuery(query, new String[]{patientUuid, locale, patientUuid, locale});
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
