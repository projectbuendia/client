package org.msf.records.sync.providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.msf.records.sync.PatientDatabase;
import org.msf.records.sync.PatientProviderContract;
import org.msf.records.sync.SelectionBuilder;

/**
 * A {@link ProviderDelegate} that provides query access to the count of patients in each tent.
 */
public class PatientCountsDelegate implements ProviderDelegate<PatientDatabase> {

    public static final String NAME = "patient-count";

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
        Cursor cursor = new SelectionBuilder().table(PatientDatabase.PATIENTS_TABLE_NAME)
                .where(selection, selectionArgs)
                .where(PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID +
                        " IS NOT NULL")
                .query(
                        dbHelper.getReadableDatabase(),
                        new String[] {
                                PatientProviderContract.PatientColumns._ID,
                                PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID,
                                "COUNT(*) AS " + PatientProviderContract.PatientColumns._COUNT,
                        },  // Projection
                        PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID, // Group
                        "",
                        sortOrder,
                        "");
        return cursor;
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
