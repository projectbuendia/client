package org.msf.records.net;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.JsonObject;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.msf.records.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A connection to the Xform handling module we are adding to OpenMRS to provide xforms.
 * This is not part of OpenMrsServer as it has entirely it's own interface, but may be merged in
 * future.
 *
 * @author nfortescue@google.com
 */
public class OpenMrsXformsConnection {

    private static final Logger LOG = Logger.create();

    private final OpenMrsConnectionDetails mConnectionDetails;

    public OpenMrsXformsConnection(OpenMrsConnectionDetails connection) {
        this.mConnectionDetails = connection;
    }

    /**
     * Get a single (full) Xform from the OpenMRS server
     * @param uuid the uuid of the form to fetch
     * @param resultListener the listener to be informed of the form asynchronously
     * @param errorListener a listener to be informed of any errors
     */
    public void getXform(String uuid, final Response.Listener<String> resultListener,
                          Response.ErrorListener errorListener) {
        Request request = new OpenMrsJsonRequest(mConnectionDetails,
                "/xform/" + uuid + "?v=full",
                null, // null implies GET
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String xml = response.getString("xml");
                            resultListener.onResponse(xml);
                        } catch (JSONException e) {
                            // The result was not in the expected format. Just log, and return
                            // results so far.
                            LOG.e(e, "response was in bad format: " + response);
                        }
                    }
                }, errorListener
        );
        // Typical response times should be close to 10s, but as the number of users grows, this
        // number scales up quickly, so use a 30s timeout to be safe.
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    /**
     * List all xforms on the server, but not their contents.
     * @param listener a listener to be told about the index entries for all forms asynchronously.
     * @param errorListener a listener to be told about any errors.
     */
    public void listXforms(final Response.Listener<List<OpenMrsXformIndexEntry>> listener,
                           final Response.ErrorListener errorListener) {
        Request request = new OpenMrsJsonRequest(mConnectionDetails, "/xform", // list all forms
                null, // null implies GET
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        LOG.i("got forms: " + response);
                        ArrayList<OpenMrsXformIndexEntry> result = new ArrayList<>();
                        try {
                            // This seems quite code heavy (parsing manually), but is reasonably
                            // efficient as we only look at the fields we need, so we are robust to
                            // changes in the rest of the object.
                            JSONArray results = response.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject entry = results.getJSONObject(i);

                                // Sometimes date_changed is not set; in this case, date_changed is
                                // simply date_created.
                                long date_changed;
                                if (entry.get("date_changed") == JSONObject.NULL) {
                                    date_changed = entry.getLong("date_created");
                                } else {
                                    date_changed = entry.getLong("date_changed");
                                }

                                OpenMrsXformIndexEntry indexEntry = new OpenMrsXformIndexEntry(
                                        entry.getString("uuid"),
                                        entry.getString("name"),
                                        date_changed);
                                result.add(indexEntry);
                            }
                        } catch (JSONException e) {
                            // The result was not in the expected format. Just log, and return
                            // results so far.
                            LOG.e(e, "response was in bad format: " + response);
                        }
                        LOG.i("returning response: " + response);
                        listener.onResponse(result);
                    }
                },
                errorListener
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    /**
     * Send a single Xform to the OpenMRS server.
     *
     * @param patientUuid null if this is to add a new patient, non-null for observation on existing
     *                  patient
     * @param resultListener the listener to be informed of the form asynchronously
     * @param errorListener a listener to be informed of any errors
     */
    public void postXformInstance(
            @Nullable String patientUuid,
            String xform,
            final Response.Listener<JSONObject> resultListener,
            Response.ErrorListener errorListener) {

        // The JsonObject members in the API as written at the moment.
        // int "patient_id"
        // int "enterer_id"
        // String "date_entered" in ISO8601 format (1977-01-10T
        // String "xml" the form.
        JsonObject post = new JsonObject();
        post.addProperty("xml", xform);
        // Don't add patient property for create new patient
        if (patientUuid != null) {
            post.addProperty("patient_uuid", patientUuid);
        }
        // TODO(nfortescue): get the enterer from the user login
        post.addProperty("enterer_id", 1);

        post.addProperty("date_entered", ISODateTimeFormat.dateTime().print(new DateTime()));
        JSONObject postBody = null;
        try {
            postBody = new JSONObject(post.toString());
        } catch (JSONException e) {
            LOG.e(e, "This should never happen converting one JSON object to another. " + post);
            errorListener.onErrorResponse(new VolleyError("failed to convert to JSON", e));
        }
        OpenMrsJsonRequest request = new OpenMrsJsonRequest(
                mConnectionDetails, "/xforminstance",
                postBody, // non-null implies POST
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        resultListener.onResponse(response);
                    }
                }, errorListener
        );
        // Set a permissive timeout.
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

}
