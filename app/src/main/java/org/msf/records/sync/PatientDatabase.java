package org.msf.records.sync;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.provider.BaseColumns._ID;
import static org.msf.records.sync.PatientProviderContract.PatientMeta.COLUMN_NAME_ADMISSION_TIMESTAMP;
import static org.msf.records.sync.PatientProviderContract.PatientMeta.COLUMN_NAME_FAMILY_NAME;
import static org.msf.records.sync.PatientProviderContract.PatientMeta.COLUMN_NAME_GIVEN_NAME;
import static org.msf.records.sync.PatientProviderContract.PatientMeta.COLUMN_NAME_LOCATION_ZONE;
import static org.msf.records.sync.PatientProviderContract.PatientMeta.COLUMN_NAME_STATUS;
import static org.msf.records.sync.PatientProviderContract.PatientMeta.COLUMN_NAME_UUID;

/**
 * A helper for the database for storing patient attributes, active patients, and chart information.
 * Stored in the same database as patients are the keys for charts too.
 */
public class PatientDatabase extends SQLiteOpenHelper {

    /** Schema version. */
    public static final int DATABASE_VERSION = 3;
    /** Filename for SQLite file. */
    public static final String DATABASE_NAME = "patients.db";

    private static final String PRIMARY_KEY = " PRIMARY KEY";
    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_INTEGER = " INTEGER";
    private static final String NOTNULL = "  NOT NULL";
    private static final String COMMA_SEP = ",";

    /** SQL statement to create "patient" table. */
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + PatientProviderContract.PatientMeta.TABLE_NAME + " (" +
                    _ID + TYPE_TEXT + PRIMARY_KEY + NOTNULL + COMMA_SEP +
                    COLUMN_NAME_GIVEN_NAME + TYPE_TEXT + COMMA_SEP +
                    COLUMN_NAME_FAMILY_NAME + TYPE_TEXT + COMMA_SEP +
                    COLUMN_NAME_STATUS + TYPE_TEXT + COMMA_SEP +
                    COLUMN_NAME_UUID + TYPE_TEXT + COMMA_SEP +
                    COLUMN_NAME_LOCATION_ZONE + TYPE_TEXT + COMMA_SEP +
                    COLUMN_NAME_ADMISSION_TIMESTAMP + TYPE_INTEGER + ")";

    /** SQL statement to drop "patient" table. */
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + PatientProviderContract.PatientMeta.TABLE_NAME;


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
