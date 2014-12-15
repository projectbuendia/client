package org.msf.records.net;

/**
 * Shared useful constants across networking/RPCs to server.
 */
public class Constants {

    /**
     * To allow data to be provided from the sync adapter rather than the network
     */
    public static final boolean OFFLINE_SUPPORT = true;

    /** Hard-coded UUID for the 'Add Patient' xform. */
    public static final String ADD_PATIENT_UUID = "c47d4d3d-f9a3-4f97-9623-d7acee81d401";

    /** Hard-coded UUID for the 'Add Observation' xform. */
    public static final String ADD_OBSERVATION_UUID = "736b90ee-fda6-4438-a6ed-71acd36381f3";
}
