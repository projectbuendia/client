package org.msf.records.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteStatement;

import java.util.List;

import static org.msf.records.sync.ChartProviderContract.ChartColumns;
import static org.msf.records.sync.ChartProviderContract.PATH_CHARTS;
import static org.msf.records.sync.ChartProviderContract.PATH_CONCEPTS;
import static org.msf.records.sync.ChartProviderContract.PATH_CONCEPT_NAMES;
import static org.msf.records.sync.ChartProviderContract.PATH_EMPTY_LOCALIZED_CHART;
import static org.msf.records.sync.ChartProviderContract.PATH_LOCALIZED_CHART;
import static org.msf.records.sync.ChartProviderContract.PATH_MOST_RECENT_CHART;
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
     * URI ID for route: /localizedchart/{locale}
     */
    public static final int EMPTY_LOCALIZED_CHART = 12;

    /**
     * URI ID for route: /localizedchart/...
     */
    public static final int LOCALIZED_CHART = 13;

    /**
     * URI ID for route: /localizedchart/...
     */
    public static final int MOST_RECENT_CHART = 14;

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
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_LOCALIZED_CHART, LOCALIZED_CHART);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_EMPTY_LOCALIZED_CHART, EMPTY_LOCALIZED_CHART);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_MOST_RECENT_CHART, MOST_RECENT_CHART);
    }

    private static final String[] PATHS = new String[]{
            PATH_OBSERVATIONS, subDirs(PATH_OBSERVATIONS),
            PATH_CONCEPTS, subDirs(PATH_CONCEPTS),
            PATH_CONCEPT_NAMES, subDirs(PATH_CONCEPT_NAMES),
            PATH_CHARTS, subDirs(PATH_CHARTS),
            PATH_LOCALIZED_CHART, /* query only, subdirs included */
            PATH_EMPTY_LOCALIZED_CHART, /* query only, subdirs included */
            PATH_MOST_RECENT_CHART, /* query only, subdirs included */
    };

    @Override
    public String[] getPaths() {
        return PATHS;
    }

    private static String subDirs(String base) {
        return base + "/*";
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
            case EMPTY_LOCALIZED_CHART:
                return queryEmptyLocalizedChart(uri, db);
            case LOCALIZED_CHART:
                return queryLocalizedChart(uri, db);
            case MOST_RECENT_CHART:
                return queryMostRecentChart(uri, db);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        builder.where(selection, selectionArgs);
        c = builder.query(db, projection, sortOrder);
        c.setNotificationUri(contentResolver, uri);
        return c;
    }

    private Cursor queryLocalizedChart(Uri uri, SQLiteDatabase db) {
        // Decode the uri, expected:
        // content://org.msf.records/localizedchart/{chart_uuid}/{locale}/{patient_uuid}
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 4) {
            throw new UnsupportedOperationException("Unknown URI " + uri);
        }
        String patientUuid = pathSegments.get(pathSegments.size() - 1);
        String locale = pathSegments.get(pathSegments.size() - 2);
        String chartUuid = pathSegments.get(pathSegments.size() - 3);

        // This scary SQL statement joins the observations with appropriate concept names to give
        // localized output in the correct order specified by a chart.
//        String query = "SELECT obs.encounter_time, group_names." + ChartColumns.NAME + " AS group_name, obs.concept_uuid, names." +
        String query = "SELECT obs.encounter_time," +
                "group_names." + ChartColumns.NAME + " AS group_name," +
                "chart." + ChartColumns.CONCEPT_UUID + "," +
                "names." + ChartColumns.NAME + " AS concept_name," +
                // Localized value for concept values
                "obs." + ChartColumns.VALUE +
                ",coalesce(value_names." + ChartColumns.NAME + ", obs." + ChartColumns.VALUE + ") " +
                "AS localized_value" +

                " FROM " +
                PatientDatabase.CHARTS_TABLE_NAME + " chart " +

                " INNER JOIN " + PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " names " +
                "ON chart." + ChartColumns.CONCEPT_UUID + "=" +
                "names." + ChartColumns.CONCEPT_UUID +

                " INNER JOIN " +
                PatientDatabase.CONCEPT_NAMES_TABLE_NAME +" group_names " +
                "ON chart." + ChartColumns.GROUP_UUID + "=" +
                "group_names." + ChartColumns.CONCEPT_UUID +

                " LEFT JOIN " +
                PatientDatabase.OBSERVATIONS_TABLE_NAME +" obs " +
                "ON chart." + ChartColumns.CONCEPT_UUID + "=" +
                "obs." + ChartColumns.CONCEPT_UUID + " AND " +
                "(obs." + ChartColumns.PATIENT_UUID + "=? OR " + // 2nd selection arg
                "obs." + ChartColumns.PATIENT_UUID + " IS NULL)" +

                // Some of the results are CODED so value is a concept UUID
                // Some are numeric so the value is fine.
                // To cope we will do a left join on the value and the name
                " LEFT JOIN " + PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " value_names " +
                "ON obs." + ChartColumns.VALUE + "= " +
                "value_names." + ChartColumns.CONCEPT_UUID +
                " AND value_names." + ChartColumns.LOCALE + "=?" + // 1st selection arg

//                " WHERE chart." + ChartColumns.CHART_UUID + "=? AND " +
                " WHERE " +
                "names." + ChartColumns.LOCALE + "=? AND " + // 3rd selection arg
                "group_names." + ChartColumns.LOCALE + "=?" + // 4th selection arg

                " ORDER BY chart." + ChartColumns.CHART_ROW + ", obs." + ChartColumns.ENCOUNTER_TIME
                ;

        return db.rawQuery(query, new String[]{patientUuid, locale, locale, locale});
    }

    private Cursor queryEmptyLocalizedChart(Uri uri, SQLiteDatabase db) {
        // Decode the uri, expected:
        // content://org.msf.records/localizedchart/{chart_uuid}/{locale}/{patient_uuid}
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 3) {
            throw new UnsupportedOperationException("Unknown URI " + uri);
        }
        String locale = pathSegments.get(pathSegments.size() - 1);
        String chartUuid = pathSegments.get(pathSegments.size() - 2);

        // This scary SQL statement joins the observations with appropriate concept names to give
        // localized output in the correct order specified by a chart.
        String query = "SELECT group_names." + ChartColumns.NAME + " AS group_name," +
                "chart." + ChartColumns.CONCEPT_UUID +
                ",names." + ChartColumns.NAME + " AS concept_name" +

                " FROM " +
                PatientDatabase.CHARTS_TABLE_NAME + " chart " +

                " INNER JOIN " + PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " names " +
                "ON chart." + ChartColumns.CONCEPT_UUID + "=" +
                "names." + ChartColumns.CONCEPT_UUID +

                " INNER JOIN " +
                PatientDatabase.CONCEPT_NAMES_TABLE_NAME +" group_names " +
                "ON chart." + ChartColumns.GROUP_UUID + "=" +
                "group_names." + ChartColumns.CONCEPT_UUID +

                " WHERE chart." + ChartColumns.CHART_UUID + "=? AND " + // 1st selection arg
                "names." + ChartColumns.LOCALE + "=? AND " + // 2nd selection arg
                "group_names." + ChartColumns.LOCALE + "=?" + // 3rd selection arg

                " ORDER BY chart." + ChartColumns.CHART_ROW
                ;

        return db.rawQuery(query, new String[]{chartUuid, locale, locale});
    }

    private Cursor queryMostRecentChart(Uri uri, SQLiteDatabase db) {
        // Decode the uri, expected:
        // content://org.msf.records/mostrecent/{patient_uuid}/{locale}
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 3) {
            throw new UnsupportedOperationException("Unknown URI " + uri);
        }
        String locale = pathSegments.get(pathSegments.size() - 1);
        String patientUuid = pathSegments.get(pathSegments.size() - 2);

        // This scary SQL statement joins the observations a subselect for the latest for each
        // concept with appropriate concept names to give localized output.
        String query = "SELECT obs.encounter_time," +
                "obs.concept_uuid,names." + ChartColumns.NAME + " AS concept_name," +
                // Localized value for concept values
                "obs." + ChartColumns.VALUE +
                ",coalesce(value_names." + ChartColumns.NAME + ", obs." + ChartColumns.VALUE + ") " +
                "AS localized_value" +

                " FROM " +
                PatientDatabase.OBSERVATIONS_TABLE_NAME + " obs " +

                " INNER JOIN " +

                "(SELECT " + ChartColumns.CONCEPT_UUID +
                ", MAX(" + ChartColumns.ENCOUNTER_TIME + ") AS maxtime " +
                "FROM " + PatientDatabase.OBSERVATIONS_TABLE_NAME +
                " WHERE " + ChartColumns.PATIENT_UUID + "=? " + // 1st selection arg
                "GROUP BY " + ChartColumns.CONCEPT_UUID + ") maxs " +

                "ON obs." + ChartColumns.ENCOUNTER_TIME + " = maxs.maxtime AND " +
                "obs." + ChartColumns.CONCEPT_UUID + "=maxs." + ChartColumns.CONCEPT_UUID +

                " INNER JOIN " +
                PatientDatabase.CONCEPT_NAMES_TABLE_NAME +" names " +
                "ON obs." + ChartColumns.CONCEPT_UUID + "=" +
                "names." + ChartColumns.CONCEPT_UUID +

                // Some of the results are CODED so value is a concept UUID
                // Some are numeric so the value is fine.
                // To cope we will do a left join on the value and the name
                " LEFT JOIN " + PatientDatabase.CONCEPT_NAMES_TABLE_NAME + " value_names " +
                "ON obs." + ChartColumns.VALUE + "=" +
                "value_names." + ChartColumns.CONCEPT_UUID +
                " AND value_names." + ChartColumns.LOCALE + "=?" + // 2nd selection arg

                " WHERE obs." + ChartColumns.PATIENT_UUID + "=? AND " + // 3rd selection arg
                "names." + ChartColumns.LOCALE + "=? " + // 4th selection arg

                " ORDER BY obs." + ChartColumns.CONCEPT_UUID
                ;

        return db.rawQuery(query, new String[]{patientUuid, locale, patientUuid, locale});
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
            case EMPTY_LOCALIZED_CHART:
                return ChartProviderContract.LOCALIZED_OBSERVATION_CONTENT_TYPE;
            case LOCALIZED_CHART:
                return ChartProviderContract.LOCALIZED_OBSERVATION_CONTENT_TYPE;
            case MOST_RECENT_CHART:
                return ChartProviderContract.LOCALIZED_OBSERVATION_CONTENT_TYPE;
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
            case EMPTY_LOCALIZED_CHART:
                throw new UnsupportedOperationException("Localized charts are query only");
            case LOCALIZED_CHART:
                throw new UnsupportedOperationException("Localized observations are query only");
            case MOST_RECENT_CHART:
                throw new UnsupportedOperationException("Most recent chart is query only");
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
                          ContentValues[] allValues) {
        if (allValues.length == 0) {
            return 0;
        }
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        assert db != null;
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
            case EMPTY_LOCALIZED_CHART:
                throw new UnsupportedOperationException("Localized charts are query only");
            case LOCALIZED_CHART:
                throw new UnsupportedOperationException("Localized observations are query only");
            case MOST_RECENT_CHART:
                throw new UnsupportedOperationException("Most recent chart is query only");
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        int numValues = allValues.length;
        ContentValues first = allValues[0];
        String[] columns = first.keySet().toArray(new String[first.size()]);
        SQLiteStatement statement = makeInsertStatement(db, tableName, columns);
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
        // Send broadcast to registered ContentObservers, to refresh UI.
        contentResolver.notifyChange(uri, null, false);
        return numValues;
    }

    private SQLiteStatement makeInsertStatement(SQLiteDatabase db, String table,
                                                String [] columns) {
        // I kind of hoped this would be provided by SQLiteDatase or DatabaseHelper,
        // But it doesn't seem to be. Innards copied from SQLiteDabase.insertWithOnConflict
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT OR REPLACE ");
        sql.append(" INTO ");
        sql.append(table);
        sql.append('(');

        int size = (columns != null && columns.length > 0) ? columns.length : 0;
        if (size <= 0) {
            throw new AssertionError();
        }
        int i = 0;
        for (String colName : columns) {
            sql.append((i > 0) ? "," : "");
            sql.append(colName);
            i++;
        }
        sql.append(')');
        sql.append(" VALUES (");
        for (i = 0; i < size; i++) {
            sql.append((i > 0) ? ",?" : "?");
        }
        sql.append(')');

        return db.compileStatement(sql.toString());
    }

    @Override
    public int delete(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
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
            case EMPTY_LOCALIZED_CHART:
                throw new UnsupportedOperationException("Localized charts are query only");
            case LOCALIZED_CHART:
                throw new UnsupportedOperationException("Localized observations are query only");
            case MOST_RECENT_CHART:
                throw new UnsupportedOperationException("Most recent chart is query only");
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
            case EMPTY_LOCALIZED_CHART:
                throw new UnsupportedOperationException("Localized charts are query only");
            case LOCALIZED_CHART:
                throw new UnsupportedOperationException("Localized observations are query only");
            case MOST_RECENT_CHART:
                throw new UnsupportedOperationException("Most recent chart is query only");
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
