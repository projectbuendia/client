package org.msf.records.ui;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.msf.records.App;
import org.msf.records.net.OdkDatabase;
import org.msf.records.net.OdkXformSyncTask;
import org.msf.records.net.OpenMrsXformIndexEntry;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.provider.FormsProviderAPI;

import java.io.File;
import java.util.List;

/**
 * Convenience class for launching ODK to display an Xform.
 */
public class OdkActivityLauncher {

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
