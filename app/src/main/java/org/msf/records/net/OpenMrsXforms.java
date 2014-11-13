package org.msf.records.net;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A connection to the Xform handling module we are adding to OpenMRS to provide xforms.
 * This is not part of OpenMrsServer as it has entirely it's own interface, but may be merged in
 * future.
 *
 * @author nfortescue@google.com
 */
public class OpenMrsXforms {
    private static final String TAG = "OpenMrsXforms";
    private static final String LOCALHOST_EMULATOR = "10.0.2.2";
    private static final String API_BASE = "/openmrs/ws/rest/v1/projectbuendia";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "Admin123";
    private static final String KNOWN_UUID = "d5bbf64a-69ba-11e4-8a42-47ebc7225440";

    private final VolleySingleton mVolley;

    public OpenMrsXforms(Context context) {
        this.mVolley = VolleySingleton.getInstance(context.getApplicationContext());
    }

    public void getXform(String uuid, final Response.Listener<String> xform,
                          Response.ErrorListener errorListener) {
        Request request = new OpenMrsJsonRequest(
                USERNAME, PASSWORD,
                "http://"+ LOCALHOST_EMULATOR +":8080"+ API_BASE +"/xform/"+uuid+"?v=full",
                null, // null implies GET
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList<XformIndexEntry> result = new ArrayList<>();
                        try {
                            String xml = response.getString("xml");
                            xform.onResponse(xml);
                        } catch (JSONException e) {
                            // The result was not in the expected format. Just log, and return
                            // results so far.
                            Log.e(TAG, "response was in bad format: " + response, e);

                        }
                    }
                }, errorListener
        );
        mVolley.addToRequestQueue(request, TAG);
    }

    public void listXforms(final Response.Listener<List<XformIndexEntry>> entries,
                           final Response.ErrorListener errorListener) {
        Request request = new OpenMrsJsonRequest(
                USERNAME, PASSWORD,
                "http://"+ LOCALHOST_EMULATOR +":8080"+ API_BASE +"/xform", // list all forms
                null, // null implies GET
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList<XformIndexEntry> result = new ArrayList<>();
                        try {
                            // This seems quite code heavy (parsing manually), but is reasonably
                            // efficient as we only look at the fields we need, so we are robust to
                            // changes in the rest of the object.
                            JSONArray results = response.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject entry = results.getJSONObject(i);
                                XformIndexEntry indexEntry = new XformIndexEntry();
                                indexEntry.name = entry.getString("name");
                                indexEntry.uuid = entry.getString("uuid");
                                result.add(indexEntry);
                            }
                        } catch (JSONException e) {
                            // The result was not in the expected format. Just log, and return
                            // results so far.
                            Log.e(TAG, "response was in bad format: " + response, e);
                        }
                        entries.onResponse(result);
                    }
                },
                errorListener
        );
        mVolley.addToRequestQueue(request, TAG);
    }

    public void listFullXforms(Response.ErrorListener errorListener) {
        Request request = new OpenMrsJsonRequest(
                "admin", "Admin123",
                "http://"+ LOCALHOST_EMULATOR +":8080"+ API_BASE +"/xform?v=full", // list all forms
                null, // null implies GET
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, response.toString());
                    }
                },
                errorListener
        );
        mVolley.addToRequestQueue(request, TAG);
    }

    public class XformIndexEntry {
        public String uuid;
        public String name;
    }
}
