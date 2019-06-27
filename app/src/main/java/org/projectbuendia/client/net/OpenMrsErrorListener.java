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

import android.graphics.Color;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.projectbuendia.client.App;
import org.projectbuendia.client.utils.Logger;

public class OpenMrsErrorListener implements ErrorListener {

    private static final Logger LOG = Logger.create();

    private int statusCode;
    private String errorType;

    /**
     * intended to be the Default behavior this method could be overridden to better
     * suit the request's need.
     * @param error
     */
    @Override
    public void onErrorResponse(VolleyError error) {
        displayErrorMessage(parseResponse(error));
    }


    public String parseResponse(VolleyError error) {
        String message = "Empty response";
        String errorMessage = error.getMessage();

        if (error.networkResponse != null) {

            statusCode = error.networkResponse.statusCode;

            if( error instanceof NetworkError) {
                errorType = "NetworkError";
            } else if( error instanceof ServerError) {
                errorType = "ServerError";
            } else if( error instanceof AuthFailureError) {
                errorType = "AuthFailureError";
            } else if( error instanceof ParseError) {
                errorType = "ParseError";
            } else if( error instanceof NoConnectionError) {
                errorType = "NoConnectionError";
            } else if( error instanceof TimeoutError) {
                errorType = "TimeoutError";
            }

            if(error.networkResponse.data != null) {
                String body = new String(error.networkResponse.data);
                errorMessage = extractMessageFromJson(body);

                if(errorMessage != null){
                    message = errorMessage;
                } else {
                    message = body;
                }
            }
        }

        return errorType + " : " + statusCode + " " + message;
    }

    /** Parsing the json formatted error response received from the OpenMRS server **/
    public String extractMessageFromJson(String json) {
        String message = null;
        try {
            JsonObject result = new JsonParser().parse(json).getAsJsonObject();
            if (result.has("error")) {
                JsonObject errorObject = result.getAsJsonObject("error");
                JsonElement element = errorObject.get("message");
                if (element == null || element.isJsonNull()) {
                    element = errorObject.get("code");
                }
                if (element != null && element.isJsonPrimitive()) {
                    message = element.getAsString();
                }
            }
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            LOG.w("Problem parsing error message: " + e.getMessage());
        }
        return message;
    }

    /** display the error message as a toast **/
    public void displayErrorMessage(String message) {
        // TODO: refactor this using eventbus and snackbar.
        Toast toast = Toast.makeText(
            App.getInstance().getApplicationContext(),
            message,
            Toast.LENGTH_LONG
        );
        View view = toast.getView();
        view.setBackgroundColor(Color.RED);
        toast.show();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorType() {
        return errorType;
    }

}