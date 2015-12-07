package org.projectbuendia.client.json;

/**
 * JSON representation of a set of patients returned by the server.
 * <p>
 * TODO: Generify this class into e.g. an IncrementalFetchResponse<JsonEncounter>
 */
public class JsonOrdersResponse {
    public JsonOrder[] results;
    // TODO(capnfabs): Rename this to syncToken.
    /** In ISO 8601 date format. */
    public String snapshotTime;
}
