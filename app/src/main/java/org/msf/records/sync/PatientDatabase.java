package org.msf.records.sync;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import org.msf.records.sync.providers.Contracts;

import static android.provider.BaseColumns._ID;

/**
 * A helper for the database for storing patient attributes, active patients, locations,
 * and chart information.
 * Stored in the same database as patients are the keys for charts too.
 */
public class PatientDatabase extends SQLiteOpenHelper {

    // TODO(dxchen): This class currently uses providers.Contracts to define DB column names. This
    // isn't strictly correct as those names are the ones used in the public interface of
    // ContentProviders. Just because a DB column name has the same name as a ContentProvider column
    // name doesn't mean that they should reference the exact same string.

    /** Schema version. */
    public static final int DATABASE_VERSION = 14;
    /** Filename for SQLite file. */
    public static final String DATABASE_NAME = "patientscipher.db";
    /**
     * Having this hardcoded into the app is NOT secure. However, it does prevent idle browsing
     * from someone who has just stolen the tablet, and isn't willing to look at source code.
     *
     * TODO(nfortescue): add something better. At the very minimum a server call and local storage
     * with expiry so that it has to sync to the server every so often. Even better some sort of
     * public key based scheme to only deliver the key on login with registered user on good device.
     */
    private static final String ENCRYPTION_PASSWORD = "Twas brilling and the slithy toves";

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
    public static final String PATIENTS_TABLE_NAME = "patients";

    /** SQL statement to create "patient" table. */
    private static final String SQL_CREATE_ENTRIES =
            CREATE_TABLE + PATIENTS_TABLE_NAME + " (" +
                    _ID + TYPE_TEXT + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    Contracts.Patients.GIVEN_NAME + TYPE_TEXT + COMMA_SEP +
                    Contracts.Patients.FAMILY_NAME + TYPE_TEXT + COMMA_SEP +
                    Contracts.Patients.UUID + TYPE_TEXT + COMMA_SEP +
                    Contracts.Patients.LOCATION_UUID + TYPE_TEXT + COMMA_SEP +
                    Contracts.Patients.ADMISSION_TIMESTAMP + TYPE_INTEGER + COMMA_SEP +
                    Contracts.Patients.BIRTHDATE + TYPE_TEXT + COMMA_SEP +
                    Contracts.Patients.GENDER + TYPE_TEXT + ")";

    public static final String CONCEPTS_TABLE_NAME = "concepts";

    private static final String SQL_CREATE_CONCEPTS =
            CREATE_TABLE + CONCEPTS_TABLE_NAME + " (" +
                    _ID + TYPE_TEXT + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    Contracts.Concepts.XFORM_ID + TYPE_INTEGER + UNIQUE + NOTNULL + COMMA_SEP +
                    Contracts.Concepts.CONCEPT_TYPE + TYPE_TEXT +
                    ")";

    public static final String CONCEPT_NAMES_TABLE_NAME = "concept_names";

    private static final String SQL_CREATE_CONCEPT_NAMES =
            CREATE_TABLE + CONCEPT_NAMES_TABLE_NAME + " (" +
                    _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    Contracts.ConceptNames.CONCEPT_UUID + TYPE_TEXT + COMMA_SEP +
                    Contracts.ConceptNames.LOCALE + TYPE_TEXT + COMMA_SEP +
                    Contracts.ConceptNames.LOCALIZED_NAME + TYPE_TEXT + COMMA_SEP +
                    UNIQUE_INDEX + Contracts.ConceptNames.CONCEPT_UUID + COMMA_SEP +
                    Contracts.ConceptNames.LOCALE +
                    ")" +
                    ")";

    public static final String LOCATIONS_TABLE_NAME = "locations";

    private static final String SQL_CREATE_LOCATIONS =
            CREATE_TABLE + LOCATIONS_TABLE_NAME + " (" +
                _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                Contracts.Locations.LOCATION_UUID + TYPE_TEXT + COMMA_SEP +
                Contracts.Locations.PARENT_UUID + TYPE_TEXT +
                ")";

    public static final String LOCATION_NAMES_TABLE_NAME = "location_names";

    private static final String SQL_CREATE_LOCATION_NAMES =
            CREATE_TABLE + LOCATION_NAMES_TABLE_NAME + " (" +
                _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                Contracts.LocationNames.LOCATION_UUID + TYPE_TEXT + COMMA_SEP +
                Contracts.LocationNames.LOCALE + TYPE_TEXT + COMMA_SEP +
                Contracts.LocationNames.LOCALIZED_NAME + TYPE_TEXT + COMMA_SEP +
                UNIQUE_INDEX + Contracts.LocationNames.LOCATION_UUID + COMMA_SEP + Contracts.LocationNames.LOCALE +
                ")" +
                ")";

    public static final String OBSERVATIONS_TABLE_NAME = "observations";

    private static final String SQL_CREATE_OBSERVATIONS =
            CREATE_TABLE + OBSERVATIONS_TABLE_NAME + " (" +
                    _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    Contracts.Observations.PATIENT_UUID + TYPE_TEXT + COMMA_SEP +
                    Contracts.Observations.ENCOUNTER_UUID + TYPE_TEXT + COMMA_SEP +
                    Contracts.Observations.ENCOUNTER_TIME + TYPE_INTEGER + COMMA_SEP +
                    Contracts.Observations.CONCEPT_UUID + TYPE_INTEGER + COMMA_SEP +
                    Contracts.Observations.VALUE + TYPE_INTEGER + COMMA_SEP +
                    Contracts.Observations.TEMP_CACHE + TYPE_INTEGER + COMMA_SEP + // really boolean
                    UNIQUE_INDEX + Contracts.Observations.PATIENT_UUID + COMMA_SEP +
                    Contracts.Observations.ENCOUNTER_UUID + COMMA_SEP +
                    Contracts.Observations.CONCEPT_UUID + ")" +
                    ")";

    public static final String CHARTS_TABLE_NAME = "charts";

    private static final String SQL_CREATE_CHARTS =
            CREATE_TABLE + CHARTS_TABLE_NAME + " (" +
                    _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    Contracts.Charts.CHART_UUID + TYPE_TEXT + COMMA_SEP +
                    Contracts.Charts.CHART_ROW + TYPE_INTEGER + COMMA_SEP +
                    Contracts.Charts.GROUP_UUID + TYPE_TEXT + COMMA_SEP +
                    Contracts.Charts.CONCEPT_UUID + TYPE_INTEGER + COMMA_SEP +
                    UNIQUE_INDEX + Contracts.Charts.CHART_UUID + COMMA_SEP +
                    Contracts.Charts.CONCEPT_UUID +
                    ")" +
                    ")";

    public static final String USERS_TABLE_NAME = "users";

    private static final String SQL_CREATE_USERS =
            CREATE_TABLE + USERS_TABLE_NAME + " (" +
                    _ID + TYPE_INTEGER + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    Contracts.Users.UUID + TYPE_TEXT + COMMA_SEP +
                    Contracts.Users.FULL_NAME + TYPE_TEXT + ")";

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

    public SQLiteDatabase getReadableDatabase() {
        return super.getReadableDatabase(ENCRYPTION_PASSWORD);
    }

    public SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase(ENCRYPTION_PASSWORD);
    }
}
