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

package org.projectbuendia.client.net;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.common.base.Preconditions;

import java.io.File;

/** A wrapper around basic ODK database operations. */
public class OdkDatabase {
    private static final String TAG = OdkDatabase.class.getSimpleName();

    /**
     * Retrieves the form id from the ODK database referred to by the given File handle.
     * @param path the form File
     * @return the corresponding form id from the ODK database, or -1 if no match was found.
     */
    public static long getFormIdForPath(File path) {
        long formId = -1;
        Cursor cursor = null;
        try {
            cursor = OdkXformSyncTask.getCursorForFormFile(
                path, new String[] {
                    BaseColumns._ID
                });
            //if any form was found by the given file.
            if(cursor.getCount() >= 1) {
                // There should only ever be one form per UUID. But if something goes wrong, we want
                // the app to keep working. Assume the latest one is correct.
                if (cursor.getCount() > 1) {
                    Log.e(TAG, "More than one form in database with the same id. This indicates an "
                        + "error occurred on insert (probably a race condition) and should be "
                        + "fixed. However, the app should still function correctly");
                }
                // getCursorForFormFile returns the most recent element first, so we can just use
                // the first one.
                cursor.moveToNext();
                formId = cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return formId;
    }
}
