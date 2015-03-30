package org.msf.records.sync;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

/**
 * Schema definition for the app's database, which contains patient attributes,
 * active patients, locations, and chart information.
 */
public class PatientDatabase extends SQLiteOpenHelper {

    /** Schema version. */
    public static final int DATABASE_VERSION = 15;

    /** Filename for SQLite file. */
    public static final String DATABASE_NAME = "patientscipher.db";

    /*
     * This deserves a brief comment on security. Patient data encrypted by a hardcoded key
     * might seem like security by obscurity. It is.
     *
     * Security of patient data for these apps is established by physical security for the tablets
     * and server, not software security and encryption. The software is designed to be used in
     * high risk zones while wearing PPE. Thus it deliberately does not have barriers to usability
     * like passwords or lock screens. Without these it is hard to implement a secure encryption
     * scheme, and so we haven't. All patient data is viewable in the app anyway.
     *
     * So why bother using SQL cipher? The major reason is as groundwork. Eventually we would like
     * to add some better security. To do this we need to make sure all code we write is compatible
     * with an encrypted database.
     *
     * However, there is some value now. The presumed attacker is someone who doesn't care very much
     * about patient data, but has broken into an Ebola Management Centre to steal the tablet to
     * re-sell. We would rather the patient data wasn't trivially readable using existing public
     * tools, so there is slight defense in depth. Firstly, as the data is stored in per-app storage
     * the device would need to be rooted, or adb used, to get access to the data. Encryption adds
     * a second layer of security, in that once they have access to the database file, it isn't
     * readable without the key. Of course as the key is in plaintext in the open source, anyone
     * technically savvy enough to use adb can almost certainly find it, but at least it isn't as
     * simple as using grep or strings.
     *
     * TODO: add something better. At the very minimum a server call and local storage
     * with expiry so that it has to sync to the server every so often. Even better some sort of
     * public key based scheme to only deliver the key on login with registered user on good device.
     *
     * A final note - we know the quote should be brillig, not brilling. No need to correct it.
     */
    private static final String ENCRYPTION_PASSWORD = "Twas brilling and the slithy toves";

    public static final String PATIENTS_TABLE = "patients";
    private static final String SQL_CREATE_PATIENTS = ""
            + " CREATE TABLE patients ("
            + "     _id TEXT PRIMARY KEY NOT NULL,"
            + "     given_name TEXT,"
            + "     family_name TEXT,"
            + "     uuid TEXT,"
            + "     location_uuid TEXT,"
            + "     admission_timestamp INTEGER,"
            + "     birthdate TEXT,"
            + "     gender TEXT"
            + " )";

    public static final String CONCEPTS_TABLE = "concepts";
    private static final String SQL_CREATE_CONCEPTS = ""
            + " CREATE TABLE concepts ("
            + "     _id TEXT PRIMARY KEY NOT NULL,"
            + "     xform_id INTEGER UNIQUE NOT NULL,"
            + "     concept_type TEXT"
            + " )";

    public static final String CONCEPT_NAMES_TABLE = "concept_names";
    private static final String SQL_CREATE_CONCEPT_NAMES = ""
            + " CREATE TABLE concept_names ("
            + "     _id INTEGER PRIMARY KEY NOT NULL,"
            + "     concept_uuid TEXT,"
            + "     locale TEXT,"
            + "     localized_name TEXT,"
            + "     UNIQUE (concept_uuid, locale)"
            + " )";

    public static final String LOCATIONS_TABLE = "locations";
    private static final String SQL_CREATE_LOCATIONS = ""
            + " CREATE TABLE locations ("
            + "     _id INTEGER PRIMARY KEY NOT NULL,"
            + "     location_uuid TEXT,"
            + "     parent_uuid TEXT"
            + " )";

    public static final String LOCATION_NAMES_TABLE = "location_names";
    private static final String SQL_CREATE_LOCATION_NAMES = ""
            + " CREATE TABLE location_names ("
            + "     _id INTEGER PRIMARY KEY NOT NULL,"
            + "     location_uuid TEXT,"
            + "     locale TEXT,"
            + "     localized_name TEXT,"
            + "     UNIQUE (location_uuid, locale)"
            + " )";

    public static final String OBSERVATIONS_TABLE = "observations";
    private static final String SQL_CREATE_OBSERVATIONS = ""
            + " CREATE TABLE observations ("
            + "     _id INTEGER PRIMARY KEY NOT NULL,"
            + "     patient_uuid TEXT,"
            + "     encounter_uuid TEXT,"
            + "     encounter_time INTEGER,"
            + "     concept_uuid INTEGER,"
            + "     value INTEGER,"
            + "     temp_cache INTEGER," // really boolean
            + "     UNIQUE (patient_uuid, encounter_uuid, concept_uuid)"
            + " )";

    public static final String CHARTS_TABLE = "charts";
    private static final String SQL_CREATE_CHARTS = ""
            + " CREATE TABLE charts ("
            + "     _id INTEGER PRIMARY KEY NOT NULL,"
            + "     chart_uuid TEXT,"
            + "     chart_row INTEGER,"
            + "     group_uuid TEXT,"
            + "     concept_uuid INTEGER,"
            + "     UNIQUE (chart_uuid, concept_uuid)"
            + " )";

    public static final String USERS_TABLE = "users";
    private static final String SQL_CREATE_USERS = ""
            + " CREATE TABLE users ("
            + "     _id INTEGER PRIMARY KEY NOT NULL,"
            + "     uuid TEXT,"
            + "     full_name TEXT"
            + " )";

    public static final String MISC_TABLE = "misc";
    private static final String SQL_CREATE_MISC = ""
            + " CREATE TABLE misc ("
            + "     _id INTEGER PRIMARY KEY NOT NULL,"
            + "     full_sync_start_time INTEGER,"
            + "     full_sync_end_time INTEGER,"
            + "     obs_sync_time INTEGER"
            + " )";

    public PatientDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PATIENTS);
        db.execSQL(SQL_CREATE_CONCEPTS);
        db.execSQL(SQL_CREATE_CONCEPT_NAMES);
        db.execSQL(SQL_CREATE_LOCATIONS);
        db.execSQL(SQL_CREATE_LOCATION_NAMES);
        db.execSQL(SQL_CREATE_OBSERVATIONS);
        db.execSQL(SQL_CREATE_CHARTS);
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_MISC);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL("DROP TABLE IF EXISTS " + PATIENTS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CONCEPTS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CONCEPT_NAMES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LOCATIONS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LOCATION_NAMES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + OBSERVATIONS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CHARTS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + USERS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + MISC_TABLE);
        onCreate(db);
    }

    public SQLiteDatabase getReadableDatabase() {
        return super.getReadableDatabase(ENCRYPTION_PASSWORD);
    }

    public SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase(ENCRYPTION_PASSWORD);
    }
}
