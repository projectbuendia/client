package org.msf.records.net;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.msf.records.utils.Logger;

/**
 * Wraps Volley up in a singleton object.
 */
public class VolleySingleton {

    private static final Logger LOG = Logger.create();
    private static VolleySingleton sInstance;
    private final RequestQueue mRequestQueue;

    private VolleySingleton(Context context) {
        // getApplicationContext() is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    /**
     * Get the VolleySingleton instance for doing multiple operations on a single context.
     * In general prefer convenience methods unless doing multiple operations.
     *
     * @param context the Android Application context
     * @return the Singleton for accessing Volley.
     */
    public static synchronized VolleySingleton getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new VolleySingleton(context);
        }
        return sInstance;
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    /**
     * A convenience method for adding a request to the Volley request queue getting all singleton
     * handling and contexts correct.
     */
    public <T> void addToRequestQueue(Request<T> req) {
        LOG.i("queueing request: " + req.getMethod() + " " + req.getUrl());
        getRequestQueue().add(req);
    }
}
