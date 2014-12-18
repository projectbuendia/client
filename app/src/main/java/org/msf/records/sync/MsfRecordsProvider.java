package org.msf.records.sync;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import static org.msf.records.sync.PatientProviderContract.CONTENT_AUTHORITY;

/**
 * A parent ContentProvider for all data for this app. Implementations of subparts are handled accessing active patients and their attributes.
 */
public class MsfRecordsProvider extends ContentProvider {

    /**
     * Android limits us to one content provider per Authority. This interface enables us to
     * multiplex content provider queries into subclasses for easy handling.
     */
    public interface SubContentProvider {
        /**
         * Get the paths (in UriMatcher format) that this content provider handles.
         */
        public String [] getPaths();

        /**
         * @see android.content.ContentProvider#getType(android.net.Uri)
         */
        public String getType(Uri uri);

        /**
         * @see android.content.ContentProvider#query(android.net.Uri, String[], String, String[], String)
         */
        public Cursor query(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                            String[] projection,
                            String selection, String[] selectionArgs,
                            String sortOrder);

        /**
         * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
         */
        public Uri insert(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                          ContentValues values);

        public int bulkInsert(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                              ContentValues[] values);

            /**
             * @see android.content.ContentProvider#delete(android.net.Uri, String, String[])
             */
        public int delete(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                          String selection, String[] selectionArgs);

        /**
         * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, String, String[])
         */
        public int update(PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
                          ContentValues values, String selection, String[] selectionArgs);
    }

    PatientDatabase mDatabaseHelper;

    private static final SubContentProvider [] SUB_PROVIDERS = new SubContentProvider[] {
            new PatientProvider(), new ChartProvider(), new LocationProvider(),
            new UserProvider()
    };
    /**
     * UriMatcher, used to decode incoming URIs.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        for (int i=0; i<SUB_PROVIDERS.length; i++) {
            for (String path : SUB_PROVIDERS[i].getPaths()) {
                sUriMatcher.addURI(CONTENT_AUTHORITY, path, i);
            }
        }
    }

    private static SubContentProvider getSubProvider(Uri uri) {
        int index = sUriMatcher.match(uri);
        if (index == -1) {
            throw new UnsupportedOperationException("" + uri);
        }
        return SUB_PROVIDERS[index];
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new PatientDatabase(getContext());
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public String getType(Uri uri) {
        return getSubProvider(uri).getType(uri);
    }

    /**
     * Performs database query
     * @param uri supporting /patients where it returns all the patients
     *        or /patient/{ID} where selects a particular patient
     * @return results
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SubContentProvider subProvider = getSubProvider(uri);
        Context ctx = getContext();
        assert ctx != null;
        return subProvider.query(mDatabaseHelper, ctx.getContentResolver(), uri, projection,
                selection, selectionArgs, sortOrder);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SubContentProvider subProvider = getSubProvider(uri);
        Context ctx = getContext();
        assert ctx != null;
        return subProvider.insert(mDatabaseHelper, ctx.getContentResolver(), uri, values);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SubContentProvider subProvider = getSubProvider(uri);
        Context ctx = getContext();
        assert ctx != null;
        return subProvider.bulkInsert(mDatabaseHelper, ctx.getContentResolver(), uri, values);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SubContentProvider subProvider = getSubProvider(uri);
        Context ctx = getContext();
        assert ctx != null;
        return subProvider.delete(mDatabaseHelper, ctx.getContentResolver(), uri,
                selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SubContentProvider subProvider = getSubProvider(uri);
        Context ctx = getContext();
        assert ctx != null;
        return subProvider.update(mDatabaseHelper, ctx.getContentResolver(), uri, values, selection,
                selectionArgs);
    }
}
