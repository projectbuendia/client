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

import org.joda.time.Instant;
import org.projectbuendia.client.net.model.ChartStructure;
import org.projectbuendia.client.net.model.ConceptList;
import org.projectbuendia.client.net.model.CustomSerialization;
import org.projectbuendia.client.net.model.PatientChart;
import org.projectbuendia.client.net.model.PatientChartList;

import java.util.HashMap;

/**
 * A connection to an OpenMRS backend to get chart information (observations on encounters
 * with patients).
 *
 * <p>There are essentially three endpoints:
 * <ul>
 *     <li><code>/patientencounters</code> gives encoded details of the observations of concept
 *     values that happen at an encounter
 *     <li><code>/concepts</code> gives localised string and type information for the concepts
 *     observed
 *     <li><code>/charts</code> gives display information about how to display those encounters,
 *     so you can have consistent ordering of observations and grouping into sections.
 * </ul>
 */
public class OpenMrsChartServer {

    private final OpenMrsConnectionDetails mConnectionDetails;

    public OpenMrsChartServer(OpenMrsConnectionDetails connectionDetails) {
        this.mConnectionDetails = connectionDetails;
    }

    /**
     * Retrieves charts from the server for a given patient.
     * @param patientUuid the UUID of the patient
     * @param patientListener a {@link Response.Listener} that handles successful chart retrieval
     * @param errorListener a {@link Response.ErrorListener} that handles failed chart retrieval
     */
    public void getChart(String patientUuid,
                         Response.Listener<PatientChart> patientListener,
                         Response.ErrorListener errorListener) {
        GsonRequest<PatientChart> request = new GsonRequest<>(
                mConnectionDetails.getBuendiaApiUrl() + "/patientencounters/" + patientUuid,
                PatientChart.class, false,
                mConnectionDetails.addAuthHeader(new HashMap<String, String>()),
                patientListener, errorListener);
        CustomSerialization.registerTo(request.getGson());
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    /**
     * Retrieves all charts from the server for all patients.
     * @param patientListener a {@link Response.Listener} that handles successful chart retrieval
     * @param errorListener a {@link Response.ErrorListener} that handles failed chart retrieval
     */
    public void getAllCharts(Response.Listener<PatientChartList> patientListener,
                             Response.ErrorListener errorListener) {
        doEncountersRequest(mConnectionDetails.getBuendiaApiUrl() + "/patientencounters",
                patientListener, errorListener);
    }

    /**
     * Get all observations that happened in an encounter after or on lastTime. Allows a client to
     * do incremental cache updating.
     *
     * @param lastTime a joda instant representing the start time for new observations (inclusive)
     * @param patientListener a listener to get the results on the event of success
     * @param errorListener a (Volley) listener to get any errors
     */
    public void getIncrementalCharts(
            Instant lastTime,
            Response.Listener<PatientChartList> patientListener,
            Response.ErrorListener errorListener) {
        doEncountersRequest(mConnectionDetails.getBuendiaApiUrl()
                        + "/patientencounters?sm=" + lastTime.getMillis(),
                patientListener, errorListener);
    }

    private void doEncountersRequest(
            String url,
            Response.Listener<PatientChartList> patientListener,
            Response.ErrorListener errorListener) {
        GsonRequest<PatientChartList> request = new GsonRequest<>(
                url,
                PatientChartList.class, false,
                mConnectionDetails.addAuthHeader(new HashMap<String, String>()),
                patientListener, errorListener);
        CustomSerialization.registerTo(request.getGson());
        request.setRetryPolicy(
                new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_VERY_LONG, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    /**
     * Retrieves all concepts from the server that are present in at least one chart.
     * @param conceptListener a {@link Response.Listener} that handles successful concept retrieval
     * @param errorListener a {@link Response.ErrorListener} that handles failed concept retrieval
     */
    public void getConcepts(Response.Listener<ConceptList> conceptListener,
                            Response.ErrorListener errorListener) {
        GsonRequest<ConceptList> request = new GsonRequest<ConceptList>(
                mConnectionDetails.getBuendiaApiUrl() + "/concept",
                ConceptList.class, false,
                mConnectionDetails.addAuthHeader(new HashMap<String, String>()),
                conceptListener, errorListener) {
        };
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_LONG, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    /**
     * Retrieves the structure of a given chart (groupings, orderings) from the server.
     * @param uuid the UUID of the chart
     * @param chartListener a {@link Response.Listener} that handles successful structure retrieval
     * @param errorListener a {@link Response.ErrorListener} that handles failed structure retrieval
     */
    public void getChartStructure(
            String uuid, Response.Listener<ChartStructure> chartListener,
            Response.ErrorListener errorListener) {
        GsonRequest<ChartStructure> request = new GsonRequest<ChartStructure>(
                mConnectionDetails.getBuendiaApiUrl() + "/chart/" + uuid + "?v=full",
                ChartStructure.class, false,
                mConnectionDetails.addAuthHeader(new HashMap<String, String>()),
                chartListener, errorListener) {
        };
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_LONG, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }
}
