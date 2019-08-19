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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.projectbuendia.client.App;
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
        if (error.networkResponse == null ||
            error instanceof NoConnectionError ||
            error instanceof AuthFailureError ||
            error instanceof TimeoutError) {
            // No toast needed; let the BuendiaApiHealthCheck catch these
            // and show a message in the snackbar.
            return;
        }

        int code = error.networkResponse.statusCode;
        String message = extractErrorMessage(error.networkResponse);
        BigToast.show(App.getInstance().getApplicationContext(), message);
    }

    /** Parses a JSON-formatted error response from the OpenMRS server. **/
    public String extractErrorMessage(NetworkResponse response) {
        byte[] data = response.data;
        String problem = "status code " + response.statusCode;
        if (data != null) {
            String json = new String(data);
            LOG.i(json);
            try {
                JsonObject result = new JsonParser().parse(json).getAsJsonObject();
                if (result.has("error")) {
                    JsonObject error = result.getAsJsonObject("error");
                    JsonElement message = error.get("message");
                    if (message != null && message.isJsonPrimitive()) {
                        return message.getAsString();
                    }
                    JsonElement detail = error.get("detail");
                    if (detail != null && detail.isJsonPrimitive()) {
                        problem = detail.getAsString().split("\n")[0];
                    }
                    JsonElement code = error.get("code");
                    if (code != null && code.isJsonPrimitive()) {
                        problem += " at " + code.getAsString();
                    }
                }
            } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
                LOG.w("Problem parsing error message: " + e.getMessage());
            }
        }
        return "Sorry, there was a problem communicating with the server (" + problem + ")";
    }
}
