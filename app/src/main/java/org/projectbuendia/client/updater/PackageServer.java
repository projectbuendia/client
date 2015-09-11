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

package org.projectbuendia.client.updater;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;

import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.net.json.JsonUpdateInfo;
import org.projectbuendia.client.net.Common;
import org.projectbuendia.client.net.GsonRequest;
import org.projectbuendia.client.net.VolleySingleton;

import java.util.List;

/** Encapsulates requests to the package server. */
public class PackageServer {

    /**
     * The package server's module name for updates to this app.  A name of "foo"
     * means the updates are named "foo-1.2.apk", "foo-1.3.apk", etc. and their
     * index is available at "foo.json".
     */
    private static final String MODULE_NAME = "buendia-client";

    private final VolleySingleton mVolley;
    private final AppSettings mSettings;

    public PackageServer(VolleySingleton volley, AppSettings settings) {
        mVolley = volley;
        mSettings = settings;
    }

    /**
     * Asynchronously issues a request to get the index of available Android updates.
     *
     * @param listener the callback to be invoked if the request succeeds
     * @param errorListener the callback to be invoked if the request fails
     */
    public void getPackageIndex(
            Response.Listener<List<JsonUpdateInfo>> listener,
            Response.ErrorListener errorListener) {
        mVolley.addToRequestQueue(
                GsonRequest.withArrayResponse(
                        mSettings.getPackageServerUrl("/" + MODULE_NAME + ".json"),
                        JsonUpdateInfo.class,
                        null /* headers */,
                        listener,
                        errorListener
                ).setRetryPolicy(
                        new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f)));
    }
}
