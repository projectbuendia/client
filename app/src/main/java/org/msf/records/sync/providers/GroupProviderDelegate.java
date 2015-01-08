package org.msf.records.sync.providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteStatement;

import org.msf.records.sync.PatientDatabase;
import org.msf.records.sync.SelectionBuilder;

/**
 * A {@link ProviderDelegate} that provides query, insert, delete, and update access to a group or
 * list of items provided directly from the database.
 */
class GroupProviderDelegate implements ProviderDelegate<PatientDatabase> {

    private final String mName;
    private final String mTableName;
    private final String mType;

    public GroupProviderDelegate(String name, String tableName) {
        mName = name;
        mTableName = tableName;
        mType = ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + mName;
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
                .where(selection, selectionArgs);
        Cursor cursor = builder.query(dbHelper.getReadableDatabase(), projection, sortOrder);
        cursor.setNotificationUri(contentResolver, uri);
        return cursor;
    }

    @Override
    public Uri insert(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues values) {
        long id = dbHelper.getWritableDatabase()
                .replaceOrThrow(mTableName, null, values);
        contentResolver.notifyChange(uri, null, false);
        return uri.buildUpon().appendPath(Long.toString(id)).build();
    }

    @Override
    public int bulkInsert(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues[] allValues) {
        if (allValues.length == 0) {
            return 0;
        }
        final SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues first = allValues[0];
        String[] columns = first.keySet().toArray(new String[first.size()]);
        SQLiteStatement statement = makeInsertStatement(db, mTableName, columns);
        db.beginTransaction();
        Object[] bindings = new Object[first.size()];
        for (ContentValues values : allValues) {
            statement.clearBindings();
            if (values.size() != first.size()) {
                throw new AssertionError();
            }
            for (int i = 0; i < bindings.length; i++) {
                Object value = values.get(columns[i]);
                // This isn't super safe, but is in our context.
                int bindingIndex = i + 1;
                if (value instanceof String) {
                    statement.bindString(bindingIndex, (String) value);
                } else if ((value instanceof Long) || value instanceof Integer) {
                    statement.bindLong(bindingIndex, ((Number) value).longValue());
                } else if ((value instanceof Double) || value instanceof Float) {
                    statement.bindDouble(bindingIndex, ((Number) value).doubleValue());
                }
                bindings[i] = value;
            }
            statement.executeInsert();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        contentResolver.notifyChange(uri, null, false);
        return allValues.length;
    }

    @Override
    public int delete(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            String selection, String[] selectionArgs) {
        int count = new SelectionBuilder()
                .table(mTableName)
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
                .where(selection, selectionArgs)
                .update(dbHelper.getWritableDatabase(), values);
        contentResolver.notifyChange(uri, null, false);
        return count;
    }

    private SQLiteStatement makeInsertStatement(
            SQLiteDatabase db, String table, String [] columns) {
        // I kind of hoped this would be provided by SQLiteDatase or DatabaseHelper,
        // But it doesn't seem to be. Innards copied from SQLiteDabase.insertWithOnConflict
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT OR REPLACE ");
        sql.append(" INTO ");
        sql.append(table);
        sql.append('(');

        int size = (columns != null && columns.length > 0) ? columns.length : 0;
        if (size <= 0) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < columns.length; i++) {
            sql.append((i > 0) ? "," : "");
            sql.append(columns[i]);
        }
        sql.append(')');
        sql.append(" VALUES (");
        for (int i = 0; i < size; i++) {
            sql.append((i > 0) ? ",?" : "?");
        }
        sql.append(')');

        return db.compileStatement(sql.toString());
    }
}
