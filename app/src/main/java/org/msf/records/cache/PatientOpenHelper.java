package org.msf.records.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;

import org.msf.records.model.Patient;

/**
 * Created by akalachman on 11/20/14.
 */
public class PatientOpenHelper extends SQLiteOpenHelper {
    // If true, enable client-side caching.
    private static final boolean CACHING_ENABLED = false;

    private static final String TAG = "PatientOpenHelper";
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "cache.db";
    private static final String PATIENT_TABLE_NAME = "patient";
    private static final String KEY_PATIENT_ID = "patient_id";
    private static final String KEY_PATIENT_JSON = "patient_json";
    private static final String DICTIONARY_TABLE_CREATE =
            "CREATE TABLE " + PATIENT_TABLE_NAME + " (" +
                    KEY_PATIENT_ID + " TEXT PRIMARY KEY, " +
                    KEY_PATIENT_JSON + " TEXT);";

    public PatientOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO(akalachman): Implement.
    }

    public Patient getPatient(String patientId) {
        if (!CACHING_ENABLED) {
            return null;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cur = db.rawQuery(
                "SELECT " + KEY_PATIENT_JSON + " FROM " + PATIENT_TABLE_NAME + " WHERE " + KEY_PATIENT_ID + "=?",
                new String[] {patientId}
        );
        cur.moveToNext();
        try {
            String patientJson = cur.getString(cur.getColumnIndex(KEY_PATIENT_JSON));
            return new Gson().fromJson(patientJson, Patient.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public boolean setPatient(String patientId, Patient patient) {
        if (!CACHING_ENABLED) {
            return false;
        }

        String patientJson = new Gson().toJson(patient);

        ContentValues values = new ContentValues();
        values.put(KEY_PATIENT_ID, patientId);
        values.put(KEY_PATIENT_JSON, patientJson);
        SQLiteDatabase db = this.getWritableDatabase();
        long rowId = db.replace(PATIENT_TABLE_NAME, null, values);

        if (rowId == -1) {
            return false;
        }

        return true;
    }
}
