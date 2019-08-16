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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;

import org.projectbuendia.client.json.JsonChartsResponse;
import org.projectbuendia.client.json.JsonConceptsResponse;

import java.util.HashMap;

/**
 * A connection to an OpenMRS backend to get chart information (observations on encounters
 * with patients).
 * <p/>
 * <p>There are essentially three endpoints:
 * <ul>
 * <li><code>/encounters</code> gives encoded details of the observations of concept
 * values that happen at an encounter
 * <li><code>/concepts</code> gives localised string and type information for the concepts
 * observed
 * <li><code>/charts</code> gives display information about how to display those encounters,
 * so you can have consistent ordering of observations and grouping into sections.
 * </ul>
 */
public class OpenMrsChartServer {

    private final OpenMrsConnectionDetails mConnectionDetails;

    public OpenMrsChartServer(OpenMrsConnectionDetails connectionDetails) {
        this.mConnectionDetails = connectionDetails;
    }

    /**
     * Retrieves all concepts from the server that are present in at least one chart.
     * @param successListener a {@link Response.Listener} that handles successful concept retrieval
     * @param errorListener   a {@link Response.ErrorListener} that handles failed concept retrieval
     */
    public void getConcepts(Response.Listener<JsonConceptsResponse> successListener,
                            Response.ErrorListener errorListener) {
        GsonRequest<JsonConceptsResponse> request = new GsonRequest<>(
            mConnectionDetails.getBuendiaApiUrl() + "/concepts",
            JsonConceptsResponse.class,
            mConnectionDetails.addAuthHeader(new HashMap<>()),
            successListener, errorListener);
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_LONG, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    /**
     * Retrieves the structure of a given chart (groupings, orderings) from the server.
     * @param uuid            the UUID of the chart
     * @param successListener a {@link Response.Listener} that handles successful structure retrieval
     * @param errorListener   a {@link Response.ErrorListener} that handles failed structure retrieval
     */
    public void getChartStructures(Response.Listener<JsonChartsResponse> successListener,
                                   Response.ErrorListener errorListener) {
        GsonRequest<JsonChartsResponse> request = new GsonRequest<>(
            mConnectionDetails.getBuendiaApiUrl() + "/charts/?v=full",
            JsonChartsResponse.class,
            mConnectionDetails.addAuthHeader(new HashMap<>()),
            successListener, errorListener);
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_LONG, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }
}
