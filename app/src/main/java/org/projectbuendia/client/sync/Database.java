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
import net.sqlcipher.database.SQLiteOpenHelper;

import org.projectbuendia.client.sync.providers.Contracts.Tables;

import java.util.HashMap;
import java.util.Map;

/**
 * Schema definition for the app's database, which contains patient attributes,
 * active patients, locations, and chart information.
 */
public class Database extends SQLiteOpenHelper {

    /** Schema version. */
    public static final int DATABASE_VERSION = 17;

    /** Filename for SQLite file. */
    public static final String DATABASE_NAME = "buendia.db";

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

    /**
     * A map of SQL table schemas, with one entry per table.  The values should
     * be strings that take the place of X in a "CREATE TABLE foo (X)" statement.
     */
    static final Map<String, String> SCHEMAS = new HashMap();
    static {
        SCHEMAS.put(Tables.PATIENTS, ""
                + "_id TEXT PRIMARY KEY NOT NULL,"
                + "given_name TEXT,"
                + "family_name TEXT,"
                + "uuid TEXT,"
                + "location_uuid TEXT,"
                + "birthdate TEXT,"
                + "gender TEXT");

        SCHEMAS.put(Tables.CONCEPTS, ""
                + "_id TEXT PRIMARY KEY NOT NULL,"
                + "xform_id INTEGER UNIQUE NOT NULL,"
                + "concept_type TEXT");

        SCHEMAS.put(Tables.CONCEPT_NAMES, ""
                + "_id INTEGER PRIMARY KEY NOT NULL,"
                + "concept_uuid TEXT,"
                + "locale TEXT,"
                + "name TEXT,"
                + "UNIQUE (concept_uuid, locale)");

        SCHEMAS.put(Tables.LOCATIONS, ""
                + "_id INTEGER PRIMARY KEY NOT NULL,"
                + "location_uuid TEXT,"
                + "parent_uuid TEXT");

        SCHEMAS.put(Tables.LOCATION_NAMES, ""
                + "_id INTEGER PRIMARY KEY NOT NULL,"
                + "location_uuid TEXT,"
                + "locale TEXT,"
                + "name TEXT,"
                + "UNIQUE (location_uuid, locale)");

        SCHEMAS.put(Tables.OBSERVATIONS, ""
                + "_id INTEGER PRIMARY KEY NOT NULL,"
                + "patient_uuid TEXT,"
                + "encounter_uuid TEXT,"
                + "encounter_time INTEGER,"
                + "concept_uuid INTEGER,"
                + "value STRING,"
                + "temp_cache INTEGER," // really boolean
                + "UNIQUE (patient_uuid, encounter_uuid, concept_uuid)");

        SCHEMAS.put(Tables.ORDERS, ""
                + "_id INTEGER PRIMARY KEY NOT NULL,"
                + "uuid TEXT,"
                + "patient_uuid TEXT,"
                + "instructions TEXT,"
                + "start_time INTEGER,"
                + "stop_time INTEGER");

        SCHEMAS.put(Tables.CHARTS, ""
                + "_id INTEGER PRIMARY KEY NOT NULL,"
                + "chart_uuid TEXT,"
                + "chart_row INTEGER,"
                + "group_uuid TEXT,"
                + "concept_uuid INTEGER,"
                + "UNIQUE (chart_uuid, concept_uuid)");

        SCHEMAS.put(Tables.USERS, ""
                + "_id INTEGER PRIMARY KEY NOT NULL,"
                + "uuid TEXT,"
                + "full_name TEXT");

        SCHEMAS.put(Tables.MISC, ""
                + "_id INTEGER PRIMARY KEY NOT NULL,"
                + "full_sync_start_time INTEGER,"
                + "full_sync_end_time INTEGER,"
                + "obs_sync_time INTEGER");
    }

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (String table : Tables.ALL) {
            db.execSQL("CREATE TABLE " + table + " (" + SCHEMAS.get(table) + ");");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache of data on the server, so its upgrade
        // policy is to discard all the data and start over.
        for (String table : Tables.ALL) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
        onCreate(db);
    }

    public SQLiteDatabase getReadableDatabase() {
        return super.getReadableDatabase(ENCRYPTION_PASSWORD);
    }

    public SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase(ENCRYPTION_PASSWORD);
    }
}
