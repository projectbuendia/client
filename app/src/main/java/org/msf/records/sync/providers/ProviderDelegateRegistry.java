package org.msf.records.sync.providers;

import android.content.UriMatcher;
import android.net.Uri;
import android.util.SparseArray;

import net.sqlcipher.database.SQLiteOpenHelper;

import org.msf.records.BuildConfig;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A registry for {@link ProviderDelegate}s.
 */
class ProviderDelegateRegistry<T extends SQLiteOpenHelper> {

    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.records";

    private final UriMatcher mUriMatcher;
    private final AtomicInteger mCodeGenerator;

    private final SparseArray<ProviderDelegate<T>> mDelegates;

    ProviderDelegateRegistry() {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mCodeGenerator = new AtomicInteger(1);
        mDelegates = new SparseArray<>();
    }

    /**
     * Registers the specified delegate to handle the specified path.
     *
     * @throws IllegalStateException if the specified path is already being handled by another
     *                               delegate
     */
    void registerDelegate(String path, ProviderDelegate<T> delegate) {
        int existingCode =
                mUriMatcher.match(Uri.parse("content://" + CONTENT_AUTHORITY + "/" + path));
        if (existingCode != UriMatcher.NO_MATCH) {
            throw new IllegalStateException(
                    "Path '" + path + "' is already registered to be handled by '"
                    + mDelegates.get(existingCode).toString() + "'.");
        }
        int code = mCodeGenerator.getAndIncrement();
        mUriMatcher.addURI(CONTENT_AUTHORITY, path, code);
        mDelegates.put(code, delegate);
    }

    /**
     * Returns the {@link ProviderDelegate} for the specified {@link Uri}.
     *
     * @throws IllegalArgumentException if no matching delegate is registered
     */
    ProviderDelegate<T> getDelegate(Uri uri) {
        int code = mUriMatcher.match(uri);
        if (code == UriMatcher.NO_MATCH) {
            throw new IllegalArgumentException(
                    "No SubContentProvider registered for URI '" + uri.toString() + "'.");
        }

        return mDelegates.get(code);
    }
}
