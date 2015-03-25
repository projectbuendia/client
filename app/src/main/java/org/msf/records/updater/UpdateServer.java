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

package org.msf.records.updater;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;

import org.msf.records.model.UpdateInfo;
import org.msf.records.net.Common;
import org.msf.records.net.GsonRequest;
import org.msf.records.net.VolleySingleton;
import org.msf.records.prefs.StringPreference;

import java.util.List;

/**
 * Encapsulates requests to the update server.
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
     * Asynchronously issues a request to get the index of available Android updates.
     *
     * @param listener the callback to be invoked if the request succeeds
     * @param errorListener the callback to be invoked if the request fails
     */
    public void getPackageIndex(
            Response.Listener<List<UpdateInfo>> listener,
            Response.ErrorListener errorListener) {
        mVolley.addToRequestQueue(
                GsonRequest.withArrayResponse(
                        getRootUrl() + "/" + MODULE_NAME + ".json",
                        UpdateInfo.class,
                        null /*headers*/,
                        listener,
                        errorListener
                ).setRetryPolicy(
                        new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f)));
    }
}
