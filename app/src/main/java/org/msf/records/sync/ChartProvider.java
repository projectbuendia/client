package org.msf.records.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import static org.msf.records.sync.ChartProviderContract.PATH_CHARTS;
import static org.msf.records.sync.ChartProviderContract.PATH_CONCEPTS;
import static org.msf.records.sync.ChartProviderContract.PATH_CONCEPT_NAMES;
import static org.msf.records.sync.ChartProviderContract.PATH_OBSERVATIONS;
import static org.msf.records.sync.PatientProviderContract.CONTENT_AUTHORITY;

/**
 * ContentProvider for the cache database for chart observations, concepts and layout.
 */
public class ChartProvider implements MsfRecordsProvider.SubContentProvider {

    private static final String TAG = "ChartProvider";

    /**
     * URI ID for route: /observations
     */
    public static final int OBSERVATIONS = 4;
    /**
     * URI ID for route: /observations/{id}
     */
    public static final int OBSERVATION_ITEMS = 5;

    /**
     * URI ID for route: /concepts
     */
    public static final int CONCEPTS = 6;

    /**
     * URI ID for route: /concepts/{id}
     */
    public static final int CONCEPT_ITEMS = 7;

    /**
     * URI ID for route: /concept_names
     */
    public static final int CONCEPT_NAMES = 8;

    /**
     * URI ID for route: /concept_names/{id}
     */
    public static final int CONCEPT_NAME_ITEMS = 9;

    /**
     * URI ID for route: /charts
     */
    public static final int CHART_STRUCTURE = 10;

    /**
     * URI ID for route: /charts/{id}
     */
    public static final int CHART_STRUCTURE_ITEMS = 11;

    /**
     * UriMatcher, used to decode incoming URIs.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_OBSERVATIONS, OBSERVATIONS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, subDirs(PATH_OBSERVATIONS), OBSERVATION_ITEMS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_CONCEPTS, CONCEPTS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, subDirs(PATH_CONCEPTS), CONCEPT_ITEMS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_CONCEPT_NAMES, CONCEPT_NAMES);
        sUriMatcher.addURI(CONTENT_AUTHORITY, subDirs(PATH_CONCEPT_NAMES), CONCEPT_NAME_ITEMS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_CHARTS, CHART_STRUCTURE);
        sUriMatcher.addURI(CONTENT_AUTHORITY, subDirs(PATH_CHARTS), CHART_STRUCTURE_ITEMS);
    }

    private static final String[] PATHS = new String[]{
            PATH_OBSERVATIONS, subDirs(PATH_OBSERVATIONS),
            PATH_CONCEPTS, subDirs(PATH_CONCEPTS),
            PATH_CONCEPT_NAMES, subDirs(PATH_CONCEPT_NAMES),
            PATH_CHARTS, subDirs(PATH_CHARTS),
    };

    @Override
    public String[] getPaths() {
        return PATHS;
    }

    private static String subDirs(String base) {
        return base + "/*";
    }

    @Override
    public Cursor query(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
                        String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        int uriMatch = sUriMatcher.match(uri);
        Cursor c;
        switch (uriMatch) {
            case OBSERVATIONS:
                builder.table(PatientDatabase.OBSERVATIONS_TABLE_NAME);
                break;
            case CONCEPTS:
                builder.table(PatientDatabase.CONCEPTS_TABLE_NAME);
                break;
            case CONCEPT_NAMES:
                builder.table(PatientDatabase.CONCEPT_NAMES_TABLE_NAME);
                break;
            case CHART_STRUCTURE:
                builder.table(PatientDatabase.CHARTS_TABLE_NAME);
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
            case OBSERVATIONS:
                return ChartProviderContract.OBSERVATION_CONTENT_TYPE;
            case CONCEPT_NAMES:
                return ChartProviderContract.CONCEPT_NAME_CONTENT_TYPE;
            case CONCEPTS:
                return ChartProviderContract.CONCEPT_CONTENT_TYPE;
            case CHART_STRUCTURE:
                return ChartProviderContract.CHART_CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
                      ContentValues values) {
        Log.i(TAG, "Inserting " + uri + ", " + values);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        assert db != null;
        final int match = sUriMatcher.match(uri);
        Uri result;
        String tableName;
        Uri preIdUri;
        switch (match) {
            case OBSERVATIONS:
                tableName = PatientDatabase.OBSERVATIONS_TABLE_NAME;
                preIdUri = ChartProviderContract.OBSERVATIONS_CONTENT_URI;
                break;
            case CONCEPT_NAMES:
                tableName = PatientDatabase.CONCEPT_NAMES_TABLE_NAME;
                preIdUri = ChartProviderContract.CONCEPT_NAMES_CONTENT_URI;
                break;
            case CONCEPTS:
                tableName = PatientDatabase.CONCEPTS_TABLE_NAME;
                preIdUri = ChartProviderContract.CONCEPTS_CONTENT_URI;
                break;
            case CHART_STRUCTURE:
                tableName = PatientDatabase.CHARTS_TABLE_NAME;
                preIdUri = ChartProviderContract.CHART_CONTENT_URI;
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
    public int delete(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
                      String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        String tableName;
        switch (match) {
            case OBSERVATIONS:
                tableName = PatientDatabase.OBSERVATIONS_TABLE_NAME;
                break;
            case CONCEPT_NAMES:
                tableName = PatientDatabase.CONCEPT_NAMES_TABLE_NAME;
                break;
            case CONCEPTS:
                tableName = PatientDatabase.CONCEPTS_TABLE_NAME;
                break;
            case CHART_STRUCTURE:
                tableName = PatientDatabase.CHARTS_TABLE_NAME;
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
    public int update(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
                      ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        String tableName;
        switch (match) {
            case OBSERVATIONS:
                tableName = PatientDatabase.OBSERVATIONS_TABLE_NAME;
                break;
            case CONCEPT_NAMES:
                tableName = PatientDatabase.CONCEPT_NAMES_TABLE_NAME;
                break;
            case CONCEPTS:
                tableName = PatientDatabase.CONCEPTS_TABLE_NAME;
                break;
            case CHART_STRUCTURE:
                tableName = PatientDatabase.CHARTS_TABLE_NAME;
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        int count = builder.table(tableName)
                .where(selection, selectionArgs)
                .update(db, values);
        contentResolver.notifyChange(uri, null, false);
        return count;
    }
}
