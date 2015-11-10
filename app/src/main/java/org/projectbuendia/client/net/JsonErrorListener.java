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
import android.graphics.PorterDuff;
import android.graphics.drawable.NinePatchDrawable;
import android.widget.Toast;

import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.projectbuendia.client.App;

public class JsonErrorListener implements ErrorListener {

    @Override
    public void onErrorResponse(VolleyError error) {
        displayErrorMessage(extractMessage(error));
    }

    protected String extractMessage(VolleyError error){
        String message = error.getMessage();
        try {
            if (error.networkResponse != null
                && error.networkResponse.data != null) {
                String text = new String(error.networkResponse.data);
                JsonObject result = new JsonParser().parse(text).getAsJsonObject();
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
            }
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            e.printStackTrace();
        }
        return message;
    }

    public void displayErrorMessage(String message) {
        Toast toast = Toast.makeText(
            App.getInstance().getApplicationContext(),
            message,
            Toast.LENGTH_LONG
        );
        NinePatchDrawable drawable = (NinePatchDrawable) toast.getView().getBackground();
        drawable.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        toast.show();
    }

}