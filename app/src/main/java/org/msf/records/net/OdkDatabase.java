package org.msf.records.net;

import android.database.Cursor;
import android.provider.BaseColumns;

import com.google.common.base.Preconditions;

import java.io.File;

/**
 * OdkDatabase is a wrapper around basic ODK database operations.
 */
public class OdkDatabase {
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
                    path, new String[]{
                            BaseColumns._ID
                    });
            Preconditions.checkArgument(cursor.getCount() == 1);
            Preconditions.checkArgument(cursor.getColumnCount() == 1);
            cursor.moveToNext();
            formId = cursor.getLong(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return formId;
    }
}
