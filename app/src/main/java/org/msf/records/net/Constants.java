package org.msf.records.net;

/**
 * Created by nfortescue on 11/17/14.
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
     * The base URL for the OpenMRS REST API from the Project Buendia module.
     */
    public static final String API_URL = "http://" + LOCALHOST_EMULATOR + ":8080" + API_PATH;

    /**
     * Recommended user for local admin in development.
     */
    public static final String LOCAL_ADMIN_USERNAME = "admin";

    /** Recommended password for local admin in development. */
    public static final String LOCAL_ADMIN_PASSWORD = "Admin123";
}