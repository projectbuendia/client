// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xform.parse.XFormParser;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.model.Preset;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.tasks.DeleteInstancesTask;
import org.odk.collect.android.utilities.FileUtils;
import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.events.FetchXformFailedEvent;
import org.projectbuendia.client.events.SubmitXformFailedEvent;
import org.projectbuendia.client.events.SubmitXformSucceededEvent;
import org.projectbuendia.client.net.OdkDatabase;
import org.projectbuendia.client.net.OdkXformSyncTask;
import org.projectbuendia.client.net.OpenMrsXformIndexEntry;
import org.projectbuendia.client.net.OpenMrsXformsConnection;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import de.greenrobot.event.EventBus;

import static android.provider.BaseColumns._ID;
import static java.lang.String.format;
import static org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns
    .CONTENT_ITEM_TYPE;
import static org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH;

/** Convenience class for launching ODK to display an Xform. */
public class OdkActivityLauncher {

    private static final Logger LOG = Logger.create();

    /**
     * Fetches all xforms from the server and caches them. If any error occurs during fetching,
     * a failed event is triggered.
     */
    public static void fetchAndCacheAllXforms() {
        new OpenMrsXformsConnection(App.getConnectionDetails()).listXforms(
            new Response.Listener<List<OpenMrsXformIndexEntry>>() {
                @Override public void onResponse(final List<OpenMrsXformIndexEntry> response) {
                    for (OpenMrsXformIndexEntry formEntry : response) {
                        fetchAndCacheXForm(formEntry);
                    }
                }
            }, new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError error) {
                    handleFetchError(error);
                }
            });
    }

    /**
     * Fetches the xform specified by the form uuid.
     *
     * @param formEntry the {@link OpenMrsXformIndexEntry} object containing the uuid form
     */
    public static void fetchAndCacheXForm(OpenMrsXformIndexEntry formEntry) {
        new OdkXformSyncTask(null).fetchAndAddXFormToDb(formEntry.uuid,
            formEntry.makeFileForForm());
    }

    /**
     * Loads the xform from the cache and launches ODK using it. If the cache is not available,
     * the app tries to fetch it from the server. If no form is got, it is triggered a failed event.
     * @param callingActivity the {@link Activity} requesting the xform; when ODK closes, the user
     *                        will be returned to this activity
     * @param uuidToShow      UUID of the form to show
     * @param requestCode     if >= 0, this code will be returned in onActivityResult() when the
     *                        activity exits
     * @param patient         the {@link org.odk.collect.android.model.Patient} that this form entry will
     *                        correspond to
     * @param fields          a {@link Preset} object with any form fields that should be
     *                        pre-populated
     */
    public static void fetchAndShowXform(
        final Activity callingActivity,
        final String uuidToShow,
        final int requestCode,
        @Nullable final org.odk.collect.android.model.Patient patient,
        @Nullable final Preset fields) {
        LOG.i("Trying to fetch it from cache.");
        if (loadXformFromCache(callingActivity, uuidToShow, requestCode, patient, fields)) {
            return;
        }

        new OpenMrsXformsConnection(App.getConnectionDetails()).listXforms(
            new Response.Listener<List<OpenMrsXformIndexEntry>>() {
                @Override public void onResponse(final List<OpenMrsXformIndexEntry> response) {
                    if (response.isEmpty()) {
                        LOG.i("No forms found");
                        EventBus.getDefault().post(new FetchXformFailedEvent(
                            FetchXformFailedEvent.Reason.NO_FORMS_FOUND));
                        return;
                    }
                    showForm(callingActivity, requestCode, patient, fields, findUuid(response,
                        uuidToShow));
                }
            }, new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError error) {
                    LOG.e(error, "Fetching xform list from server failed. ");
                    handleFetchError(error);
                }
            });
    }

    /**
     * Shows the form with the given id in ODK collect.
     * @param callingActivity the {@link Activity} requesting the xform; when ODK closes, the user
     *                        will be returned to this activity
     * @param requestCode     if >= 0, this code will be returned in onActivityResult() when the
     *                        activity exits
     * @param formId          the id of the form to fetch
     * @param patient         the {@link org.odk.collect.android.model.Patient} that this form entry will
     *                        correspond to
     * @param fields          a {@link Preset} object with any form fields that should be
     *                        pre-populated
     */
    public static void showOdkCollect(
        Activity callingActivity,
        int requestCode,
        long formId,
        @Nullable org.odk.collect.android.model.Patient patient,
        @Nullable Preset fields) {
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
     * Loads the xform from the cache and launches ODK using it. Return true if the cache is
     * available.
     * @param callingActivity the {@link Activity} requesting the xform; when ODK closes, the user
     *                        will be returned to this activity
     * @param uuidToShow      UUID of the form to show
     * @param requestCode     if >= 0, this code will be returned in onActivityResult() when the
     *                        activity exits
     * @param patient         the {@link org.odk.collect.android.model.Patient} that this form entry will
     *                        correspond to
     * @param fields          a {@link Preset} object with any form fields that should be
     *                        pre-populated
     */
    private static boolean loadXformFromCache(final Activity callingActivity,
                                              final String uuidToShow,
                                              final int requestCode,
                                              @Nullable final org.odk.collect.android.model.Patient patient,
                                              @Nullable final Preset fields) {
        List<OpenMrsXformIndexEntry> entries = getLocalFormEntries();
        OpenMrsXformIndexEntry formToShow = findUuid(entries, uuidToShow);
        if (!formToShow.makeFileForForm().exists()) return false;

        LOG.i(format("Using form %s from local cache.", uuidToShow));
        showForm(callingActivity, requestCode, patient, fields, formToShow);

        return true;
    }

    private static List<OpenMrsXformIndexEntry> getLocalFormEntries() {
        List<OpenMrsXformIndexEntry> entries = new ArrayList<>();

        final ContentResolver resolver = App.getInstance().getContentResolver();
        Cursor c = resolver.query(Contracts.Forms.CONTENT_URI, new String[] {Contracts.Forms.UUID,
            Contracts.Forms.NAME}, null, null, null);
        try {
            while (c.moveToNext()) {
                String uuid = Utils.getString(c, Contracts.Forms.UUID);
                String name = Utils.getString(c, Contracts.Forms.NAME);
                long date = 0; // date is not important here
                entries.add(new OpenMrsXformIndexEntry(uuid, name, date));
            }
        } finally {
            c.close();
        }

        return entries;
    }

    /**
     * Launches ODK using the requested form.
     * @param callingActivity the {@link Activity} requesting the xform; when ODK closes, the user
     *                        will be returned to this activity
     * @param requestCode     if >= 0, this code will be returned in onActivityResult() when the
     *                        activity exits
     * @param patient         the {@link org.odk.collect.android.model.Patient} that this form entry will
     *                        correspond to
     * @param fields          a {@link Preset} object with any form fields that should be
     *                        pre-populated
     * @param formToShow    a {@link OpenMrsXformIndexEntry} object representing the form that
     *                       should be opened
     */
    private static void showForm(final Activity callingActivity,
                                 final int requestCode,
                                 @Nullable final org.odk.collect.android.model.Patient patient,
                                 @Nullable final Preset fields,
                                 final OpenMrsXformIndexEntry formToShow) {
        new OdkXformSyncTask(new OdkXformSyncTask.FormWrittenListener() {
            @Override public void formWritten(File path, String uuid) {
                LOG.i("wrote form " + path);
                showOdkCollect(
                    callingActivity,
                    requestCode,
                    OdkDatabase.getFormIdForPath(path),
                    patient,
                    fields);
            }
        }).execute(formToShow);
    }

    // Out of a list of OpenMRS Xform entries, find the form that matches the given uuid, or
    // return null if no xform is found.
    private static OpenMrsXformIndexEntry findUuid(
        List<OpenMrsXformIndexEntry> allEntries, String uuid) {
        for (OpenMrsXformIndexEntry entry : allEntries) {
            if (entry.uuid.equals(uuid)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Convenient shared code for handling an ODK activity result.
     * @param context           the application context
     * @param settings          the application settings
     * @param patientUuid       the patient to add an observation to, or null to create a new patient
     * @param resultCode        the result code sent from Android activity transition
     * @param data              the incoming intent
     */
    public static void sendOdkResultToServer(
        final Context context,
        final AppSettings settings,
        @Nullable final String patientUuid,
        int resultCode,
        Intent data) {

        if(isActivityCanceled(resultCode, data)) return;

        final Uri uri = data.getData();
        if(!validateContentUriType(context, uri, CONTENT_ITEM_TYPE)) {
            LOG.e("Tried to load a content URI of the wrong type: " + uri);
            EventBus.getDefault().post(
                new SubmitXformFailedEvent(SubmitXformFailedEvent.Reason.CLIENT_ERROR));
            return;
        }

        final String filePath = getFormFilePath(context, uri);
        if(!validateFilePath(filePath, uri)) {
            LOG.e("No file path for form instance: " + uri);
            EventBus.getDefault().post(
                new SubmitXformFailedEvent(SubmitXformFailedEvent.Reason.CLIENT_ERROR));
            return;
        }

        final Long formIdToDelete = getIdToDeleteAfterUpload(context, uri);
        if(!validateIdToDeleteAfterUpload(formIdToDelete, uri)) {
            LOG.e("No id to delete for after upload: " + uri);
            EventBus.getDefault().post(
                new SubmitXformFailedEvent(SubmitXformFailedEvent.Reason.CLIENT_ERROR));
            return;
        }

        // Temporary code for messing about with xform instance, reading values.
        byte[] fileBytes = FileUtils.getFileAsBytes(new File(filePath));

        // get the root of the saved and template instances
        final TreeElement savedRoot = XFormParser.restoreDataModel(fileBytes, null).getRoot();

        final String xml = readFromPath(filePath);
        if(!validateXml(xml)) {
            LOG.e("Xml form is not valid.");
            EventBus.getDefault().post(
                new SubmitXformFailedEvent(SubmitXformFailedEvent.Reason.CLIENT_ERROR));
            return;
        }

        sendFormToServer(patientUuid, xml,
            new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                    LOG.i("Created new encounter successfully on server" + response.toString());
                    // Only locally cache new observations, not new patients.
                    if (patientUuid != null) {
                        updateObservationCache(patientUuid, savedRoot, context.getContentResolver());
                    }
                    if (!settings.getKeepFormInstancesLocally()) {
                        deleteLocalFormInstances(formIdToDelete);
                    }
                    EventBus.getDefault().post(new SubmitXformSucceededEvent());
                }
            }, new Response.ErrorListener() {
                @Override public void onErrorResponse(VolleyError error) {
                    LOG.e(error, "Error submitting form to server");
                    handleSubmitError(error);
                }
            });
    }

    /**
     * Checks if the file path is valid. If so, it returns {@code true}. Otherwise returns
     * <code>false</code>.
     * @param filePath               the file path to be validated
     * @param uri                    the form uri
     */
    private static boolean validateFilePath(String filePath, Uri uri) {
        return filePath != null;
    }

    /**
     * Checks if the URI has a valid type. If so, returns {@code true}. Otherwise,  returns {@code false}
     * @param context           the application context
     * @param uri               the URI to be checked
     * @param validType         the accepted type for URI
     */
    private static boolean validateContentUriType(final Context context, final Uri uri,
                                                  final String validType) {
        return context.getContentResolver().getType(uri).equals(validType);
    }

    /**
     * Validates the id to be deleted after the form upload. If id is valid, it returns
     * {@code true}. Otherwise, returns {@code false}.
     * @param id           the id to be deleted
     * @param uri               the URI containing the id to be deleted
     */
    private static boolean validateIdToDeleteAfterUpload(final Long id, Uri uri) {
       return id != null;
    }

    /**
     * Validates the xml. Returns {@code true} if it is valid. Otherwise, returns {@code false}
     */
    private static boolean validateXml(String xml) {
       return xml != null;
    }

    private static void deleteLocalFormInstances(Long formIdToDelete) {
        //Code largely copied from InstanceUploaderTask to delete on upload
        DeleteInstancesTask dit = new DeleteInstancesTask();
        dit.setContentResolver(
            Collect.getInstance().getApplication()
                .getContentResolver());
        dit.execute(formIdToDelete);
    }

    /**
     * Returns the form file path queried from the given {@link Uri}. If no file path was found,
     * it returns <code>null</code>.
     * @param context           the application context
     * @param uri               the URI containing the form file path
     */
    private static String getFormFilePath(final Context context, final Uri uri) {
        Cursor instanceCursor = null;
        try {
            instanceCursor = getCursorAtRightPosition(context, uri);
            if(instanceCursor == null) return null;

            return instanceCursor.getString(instanceCursor.getColumnIndex(INSTANCE_FILE_PATH));
        } finally {
            if (instanceCursor != null) {
                instanceCursor.close();
            }
        }
    }

    /**
     * Returns the id to be deleted after the form upload, which was queried from the given
     * {@link Uri}. If no id was found, it returns <code>null</code>.
     * @param context           the application context
     * @param uri               the URI containing the id to be deleted
     */
    private static Long getIdToDeleteAfterUpload(final Context context, final Uri uri) {
        Cursor instanceCursor = null;
        try {
            instanceCursor = getCursorAtRightPosition(context, uri);
            if(instanceCursor == null) return null;

            int columnIndex = instanceCursor.getColumnIndex(_ID);
            if (columnIndex == -1) return  null;

           return instanceCursor.getLong(columnIndex);
        } finally {
            if (instanceCursor != null) {
                instanceCursor.close();
            }
        }
    }

    /**
     * Returns the form {@link Cursor} ready to be used. If no form was found, it triggers a
     * {@link SubmitXformFailedEvent} event and returns <code>null</code>.
     * @param context           the application context
     * @param uri               the URI to be queried
     */
    private static Cursor getCursorAtRightPosition(final Context context, final Uri uri) {
        Cursor instanceCursor = context.getContentResolver().query(uri, null, null, null, null);
        if (instanceCursor.getCount() != 1) {
            LOG.e("The form that we tried to load did not exist: " + uri);
            EventBus.getDefault().post(
                new SubmitXformFailedEvent(SubmitXformFailedEvent.Reason.CLIENT_ERROR));
            return null;
        }
        instanceCursor.moveToFirst();

        return instanceCursor;
    }

    /**
     * Returns true if the activity was canceled
     * @param resultCode        the result code sent from Android activity transition
     * @param data              the incoming intent
     */
    private static boolean isActivityCanceled(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) return true;
        if (data == null || data.getData() == null) {
            LOG.i("No data for form result, probably cancelled.");
            return true;
        }
        return false;
    }

    private static void sendFormToServer(String patientUuid, String xml,
                                         Response.Listener<JSONObject> successListener,
                                         Response.ErrorListener errorListener) {
        OpenMrsXformsConnection connection =
            new OpenMrsXformsConnection(App.getConnectionDetails());
        connection.postXformInstance(patientUuid, xml, successListener, errorListener);
    }

    private static void handleSubmitError(VolleyError error) {
        SubmitXformFailedEvent.Reason reason =  SubmitXformFailedEvent.Reason.UNKNOWN;

        if (error instanceof TimeoutError) {
            reason = SubmitXformFailedEvent.Reason.SERVER_TIMEOUT;
        } else if (error.networkResponse != null) {
            switch (error.networkResponse.statusCode) {
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                case HttpURLConnection.HTTP_FORBIDDEN:
                    reason = SubmitXformFailedEvent.Reason.SERVER_AUTH;
                    break;
                case HttpURLConnection.HTTP_NOT_FOUND:
                    reason = SubmitXformFailedEvent.Reason.SERVER_BAD_ENDPOINT;
                    break;
                case HttpURLConnection.HTTP_INTERNAL_ERROR:
                    if (error.networkResponse.data == null) {
                        LOG.e("Server error, but no internal error stack trace available.");
                    } else {
                        LOG.e(new String(error.networkResponse.data, Charsets.UTF_8));
                        LOG.e("Server error. Internal error stack trace:\n");
                    }
                    reason = SubmitXformFailedEvent.Reason.SERVER_ERROR;
                    break;
                default:
                    reason = SubmitXformFailedEvent.Reason.SERVER_ERROR;
                    break;
            }
        }

        EventBus.getDefault().post(new SubmitXformFailedEvent(reason, error));
    }

    private static void handleFetchError(VolleyError error) {
        FetchXformFailedEvent.Reason reason =
            FetchXformFailedEvent.Reason.SERVER_UNKNOWN;
        if (error.networkResponse != null) {
            switch (error.networkResponse.statusCode) {
                case HttpURLConnection.HTTP_FORBIDDEN:
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    reason = FetchXformFailedEvent.Reason.SERVER_AUTH;
                    break;
                case HttpURLConnection.HTTP_NOT_FOUND:
                    reason = FetchXformFailedEvent.Reason.SERVER_BAD_ENDPOINT;
                    break;
                case HttpURLConnection.HTTP_INTERNAL_ERROR:
                default:
                    reason = FetchXformFailedEvent.Reason.SERVER_UNKNOWN;
            }
        }
        EventBus.getDefault().post(new FetchXformFailedEvent(reason, error));
    }

    /**
     * Returns the xml form as a String from the path. If for any reason, the file couldn't be read,
     * it returns <code>null</code>
     * @param path      the path to be read
     */
    private static String readFromPath(String path) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            LOG.e(e, format("Failed to read xml form into a String. FilePath=  ", path));
            return null;
        }
    }

    /**
     * Caches the observation changes locally for a given patient.
     */
    private static void updateObservationCache(String patientUuid, TreeElement savedRoot,
                                               ContentResolver resolver) {
        ContentValues common = new ContentValues();
        // It's critical that UUID is {@code null} for temporary observations, so we make it
        // explicit here. See {@link Contracts.Observations.UUID} for details.
        common.put(Contracts.Observations.UUID, (String) null);
        common.put(Contracts.Observations.PATIENT_UUID, patientUuid);

        final DateTime encounterTime = getEncounterAnswerDateTime(savedRoot);
        if(encounterTime == null) return;
        common.put(Contracts.Observations.ENCOUNTER_MILLIS, encounterTime.getMillis());
        common.put(Contracts.Observations.ENCOUNTER_UUID, UUID.randomUUID().toString());

        Set<Integer> xformConceptIds = new HashSet<>();
        List<ContentValues> toInsert = getAnsweredObservations(common, savedRoot, xformConceptIds);
        Map<String, String> xformIdToUuid = mapFormConceptIdToUuid(xformConceptIds, resolver);

        // Remap concept ids to uuids, skipping anything we can't remap.
        for (Iterator<ContentValues> i = toInsert.iterator(); i.hasNext(); ) {
            ContentValues values = i.next();
            if (!mapIdToUuid(xformIdToUuid, values, Contracts.Observations.CONCEPT_UUID)) {
                i.remove();
            }
            mapIdToUuid(xformIdToUuid, values, Contracts.Observations.VALUE);
        }

        resolver.bulkInsert(Contracts.Observations.CONTENT_URI,
            toInsert.toArray(new ContentValues[toInsert.size()]));
    }

    /** Get a map from XForm ids to UUIDs from our local concept database. */
    private static Map<String, String> mapFormConceptIdToUuid(Set<Integer> xformConceptIds,
                                                              ContentResolver resolver) {
        String inClause = Joiner.on(",").join(xformConceptIds);

        HashMap<String, String> xformIdToUuid = new HashMap<>();
        Cursor cursor = resolver.query(Contracts.Concepts.CONTENT_URI,
            new String[] {Contracts.Concepts.UUID, Contracts.Concepts.XFORM_ID},
            Contracts.Concepts.XFORM_ID + " IN (" + inClause + ")",
            null, null);

        try {
            while (cursor.moveToNext()) {
                xformIdToUuid.put(Utils.getString(cursor, Contracts.Concepts.XFORM_ID),
                    Utils.getString(cursor, Contracts.Concepts.UUID));
            }
        } finally {
            cursor.close();
        }

        return xformIdToUuid;
    }

    /**
     * Returns a {@link ContentValues} list containing the id concept and the answer valeu from
     * all answered observations. Returns a empty {@link List} if no observation was answered.
     *
     * @param common                        the current content values.
     * @param savedRoot                     the root tree form element
     * @param xformConceptIdsAccumulator    the set to store the form concept ids found
     */
    private static List<ContentValues> getAnsweredObservations(ContentValues common,
                                                                    TreeElement savedRoot,
                                                                    Set<Integer> xformConceptIdsAccumulator) {
        List<ContentValues> answeredObservations = new ArrayList<>();
        for (int i = 0; i < savedRoot.getNumChildren(); i++) {
            TreeElement group = savedRoot.getChildAt(i);
            if (group.getNumChildren() == 0) continue;
            for (int j = 0; j < group.getNumChildren(); j++) {
                TreeElement question = group.getChildAt(j);
                TreeElement openmrsConcept = question.getAttribute(null, "openmrs_concept");
                TreeElement openmrsDatatype = question.getAttribute(null, "openmrs_datatype");
                if (openmrsConcept == null || openmrsDatatype == null) continue;

                // Get the concept for the question.
                // eg "5088^Temperature (C)^99DCT"
                String encodedConcept = (String) openmrsConcept.getValue().getValue();
                Integer id = getConceptId(xformConceptIdsAccumulator, encodedConcept);
                if (id == null) continue;

                // Also get for the answer if a coded question
                TreeElement valueChild = question.getChild("value", 0);
                IAnswerData answer = valueChild.getValue();
                if (answer == null || answer.getValue() == null) continue;

                Object answerObject = answer.getValue();
                String value;
                if ("CWE".equals(openmrsDatatype.getValue().getValue())) {
                    value = getConceptId(xformConceptIdsAccumulator, answerObject.toString()).toString();
                } else {
                    value = answerObject.toString();
                }

                ContentValues observation = new ContentValues(common);
                // Set to the id for now, we'll replace with uuid later
                observation.put(Contracts.Observations.CONCEPT_UUID, id.toString());
                observation.put(Contracts.Observations.VALUE, value);

                answeredObservations.add(observation);
            }
        }
        return answeredObservations;
    }

    /**
     * Returns the encounter's answer date time. Returns <code>null</code> if it cannot be retrieved.
     */
    private static DateTime getEncounterAnswerDateTime(TreeElement root) {
        TreeElement encounter = root.getChild("encounter", 0);
        if (encounter == null) {
            LOG.e("No encounter found in instance");
            return null;
        }

        TreeElement encounterDatetime =
            encounter.getChild("encounter.encounter_datetime", 0);
        if (encounterDatetime == null) {
            LOG.e("No encounter date time found in instance");
            return null;
        }

        IAnswerData dateTimeValue = encounterDatetime.getValue();
        try {
         return  ISODateTimeFormat.dateTime().parseDateTime((String) dateTimeValue.getValue());
        } catch (IllegalArgumentException e) {
            LOG.e("Could not parse datetime" + dateTimeValue.getValue());
            return null;
        }
    }

    private static Integer getConceptId(Set<Integer> accumulator, String encodedConcept) {
        Integer id = getConceptId(encodedConcept);
        if (id != null) {
            accumulator.add(id);
        }
        return id;
    }

    private static boolean mapIdToUuid(
        Map<String, String> idToUuid, ContentValues values, String key) {
        String id = (String) values.get(key);
        String uuid = idToUuid.get(id);
        if (uuid == null) {
            return false;
        }
        values.put(key, uuid);
        return true;
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
}
