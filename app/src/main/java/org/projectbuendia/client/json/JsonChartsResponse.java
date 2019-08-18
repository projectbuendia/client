package org.projectbuendia.client.json;

/** JSON format of a response to GET /charts?v=full */
public class JsonChartsResponse {
    public JsonChart[] results;
    public String snapshotTime;
}
