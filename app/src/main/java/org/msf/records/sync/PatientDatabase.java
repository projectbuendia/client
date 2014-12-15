package org.msf.records.sync;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.provider.BaseColumns._ID;
import static org.msf.records.sync.ChartProviderContract.ChartColumns;
import static org.msf.records.sync.LocationProviderContract.LocationColumns;
import static org.msf.records.sync.PatientProviderContract.PatientColumns.COLUMN_NAME_ADMISSION_TIMESTAMP;
import static org.msf.records.sync.PatientProviderContract.PatientColumns.COLUMN_NAME_AGE_MONTHS;
import static org.msf.records.sync.PatientProviderContract.PatientColumns.COLUMN_NAME_AGE_YEARS;
import static org.msf.records.sync.PatientProviderContract.PatientColumns.COLUMN_NAME_FAMILY_NAME;
import static org.msf.records.sync.PatientProviderContract.PatientColumns.COLUMN_NAME_GENDER;
import static org.msf.records.sync.PatientProviderContract.PatientColumns.COLUMN_NAME_GIVEN_NAME;
import static org.msf.records.sync.PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID;
import static org.msf.records.sync.PatientProviderContract.PatientColumns.COLUMN_NAME_UUID;

/**
 * A helper for the database for storing patient attributes, active patients, locations,
 * and chart information.
 * Stored in the same database as patients are the keys for charts too.
 */
public class PatientDatabase extends SQLiteOpenHelper {

    /** Schema version. */
    public static final int DATABASE_VERSION = 12;
    /** Filename for SQLite file. */
    public static final String DATABASE_NAME = "patients.db";

    private static final String CREATE_TABLE = "CREATE TABLE ";
    private static final String PRIMARY_KEY = " PRIMARY KEY";
    private static final String UNIQUE = " UNIQUE";
    private static final String UNIQUE_INDEX = "UNIQUE(";
    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_INTEGER = " INTEGER";
    private static final String NOTNULL = "  NOT NULL";
    private static final String COMMA_SEP = ",";

    /**
     * Table name where records are stored for "patient" resources.
     */
    static final String PATIENTS_TABLE_NAME = "patients";

    /** SQL statement to create "patient" table. */
    private static final String SQL_CREATE_ENTRIES =
            CREATE_TABLE + PATIENTS_TABLE_NAME + " (" +
                    _ID + TYPE_TEXT + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    COLUMN_NAME_GIVEN_NAME + TYPE_TEXT + COMMA_SEP +
                    COLUMN_NAME_FAMILY_NAME + TYPE_TEXT + COMMA_SEP +
                    COLUMN_NAME_UUID + TYPE_TEXT + COMMA_SEP +
                    COLUMN_NAME_LOCATION_UUID + TYPE_TEXT + COMMA_SEP +
                    COLUMN_NAME_ADMISSION_TIMESTAMP + TYPE_INTEGER + COMMA_SEP +
                    COLUMN_NAME_AGE_YEARS + TYPE_INTEGER + COMMA_SEP +
                    COLUMN_NAME_AGE_MONTHS + TYPE_INTEGER + COMMA_SEP +
                    COLUMN_NAME_GENDER + TYPE_TEXT + ")";

    static final String CONCEPTS_TABLE_NAME = "concepts";

    private static final String SQL_CREATE_CONCEPTS =
            CREATE_TABLE + CONCEPTS_TABLE_NAME + " (" +
                    _ID + TYPE_TEXT + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    ChartColumns.CONCEPT_TYPE + TYPE_TEXT +
                    ")";

    public static final String CONCEPT_NAMES_TABLE_NAME = "concept_names";

    private static final String SQL_CREATE_CONCEPT_NAMES =
            CREATE_TABLE + CONCEPT_NAMES_TABLE_NAME + " (" +
                    _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    ChartColumns.CONCEPT_UUID + TYPE_TEXT + COMMA_SEP +
                    ChartColumns.LOCALE + TYPE_TEXT + COMMA_SEP +
                    ChartColumns.NAME + TYPE_TEXT + COMMA_SEP +
                    UNIQUE_INDEX + ChartColumns.CONCEPT_UUID + COMMA_SEP + ChartColumns.LOCALE +
                    ")" +
                    ")";

    static final String LOCATIONS_TABLE_NAME = "locations";

    private static final String SQL_CREATE_LOCATIONS =
            CREATE_TABLE + LOCATIONS_TABLE_NAME + " (" +
                _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                LocationColumns.LOCATION_UUID + TYPE_TEXT + COMMA_SEP +
                LocationColumns.PARENT_UUID + TYPE_TEXT +
                ")";

    static final String LOCATION_NAMES_TABLE_NAME = "location_names";

    private static final String SQL_CREATE_LOCATION_NAMES =
            CREATE_TABLE + LOCATION_NAMES_TABLE_NAME + " (" +
                _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                LocationColumns.LOCATION_UUID + TYPE_TEXT + COMMA_SEP +
                LocationColumns.LOCALE + TYPE_TEXT + COMMA_SEP +
                LocationColumns.NAME + TYPE_TEXT + COMMA_SEP +
                UNIQUE_INDEX + LocationColumns.LOCATION_UUID + COMMA_SEP + LocationColumns.LOCALE +
                ")" +
                ")";

    public static final String OBSERVATIONS_TABLE_NAME = "observations";

    private static final String SQL_CREATE_OBSERVATIONS =
            CREATE_TABLE + OBSERVATIONS_TABLE_NAME + " (" +
                    _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    ChartColumns.PATIENT_UUID + TYPE_TEXT + COMMA_SEP +
                    ChartColumns.ENCOUNTER_UUID + TYPE_TEXT + COMMA_SEP +
                    ChartColumns.ENCOUNTER_TIME + TYPE_INTEGER + COMMA_SEP +
                    ChartColumns.CONCEPT_UUID + TYPE_INTEGER + COMMA_SEP +
                    ChartColumns.VALUE + TYPE_INTEGER + COMMA_SEP +
                    ChartColumns.TEMP_CACHE + TYPE_INTEGER + COMMA_SEP + //actually boolean
                    UNIQUE_INDEX + ChartColumns.PATIENT_UUID + COMMA_SEP +
                    ChartColumns.ENCOUNTER_UUID + COMMA_SEP + ChartColumns.CONCEPT_UUID + ")" +
                    ")";

    public static final String CHARTS_TABLE_NAME = "charts";

    private static final String SQL_CREATE_CHARTS =
            CREATE_TABLE + CHARTS_TABLE_NAME + " (" +
                    _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    ChartColumns.CHART_UUID + TYPE_TEXT + COMMA_SEP +
                    ChartColumns.CHART_ROW + TYPE_INTEGER + COMMA_SEP +
                    ChartColumns.GROUP_UUID + TYPE_TEXT + COMMA_SEP +
                    ChartColumns.CONCEPT_UUID + TYPE_INTEGER + COMMA_SEP +
                    UNIQUE_INDEX + ChartColumns.CHART_UUID + COMMA_SEP + ChartColumns.CONCEPT_UUID +
                    ")" +
                    ")";

    static final String USERS_TABLE_NAME = "users";

    private static final String SQL_CREATE_USERS =
            CREATE_TABLE + USERS_TABLE_NAME + " (" +
                    _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    UserProviderContract.UserColumns.UUID + TYPE_TEXT + COMMA_SEP +
                    UserProviderContract.UserColumns.FULL_NAME + TYPE_TEXT + ")";

    private static String makeDropTable(String tableName) {
        return "DROP TABLE IF EXISTS " + tableName;
    }

    public PatientDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_CONCEPT_NAMES);
        db.execSQL(SQL_CREATE_CONCEPTS);
        db.execSQL(SQL_CREATE_CHARTS);
        db.execSQL(SQL_CREATE_LOCATIONS);
        db.execSQL(SQL_CREATE_LOCATION_NAMES);
        db.execSQL(SQL_CREATE_OBSERVATIONS);
        db.execSQL(SQL_CREATE_USERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(makeDropTable(PATIENTS_TABLE_NAME));
        db.execSQL(makeDropTable(OBSERVATIONS_TABLE_NAME));
        db.execSQL(makeDropTable(CONCEPTS_TABLE_NAME));
        db.execSQL(makeDropTable(CONCEPT_NAMES_TABLE_NAME));
        db.execSQL(makeDropTable(CHARTS_TABLE_NAME));
        db.execSQL(makeDropTable(LOCATIONS_TABLE_NAME));
        db.execSQL(makeDropTable(LOCATION_NAMES_TABLE_NAME));
        db.execSQL(makeDropTable(USERS_TABLE_NAME));
        onCreate(db);
    }
}
