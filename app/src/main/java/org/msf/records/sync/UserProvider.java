package org.msf.records.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import net.sqlcipher.database.SQLiteDatabase;

import static org.msf.records.sync.PatientProviderContract.CONTENT_AUTHORITY;

/**
 * UserProvider for the cache database for users.
 */
public class UserProvider implements MsfRecordsProvider.SubContentProvider {

    private static final String TAG = "UserProvider";

    /**
     * URI ID for route: /users
     */
    public static final int USERS = 20;

    /**
     * UriMatcher, used to decode incoming URIs.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);


    static {
        sUriMatcher.addURI(CONTENT_AUTHORITY, UserProviderContract.PATH_USERS, USERS);
    }

    private static final String[] PATHS = new String[] {
            UserProviderContract.PATH_USERS
    };

    @Override
    public String[] getPaths() {
        return PATHS;
    }

    @Override
    public Cursor query(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                        String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        int uriMatch = sUriMatcher.match(uri);
        Cursor c;
        switch (uriMatch) {
            case USERS:
                builder.table(PatientDatabase.USERS_TABLE_NAME);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        builder.where(selection, selectionArgs);
        c = builder.query(db, projection, sortOrder);
        c.setNotificationUri(contentResolver, uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case USERS:
                return UserProviderContract.USER_CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                      ContentValues values) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        assert db != null;
        final int match = sUriMatcher.match(uri);
        Uri result;
        String tableName;
        Uri preIdUri;
        switch (match) {
            case USERS:
                tableName = PatientDatabase.USERS_TABLE_NAME;
                preIdUri = UserProviderContract.USERS_CONTENT_URI;
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        long id = db.replaceOrThrow(tableName, null, values);
        result = Uri.parse(preIdUri + "/" + id);
        // Send broadcast to registered ContentObservers, to refresh UI.
        contentResolver.notifyChange(uri, null, false);
        return result;
    }

    @Override
    public int bulkInsert(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                          ContentValues[] values) {
        // TODO(nfortescue): optimise this.
        int numValues = values.length;
        for (int i = 0; i < numValues; i++) {
            insert(dbHelper, contentResolver, uri, values[i]);
        }
        return numValues;
    }

    @Override
    public int delete(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                      String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        String tableName;
        switch (match) {
            case USERS:
                tableName = PatientDatabase.USERS_TABLE_NAME;
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        int count = builder.table(tableName)
                .where(selection, selectionArgs)
                .delete(db);
        // Send broadcast to registered ContentObservers, to refresh UI.
        contentResolver.notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                      ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case USERS:
                builder.table(PatientDatabase.USERS_TABLE_NAME);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        int count = builder.where(selection, selectionArgs)
                .update(db, values);
        contentResolver.notifyChange(uri, null, false);
        return count;
    }
}
