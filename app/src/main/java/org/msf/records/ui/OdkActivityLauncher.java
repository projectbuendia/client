package org.msf.records.ui;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;
import org.msf.records.App;
import org.msf.records.net.OdkDatabase;
import org.msf.records.net.OdkXformSyncTask;
import org.msf.records.net.OpenMrsXformIndexEntry;
import org.msf.records.net.OpenMrsXformsConnection;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.tasks.DeleteInstancesTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import static org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH;

/**
 * Convenience class for launching ODK to display an Xform.
 */
public class OdkActivityLauncher {

    private static final String TAG = "OdkActivityLauncher";

    public static final int ODK_COLLECT_REQUEST_CODE = 1;

    public static void fetchXforms(final Activity callingActivity, final String uuidToShow) {
        final String tag = "fetchXforms";
        App.getmOpenMrsXformsConnection().listXforms(
                new Response.Listener<List<OpenMrsXformIndexEntry>>() {
                    @Override
                    public void onResponse(final List<OpenMrsXformIndexEntry> response) {
                        if (response.isEmpty()) {
                            Log.i(tag, "No forms found");
                            return;
                        }
                        // Cache all the forms into the ODK form cache
                        new OdkXformSyncTask(new OdkXformSyncTask.FormWrittenListener() {
                            @Override
                            public void formWritten(File path, String uuid) {
                                Log.i(tag, "wrote form " + path);
                                showOdkCollect(callingActivity, OdkDatabase.getFormIdForPath(path));
                            }
                        }).execute(findUuid(response, uuidToShow));
                    }
                }, getErrorListenerForTag(tag));
    }

    public static void showOdkCollect(Activity callingActivity, long formId) {
        Intent intent = new Intent(callingActivity, FormEntryActivity.class);
        Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, formId);
        intent.setData(formUri);
        intent.setAction(Intent.ACTION_PICK);
        callingActivity.startActivityForResult(intent, ODK_COLLECT_REQUEST_CODE);
    }


    /**
     * Convenient shared code for handling an ODK activity result.
     *
     * @param callingActivity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public static void sendOdkResultToServer(Activity callingActivity, int requestCode,
                                         int resultCode,
                                         Intent data) {
        if (requestCode != OdkActivityLauncher.ODK_COLLECT_REQUEST_CODE) {
            return;
        }

        if (data == null || data.getData() == null) {
            // Cancelled.
            Log.i(TAG, "No data for form result, probably cancelled.");
            return;
        }

        Uri uri = data.getData();

        if (!callingActivity.getContentResolver().getType(uri).equals(
                InstanceProviderAPI.InstanceColumns.CONTENT_ITEM_TYPE)) {
            Log.e(TAG, "Tried to load a content URI of the wrong type: " + uri);
            return;
        }

        Cursor instanceCursor = null;
        try {
            instanceCursor = callingActivity.getContentResolver().query(uri,
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
                    .getColumnIndex(InstanceProviderAPI.InstanceColumns._ID);
            if (columnIndex == -1) {
                Log.e(TAG, "No id to delete for after upload: " + uri);
                return;
            }
            final long idToDelete = instanceCursor.getLong(columnIndex);

            sendFormToServer(null /* create new patient */, readFromPath(instancePath),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.i(TAG, "Created new patient successfully on server"
                                    + response.toString());

                            // Code largely copied from InstanceUploaderTask to delete on upload
                            DeleteInstancesTask dit = new DeleteInstancesTask();
                            dit.setContentResolver(
                                    Collect.getInstance().getApplication().getContentResolver());
                            dit.execute(idToDelete);
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

    private static void sendFormToServer(String patientId, String xml,
                                  Response.Listener<JSONObject> successListener) {
        OpenMrsXformsConnection connection = App.getmOpenMrsXformsConnection();
        connection.postXformInstance(patientId, xml,
                successListener,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Did not submit  form to server successfully", error);
                    }
                });
    }

    private static String readFromPath(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        while((line = reader.readLine()) != null) {
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
}
