package org.msf.records.net;

import android.support.annotation.Nullable;

import com.android.volley.Response;

import org.msf.records.net.model.Location;
import org.msf.records.net.model.NewUser;
import org.msf.records.net.model.Patient;
import org.msf.records.net.model.PatientDelta;
import org.msf.records.net.model.User;

import java.util.List;
import java.util.Map;

/**
 * An interfacing abstracting the idea of an RPC to a server. Allows calls to be abstracted between
 * the existing custom project buendia server, and an OpenMSR server.
 */
public interface Server {

    public static final String PATIENT_ID_KEY = "id";
    public static final String PATIENT_STATUS_KEY = "status";
    public static final String PATIENT_GIVEN_NAME_KEY = "given_name";
    public static final String PATIENT_FAMILY_NAME_KEY = "family_name";
    @Deprecated public static final String PATIENT_DOB_YEARS_KEY = "age_years";
    @Deprecated public static final String PATIENT_DOB_MONTHS_KEY = "age_months";
    @Deprecated public static final String PATIENT_AGE_TYPE_KEY = "age_type";
    public static final String PATIENT_BIRTHDATE_KEY = "birthdate";
    public static final String PATIENT_GENDER_KEY = "gender";
    public static final String PATIENT_IMPORTANT_INFORMATION_KEY = "important_information";
    public static final String PATIENT_MOVEMENT_KEY = "movement";
    public static final String PATIENT_ASSIGNED_LOCATION = "assigned_location";

    /**
     * Adds a patient.
     */
    void addPatient(
            PatientDelta patientDelta,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener,
            String logTag);

    /**
     * Updates a patient.
     */
    public void updatePatient(
            String patientId,
            PatientDelta patientDelta,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener,
            String logTag);

    /**
     * Creates a new user.
     *
     * @param user the NewUser to add
     * @param logTag a unique argument for tagging logs to aid debugging
     */
    public void addUser(
            NewUser user,
            Response.Listener<User> userListener,
            Response.ErrorListener errorListener,
            String logTag);

    /**
     * Get the patient record for an existing patient. Currently we are just using a String-String
     * map for parameters, but this is a bit close in implementation details to the old Buendia UI
     * so it will probably need to be generalized in future.
     *
     * @param patientId the unique patient id representing the patients
     * @param logTag a unique argument for tagging logs to aid debugging
     */
    public void getPatient(
            String patientId,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener,
            String logTag);

    /**
     * Update the location of a patient
     *
     * @param patientId the id of the patient to update
     * @param newLocationId the id of the new location that the patient is assigned to
     */
    public void updatePatientLocation(String patientId, String newLocationId);

    /**
     * List all existing patients.
     *
     * @param logTag a unique argument for tagging logs to aid debugging
     */
    public void listPatients(@Nullable String filterState,
                             @Nullable String filterLocation,
                             @Nullable String filterQueryTerm,
                             Response.Listener<List<Patient>> patientListener,
                             Response.ErrorListener errorListener, String logTag);

    /**
     * List all existing users.
     *
     * @param logTag a unique argument for tagging logs to aid debugging
     */
    public void listUsers(@Nullable String filterQueryTerm,
                          Response.Listener<List<User>> userListener,
                          Response.ErrorListener errorListener, String logTag);

    /**
     * Add a new location to the server.
     *
     * @param location uuid must not be set, parent_uuid must be set, and the names map must have a
     *                 name for at least one locale.
     * @param locationListener the listener to be informed of the newly added location
     * @param errorListener listener to be informed of any errors
     * @param logTag a unique argument for tagging logs to aid debugging
     */
    public void addLocation(Location location,
                            final Response.Listener<Location> locationListener,
                            final Response.ErrorListener errorListener,
                            final String logTag);

    /**
     * Update the names for a location on the server.
     *
     * @param location the location, only uuid and new locale names for the location will be used,
     *                 but ideally the other arguments should be correct
     * @param locationListener the listener to be informed of the newly added location
     * @param errorListener listener to be informed of any errors
     * @param logTag a unique argument for tagging logs to aid debugging
     */
    public void updateLocation(Location location,
                               final Response.Listener<Location> locationListener,
                               final Response.ErrorListener errorListener,
                               final String logTag);

    /**
     * Delete a given location from the server. The location should not be the EMC location or
     * one of the zones - just a client added location, tent or bed.
     */
    public void deleteLocation(String locationUuid,
                               final Response.ErrorListener errorListener,
                               final String logTag);

    /**
     * List all locations.
     *
     * @param logTag a unique argument for tagging logs to aid debugging
     */
    public void listLocations(Response.Listener<List<Location>> locationListener,
                              Response.ErrorListener errorListener,
                              String logTag);

    /**
     * Cancel all requests associated with the given tag.
     * @param logTag
     */
    public void cancelPendingRequests(String logTag);
}
