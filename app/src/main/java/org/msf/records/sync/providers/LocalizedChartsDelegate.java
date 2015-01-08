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
public class LocalizedChartsDelegate implements ProviderDelegate<PatientDatabase> {

    public static final String NAME = "localized-charts";

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
        String query = "SELECT obs.encounter_time," +
                "group_names." + ChartProviderContract.ChartColumns.NAME + " AS group_name," +
                "chart." + ChartProviderContract.ChartColumns.CONCEPT_UUID + "," +
                "names." + ChartProviderContract.ChartColumns.NAME + " AS concept_name," +
                // Localized value for concept values
                "obs." + ChartProviderContract.ChartColumns.VALUE +
                ",coalesce(value_names." + ChartProviderContract.ChartColumns.NAME + ", obs." + ChartProviderContract.ChartColumns.VALUE + ") " +
                "AS localized_value" +

                " FROM " +
                PatientDatabase.CHARTS_TABLE_NAME + " chart " +

                " INNER JOIN " + PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " names " +
                "ON chart." + ChartProviderContract.ChartColumns.CONCEPT_UUID + "=" +
                "names." + ChartProviderContract.ChartColumns.CONCEPT_UUID +

                " INNER JOIN " +
                PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " group_names " +
                "ON chart." + ChartProviderContract.ChartColumns.GROUP_UUID + "=" +
                "group_names." + ChartProviderContract.ChartColumns.CONCEPT_UUID +

                " LEFT JOIN " +
                PatientDatabase.OBSERVATIONS_TABLE_NAME + " obs " +
                "ON chart." + ChartProviderContract.ChartColumns.CONCEPT_UUID + "=" +
                "obs." + ChartProviderContract.ChartColumns.CONCEPT_UUID + " AND " +
                "(obs." + ChartProviderContract.ChartColumns.PATIENT_UUID + "=? OR " + // 2nd selection arg
                "obs." + ChartProviderContract.ChartColumns.PATIENT_UUID + " IS NULL)" +

                // Some of the results are CODED so value is a concept UUID
                // Some are numeric so the value is fine.
                // To cope we will do a left join on the value and the name
                " LEFT JOIN " + PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " value_names " +
                "ON obs." + ChartProviderContract.ChartColumns.VALUE + "= " +
                "value_names." + ChartProviderContract.ChartColumns.CONCEPT_UUID +
                " AND value_names." + ChartProviderContract.ChartColumns.LOCALE + "=?" + // 1st selection arg

//                " WHERE chart." + ChartColumns.CHART_UUID + "=? AND " +
                " WHERE " +
                "names." + ChartProviderContract.ChartColumns.LOCALE + "=? AND " + // 3rd selection arg
                "group_names." + ChartProviderContract.ChartColumns.LOCALE + "=?" + // 4th selection arg

                " ORDER BY chart." + ChartProviderContract.ChartColumns.CHART_ROW + ", obs." + ChartProviderContract.ChartColumns.ENCOUNTER_TIME
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
