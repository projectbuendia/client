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

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.circle.android.api.OkHttpStack;
import com.facebook.stetho.okhttp.StethoInterceptor;
import com.squareup.okhttp.OkHttpClient;

/** Wraps Volley up in a singleton object. */
public class VolleySingleton {
    private static VolleySingleton sInstance;

    private final RequestQueue mRequestQueue;

    /**
     * Get the VolleySingleton instance for doing multiple operations on a single context.
     * In general prefer convenience methods unless doing multiple operations.
     * @param context the Android Application context
     * @return the Singleton for accessing Volley.
     */
    public static synchronized VolleySingleton getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new VolleySingleton(context);
        }
        return sInstance;
    }

    /**
     * A convenience method for adding a request to the Volley request queue getting all singleton
     * handling and contexts correct.
     */
    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    private VolleySingleton(Context context) {
        // Let Stetho inspect all our network requests.
        final OkHttpClient client = new OkHttpClient();
        client.networkInterceptors().add(new StethoInterceptor());

        // getApplicationContext() is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        mRequestQueue = Volley.newRequestQueue(
            context.getApplicationContext(), new OkHttpStack(client));
    }
}
