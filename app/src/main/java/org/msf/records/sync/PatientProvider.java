package org.msf.records.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.msf.records.sync.PatientProviderContract.CONTENT_AUTHORITY;
import static org.msf.records.sync.PatientProviderContract.PATH_PATIENTS;
import static org.msf.records.sync.PatientProviderContract.PATH_PATIENTS_TENTS;
import static org.msf.records.sync.PatientProviderContract.PATH_PATIENTS_ZONES;
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
     * URI ID for route: /zones
     */
    public static final int ROUTE_ZONES = 3;

    /**
     * URI ID for route: /tents
     */
    public static final int ROUTE_TENTS = 4;

    /**
     * URI ID for route: /tentpatients/
     */
    public static final int ROUTE_TENT_PATIENT_COUNTS = 5;

    /**
     * UriMatcher, used to decode incoming URIs.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Simple bean for holding a mutable int (Integer is immutable).
    private class MutableInt {
        public MutableInt(int value) {
            this.value = value;
        }

        public int value;
    }

    static {
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PATIENTS, ROUTE_PATIENTS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PATIENTS + "/*", ROUTE_PATIENTS_ID);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PATIENTS_ZONES, ROUTE_ZONES);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PATIENTS_TENTS, ROUTE_TENTS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_TENT_PATIENT_COUNTS, ROUTE_TENT_PATIENT_COUNTS);
    }

    @Override
    public String[] getPaths() {
        return new String[] {
                PATH_PATIENTS,
                PATH_PATIENTS + "/*",
                PATH_PATIENTS_ZONES,
                PATH_PATIENTS_TENTS,
                PATH_TENT_PATIENT_COUNTS
        };
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ROUTE_PATIENTS:
            case ROUTE_TENTS:
            case ROUTE_TENT_PATIENT_COUNTS:
            case ROUTE_ZONES:
                return PatientProviderContract.CONTENT_TYPE;
            case ROUTE_PATIENTS_ID:
                return PatientProviderContract.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
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
            case ROUTE_TENTS:  //ContentProviders don't support group by, this is a way around it
                builder.table(PatientDatabase.PATIENTS_TABLE_NAME)
                        .where(selection, selectionArgs);
                Cursor tentsCursor = builder.query(db, projection,
                        PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_TENT, "", sortOrder, "");
                tentsCursor.setNotificationUri(contentResolver, uri);
                return tentsCursor;
            case ROUTE_ZONES:  //ContentProviders don't support group by, this is a way around it
                builder.table(PatientDatabase.PATIENTS_TABLE_NAME)
                        .where(selection, selectionArgs);
                Cursor zonesCursor = builder.query(db, projection,
                        PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_ZONE, "", sortOrder, "");
                zonesCursor.setNotificationUri(contentResolver, uri);
                return zonesCursor;
            case ROUTE_TENT_PATIENT_COUNTS: // Build a cursor manually since we can't use GROUP BY
                builder.table(PatientDatabase.PATIENTS_TABLE_NAME)
                        .where(selection, selectionArgs);
                Cursor allPatients = builder.query(
                        db, new String[] {
                                PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID
                        }, sortOrder);
                HashMap<String, MutableInt> counts = new HashMap<String, MutableInt>();
                while (allPatients.moveToNext()) {
                    String locationId = allPatients.getString(0);
                    if (!counts.containsKey(locationId)) {
                        counts.put(locationId, new MutableInt(0));
                    }
                    counts.get(locationId).value++;
                }

                MatrixCursor result = new MatrixCursor(new String[] {
                       LocationProviderContract.LocationColumns._ID,
                       PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID,
                       PatientProviderContract.PatientColumns.COLUMN_NAME_TENT_PATIENT_COUNT});
                int i = 0;
                for (Map.Entry<String, MutableInt> countEntry : counts.entrySet()) {
                    // Reject bad locations.
                    if (countEntry.getKey() == null) {
                        continue;
                    }
                    result.addRow(
                            new Object[] { i, countEntry.getKey(), countEntry.getValue().value });
                    i++;
                }
                return result;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
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
            case ROUTE_TENTS:
            case ROUTE_TENT_PATIENT_COUNTS:
            case ROUTE_ZONES:
                throw new UnsupportedOperationException("Insert not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        contentResolver.notifyChange(uri, null, false);
        return result;
    }

    @Override
    public int delete(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
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
            case ROUTE_TENTS:
            case ROUTE_ZONES:
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
    public int update(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
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
            case ROUTE_TENTS:
            case ROUTE_ZONES:
            case ROUTE_TENT_PATIENT_COUNTS:
                throw new UnsupportedOperationException("Update not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        contentResolver.notifyChange(uri, null, false);
        return count;
    }
}
