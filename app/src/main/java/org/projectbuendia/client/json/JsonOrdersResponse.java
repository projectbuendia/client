package org.projectbuendia.client.json;

/** JSON format of a response to GET /orders?v=full */
public class JsonOrdersResponse {
    public JsonOrder[] results;
    // TODO(capnfabs): Rename this to syncToken.
    /** In ISO 8601 date format. */
    public String snapshotTime;
}
