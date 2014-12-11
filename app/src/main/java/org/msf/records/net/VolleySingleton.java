package org.msf.records.net;

import android.content.Context;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Wrap Volley up in a Singleton object.
 */
public class VolleySingleton {

    private static final String TAG = VolleySingleton.class.getSimpleName();

    private static VolleySingleton mInstance;
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
        if (mInstance == null) {
            mInstance = new VolleySingleton(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    /**
     * A convenience method for adding a request to the Volley request queue getting all singleton
     * handling and contexts correct.
     */
    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }
}
