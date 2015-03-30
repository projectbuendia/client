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

    private static final String SQL_CREATE_ENTRIES = "create table patients ("
            + "_id text primary key not null,"
            + "given_name text,"
            + "family_name text,"
            + "uuid text,"
            + "location_uuid text,"
            + "admission_timestamp integer,"
            + "birthdate text,"
            + "gender text"
            + ")";

    private static final String SQL_CREATE_CONCEPTS = "create table concepts ("
            + "_id text primary key not null,"
            + "xform_id integer unique not null,"
            + "concept_type text"
            + ")";

    private static final String SQL_CREATE_CONCEPT_NAMES = "create table concept_names ("
            + "_id integer primary key not null,"
            + "concept_uuid text,"
            + "locale text,"
            + "localized_name text,"
            + "unique(concept_uuid, locale)"
            + ")";

    private static final String SQL_CREATE_LOCATIONS = "create table locations ("
            + "_id integer primary key not null,"
            + "location_uuid text,"
            + "parent_uuid text"
            + ")";

    private static final String SQL_CREATE_LOCATION_NAMES = "create table location_names ("
            + "_id integer primary key not null,"
            + "location_uuid text,"
            + "locale text,"
            + "localized_name text,"
            + "unique (location_uuid, locale)"
            + ")";

    private static final String SQL_CREATE_OBSERVATIONS = "create table observations ("
            + "_id integer primary key not null,"
            + "patient_uuid text,"
            + "encounter_uuid text,"
            + "encounter_time integer,"
            + "concept_uuid integer,"
            + "value integer,"
            + "temp_cache integer," // really boolean
            + "unique(patient_uuid, encounter_uuid, concept_uuid)"
            + ")";

    private static final String SQL_CREATE_CHARTS = "create table charts ("
            + "_id integer primary key not null,"
            + "chart_uuid text,"
            + "chart_row integer,"
            + "group_uuid text,"
            + "concept_uuid integer,"
            + "unique(chart_uuid, concept_uuid)"
            + ")";

    private static final String SQL_CREATE_USERS = "create table users ("
            + "_id integer primary key not null,"
            + "uuid text,"
            + "full_name text"
            + ")";

    private static final String SQL_CREATE_MISC = "create table misc ("
            + "_id integer primary key not null,"
            + "full_sync_start_time integer,"
            + "full_sync_end_time integer,"
            + "obs_sync_time integer"
            + ")";

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
        db.execSQL(SQL_CREATE_MISC);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL("drop table if exists patients");
        db.execSQL("drop table if exists observations");
        db.execSQL("drop table if exists concepts");
        db.execSQL("drop table if exists concept_names");
        db.execSQL("drop table if exists charts");
        db.execSQL("drop table if exists locations");
        db.execSQL("drop table if exists location_names");
        db.execSQL("drop table if exists users");
        db.execSQL("drop table if exists misc");
        onCreate(db);
    }

    public SQLiteDatabase getReadableDatabase() {
        return super.getReadableDatabase(ENCRYPTION_PASSWORD);
    }

    public SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase(ENCRYPTION_PASSWORD);
    }
}
