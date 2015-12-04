package org.projectbuendia.client.models;

import org.json.JSONException;
import org.json.JSONObject;

public class VoidObs {

    public String Uuid;
    public String VoidedBy;
    public String DateVoided;

    public VoidObs(String uuid, String voidedBy, String dateVoided) {
        this.Uuid = uuid;
        this.VoidedBy = voidedBy;
        this.DateVoided = dateVoided;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uuid", Uuid);
        json.put("by", VoidedBy);
        json.put("date", DateVoided);

        return json;
    }
}
