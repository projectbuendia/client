package org.msf.records.updater;

import android.support.annotation.Nullable;

import org.msf.records.model.UpdateInfo;
import org.msf.records.net.GsonRequest;
import org.msf.records.net.VolleySingleton;

import com.android.volley.Response;

/**
 * An object that talks to the update server.
 */
public class UpdateServer {

    public static final String ROOT_URL = "http://buendia.whitespell.com:8080/";

    private final VolleySingleton mVolley;
    private final String mRootUrl;

    public UpdateServer(VolleySingleton volley, @Nullable String rootUrl) {
        mVolley = volley;
        mRootUrl = rootUrl == null ? ROOT_URL : rootUrl;
    }

    /**
     * Asynchronously issues a request to get the Android update info
     *
     * @param listener the callback to be invoked if the request succeeds
     * @param errorListener the callback to be invoked if the request fails
     * @param tag a string that can be used to cancel this request or {@code null} to use the
     *            default tag
     */
    public void getAndroidUpdateInfo(
            Response.Listener<UpdateInfo> listener,
            Response.ErrorListener errorListener,
            @Nullable String tag) {
        mVolley.addToRequestQueue(
                new GsonRequest<UpdateInfo>(
                        mRootUrl + "androidclient/version.json",
                        UpdateInfo.class,
                        false /*array*/,
                        null /*headers*/,
                        listener,
                        errorListener
                ),
                tag
        );
    }
}
