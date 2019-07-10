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
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.apache.http.protocol.HTTP;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Map;

import javax.annotation.Nullable;

/** A {@link Request} with a JSON response parsed by {@link Gson}. */
public class GsonRequest<T> extends Request<T> {
    private final GsonBuilder mGson = new GsonBuilder();
    private final Type mType;
    private final Map<String, String> mHeaders;
    private final Response.Listener<T> mListener;
    private Map<String, String> mBody = null;

    private static Logger LOG = Logger.create();

    /**
     * Makes a GET request and returns a parsed object from JSON.
     * @param url           URL of the request to make
     * @param type          The type of the base response JSON object. Gson reflects on this
     *                      parameter to create the response object. Note that in most situations,
     *                      using {@code MyJsonResponseType.class} will work, but if your response
     *                      object makes use of generics, you will need to generate a type from a
     *                      {@link com.google.gson.reflect.TypeToken TypeToken}, using something
     *                      like {@code new TypeToken<MyJsonResponseType<SubType>>{}.getType()}.
     * @param headers       Map of request headers
     * @param listener      a {@link Response.Listener} that handles successful requests
     * @param errorListener a {@link Response.ErrorListener} that handles failed requests
     */
    public GsonRequest(String url, Type type, Map<String, String> headers,
                       Response.Listener<T> listener, Response.ErrorListener errorListener) {
        this(Method.GET, null, url, type, headers, listener, errorListener);
    }

    /**
     * Makes a request using an arbitrary HTTP method and returns a parsed object from JSON.
     * @param method        the request method
     * @param body          the request body
     * @param url           URL of the request to make
     * @param type          The type of the base response JSON object. Gson reflects on this
     *                      parameter to create the response object. Note that in most situations,
     *                      using {@code MyJsonResponseType.class} will work, but if your response
     *                      object makes use of generics, you will need to generate a type from a
     *                      {@link com.google.gson.reflect.TypeToken TypeToken}, using something
     *                      like {@code new TypeToken<MyJsonResponseType<SubType>>{}.getType()}.
     * @param headers       Map of request headers
     * @param listener      a {@link Response.Listener} that handles successful requests
     * @param errorListener a {@link Response.ErrorListener} that handles failed requests
     */
    public GsonRequest(int method,
                       @Nullable Map<String, String> body,
                       String url, Type type, Map<String, String> headers,
                       Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mBody = body;
        this.mType = type;
        this.mHeaders = headers;
        this.mListener = listener;
    }

    @Override public Map<String, String> getHeaders() throws AuthFailureError {
        return mHeaders != null ? mHeaders : super.getHeaders();
    }

    public GsonBuilder getGson() {
        return mGson;
    }

    @Override protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }

    @Override protected Map<String, String> getParams() {
        return mBody;
    }

    @Override protected Response<T> parseNetworkResponse(NetworkResponse response) {
        LOG.finish("HTTP." + getSequence(), "Response to %s -> %s", Utils.repr(this), Utils.repr(response.data, 500));
        try {
            String json = new String(
                response.data,
                HTTP.UTF_8);  // TODO: HttpHeaderParser.parseCharset(response.mHeaders).
            Gson gsonParser = mGson.create();
            //noinspection unchecked
            return (Response<T>) Response.success(
                gsonParser.fromJson(json, mType),
                HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }
}
