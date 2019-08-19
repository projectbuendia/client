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

package org.projectbuendia.client.widgets;

import android.app.ProgressDialog;
import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.sync.Database;
import org.projectbuendia.client.utils.Logger;

import java.io.File;

/** Custom Android preference widget that clears the database if new text is entered. */
public class EditAndClearDataPreference extends EditTextPreference {
    private static Logger LOG = Logger.create();
    private ProgressDialog dialog = null;

    public EditAndClearDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPositiveButtonText(R.string.clear_data_button);
    }

    public void onDialogClosed(boolean positive) {
        super.onDialogClosed(positive);
        if (positive) {
            dialog = ProgressDialog.show(getContext(), "Clearing data", "Clearing all local data...");
            App.getInstance().getSyncManager().setNewSyncsSuppressed(true);
            App.getInstance().getSyncManager().stopSyncing(this::clearAllData);
        }
    }

    private void clearAllData() {
        clearDatabase();
        clearMemoryState();
        clearOdkState();
        App.getInstance().getSyncManager().setNewSyncsSuppressed(false);
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void clearDatabase() {
        try {
            Database db = new Database(App.getInstance().getApplicationContext());
            db.clear();
            db.close();
        } catch (Throwable t) {
            LOG.e(t, "Failed to clear database");
        }
    }

    private void clearMemoryState() {
        try {
            App.getUserManager().reset();
            App.getModel().reset();
            App.getSettings().setSyncAccountInitialized(false);
        } catch (Throwable t) {
            LOG.e(t, "Failed to clear in-memory state");
        }
    }

    private void clearOdkState() {
        try {
            File filesDir = App.getInstance().getApplicationContext().getFilesDir();
            File odkDir = new File(filesDir, "odk");
            File odkTempDir = new File(filesDir, "odk-deleted." + System.currentTimeMillis());
            odkDir.renameTo(odkTempDir);
            recursivelyDelete(odkTempDir);
        } catch (Throwable t) {
            LOG.e(t, "Failed to clear ODK state");
        }
    }

    private void recursivelyDelete(File path) {
        if (path.isDirectory()) {
            for (File child : path.listFiles()) {
                recursivelyDelete(child);
            }
        }
        path.delete();
    }
}
