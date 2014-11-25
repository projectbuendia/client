package org.msf.records.net;

/**
 * Shared useful constants across networking/RPCs to server.
 */
public class Constants {

    /**
     * Whether to use an external server
     */
    public static final boolean EXTERNAL_SERVER = true;

    /**
     * To allow data to be provided from the sync adapter rather than the network
     */
    public static final boolean OFFLINE_SUPPORT = true;

    /**
     * IP address of localhost for the computer the emulator is running on.
     *
     * See http://developer.android.com/tools/devices/emulator.html#emulatornetworking
     */
    public static final String LOCALHOST_HOST = "10.0.2.2:8080";

    /**
     * IP address of GCE server
     */
    public static final String EXTERNAL_HOST = "104.155.15.141:8080";

    /**
     * The base path for the OpenMRS REST API from the Project Buendia module.
     */
    public static final String API_PATH = "/openmrs/ws/rest/v1/projectbuendia";

    /**
     * Recommended user for local admin in development.
     */
    private static final String LOCAL_ADMIN_USERNAME = "admin";

    /** Recommended password for local admin in development. */
    private static final String LOCAL_ADMIN_PASSWORD = "Admin123";

    /**
     * Recommended user for GCE admin in development.
     */
    private static final String EXTERNAL_ADMIN_USERNAME = "android";

    /** Recommended password for GCE admin in development. */
    private static final String EXTERNAL_ADMIN_PASSWORD = "Android123";

    /**
     * The base URL for the OpenMRS REST API from the Project Buendia module, when running against
     * a local server.
     */
    public static final String API_URL = "http://" + (EXTERNAL_SERVER ? EXTERNAL_HOST : LOCALHOST_HOST) + API_PATH;

    public static final String API_ADMIN_USERNAME = (EXTERNAL_SERVER ? EXTERNAL_ADMIN_USERNAME : LOCAL_ADMIN_USERNAME);
    public static final String API_ADMIN_PASSWORD = (EXTERNAL_SERVER ? EXTERNAL_ADMIN_PASSWORD : LOCAL_ADMIN_PASSWORD);


    /** Hard-coded UUID for the 'Add Patient' xform. */
    public static final String ADD_PATIENT_UUID = "c47d4d3d-f9a3-4f97-9623-d7acee81d401";

    /** Hard-coded UUID for the 'Add Patient' xform. */
    public static final String ADD_OBSERVATION_UUID = "736b90ee-fda6-4438-a6ed-71acd36381f3";

}
