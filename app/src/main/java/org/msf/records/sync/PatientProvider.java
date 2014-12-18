package org.msf.records.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import net.sqlcipher.database.SQLiteDatabase;

import static org.msf.records.sync.PatientProviderContract.CONTENT_AUTHORITY;
import static org.msf.records.sync.PatientProviderContract.PATH_PATIENTS;
import static org.msf.records.sync.PatientProviderContract.PATH_TENT_PATIENT_COUNTS;

/**
 * ContentProvider code for handling patient related URIs.
 */
public class PatientProvider implements MsfRecordsProvider.SubContentProvider {

    /**
     * URI ID for route: /patients
     */
    public static final int ROUTE_PATIENTS = 1;

    /**
     * URI ID for route: /patients/{ID}
     */
    public static final int ROUTE_PATIENTS_ID = 2;

    /**
     * URI ID for route: /tentpatients/
     */
    public static final int ROUTE_TENT_PATIENT_COUNTS = 3;

    /**
     * UriMatcher, used to decode incoming URIs.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PATIENTS, ROUTE_PATIENTS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PATIENTS + "/*", ROUTE_PATIENTS_ID);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_TENT_PATIENT_COUNTS, ROUTE_TENT_PATIENT_COUNTS);
    }

    @Override
    public String[] getPaths() {
        return new String[] {
                PATH_PATIENTS,
                PATH_PATIENTS + "/*",
                PATH_TENT_PATIENT_COUNTS
        };
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ROUTE_PATIENTS:
            case ROUTE_TENT_PATIENT_COUNTS:
                return PatientProviderContract.CONTENT_TYPE;
            case ROUTE_PATIENTS_ID:
                return PatientProviderContract.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                        String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case ROUTE_PATIENTS_ID:
                // Return a single entry, by ID.
                String id = uri.getLastPathSegment();
                builder.where(PatientProviderContract.PatientColumns._ID + "=?", id);
            case ROUTE_PATIENTS:
                // Return all known entries.
                builder.table(PatientDatabase.PATIENTS_TABLE_NAME)
                        .where(selection, selectionArgs);
                Cursor c = builder.query(db, projection, sortOrder);
                // Note: Notification URI must be manually set here for loaders to correctly
                // register ContentObservers.
                c.setNotificationUri(contentResolver, uri);
                return c;
            case ROUTE_TENT_PATIENT_COUNTS: // Build a cursor manually since we can't use GROUP BY
                builder.table(PatientDatabase.PATIENTS_TABLE_NAME)
                        .where(selection, selectionArgs)
                        .where(PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID +
                                " IS NOT NULL");
                Cursor countsCursor = builder.query(
                        db, new String[] {
                                PatientProviderContract.PatientColumns._ID,
                                PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID,
                                "COUNT(*) AS " + PatientProviderContract.PatientColumns._COUNT,
                        },  // Projection
                        PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID, // Group
                        "", sortOrder, "");
                countsCursor.setNotificationUri(contentResolver, uri);
                return countsCursor;
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
        switch (match) {
            case ROUTE_PATIENTS:
                long id = db.insertOrThrow(PatientDatabase.PATIENTS_TABLE_NAME, null, values);
                result = Uri.parse(PatientProviderContract.CONTENT_URI + "/" + id);
                break;
            case ROUTE_PATIENTS_ID:
            case ROUTE_TENT_PATIENT_COUNTS:
                throw new UnsupportedOperationException("Insert not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        contentResolver.notifyChange(uri, null, false);
        return result;
    }

    @Override
    public int bulkInsert(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                          ContentValues[] values) {
        // TODO(nfortescue): optimise this.
        int numValues = values.length;
        for (ContentValues value : values) {
            insert(dbHelper, contentResolver, uri, value);
        }
        return numValues;
    }

    @Override
    public int delete(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                      String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_PATIENTS:
                count = builder.table(PatientDatabase.PATIENTS_TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case ROUTE_PATIENTS_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(PatientDatabase.PATIENTS_TABLE_NAME)
                        .where(PatientProviderContract.PatientColumns._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case ROUTE_TENT_PATIENT_COUNTS:
                throw new UnsupportedOperationException("Delete not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        contentResolver.notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                      ContentValues values, String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_PATIENTS:
                count = builder.table(PatientDatabase.PATIENTS_TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case ROUTE_PATIENTS_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(PatientDatabase.PATIENTS_TABLE_NAME)
                        .where(PatientProviderContract.PatientColumns._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case ROUTE_TENT_PATIENT_COUNTS:
                throw new UnsupportedOperationException("Update not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        contentResolver.notifyChange(uri, null, false);
        return count;
    }
}
