package org.msf.records;

import android.app.Application;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import org.msf.records.utils.LruBitmapCache;

/**
 * Created by Gil on 08/10/2014.
 */
public class App extends Application {
    public static final String TAG = App.class.getSimpleName();

    public static String API_ROOT_URL = "http://buendia.whitespell.com:8080/";

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    private static App mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        API_ROOT_URL = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString("api_root_url", "");
    }

    public static synchronized App getInstance() {
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }

    public ImageLoader getImageLoader() {
        getRequestQueue();
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(this.mRequestQueue,
                    new LruBitmapCache());
        }
        return this.mImageLoader;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }
}
