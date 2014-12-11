package org.msf.records.ui;

import android.app.Activity;
import android.content.ContentUris;
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

import org.json.JSONObject;
import org.msf.records.App;
import org.msf.records.events.CreatePatientSucceededEvent;
import org.msf.records.net.OdkDatabase;
import org.msf.records.net.OdkXformSyncTask;
import org.msf.records.net.OpenMrsXformIndexEntry;
import org.msf.records.net.OpenMrsXformsConnection;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

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

    private static final String TAG = "OdkActivityLauncher";

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
                            Log.i(TAG, "No forms found");
                            return;
                        }
                        // Cache all the forms into the ODK form cache
                        new OdkXformSyncTask(new OdkXformSyncTask.FormWrittenListener() {
                            @Override
                            public void formWritten(File path, String uuid) {
                                Log.i(TAG, "wrote form " + path);
                                showOdkCollect(
                                        callingActivity,
                                        requestCode,
                                        OdkDatabase.getFormIdForPath(path),
                                        patient,
                                        fields);
                            }
                        }).execute(findUuid(response, uuidToShow));
                    }
                }, getErrorListenerForTag(TAG));
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
     * @param data the incoming intent
     */
    public static void sendOdkResultToServer(
            Context context,
            @Nullable String patientUuid,
            int resultCode,
            Intent data) {

        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        if (data == null || data.getData() == null) {
            // Cancelled.
            Log.i(TAG, "No data for form result, probably cancelled.");
            return;
        }

        Uri uri = data.getData();

        if (!context.getContentResolver().getType(uri).equals(
                InstanceProviderAPI.InstanceColumns.CONTENT_ITEM_TYPE)) {
            Log.e(TAG, "Tried to load a content URI of the wrong type: " + uri);
            return;
        }

        Cursor instanceCursor = null;
        try {
            instanceCursor = context.getContentResolver().query(uri,
                    null, null, null, null);
            if (instanceCursor.getCount() != 1) {
                Log.e(TAG, "The form that we tried to load did not exist: " + uri);
                return;
            }
            instanceCursor.moveToFirst();
            String instancePath = instanceCursor.getString(
                    instanceCursor.getColumnIndex(INSTANCE_FILE_PATH));
            if (instancePath == null) {
                Log.e(TAG, "No file path for form instance: " + uri);
                return;

            }
            int columnIndex = instanceCursor
                    .getColumnIndex(_ID);
            if (columnIndex == -1) {
                Log.e(TAG, "No id to delete for after upload: " + uri);
                return;
            }
            final long idToDelete = instanceCursor.getLong(columnIndex);

            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            final boolean keepFormInstancesLocally =
                    preferences.getBoolean("keep_form_instances_locally", false);

            sendFormToServer(patientUuid, readFromPath(instancePath),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.i(TAG, "Created new patient successfully on server"
                                    + response.toString());

                            if (!keepFormInstancesLocally) {
                                //Code largely copied from InstanceUploaderTask to delete on upload
                                DeleteInstancesTask dit = new DeleteInstancesTask();
                                dit.setContentResolver(
                                        Collect.getInstance().getApplication().getContentResolver());
                                dit.execute(idToDelete);

                                // TODO(dxchen): Change this to a proper event type.
                                EventBus.getDefault().post(new CreatePatientSucceededEvent());
                            }
                        }
                    });
        } catch (IOException e) {
            Log.e(TAG, "Failed to read xml form into a String " + uri, e);
        } finally {
            if (instanceCursor != null) {
                instanceCursor.close();
            }
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
                        Log.e(TAG, "Did not submit form to server successfully", error);
                        if (error.networkResponse != null
                                && error.networkResponse.statusCode == 500) {
                            Log.e(TAG, "Internal error stack trace:\n");
                            // TODO(dxchen): This could throw an NPE!
                            Log.e(TAG, new String(error.networkResponse.data, Charsets.UTF_8));
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
                Log.e(tag, error.toString());
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
        Collect.createODKDirs();

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
                Log.e(TAG, "Loading forms for displaying " + jrFormId + " and got no forms,");
                return;
            }
            if (formCursor.getCount() != 1) {
                Log.e(TAG, "Loading forms for displaying instance, expected only 1. Got multiple so using first.");
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
