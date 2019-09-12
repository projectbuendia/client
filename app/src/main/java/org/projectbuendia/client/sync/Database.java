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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.projectbuendia.client.providers.Contracts.Table;
import org.projectbuendia.client.utils.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Schema definition for the app's database, which contains patient attributes,
 * active patients, locations, and chart information.
 */
public class Database extends SQLiteOpenHelper {

    private static final Logger LOG = Logger.create();

    /** Schema version. */
    public static final int DATABASE_VERSION = 37;

    /** Filename for SQLite file. */
    public static final String DATABASE_FILENAME = "buendia.db";

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
            + "birthdate TEXT,"
            + "sex TEXT,"
            + "pregnancy INTEGER,"
            + "location_uuid TEXT,"
            + "bed_number TEXT"
        );

        SCHEMAS.put(Table.CONCEPTS, ""
            + "uuid TEXT PRIMARY KEY NOT NULL,"
            + "xform_id INTEGER UNIQUE NOT NULL,"
            + "type TEXT,"
            + "name TEXT");

        SCHEMAS.put(Table.FORMS, ""
            + "uuid TEXT PRIMARY KEY NOT NULL,"
            + "name TEXT,"
            + "version TEXT");

        SCHEMAS.put(Table.LOCATIONS, ""
            + "uuid TEXT PRIMARY KEY NOT NULL,"
            + "name TEXT,"
            + "parent_uuid TEXT");

        SCHEMAS.put(Table.OBSERVATIONS, ""
            // uuid intentionally allows null values, because temporary observations inserted
            // locally after submitting a form don't have UUIDs. Note that PRIMARY KEY in SQLite
            // (and many other databases) treats all NULL values as different from all other values,
            // so it's still ok to insert multiple records with a NULL UUID.
            + "uuid TEXT PRIMARY KEY,"
            + "patient_uuid TEXT,"
            + "encounter_uuid TEXT,"
            + "encounter_millis INTEGER,"
            + "concept_uuid TEXT,"
            + "enterer_uuid TEXT,"
            + "value STRING,"
            + "voided INTEGER,"
            + "UNIQUE (patient_uuid, encounter_uuid, concept_uuid)");

        SCHEMAS.put(Table.ORDERS, ""
            + "uuid TEXT PRIMARY KEY NOT NULL,"
            + "patient_uuid TEXT,"
            + "instructions TEXT,"
            + "start_millis INTEGER,"
            + "stop_millis INTEGER");

        // TODO(ping): Store multiple charts, sourced from multiple forms,
        // using a CHARTS table (uuid, name), where order is determined by name.
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

        SCHEMAS.put(Table.BOOKMARKS, ""
            + "table_name TEXT PRIMARY KEY NOT NULL,"
            + "bookmark TEXT NOT NULL");
    }

    public Database(Context context) {
        super(context, DATABASE_FILENAME, null, DATABASE_VERSION);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache of data on the server, so its upgrade
        // policy is to discard all the data and start over.
        clear(db);
    }

    private void clear(SQLiteDatabase db) {
        LOG.i("Dropping all tables");
        for (Table table : Table.values()) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
        onCreate(db);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        LOG.i("Creating tables");
        for (Table table : Table.values()) {
            db.execSQL("CREATE TABLE " + table + " (" + SCHEMAS.get(table) + ");");
        }
    }

    public void clear() {
        // Never call zero-argument clear() from onUpgrade, as getWritableDatabase
        // can trigger onUpgrade, leading to endless recursion.
        clear(getWritableDatabase());
    }
}
