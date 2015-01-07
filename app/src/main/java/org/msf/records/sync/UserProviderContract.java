package org.msf.records.sync;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Columns and other ContentProvider related interface Strings for accessing user data.
 */
public class UserProviderContract {

    /**
     * Collection of static Strings so should not be instantiated.
     */
    private UserProviderContract(){
    }

    /**
     * Base URI. (content://org.msf.records)
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" +
            PatientProviderContract.CONTENT_AUTHORITY);
    /**
     * Path component for the users table.
     */
    static final String PATH_USERS = "users";

    public static final Uri USERS_CONTENT_URI =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_USERS).build();

    /**
     * Columns supported by the various user URIs.
     */
    public static class UserColumns implements BaseColumns {
        /**
         * UUID for a user
         */
        public static final String UUID = "uuid";

        /**
         * User's full name
         */
        public static final String FULL_NAME = "full_name";
    }
}
