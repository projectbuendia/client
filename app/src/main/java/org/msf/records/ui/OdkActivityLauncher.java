package org.msf.records.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xform.parse.XFormParser;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;
import org.msf.records.App;
import org.msf.records.events.FetchXformFailedEvent;
import org.msf.records.events.FetchXformSucceededEvent;
import org.msf.records.net.OdkDatabase;
import org.msf.records.net.OdkXformSyncTask;
import org.msf.records.net.OpenMrsXformIndexEntry;
import org.msf.records.net.OpenMrsXformsConnection;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.utils.Logger;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.activities.FormHierarchyActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.listeners.FormLoaderListener;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.model.PrepopulatableFields;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.tasks.DeleteInstancesTask;
import org.odk.collect.android.tasks.FormLoaderTask;
import org.odk.collect.android.utilities.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import de.greenrobot.event.EventBus;

import static android.provider.BaseColumns._ID;
import static org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns.CONTENT_URI;
import static org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH;
import static org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns.JR_FORM_ID;

/**
 * Convenience class for launching ODK to display an Xform.
 */
public class OdkActivityLauncher {

    private static final Logger LOG = Logger.create();

    public static void fetchAndShowXform(final Activity callingActivity, final String uuidToShow,
                                         final int requestCode) {
        fetchAndShowXform(
                callingActivity, uuidToShow, requestCode, null /*patient*/, null /*fields*/);
    }

    public static void fetchAndShowXform(
            final Activity callingActivity,
            final String uuidToShow,
            final int requestCode,
            final org.odk.collect.android.model.Patient patient,
            final PrepopulatableFields fields) {
        new OpenMrsXformsConnection(App.getConnectionDetails()).listXforms(
                new Response.Listener<List<OpenMrsXformIndexEntry>>() {
                    @Override
                    public void onResponse(final List<OpenMrsXformIndexEntry> response) {
                        if (response.isEmpty()) {
                            LOG.i("No forms found");
                            EventBus.getDefault().post(new FetchXformFailedEvent());
                            return;
                        }
                        // Cache all the forms into the ODK form cache
                        new OdkXformSyncTask(new OdkXformSyncTask.FormWrittenListener() {
                            @Override
                            public void formWritten(File path, String uuid) {
                                LOG.i("wrote form " + path);
                                showOdkCollect(
                                        callingActivity,
                                        requestCode,
                                        OdkDatabase.getFormIdForPath(path),
                                        patient,
                                        fields);
                            }
                        }).execute(findUuid(response, uuidToShow));

                        EventBus.getDefault().post(new FetchXformSucceededEvent());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        EventBus.getDefault().post(new FetchXformFailedEvent());
                    }
                });
    }

    public static void showOdkCollect(Activity callingActivity, int requestCode, long formId) {
        showOdkCollect(callingActivity, requestCode, formId, null /*patient*/, null /*fields*/);
    }

    public static void showOdkCollect(
            Activity callingActivity,
            int requestCode,
            long formId,
            org.odk.collect.android.model.Patient patient,
            PrepopulatableFields fields) {
        Intent intent = new Intent(callingActivity, FormEntryActivity.class);
        Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, formId);
        intent.setData(formUri);
        intent.setAction(Intent.ACTION_PICK);
        if (patient != null) {
            intent.putExtra("patient", patient);
        }
        if (fields != null) {
            intent.putExtra("fields", fields);
        }
        callingActivity.startActivityForResult(intent, requestCode);
    }

    /**
     * Convenient shared code for handling an ODK activity result.
     *
     * @param patientUuid the patient to add an observation to, or null to create a new patient
     * @param resultCode the result code sent from Android activity transition
     * @param updateClientCache true if we should update the client database with temporary observations
     * @param data the incoming intent
     */
    public static void sendOdkResultToServer(
            Context context,
            @Nullable String patientUuid,
            boolean updateClientCache,
            int resultCode,
            Intent data) {

        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        if (data == null || data.getData() == null) {
            // Cancelled.
            LOG.i("No data for form result, probably cancelled.");
            return;
        }

        Uri uri = data.getData();

        if (!context.getContentResolver().getType(uri).equals(
                InstanceProviderAPI.InstanceColumns.CONTENT_ITEM_TYPE)) {
            LOG.e("Tried to load a content URI of the wrong type: " + uri);
            return;
        }

        Cursor instanceCursor = null;
        try {
            instanceCursor = context.getContentResolver().query(uri,
                    null, null, null, null);
            if (instanceCursor.getCount() != 1) {
                LOG.e("The form that we tried to load did not exist: " + uri);
                return;
            }
            instanceCursor.moveToFirst();
            String instancePath = instanceCursor.getString(
                    instanceCursor.getColumnIndex(INSTANCE_FILE_PATH));
            if (instancePath == null) {
                LOG.e("No file path for form instance: " + uri);
                return;

            }
            int columnIndex = instanceCursor
                    .getColumnIndex(_ID);
            if (columnIndex == -1) {
                LOG.e("No id to delete for after upload: " + uri);
                return;
            }
            final long idToDelete = instanceCursor.getLong(columnIndex);

            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            final boolean keepFormInstancesLocally =
                    preferences.getBoolean("keep_form_instances_locally", false);

            // Temporary code for messing about with xform instance, reading values.
            //
            byte[] fileBytes = FileUtils.getFileAsBytes(new File(instancePath));

            // get the root of the saved and template instances
            TreeElement savedRoot = XFormParser.restoreDataModel(fileBytes, null).getRoot();

            if (patientUuid != null && updateClientCache) { // Only locally cache new observations, not new patients
                updateClientCache(patientUuid, savedRoot, context.getContentResolver());
            }

            sendFormToServer(patientUuid, readFromPath(instancePath),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            LOG.i("Created new patient successfully on server"
                                    + response.toString());

                            if (!keepFormInstancesLocally) {
                                //Code largely copied from InstanceUploaderTask to delete on upload
                                DeleteInstancesTask dit = new DeleteInstancesTask();
                                dit.setContentResolver(
                                        Collect.getInstance().getApplication().getContentResolver());
                                dit.execute(idToDelete);
                            }
                        }
                    });
        } catch (IOException e) {
            LOG.e(e, "Failed to read xml form into a String " + uri);
        } finally {
            if (instanceCursor != null) {
                instanceCursor.close();
            }
        }
    }

    private static void updateClientCache(String patientUuid, TreeElement savedRoot,
                                          ContentResolver resolver) {
        // id, fill in auto
        // patient uuid: context
        // encounter uuid: make one up
        // encounter time: <encounter><encounter.encounter_datetime>2014-12-15T13:33:00.000Z</encounter.encounter_datetime>
        // concept uuid: <vitals><temperature_c openmrs_concept="5088^Temperature (C)^99DCT" openmrs_datatype="NM"> <- 5088
        // value: 	<vitals><temperature_c openmrs_concept="5088^Temperature (C)^99DCT" openmrs_datatype="NM">
        //              <value>36.0</value>
        // temp_cache: true

        // or for coded
        // <symptoms_abinaryinvisible>
        //   <weakness multiple="0" openmrs_concept="5226^WEAKNESS^99DCT" openmrs_datatype="CWE">
        //      <value>1066^NO^99DCT</value>

        ContentValues common = new ContentValues();
        common.put(Contracts.Observations.PATIENT_UUID, patientUuid);

        TreeElement encounter = savedRoot.getChild("encounter", 0);
        if (encounter == null) {
            LOG.e("No encounter found in instance");
            return;
        }

        TreeElement encounterDatetime =
                encounter.getChild("encounter.encounter_datetime", 0);
        if (encounterDatetime == null) {
            LOG.e("No encounter date time found in instance");
            return;
        }
        IAnswerData dateTimeValue = encounterDatetime.getValue();
        try {

            DateTime encounterTime =
                    ISODateTimeFormat.dateTime().parseDateTime((String) dateTimeValue.getValue());
            long secondsSinceEpoch = encounterTime.getMillis() / 1000L;
            common.put(Contracts.Observations.ENCOUNTER_TIME, secondsSinceEpoch);
            common.put(Contracts.Observations.ENCOUNTER_UUID, UUID.randomUUID().toString());
            common.put(Contracts.Observations.TEMP_CACHE, 1);
        } catch (IllegalArgumentException e) {
            LOG.e("Could not parse datetime" + dateTimeValue.getValue());
            return;
        }

        ArrayList<ContentValues> toInsert = new ArrayList<>();
        HashSet<Integer> xformConceptIds = new HashSet<>();
        for (int i=0; i<savedRoot.getNumChildren(); i++) {
            TreeElement group = savedRoot.getChildAt(i);
            if (group.getNumChildren() == 0) {
                continue;
            }
            for (int j=0; j< group.getNumChildren(); j++) {
                TreeElement question = group.getChildAt(j);
                TreeElement openmrsConcept = question.getAttribute(null, "openmrs_concept");
                TreeElement openmrsDatatype = question.getAttribute(null, "openmrs_datatype");
                if (openmrsConcept == null || openmrsDatatype == null) {
                    continue;
                }
                // Get the concept for the question.
                // eg "5088^Temperature (C)^99DCT"
                String encodedConcept = (String) openmrsConcept.getValue().getValue();
                Integer id = getConceptId(xformConceptIds, encodedConcept);
                if (id == null) {
                    continue;
                }
                // Also get for the answer if a coded question
                String value;
                TreeElement valueChild = question.getChild("value", 0);
                IAnswerData answer = valueChild.getValue();
                if (answer == null) {
                    continue;
                }
                Object answerObject = answer.getValue();
                if (answerObject == null) {
                    continue;
                }
                if ("CWE".equals(openmrsDatatype.getValue().getValue())) {
                    value = getConceptId(xformConceptIds, answerObject.toString()).toString();
                } else {
                    value = answerObject.toString();
                }

                ContentValues observation = new ContentValues(common);
                // Set to the id for now, we'll replace with uuid later
                observation.put(Contracts.Observations.CONCEPT_UUID, id.toString());
                observation.put(Contracts.Observations.VALUE, value);
                toInsert.add(observation);
            }
        }

        String inClause = Joiner.on(",").join(xformConceptIds);
        // Get a map from client ids to UUIDs from our local concept database.
        HashMap<String, String> idToUuid = new HashMap<>();
        Cursor cursor = resolver.query(Contracts.Concepts.CONTENT_URI,
                new String[]{Contracts.Concepts._ID, Contracts.Concepts.XFORM_ID},
                Contracts.Concepts.XFORM_ID + " IN (" + inClause + ")",
                null, null);
        try {
            while (cursor.moveToNext()) {
                idToUuid.put(cursor.getString(cursor.getColumnIndex(Contracts.Concepts.XFORM_ID)),
                        cursor.getString(cursor.getColumnIndex(Contracts.Concepts._ID)));
            }
        } finally {
            cursor.close();
        }

        // Remap concept ids to uuids, skipping anything we can't remap.
        for (Iterator<ContentValues> i = toInsert.iterator(); i.hasNext();) {
            ContentValues values = i.next();
            if (!mapIdToUuid(idToUuid, values, Contracts.Observations.CONCEPT_UUID)) {
                i.remove();
            }
            mapIdToUuid(idToUuid, values, Contracts.Observations.VALUE);
        }
        resolver.bulkInsert(Contracts.Observations.CONTENT_URI,
                toInsert.toArray(new ContentValues[toInsert.size()]));
    }

    private static boolean mapIdToUuid(HashMap<String, String> idToUuid, ContentValues values, String key) {
        String id = (String) values.get(key);
        String uuid = idToUuid.get(id);
        if (uuid == null) {
            return false;
        }
        values.put(key, uuid);
        return true;
    }

    private static Integer getConceptId(Set<Integer> accumulator, String encodedConcept) {
        Integer id = getConceptId(encodedConcept);
        if (id != null) {
            accumulator.add(id);
        }
        return id;
    }

    private static Integer getConceptId(String encodedConcept) {
        int idEnd = encodedConcept.indexOf('^');
        if (idEnd == -1) {
            return null;
        }
        String idString = encodedConcept.substring(0, idEnd);
        try {
            return Integer.parseInt(idString);
        } catch (NumberFormatException ex) {
            LOG.w("Strangely formatted id String " + idString);
            return null;
        }
    }

    private static void sendFormToServer(String patientUuid, String xml,
                                         Response.Listener<JSONObject> successListener) {
        OpenMrsXformsConnection connection =
                new OpenMrsXformsConnection(App.getConnectionDetails());
        connection.postXformInstance(patientUuid, xml,
                successListener,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        LOG.e("Did not submit form to server successfully", error);
                        if (error.networkResponse != null
                                && error.networkResponse.statusCode == 500) {
                            LOG.e("Internal error stack trace:\n");
                            // TODO(dxchen): This could throw an NPE!
                            LOG.e(new String(error.networkResponse.data, Charsets.UTF_8));
                        }
                    }
                });
    }

    private static String readFromPath(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    // Out of a list of OpenMRS Xform entries, find the form that matches the given uuid, or
    // return null if no xform is found.
    private static OpenMrsXformIndexEntry findUuid(List<OpenMrsXformIndexEntry> allEntries, String uuid) {
        for (OpenMrsXformIndexEntry entry : allEntries) {
            if (entry.uuid.equals(uuid)) {
                return entry;
            }
        }
        return null;
    }

    private static Response.ErrorListener getErrorListenerForTag(final String tag) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                LOG.e(error.toString());
            }
        };
    }

    /**
     * Show the ODK activity for viewing a saved form.
     *
     * @param caller the calling activity.
     */
    public static void showSavedXform(final Activity caller) {

        // This has to be at the start of anything that uses the ODK file system.
        Collect.getInstance().createODKDirs();

        final String selection = InstanceProviderAPI.InstanceColumns.STATUS + " != ?";
        final String[] selectionArgs = {InstanceProviderAPI.STATUS_SUBMITTED};
        final String sortOrder = InstanceProviderAPI.InstanceColumns.STATUS + " DESC, "
                + InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " ASC";

        final Uri instanceUri;
        final String instancePath;
        final String jrFormId;
        Cursor instanceCursor = null;
        try {
            instanceCursor = caller.getContentResolver().query(
                    CONTENT_URI, new String[]{_ID, INSTANCE_FILE_PATH, JR_FORM_ID}, selection,
                    selectionArgs, sortOrder);
            if (instanceCursor.getCount() == 0) {
                return;
            }
            instanceCursor.moveToFirst();

            // The URI code mostly copied from InstanceChooserList.onListItemClicked()
            instanceUri =
                    ContentUris.withAppendedId(CONTENT_URI,
                            instanceCursor.getLong(instanceCursor.getColumnIndex(_ID)));
            instancePath = instanceCursor.getString(instanceCursor.getColumnIndex(INSTANCE_FILE_PATH));
            jrFormId = instanceCursor.getString(instanceCursor.getColumnIndex(JR_FORM_ID));
        } finally {
            if (instanceCursor != null) {
                instanceCursor.close();
            }
        }

        // It looks like we need to load the form as well. Which is odd, because
        // the main menu doesn't seem to do this, but without the FormLoaderTask run
        // there is no form manager for the HierarchyActivity.
        FormLoaderTask loaderTask = new FormLoaderTask(instancePath, null, null);

        final String formPath;
        Cursor formCursor = null;
        try {
            formCursor = caller.getContentResolver().query(
                    FormsProviderAPI.FormsColumns.CONTENT_URI,
                    new String[]{FormsProviderAPI.FormsColumns.FORM_FILE_PATH},
                    FormsProviderAPI.FormsColumns.JR_FORM_ID + " = ?",
                    new String[]{jrFormId}, null);
            if (formCursor.getCount() == 0) {
                LOG.e("Loading forms for displaying " + jrFormId + " and got no forms,");
                return;
            }
            if (formCursor.getCount() != 1) {
                LOG.e("Loading forms for displaying instance, expected only 1. Got multiple so using first.");
            }
            formCursor.moveToFirst();
            formPath = formCursor.getString(
                    formCursor.getColumnIndex(FormsProviderAPI.FormsColumns.FORM_FILE_PATH));
        } finally {
            if (formCursor != null) {
                formCursor.close();
            }
        }

        loaderTask.setFormLoaderListener(new FormLoaderListener() {
            @Override
            public void loadingComplete(FormLoaderTask task) {
                // This was extracted from FormEntryActivity.loadingComplete()
                FormController formController = task.getFormController();
                Collect.getInstance().setFormController(formController);

                Intent intent = new Intent(caller, FormHierarchyActivity.class);
                intent.setData(instanceUri);
                intent.setAction(Intent.ACTION_PICK);
                caller.startActivity(intent);
            }

            @Override
            public void loadingError(String errorMsg) {
            }

            @Override
            public void onProgressStep(String stepMessage) {
            }
        });
        loaderTask.execute(formPath);
    }
}
