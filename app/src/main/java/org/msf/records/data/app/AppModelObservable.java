package org.msf.records.data.app;

import android.database.ContentObserver;

/**
 * An interface for app model types on which {@link ContentObserver}s can be registered.
 *
 * <p>At present, the only types that can be observed are group or aggregate types; individual model
 * classes cannot be observed.
 */
public interface AppModelObservable {

    void registerContentObserver(ContentObserver observer);

    void unregisterContentObserver(ContentObserver observer);

    /**
     * Closes the the object and any backing types.
     *
     * <p>Subsequent calls to accessor methods may return dummy values or throw exceptions.
     */
    void close();
}
