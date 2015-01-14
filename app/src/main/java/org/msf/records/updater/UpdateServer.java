package org.msf.records.updater;

import android.support.annotation.Nullable;

import com.android.volley.Response;

import org.msf.records.model.UpdateInfo;
import org.msf.records.net.GsonRequest;
import org.msf.records.net.VolleySingleton;

import java.util.List;

/**
 * An object that talks to the update server.
 */
public class UpdateServer {

    public static final String ROOT_URL = "http://packages.projectbuendia.org/";

    private final VolleySingleton mVolley;
    private final String mRootUrl;

    public UpdateServer(VolleySingleton volley, @Nullable String rootUrl) {
        mVolley = volley;
        mRootUrl = rootUrl == null ? ROOT_URL : rootUrl;
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
                        mRootUrl + "android-client.json",
                        UpdateInfo.class,
                        null /*headers*/,
                        listener,
                        errorListener
                ));
    }
}
