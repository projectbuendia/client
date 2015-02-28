package org.msf.records.sync.providers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import net.sqlcipher.database.SQLiteOpenHelper;

/**
 * A {@link ContentProvider} that delegates responsibility to {@link ProviderDelegate}s.
 */
abstract class DelegatingProvider<T extends SQLiteOpenHelper> extends ContentProvider {

    private ProviderDelegateRegistry<T> mRegistry;
    protected ContentResolver mContentResolver;
    protected T mDatabaseHelper;

    @Override
    public boolean onCreate() {
        mRegistry = getRegistry();
        mDatabaseHelper = getDatabaseHelper();
        mContentResolver = getContext().getContentResolver();

        return true;
    }

    @Override
    public String getType(Uri uri) {
        return mRegistry.getDelegate(uri).getType();
    }

    @Override
    public Cursor query(
            Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return mRegistry.getDelegate(uri)
                .query(
                        mDatabaseHelper, mContentResolver, uri, projection, selection,
                        selectionArgs, sortOrder);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return mRegistry.getDelegate(uri)
                .insert(mDatabaseHelper, mContentResolver, uri, values);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        return mRegistry.getDelegate(uri)
                .bulkInsert(mDatabaseHelper, mContentResolver, uri, values);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return mRegistry.getDelegate(uri)
                .delete(mDatabaseHelper, mContentResolver, uri, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return mRegistry.getDelegate(uri)
                .update(
                        mDatabaseHelper, mContentResolver, uri, values, selection, selectionArgs);
    }

    protected abstract T getDatabaseHelper();

    protected abstract ProviderDelegateRegistry<T> getRegistry();
}
