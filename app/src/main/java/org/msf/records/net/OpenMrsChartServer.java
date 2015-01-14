package org.msf.records.net;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;

import org.joda.time.Instant;
import org.msf.records.net.model.ChartStructure;
import org.msf.records.net.model.ConceptList;
import org.msf.records.net.model.CustomSerialization;
import org.msf.records.net.model.PatientChart;
import org.msf.records.net.model.PatientChartList;

import java.util.HashMap;

/**
 * A connection to an Open MRS backend to get chart information. This is observations on encounters
 * with patients. The design for the resources is here:
 * https://docs.google.com/document/d/17Dub0KZDEahIqJgNOW6kH789K0Brwj9leapSMY1JC-k/edit
 * <p>
 * There are essentially three endpoints. /patientencounters which give encoded details of
 * the observations of concept values that happen at an encounter, /concepts which gives localised
 * string and type information for the concepts observed, and /charts which give display information
 * about how to display those encounters, so you can have consistent ordering of observations and
 * grouping into sections.
 */
public class OpenMrsChartServer {

    private final OpenMrsConnectionDetails mConnectionDetails;

    public OpenMrsChartServer(OpenMrsConnectionDetails connectionDetails) {
        this.mConnectionDetails = connectionDetails;
    }

    public void getChart(String patientUuid,
                         Response.Listener<PatientChart> patientListener,
                         Response.ErrorListener errorListener) {
        GsonRequest<PatientChart> request = new GsonRequest<>(
                mConnectionDetails.getRootUrl() + "/patientencounters/" + patientUuid,
                PatientChart.class, false,
                mConnectionDetails.addAuthHeader(new HashMap<String, String>()),
                patientListener, errorListener);
        CustomSerialization.registerTo(request.getGson());
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    public void getAllCharts(Response.Listener<PatientChartList> patientListener,
                             Response.ErrorListener errorListener) {
        doEncountersRequest(mConnectionDetails.getRootUrl() + "/patientencounters", patientListener,
                errorListener);
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
        doEncountersRequest(
                mConnectionDetails.getRootUrl() + "/patientencounters?sm=" + lastTime.getMillis(),
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
        request.setRetryPolicy(new DefaultRetryPolicy(100000, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    public void getConcepts(Response.Listener<ConceptList> conceptListener,
                            Response.ErrorListener errorListener) {
        GsonRequest<ConceptList> request = new GsonRequest<ConceptList>(
                mConnectionDetails.getRootUrl() + "/concept",
                ConceptList.class, false,
                mConnectionDetails.addAuthHeader(new HashMap<String, String>()),
                conceptListener, errorListener) {
        };
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    public void getChartStructure(
            String uuid, Response.Listener<ChartStructure> chartListener,
            Response.ErrorListener errorListener) {
        GsonRequest<ChartStructure> request = new GsonRequest<ChartStructure>(
                mConnectionDetails.getRootUrl() + "/chart/" + uuid + "?v=full",
                ChartStructure.class, false,
                mConnectionDetails.addAuthHeader(new HashMap<String, String>()),
                chartListener, errorListener) {
        };
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }
}
