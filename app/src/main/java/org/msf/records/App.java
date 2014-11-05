package org.msf.records;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import org.msf.records.net.BuendiaServer;
import org.msf.records.net.OpenMrsServer;
import org.msf.records.net.Server;
import org.msf.records.utils.LruBitmapCache;

/**
 * Created by Gil on 08/10/2014.
 */
public class App extends Application {

    private static Server mServer;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        synchronized (App.class) {
            if (preferences.getBoolean("use_openmrs", false)) {
                String openmrsRootUrl = preferences.getString("openmrs_root_url", null);
                mServer = new OpenMrsServer(getApplicationContext(), openmrsRootUrl);
            } else {
                String apiRootUrl = preferences.getString("api_root_url", null);
                mServer = new BuendiaServer(getApplicationContext(), apiRootUrl);
            }
        }
    }

    public static synchronized Server getServer() {
        return mServer;
    }
}
