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

package org.msf.records.net;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.apache.http.protocol.HTTP;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A {@link Request} with a JSON response parsed by {@link Gson}.
 */
public class GsonRequest<T> extends Request<T> {

    private final GsonBuilder mGson = new GsonBuilder();
    private final Class<T> mClazz;
    private final Map<String, String> mHeaders;
    private final Response.Listener<T> mListener;
    private final boolean mIsArray;
    private Map<String,String> mBody = null;

    /**
     * Creates an instance of {@link GsonRequest} that expects an array of Gson objects as a
     * response.
     */
    public static <T> GsonRequest<List<T>> withArrayResponse(
            String url,
            Class<T> clazz,
            Map<String, String> headers,
            Response.Listener<List<T>> listener,
            Response.ErrorListener errorListener) {
        // TODO: This current class does not handle arrays well because it doesn't properly
        // use Java generics. Until we can fix it, we'll just cast a lot to make Java happy.
        return (GsonRequest<List<T>>) new GsonRequest<>(
                url, clazz, true, headers, (Response.Listener<T>) listener, errorListener);
    }

    /**
     * Makes a GET request and returns a parsed object from JSON.
     *
     * @param url URL of the request to make
     * @param clazz Relevant class object, for Gson's reflection
     * @param headers Map of request headers
     * @param listener a {@link Response.Listener} that handles successful requests
     * @param errorListener a {@link Response.ErrorListener} that handles failed requests
     */
    public GsonRequest(String url, Class<T> clazz, boolean array, Map<String, String> headers,
                       Response.Listener<T> listener, Response.ErrorListener errorListener) {
        this(Method.GET, null, url, clazz, array, headers, listener, errorListener);
    }

    /**
     * Makes a request using an arbitrary HTTP method and returns a parsed object from JSON.
     *
     * @param method the request method
     * @param body the request body
     * @param url URL of the request to make
     * @param clazz Relevant class object, for Gson's reflection
     * @param isArray true if the response is expected to contain an array of items
     * @param headers Map of request headers
     * @param listener a {@link Response.Listener} that handles successful requests
     * @param errorListener a {@link Response.ErrorListener} that handles failed requests
     */
    public GsonRequest(int method,
                       @Nullable Map<String, String> body,
                       String url, Class<T> clazz, boolean isArray, Map<String, String> headers,
                       Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mBody = body;
        this.mClazz = clazz;
        this.mHeaders = headers;
        this.mListener = listener;
        this.mIsArray = isArray;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return mHeaders != null ? mHeaders : super.getHeaders();
    }

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }

    @Override
    protected Map<String,String> getParams() {
        return mBody;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(
                    response.data,
                    HTTP.UTF_8);  // TODO: HttpHeaderParser.parseCharset(response.mHeaders).
            Gson gsonParser = mGson.create();
            Log.d("SyncAdapter", "parsing response");
            if (mIsArray) {
                JsonParser parser = new JsonParser();
                JsonArray array = (JsonArray) parser.parse(json);
                ArrayList<T> elements = new ArrayList<>();
                for (int i = 0; i < array.size(); i++) {
                    elements.add(gsonParser.fromJson(array.get(i).toString(), mClazz));
                }
                return (Response<T>) Response.success(
                        elements,
                        HttpHeaderParser.parseCacheHeaders(response));
            } else {
                return Response.success(
                        gsonParser.fromJson(json, mClazz),
                        HttpHeaderParser.parseCacheHeaders(response));
            }
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }

    public GsonBuilder getGson() {
        return mGson;
    }
}
