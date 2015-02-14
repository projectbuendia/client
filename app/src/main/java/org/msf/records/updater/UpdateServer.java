package org.msf.records.updater;

import com.android.volley.Response;

import org.msf.records.model.UpdateInfo;
import org.msf.records.net.GsonRequest;
import org.msf.records.net.VolleySingleton;
import org.msf.records.prefs.StringPreference;

import java.util.List;

/**
 * An object that talks to the update server.
 */
public class UpdateServer {

    /**
     * The package server's module name for updates to this app.  A name of "foo"
     * means the updates are named "foo-1.2.apk", "foo-1.3.apk", etc. and their
     * index is available at "foo.json".
     */
    private static final String MODULE_NAME = "buendia-client";

    private final VolleySingleton mVolley;
    private final StringPreference mRootUrl;

    public UpdateServer(VolleySingleton volley, StringPreference rootUrl) {
        mVolley = volley;
        mRootUrl = rootUrl;
    }

    /** Returns the package server root URL preference, with trailing slashes removed. */
    public String getRootUrl() {
        return mRootUrl.get().replaceAll("/*$", "");
    }

    /**
     * Asynchronously issues a request to get the Android update info.
     *
     * @param listener the callback to be invoked if the request succeeds
     * @param errorListener the callback to be invoked if the request fails
     */
    public void getAndroidUpdateInfo(
            Response.Listener<List<UpdateInfo>> listener,
            Response.ErrorListener errorListener) {
        mVolley.addToRequestQueue(
                GsonRequest.withArrayResponse(
                        getRootUrl() + "/" + MODULE_NAME + ".json",
                        UpdateInfo.class,
                        null /*headers*/,
                        listener,
                        errorListener
                ));
    }
}
