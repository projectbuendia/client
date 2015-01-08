package org.msf.records.sync.providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import org.msf.records.sync.PatientDatabase;

import java.util.List;

/**
 * A {@link SingleProviderDelegate} that supports insertion.
 */
public class InsertableSingleProviderDelegate extends SingleProviderDelegate {

    public InsertableSingleProviderDelegate(String name, String tableName, String idColumn) {
        super(name, tableName, idColumn);
    }

    @Override
    public Uri insert(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues values) {
        values.put(mIdColumn, uri.getLastPathSegment());
        long id = dbHelper.getWritableDatabase().replaceOrThrow(mTableName, null, values);
        contentResolver.notifyChange(uri, null, false);
        return getPrefixUriBuilder(uri).appendPath(Long.toString(id)).build();
    }

    private static Uri.Builder getPrefixUriBuilder(Uri uri) {
        Uri.Builder prefixBuilder = uri.buildUpon()
                .path("");
        List<String> pathSegments = uri.getPathSegments();
        for (int i = 0; i < pathSegments.size() - 1; i++) {
            prefixBuilder.appendPath(pathSegments.get(i));
        }

        return prefixBuilder;
    }
}
