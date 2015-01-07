package org.msf.records.sync.providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.msf.records.sync.ChartProviderContract;
import org.msf.records.sync.PatientDatabase;

import java.util.List;

/**
 * A {@link ProviderDelegate} that provides query access to all localized locations.
 */
public class MostRecentLocalizedChartsDelegate implements ProviderDelegate<PatientDatabase> {

    public static final String NAME = "most-recent-localized-chart";

    public static final String TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + NAME;

    @Override
    public String getType() {
        return TYPE;
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
        String query = "SELECT obs.encounter_time," +
                "obs.concept_uuid,names." + ChartProviderContract.ChartColumns.NAME + " AS concept_name," +
                // Localized value for concept values
                "obs." + ChartProviderContract.ChartColumns.VALUE +
                ",coalesce(value_names." + ChartProviderContract.ChartColumns.NAME + ", obs." + ChartProviderContract.ChartColumns.VALUE + ") " +
                "AS localized_value" +

                " FROM " +
                PatientDatabase.OBSERVATIONS_TABLE_NAME + " obs " +

                " INNER JOIN " +

                "(SELECT " + ChartProviderContract.ChartColumns.CONCEPT_UUID +
                ", MAX(" + ChartProviderContract.ChartColumns.ENCOUNTER_TIME + ") AS maxtime " +
                "FROM " + PatientDatabase.OBSERVATIONS_TABLE_NAME +
                " WHERE " + ChartProviderContract.ChartColumns.PATIENT_UUID + "=? " + // 1st selection arg
                "GROUP BY " + ChartProviderContract.ChartColumns.CONCEPT_UUID + ") maxs " +

                "ON obs." + ChartProviderContract.ChartColumns.ENCOUNTER_TIME + " = maxs.maxtime AND " +
                "obs." + ChartProviderContract.ChartColumns.CONCEPT_UUID + "=maxs." + ChartProviderContract.ChartColumns.CONCEPT_UUID +

                " INNER JOIN " +
                PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " names " +
                "ON obs." + ChartProviderContract.ChartColumns.CONCEPT_UUID + "=" +
                "names." + ChartProviderContract.ChartColumns.CONCEPT_UUID +

                // Some of the results are CODED so value is a concept UUID
                // Some are numeric so the value is fine.
                // To cope we will do a left join on the value and the name
                " LEFT JOIN " + PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " value_names " +
                "ON obs." + ChartProviderContract.ChartColumns.VALUE + "=" +
                "value_names." + ChartProviderContract.ChartColumns.CONCEPT_UUID +
                " AND value_names." + ChartProviderContract.ChartColumns.LOCALE + "=?" + // 2nd selection arg

                " WHERE obs." + ChartProviderContract.ChartColumns.PATIENT_UUID + "=? AND " + // 3rd selection arg
                "names." + ChartProviderContract.ChartColumns.LOCALE + "=? " + // 4th selection arg

                " ORDER BY obs." + ChartProviderContract.ChartColumns.CONCEPT_UUID
                ;

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
