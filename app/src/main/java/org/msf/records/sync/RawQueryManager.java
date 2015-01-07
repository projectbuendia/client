package org.msf.records.sync;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.LruCache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * An object that provides access to raw queries stored as application assets.
 */
public class RawQueryManager {

    public enum Name {

        LOCALIZED_LOCATIONS("localized-locations.sql");

        public final String fileName;

        private Name(String fileName) {
            this.fileName = fileName;
        }
    }

    private final AssetManager mAssetManager;

    private final LruCache<Name, String> mCache;

    public RawQueryManager(Context context) {
        mAssetManager = context.getAssets();
        mCache = new LruCache<>(32);
    }

    /**
     * Returns the raw query for the specified query name.
     */
    public String getRawQuery(Name name) {
        synchronized (mCache) {
            String statement = mCache.get(name);
            if (statement == null) {
                try {
                    InputStream stream = mAssetManager.open(name.fileName);

                    // Read to the end of the stream.
                    Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
                    statement = scanner.hasNext() ? scanner.next() : "";

                    mCache.put(name, statement);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read query with name '" + name + "'.", e);
                }
            }

            return statement;
        }
    }
}
