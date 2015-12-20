// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.sync;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SQLiteOpenHelper;

import org.projectbuendia.client.BuildConfig;
import org.projectbuendia.client.providers.Contracts.Table;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Schema definition for the app's database, which contains patient attributes,
 * active patients, locations, and chart information.
 */
public class Database extends SQLiteOpenHelper {

    /** Schema version. */
    public static final int DATABASE_VERSION = 29;

    /** Filename for SQLite file. */
    public static final String DATABASE_FILENAME = "buendia.db";

    File file;

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
     * TODO/security: add something better. At the very minimum a server call and local storage
     * with expiry so that it has to sync to the server every so often. Even better some sort of
     * public key based scheme to only deliver the key on login with registered user on good device.
     */
    private static final String ENCRYPTION_PASSWORD = BuildConfig.ENCRYPTION_PASSWORD;

    /**
     * A map of SQL table schemas, with one entry per table.  The values should
     * be strings that take the place of X in a "CREATE TABLE foo (X)" statement.
     */
    static final Map<Table, String> SCHEMAS = new HashMap<>();

    // For descriptions of these tables and the meanings of their columns, see Contracts.java.
    static {
        SCHEMAS.put(Table.PATIENTS, ""
            + "uuid TEXT PRIMARY KEY NOT NULL,"
            + "id TEXT,"
            + "given_name TEXT,"
            + "family_name TEXT,"
            + "location_uuid TEXT,"
            + "birthdate TEXT,"
            + "gender TEXT");

        SCHEMAS.put(Table.CONCEPTS, ""
            + "uuid TEXT PRIMARY KEY NOT NULL,"
            + "xform_id INTEGER UNIQUE NOT NULL,"
            + "concept_type TEXT");

        SCHEMAS.put(Table.CONCEPT_NAMES, ""
            + "concept_uuid TEXT,"
            + "locale TEXT,"
            + "name TEXT,"
            + "UNIQUE (concept_uuid, locale)");

        SCHEMAS.put(Table.FORMS, ""
            + "uuid TEXT PRIMARY KEY NOT NULL,"
            + "name TEXT,"
            + "version TEXT");

        SCHEMAS.put(Table.LOCATIONS, ""
            + "uuid TEXT PRIMARY KEY NOT NULL,"
            + "parent_uuid TEXT");

        SCHEMAS.put(Table.LOCATION_NAMES, ""
            + "location_uuid TEXT,"
            + "locale TEXT,"
            + "name TEXT,"
            + "UNIQUE (location_uuid, locale)");

        SCHEMAS.put(Table.OBSERVATIONS, ""
            // uuid intentionally allows null values, because temporary observations inserted
            // locally after submitting a form don't have UUIDs. Note that PRIMARY KEY in SQLite
            // (and many other databases) treats all NULL values as different from all other values,
            // so it's still ok to insert multiple records with a NULL UUID.
            + "uuid TEXT PRIMARY KEY,"
            + "patient_uuid TEXT,"
            + "encounter_uuid TEXT,"
            + "encounter_millis INTEGER,"
            + "concept_uuid INTEGER,"
            + "value STRING,"
            + "UNIQUE (patient_uuid, encounter_uuid, concept_uuid)");

        SCHEMAS.put(Table.ORDERS, ""
            + "uuid TEXT PRIMARY KEY NOT NULL,"
            + "patient_uuid TEXT,"
            + "instructions TEXT,"
            + "start_millis INTEGER,"
            + "stop_millis INTEGER");

        SCHEMAS.put(Table.CHART_ITEMS, ""
            + "rowid INTEGER PRIMARY KEY NOT NULL,"
            + "chart_uuid TEXT,"
            + "weight INTEGER,"
            + "section_type TEXT,"
            + "parent_rowid INTEGER,"
            + "label TEXT,"
            + "type TEXT,"
            + "required INTEGER,"
            + "concept_uuids TEXT,"
            + "format TEXT,"
            + "caption_format TEXT,"
            + "css_class TEXT,"
            + "css_style TEXT,"
            + "script TEXT");

        SCHEMAS.put(Table.USERS, ""
            + "uuid TEXT PRIMARY KEY NOT NULL,"
            + "full_name TEXT");

        // TODO/cleanup: Store miscellaneous values in the "misc" table as rows with a key column
        // and a value column, not all values in one row with an ever-growing number of columns.
        SCHEMAS.put(Table.MISC, ""
            + "full_sync_start_millis INTEGER,"
            + "full_sync_end_millis INTEGER");

        SCHEMAS.put(Table.SYNC_TOKENS, ""
            + "table_name TEXT PRIMARY KEY NOT NULL,"
            + "sync_token TEXT NOT NULL");

        SCHEMAS.put(Table.UNSYNC_TOKENS, ""
            + "uuid TEXT PRIMARY KEY NOT NULL,"
            + "patient_uuid TEXT,"
            + "xml");

    }

    public Database(Context context) {
        super(context, DATABASE_FILENAME, null, DATABASE_VERSION);
        file = context.getDatabasePath(DATABASE_FILENAME);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache of data on the server, so its upgrade
        // policy is to discard all the data and start over.
        clear(db);
    }

    public void clear(SQLiteDatabase db) {
        for (Table table : Table.values()) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
        onCreate(db);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        for (Table table : Table.values()) {
            db.execSQL("CREATE TABLE " + table + " (" + SCHEMAS.get(table) + ");");
        }
    }

    public void clear() {
        // Never call zero-argument clear() from onUpgrade, as getWritableDatabase
        // can trigger onUpgrade, leading to endless recursion.
        clear(getWritableDatabase());
    }

    private void deleteDatabaseIfPasswordIncorrect() {
        try {
            getWritableDatabase(ENCRYPTION_PASSWORD);
        } catch (SQLiteException e) {
            if (e.getMessage().contains("encrypt")) {
                // Incorrect or missing encryption password; delete the database and start over.
                file.delete();
            }
        }
    }

    public SQLiteDatabase getWritableDatabase() {
        deleteDatabaseIfPasswordIncorrect();
        return getWritableDatabase(ENCRYPTION_PASSWORD);
    }

    public SQLiteDatabase getReadableDatabase() {
        deleteDatabaseIfPasswordIncorrect();
        return getReadableDatabase(ENCRYPTION_PASSWORD);
    }
}
