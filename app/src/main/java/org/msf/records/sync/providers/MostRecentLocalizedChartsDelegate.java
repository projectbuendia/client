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

import org.msf.records.sync.Database;

import java.util.List;

/** A {@link ProviderDelegate} that provides query access to all localized locations. */
public class MostRecentLocalizedChartsDelegate implements ProviderDelegate<Database> {

    @Override
    public String getType() {
        return Contracts.LocalizedCharts.GROUP_CONTENT_TYPE;
    }

    @Override
    public Cursor query(
            Database dbHelper, ContentResolver contentResolver, Uri uri, String[] projection,
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
        String query = ""
                + " select obs._id,"
                + "     obs.encounter_time,"
                + "     obs.concept_uuid,"
                + "     names.localized_name as concept_name,"
                // Localized value for concept values
                + "     obs.value,"
                + "     coalesce(value_names.localized_name, obs.value) as localized_value"
                + " from observations obs"

                + " inner join ("
                + "     select concept_uuid,"
                + "         max(encounter_time) as maxtime"
                + "     from observations"
                + "     where patient_uuid = ?" // 1st selection arg
                + "     group by concept_uuid"
                + " ) maxs"
                + " on obs.encounter_time = maxs.maxtime and"
                + "     obs.concept_uuid = maxs.concept_uuid"

                + " inner join concept_names names"
                + " on obs.concept_uuid = names.concept_uuid"

                // Some of the results are CODED so value is a concept UUID
                // Some are numeric so the value is fine.
                // To cope we will do a left join on the value and the name
                + " left join concept_names value_names"
                + " on obs.value = value_names.concept_uuid"
                + "     and value_names.locale = ?" // 2nd selection arg

                + " where obs.patient_uuid = ? and " // 3rd sel. arg
                + "     names.locale = ? " // 4th selection arg

                + " order by obs.concept_uuid, obs._id";

        return dbHelper.getReadableDatabase()
                .rawQuery(query, new String[]{patientUuid, locale, patientUuid, locale});
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
