package org.msf.records.net;

/**
 * Shared useful constants across networking/RPCs to server.
 */
public class Constants {
    /**
     * IP address of localhost for the computer the emulator is running on.
     *
     * See http://developer.android.com/tools/devices/emulator.html#emulatornetworking
     */
    public static final String LOCALHOST_EMULATOR = "10.0.2.2";

    /**
     * The base path for the OpenMRS REST API from the Project Buendia module.
     */
    public static final String API_PATH = "/openmrs/ws/rest/v1/projectbuendia";

    /**
     * The base URL for the OpenMRS REST API from the Project Buendia module, when running against
     * a local server.
     */
    public static final String API_URL = "http://" + LOCALHOST_EMULATOR + ":8080" + API_PATH;

    /**
     * Recommended user for local admin in development.
     */
    public static final String LOCAL_ADMIN_USERNAME = "admin";

    /** Recommended password for local admin in development. */
    public static final String LOCAL_ADMIN_PASSWORD = "Admin123";

    /** Hard-coded UUID for the 'Add Patient' xform. */
    public static final String ADD_PATIENT_UUID = "c47d4d3d-f9a3-4f97-9623-d7acee81d401";

    /** Hard-coded UUID for the 'Add Patient' xform. */
    public static final String ADD_OBSERVATION_UUID = "736b90ee-fda6-4438-a6ed-71acd36381f3";

    /**
     * Our default GCE instance for development.
     */
    public static final String GCE_INSTANCE = "http://104.155.15.141:8080";

    /**
     * The base URL for the OpenMRS REST API from the Project Buendia module, when running against
     * the GCE instance.
     */
    public static final String GCE_URL = GCE_INSTANCE + API_PATH;
}
