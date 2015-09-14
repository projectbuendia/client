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

package org.projectbuendia.client.net;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.base.Preconditions;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.tasks.DiskSyncTask;
import org.projectbuendia.client.App;
import org.projectbuendia.client.events.FetchXformFailedEvent;
import org.projectbuendia.client.events.FetchXformSucceededEvent;
import org.projectbuendia.client.utils.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.greenrobot.event.EventBus;

/**
 * Synchronizes 1 or more OpenMRS provided forms into the ODK database storage. Very like
 * {@link org.odk.collect.android.tasks.DiskSyncTask} or
 * {@link org.odk.collect.android.tasks.DownloadFormsTask}
 * <p/>
 * <p>Takes the UUID and, if the form doesn't exist in ODK storage, fetches it from OpenMRS, then
 * creates {$uuid}.xml in storage. Finally, the form is inserted into ODK's local metadata DB.
 */
public class OdkXformSyncTask extends AsyncTask<OpenMrsXformIndexEntry, Void, Void> {

    private static final Logger LOG = Logger.create();

    @Nullable
    private final FormWrittenListener mFormWrittenListener;

    public static interface FormWrittenListener {
        public void formWritten(File path, String uuid);
    }

    public OdkXformSyncTask(@Nullable FormWrittenListener formWrittenListener) {
        this.mFormWrittenListener = formWrittenListener;
    }

    private static boolean writeStringToFile(String response, File proposedPath) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(proposedPath);
            writer.write(response);
            return true;
        } catch (IOException e) {
            LOG.e(e, "failed to write downloaded xform to ODK forms directory");
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOG.e(e, "failed to close writer into ODK directory");
                }
            }
        }
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
            final boolean isUpdate;
            final boolean usersHaveChanged = App.getUserManager().isDirty();
            try {
                cursor = getCursorForFormFile(proposedPath, new String[] {
                    FormsProviderAPI.FormsColumns.DATE
                });
                boolean isInDatabase = cursor.getCount() > 0;
                if (isInDatabase) {
                    if (cursor.getCount() != 1) {
                        LOG.e("Saw " + cursor.getCount() + " rows for " + proposedPath.getPath());
                        // In a fail-fast environment we would crash here, but we will keep going
                        // to lead the code more robust to errors in the field.
                    }
                    Preconditions.checkArgument(cursor.getColumnCount() == 1);
                    cursor.moveToNext();
                    long existingTimestamp = cursor.getLong(0);
                    isNew = (existingTimestamp < formInfo.dateChanged);
                    isUpdate = true;

                    if (isNew || usersHaveChanged) {
                        LOG.i("Form " + formInfo.uuid + " requires an update."
                            + " (Local creation date: " + existingTimestamp
                            + ", (Latest version: " + formInfo.dateChanged + ")"
                            + ", (Invalidated by UserManager: " + usersHaveChanged + ")");
                    }
                } else {
                    LOG.i("Form " + formInfo.uuid + " not found in database.");
                    isNew = true;
                    isUpdate = false;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            if (!isNew && !usersHaveChanged) {
                LOG.i("Using form " + formInfo.uuid + " from local cache.");
                if (mFormWrittenListener != null) {
                    mFormWrittenListener.formWritten(proposedPath, formInfo.uuid);
                }
                EventBus.getDefault().post(new FetchXformSucceededEvent());
                continue;
            }

            LOG.i("fetching " + formInfo.uuid);
            // Doesn't exist, so insert it
            // Fetch the file from OpenMRS
            openMrsXformsConnection.getXform(formInfo.uuid, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    LOG.i("adding form to db " + response);
                    new AddFormToDbAsyncTask(mFormWrittenListener, formInfo.uuid, isUpdate)
                        .execute(new FormToWrite(response, proposedPath));
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    LOG.e(error, "failed to fetch file");
                    EventBus.getDefault().post(new FetchXformFailedEvent(
                        FetchXformFailedEvent.Reason.SERVER_FAILED_TO_FETCH, error));
                }
            });
        }
        return null;
    }

    /**
     * Get a Cursor for the form from the filename. If there is more than one they are ordered
     * descending by id, so most recent is first.
     * @param proposedPath the path for the forms file
     * @param projection   a projection of fields to get
     * @return the Cursor pointing to ideally one form.
     */
    public static Cursor getCursorForFormFile(File proposedPath, String[] projection) {
        String[] selectionArgs = {
            proposedPath.getAbsolutePath()
        };
        String selection = FormsProviderAPI.FormsColumns.FORM_FILE_PATH + "=?";
        return Collect.getInstance()
            .getApplication()
            .getContentResolver()
            .query(FormsProviderAPI.FormsColumns.CONTENT_URI, projection, selection,
                selectionArgs, FormsProviderAPI.FormsColumns._ID + " DESC");
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

        private final FormWrittenListener mFormWrittenListener;
        private final String mUuid;
        private final boolean mUpdate;

        private AddFormToDbAsyncTask(
            @Nullable FormWrittenListener formWrittenListener,
            String uuid,
            boolean update) {
            mFormWrittenListener = formWrittenListener;
            mUuid = uuid;
            mUpdate = update;
        }

        @Override
        protected File doInBackground(FormToWrite[] params) {
            Preconditions.checkArgument(params.length != 0);

            String form = params[0].form;
            File proposedPath = params[0].path;
            // Write file into OpenMRS forms directory.
            if (!writeStringToFile(form, proposedPath)) {
                // we failed to load it, just skip for now
                return null;
            }

            // do the equivalent of DownloadFormsTask.findExistingOrCreateNewUri() or
            // DiskSyncTask step 4 to insert the file into the database
            ContentValues cv;
            try {
                cv = DiskSyncTask.buildContentValues(proposedPath);
            } catch (IllegalArgumentException e) {
                // yuck, but this is what it throws on a bad parse
                LOG.e(e, "Failed to parse: " + proposedPath);
                return null;
            }

            // insert into content provider
            try {
                ContentResolver contentResolver =
                    Collect.getInstance().getApplication().getContentResolver();
                // Always replace existing forms.
                cv.put(FormsProviderAPI.SQL_INSERT_OR_REPLACE, true);
                contentResolver.insert(FormsProviderAPI.FormsColumns.CONTENT_URI, cv);
            } catch (SQLException e) {
                LOG.i(e, "failed to insert fetched file");
            }
            return proposedPath;
        }

        @Override
        protected void onPostExecute(File path) {
            super.onPostExecute(path);
            if (mFormWrittenListener != null && path != null) {
                mFormWrittenListener.formWritten(path, mUuid);
            }
            EventBus.getDefault().post(new FetchXformSucceededEvent());

            App.getUserManager().setDirty(false);
        }
    }
}
