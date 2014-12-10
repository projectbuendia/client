package org.msf.records.net;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.base.Preconditions;

import org.msf.records.App;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.tasks.DiskSyncTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Synchronizes 1 or more OpenMRS provided forms into the ODK database storage. Very like
 * {@see org.odk.collect.android.tasks.DiskSyncTask} or
 * {@see org.odk.collect.android.tasks.DownloadFormsTask}
 *
 * Takes the UUID, if it doesn't exist in ODK storage fetches it from OpenMRS, then creates
 * {$uuid}.xml in storage. Finally inserts into ODK local metadata DB.
 *
 * @author nfortescue@google.com
 */
public class OdkXformSyncTask extends AsyncTask<OpenMrsXformIndexEntry, Void, Void> {

    private static final String TAG = "OdkXformSyncTask";

    @Nullable
    private FormWrittenListener formWrittenListener;

    public OdkXformSyncTask(@Nullable FormWrittenListener formWrittenListener) {
        this.formWrittenListener = formWrittenListener;
    }

    @Override
    protected Void doInBackground(OpenMrsXformIndexEntry... formInfos) {

        OpenMrsXformsConnection openMrsXformsConnection =
                new OpenMrsXformsConnection(App.getConnectionDetails());

        for (final OpenMrsXformIndexEntry formInfo : formInfos) {
            final File proposedPath = formInfo.makeFileForForm();

            // Check if the uuid already exists in the database.
            Cursor cursor = null;
            boolean isNew;
            try {
                cursor = getCursorForFormFile(proposedPath, new String[]{
                        FormsProviderAPI.FormsColumns.DATE
                });
                boolean isInDatabase = cursor.getCount() > 0;
                if (isInDatabase) {
                    Preconditions.checkArgument(cursor.getCount() == 1);
                    Preconditions.checkArgument(cursor.getColumnCount() == 1);
                    cursor.moveToNext();
                    long existingTimestamp = cursor.getLong(0);
                    isNew = (existingTimestamp < formInfo.dateChanged);

                    if (isNew) {
                        Log.i(TAG, "Form " + formInfo.uuid + " requires an update." +
                                " (Local creation date: " + existingTimestamp +
                                ", (Latest version: " + formInfo.dateChanged + ")");
                    }
                } else {
                    Log.i(TAG, "Form " + formInfo.uuid + " not found in database.");
                    isNew = true;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            if (!isNew) {
                Log.i(TAG, "Using form " + formInfo.uuid + " from local cache.");
                if (formWrittenListener != null) {
                    formWrittenListener.formWritten(proposedPath, formInfo.uuid);
                }
                continue;
            }

            Log.i(TAG, "fetching " + formInfo.uuid);
            // Doesn't exist, so insert it
            // Fetch the file from OpenMRS
            openMrsXformsConnection.getXform(formInfo.uuid, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i(TAG, "adding form to db " + response);
                    new AddFormToDbAsyncTask(formWrittenListener, formInfo.uuid)
                            .execute(new FormToWrite(response, proposedPath));
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // TODO(nfortescue): design error handling properly
                    Log.e(TAG, "failed to fetch file");
                }
            });
        }
        return null;
    }

    public static Cursor getCursorForFormFile(File proposedPath, String [] projection) {
        String[] selectionArgs = {
                proposedPath.getAbsolutePath()
        };
        String selection = FormsProviderAPI.FormsColumns.FORM_FILE_PATH + "=?";
        return Collect.getInstance()
                .getApplication()
                .getContentResolver()
                .query(FormsProviderAPI.FormsColumns.CONTENT_URI, projection, selection,
                        selectionArgs, null);
    }

    private static boolean writeStringToFile(String response, File proposedPath) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(proposedPath);
            writer.write(response);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "failed to write downloaded xform to ODK forms directory", e);
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.e(TAG, "failed to close writer into ODK directory", e);
                }
            }
        }
    }

    private static class FormToWrite {
        public final String form;
        public final File path;

        private FormToWrite(String form, File path) {
            this.form = Preconditions.checkNotNull(form);
            this.path = Preconditions.checkNotNull(path);
        }
    }

    private static class AddFormToDbAsyncTask extends AsyncTask<FormToWrite, Void, File> {

        private final FormWrittenListener formWrittenListener;
        private String mPath;
        private String mUuid;

        private AddFormToDbAsyncTask(
                @Nullable FormWrittenListener formWrittenListener, String uuid) {
            this.formWrittenListener = formWrittenListener;
            mUuid = uuid;
        }

        @Override
        protected File doInBackground(FormToWrite[] params) {
            Preconditions.checkArgument(params.length != 0);

            // really really hacky - fix the form problem, should be done server side
            String form = params[0].form.replaceAll("&amp;", "&");


            File proposedPath = params[0].path;
            // Write file into OpenMRS forms directory.
            if (!writeStringToFile(form, proposedPath)) {
                // we failed to load it, just skip for now
                return null;
            }

            // do the equivalent of DownloadFormsTask.findExistingOrCreateNewUri() or
            // DiskSyncTask step 4 to insert the file into the database
            ContentValues contentValues;
            try {
                contentValues = DiskSyncTask.buildContentValues(proposedPath);
            } catch (IllegalArgumentException ex) { // yuck, but this is what it throws on a bad parse
                Log.e(TAG, "Failed to parse: " + proposedPath, ex);
                return null;
            }

            // insert into content provider
            try {
                // insert failures are OK and expected if multiple
                // DiskSync scanners are active.
                Collect.getInstance().getApplication().getContentResolver()
                        .insert(FormsProviderAPI.FormsColumns.CONTENT_URI, contentValues);
            } catch (SQLException e) {
                Log.i(TAG, "failed to insert fetched file", e);
            }
            return proposedPath;
        }

        @Override
        protected void onPostExecute(File path) {
            super.onPostExecute(path);
            if (formWrittenListener != null && path != null) {
                formWrittenListener.formWritten(path, mUuid);
            }
        }
    }

    public static interface FormWrittenListener {
        public void formWritten(File path, String uuid);
    }
}
