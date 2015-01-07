package org.msf.records.sync.providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.msf.records.sync.PatientDatabase;
import org.msf.records.sync.SelectionBuilder;

/**
 * A {@link ProviderDelegate} that provides query, delete, and update access to a single item
 * provided directly from the database.
 */
public class SingleProviderDelegate implements ProviderDelegate<PatientDatabase> {

    protected final String mName;
    protected final String mTableName;
    protected final String mType;
    protected final String mIdColumn;

    /**
     * Creates an instance of {@link SingleProviderDelegate}.
     */
    public SingleProviderDelegate(String name, String tableName, String idColumn) {
        mName = name;
        mTableName = tableName;
        mIdColumn = idColumn;
        mType = ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + mName;
    }

    @Override
    public String getType() {
        return mType;
    }

    @Override
    public Cursor query(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SelectionBuilder builder = new SelectionBuilder()
                .table(mTableName)
                .where(mIdColumn + "=?", uri.getLastPathSegment())
                .where(selection, selectionArgs);
        Cursor cursor = builder.query(dbHelper.getReadableDatabase(), projection, sortOrder);
        cursor.setNotificationUri(contentResolver, uri);
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
            ContentValues[] allValues) {
        throw new UnsupportedOperationException(
                "Bulk insert is not supported for URI '" + uri + "'.");
    }

    @Override
    public int delete(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            String selection, String[] selectionArgs) {
        int count = new SelectionBuilder()
                .table(mTableName)
                .where(mIdColumn + "=?", uri.getLastPathSegment())
                .where(selection, selectionArgs)
                .delete(dbHelper.getWritableDatabase());
        contentResolver.notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues values, String selection, String[] selectionArgs) {
        int count = new SelectionBuilder()
                .table(mTableName)
                .where(mIdColumn + "=?", uri.getLastPathSegment())
                .where(selection, selectionArgs)
                .update(dbHelper.getWritableDatabase(), values);
        contentResolver.notifyChange(uri, null, false);
        return count;
    }
}
