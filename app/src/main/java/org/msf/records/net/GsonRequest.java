package org.msf.records.net;

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
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by Gil on 08/10/2014.
 */
public class GsonRequest<T> extends Request<T> {

    private final GsonBuilder gson = new GsonBuilder();
    private final Class<T> clazz;
    private final Map<String, String> headers;
    private final Response.Listener<T> listener;
    private final boolean array;
    private Map<String,String> body = null;

    /**
     * Make a GET request and return a parsed object from JSON.
     *
     * @param url URL of the request to make
     * @param clazz Relevant class object, for Gson's reflection
     * @param headers Map of request headers
     */
    public GsonRequest(String url, Class clazz, boolean array, Map<String, String> headers,
                       Response.Listener<T> listener, Response.ErrorListener errorListener) {
        this(Method.GET, null, url, clazz, array, headers, listener, errorListener);
    }

    public GsonRequest(int method,
                       @Nullable Map<String, String> body,
                       String url, Class clazz, boolean array, Map<String, String> headers,
                       Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.body = body;
        this.clazz = clazz;
        this.headers = headers;
        this.listener = listener;
        this.array = array;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    @Override
    protected void deliverResponse(T response) {
        listener.onResponse(response);
    }

    @Override
    protected Map<String,String> getParams(){
        return body;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(
                    response.data,
                    HTTP.UTF_8);  // TODO(dxchen): HttpHeaderParser.parseCharset(response.headers).
            Gson gsonParser = gson.create();
            if(array){
                JsonParser parser = new JsonParser();
                JsonArray array = (JsonArray) parser.parse(json);
                ArrayList elements = new ArrayList();
                for (int i = 0; i < array.size(); i++) {
                    elements.add(gsonParser.fromJson(array.get(i).toString(), clazz));
                }
                return (Response<T>) Response.success(elements, HttpHeaderParser.parseCacheHeaders(response));
            } else {
                return Response.success(
                        gsonParser.fromJson(json, clazz),
                        HttpHeaderParser.parseCacheHeaders(response));
            }
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        } catch (Exception e){
            return Response.error(new ParseError(e));
        }
    }

    public GsonBuilder getGson() {
        return gson;
    }
}
