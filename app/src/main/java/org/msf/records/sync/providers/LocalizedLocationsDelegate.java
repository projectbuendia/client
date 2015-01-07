package org.msf.records.sync.providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.msf.records.sync.PatientDatabase;
import org.msf.records.sync.RawQueryManager;

import java.util.List;

/**
 * A {@link ProviderDelegate} that provides query access to all localized locations.
 */
public class LocalizedLocationsDelegate implements ProviderDelegate<PatientDatabase> {

    private final RawQueryManager mRawQueryManager;

    public LocalizedLocationsDelegate(RawQueryManager rawQueryManager) {
        mRawQueryManager = rawQueryManager;
    }

    @Override
    public String getType() {
        return Contracts.LocalizedLocations.GROUP_CONTENT_TYPE;
    }

    @Override
    public Cursor query(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        // URI expected to be of form ../localized-locations/{locale}.
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 2) {
            throw new UnsupportedOperationException("URI '" + uri + "' is malformed.");
        }

        String locale = pathSegments.get(1);
        String query = mRawQueryManager.getRawQuery(RawQueryManager.Name.LOCALIZED_LOCATIONS);

        return dbHelper.getReadableDatabase().rawQuery(query, new String[] { locale });
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
