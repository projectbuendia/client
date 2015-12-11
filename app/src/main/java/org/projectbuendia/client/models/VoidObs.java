package org.projectbuendia.client.models;

import org.json.JSONException;
import org.json.JSONObject;

public class VoidObs {

    public String Uuid;

    public VoidObs(String uuid) {
        this.Uuid = uuid;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uuid", Uuid);

        return json;
    }
}
