package org.msf.records.sync.providers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import net.sqlcipher.database.SQLiteOpenHelper;

/**
 * A delegate used to handle a single URI for {@link DelegatingProvider}.
 */
public interface ProviderDelegate<T extends SQLiteOpenHelper> {

    static final String TYPE_PACKAGE_PREFIX = "/vnd.msf.records.";

    /**
     * Returns the MIME type this delegate provides.
     */
    String getType();

    /**
     * Handles a query request.
     *
     * @see ContentProvider#query
     */
    Cursor query(
            T dbHelper, ContentResolver contentResolver, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder);

    /**
     * Handles an insert request.
     *
     * @see ContentProvider#insert
     */
    Uri insert(
            T dbHelper, ContentResolver contentResolver, Uri uri, ContentValues values);

    /**
     * Handles a bulk insert request.
     *
     * @see ContentProvider#bulkInsert
     */
    int bulkInsert(
            T dbHelper, ContentResolver contentResolver, Uri uri, ContentValues[] values);

    /**
     * Handles a delete request.
     *
     * @see ContentProvider#delete
     */
    public int delete(
            T dbHelper, ContentResolver contentResolver, Uri uri, String selection,
            String[] selectionArgs);

    /**
     * Handles an update request.
     *
     * @see ContentProvider#update
     */
    int update(
            T dbHelper, ContentResolver contentResolver, Uri uri, ContentValues values,
            String selection, String[] selectionArgs);
}
