package org.msf.records.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import org.msf.records.utils.SelectionBuilder;

/**
 * Created by Gil on 21/11/14.
 */
public class PatientProvider extends ContentProvider {

    PatientDatabase mDatabaseHelper;

    /**
     * Content authority for this provider.
     */
    private static final String AUTHORITY = PatientContract.CONTENT_AUTHORITY;

    /**
     * URI ID for route: /patients
     */
    public static final int ROUTE_PATIENTS = 1;

    /**
     * URI ID for route: /patients/{ID}
     */
    public static final int ROUTE_PATIENTS_ID = 2;

    /**
     * UriMatcher, used to decode incoming URIs.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, "patients", ROUTE_PATIENTS);
        sUriMatcher.addURI(AUTHORITY, "patients/*", ROUTE_PATIENTS_ID);
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new PatientDatabase(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ROUTE_PATIENTS:
                return PatientContract.Patient.CONTENT_TYPE;
            case ROUTE_PATIENTS_ID:
                return PatientContract.Patient.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Performs database query
     * @param uri supporting /patients where it returns all the patients
     *        or /patient/{ID} where selects a particular patient
     * @return results
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case ROUTE_PATIENTS_ID:
                // Return a single entry, by ID.
                String id = uri.getLastPathSegment();
                builder.where(PatientContract.Patient._ID + "=?", id);
            case ROUTE_PATIENTS:
                // Return all known entries.
                builder.table(PatientContract.Patient.TABLE_NAME)
                        .where(selection, selectionArgs);
                Cursor c = builder.query(db, projection, sortOrder);
                // Note: Notification URI must be manually set here for loaders to correctly
                // register ContentObservers.
                Context ctx = getContext();
                assert ctx != null;
                c.setNotificationUri(ctx.getContentResolver(), uri);
                return c;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        assert db != null;
        final int match = sUriMatcher.match(uri);
        Uri result;
        switch (match) {
            case ROUTE_PATIENTS:
                long id = db.insertOrThrow(PatientContract.Patient.TABLE_NAME, null, values);
                result = Uri.parse(PatientContract.Patient.CONTENT_URI + "/" + id);
                break;
            case ROUTE_PATIENTS_ID:
                throw new UnsupportedOperationException("Insert not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return result;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_PATIENTS:
                count = builder.table(PatientContract.Patient.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case ROUTE_PATIENTS_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(PatientContract.Patient.TABLE_NAME)
                        .where(PatientContract.Patient._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_PATIENTS:
                count = builder.table(PatientContract.Patient.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case ROUTE_PATIENTS_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(PatientContract.Patient.TABLE_NAME)
                        .where(PatientContract.Patient._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    static class PatientDatabase extends SQLiteOpenHelper {

        /** Schema version. */
        public static final int DATABASE_VERSION = 1;
        /** Filename for SQLite file. */
        public static final String DATABASE_NAME = "records.db";

        private static final String TYPE_TEXT = " TEXT";
        private static final String TYPE_INTEGER = " INTEGER";
        private static final String COMMA_SEP = ",";
        /** SQL statement to create "entry" table. */
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + PatientContract.Patient.TABLE_NAME + " (" +
                        PatientContract.Patient._ID + TYPE_TEXT + " PRIMARY KEY" + COMMA_SEP +
                        PatientContract.Patient.COLUMN_NAME_PATIENT_ID + TYPE_TEXT + COMMA_SEP +
                        PatientContract.Patient.COLUMN_NAME_TITLE + TYPE_TEXT + ")";

        /** SQL statement to drop "patient" table. */
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + PatientContract.Patient.TABLE_NAME;


        public PatientDatabase(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
    }
}
