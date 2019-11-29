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

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.projectbuendia.client.App;
import org.projectbuendia.client.diagnostics.HealthIssue;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.utils.Logger;

public class OpenMrsErrorListener implements ErrorListener {

    private static final Logger LOG = Logger.create();

    /** Default error-handling behaviour; can be overridden to suit the situation. */
    @Override public void onErrorResponse(VolleyError error) {
        displayVolleyError(error);
    }

    /** Displays a VolleyError as a toast, if it can be made meaningful to the user. */
    public void displayVolleyError(VolleyError error) {
        // TODO(ping): Hand off to the HealthMonitor or use the snackbar.
        LOG.w(error.getClass().getSimpleName() + ": " + error.getMessage());
        if (error instanceof NoConnectionError &&
            App.getTroubleshooter() != null &&
            App.getTroubleshooter().hasIssue(HealthIssue.SERVER_HOST_UNREACHABLE)) {
            return;
        }
        BigToast.show(formatErrorMessage(error));
    }

    /** Parses a JSON-formatted error response from the OpenMRS server. **/
    public static String formatErrorMessage(VolleyError volleyError) {
        if (volleyError instanceof NoConnectionError) {
            return "Failed to connect to the server.";
        }
        if (volleyError instanceof TimeoutError) {
            return "Server did not respond.";
        }
        if (volleyError == null || volleyError.networkResponse == null) {
            return "Sorry, there was a problem communicating with the server.";
        }
        NetworkResponse response = volleyError.networkResponse;
        byte[] data = response.data;
        String problem = "status code " + response.statusCode;
        if (data != null) {
            String json = new String(data);
            LOG.i(json);
            try {
                JsonObject result = new JsonParser().parse(json).getAsJsonObject();
                JsonArray errors = result.getAsJsonArray("errors");
                if (errors != null && errors.size() > 0) {
                    JsonObject error = (JsonObject) errors.get(0);
                    JsonElement message = error.get("message");
                    if (message != null && message.isJsonPrimitive()) {
                        return message.getAsString();
                    }
                    JsonArray frames = error.getAsJsonArray("frames");
                    JsonElement frame = frames.get(0);
                    return frame.getAsString();
                }
            } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
                LOG.w("Problem parsing error message: " + e.getMessage());
            }
        }
        if (volleyError instanceof AuthFailureError) {
            return "Operation was not permitted by the server (" + problem + ").";
        }
        return "Sorry, there was a problem communicating with the server (" + problem + ").";
    }
}
