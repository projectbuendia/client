package org.msf.records.net;

import android.support.annotation.Nullable;

import com.android.volley.Response;

import org.msf.records.net.model.NewUser;
import org.msf.records.net.model.Patient;
import org.msf.records.net.model.User;

import java.util.List;
import java.util.Map;

/**
 * An interfacing abstracting the idea of an RPC to a server. Allows calls to be abstracted between
 * the existing custom project buendia server, and an OpenMSR server.
 *
 * Created by nfortescue on 11/3/14.
 */
public interface Server {

    public static final String PATIENT_ID_KEY = "id";
    public static final String PATIENT_STATUS_KEY = "status";
    public static final String PATIENT_GIVEN_NAME_KEY = "given_name";
    public static final String PATIENT_FAMILY_NAME_KEY = "family_name";
    public static final String PATIENT_DOB_YEARS_KEY = "age_years";
    public static final String PATIENT_DOB_MONTHS_KEY = "age_months";
    public static final String PATIENT_AGE_TYPE_KEY = "age_type";
    public static final String PATIENT_GENDER_KEY = "gender";
    public static final String PATIENT_IMPORTANT_INFORMATION_KEY = "important_information";
    public static final String PATIENT_ORIGIN_LOCATION_KEY = "origin_location";
    public static final String PATIENT_MOVEMENT_KEY = "movement";
    public static final String PATIENT_LOCATION_ZONE_KEY = "assigned_location_zone_id";
    public static final String PATIENT_LOCATION_TENT_KEY = "assigned_location_tent_id";
    public static final String PATIENT_LOCATION_BED_KEY = "assigned_location_bed";

    /**
     * Create a patient record for a new patient. Currently we are just using a String-String
     * map for parameters, but this is a bit close in implementation details to the old Buendia UI
     * so it will probably need to be generalized in future.
     *
     * @param patientArguments a String-String map for the patient arguments, key constants
     * @param logTag a unique argument for tagging logs to aid debugging
     */
    public void addPatient(
            Map<String, String> patientArguments,
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
     * Update a patient record for an existing patient. Currently we are just using a String-String
     * map for parameters, but this is a bit close in implementation details to the old Buendia UI
     * so it will probably need to be generalized in future.
     *
     * @param patientChanges a Patient map for the patient arguments. If a field is null then
     *                         the value is not touched. There is no way to delete values.
     * @param logTag a unique argument for tagging logs to aid debugging
     */
    public void updatePatient(
            String patientId,
            Patient patientChanges,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener,
            String logTag);

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
     * Cancel all requests associated with the given tag.
     * @param logTag
     */
    public void cancelPendingRequests(String logTag);
}
