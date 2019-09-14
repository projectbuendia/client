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

package org.projectbuendia.client.ui.chart;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.events.data.EncounterAddFailedEvent;
import org.projectbuendia.client.events.data.EncounterAddFailedEvent.Reason;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.utils.Utils;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/** A dialog that allows users to assign or change a patient's general condition. */
public final class AssignGeneralConditionDialog
    implements AdapterView.OnItemClickListener {

    @Nullable private AlertDialog mDialog;
    @Nullable private GridView mGridView;
    @Nullable private GeneralConditionAdapter mAdapter;

    private final Context mContext;
    @Nullable private final String mCurrentConditionUuid;
    private final ConditionSelectedCallback mConditionSelectedCallback;

    // TODO: Consider making this an event bus event rather than a callback so that we don't
    // have to worry about Activity context leaks.
    public interface ConditionSelectedCallback {
        /** Called when then user selects a general condition other than the current one. */
        void onNewConditionSelected(String newConditionUuid);
    }

    /**
     * Creates a new dialog.
     * @param context                   an activity context
     * @param currentConditionUuid      optional UUID representing the current general condition; may
     *                                  eventually be used for highlighting the selected entry but is
     *                                  currently unused
     * @param conditionSelectedCallback callback that responds to a condition selection
     */
    public AssignGeneralConditionDialog(
        Context context,
        @Nullable String currentConditionUuid,
        ConditionSelectedCallback conditionSelectedCallback) {
        mContext = checkNotNull(context);
        mCurrentConditionUuid = currentConditionUuid;
        mConditionSelectedCallback = checkNotNull(conditionSelectedCallback);
    }

    /** Builds and displays the dialog. */
    public void show() {
        FrameLayout frameLayout = new FrameLayout(mContext); // needed for outer margins to work
        View.inflate(mContext, R.layout.condition_grid, frameLayout);
        mGridView = frameLayout.findViewById(R.id.condition_selection_conditions);

        if (mGridView != null) {
            mAdapter = new GeneralConditionAdapter(
                mContext, ConceptUuids.GENERAL_CONDITION_UUIDS, mCurrentConditionUuid);
            mGridView.setAdapter(mAdapter);
            mGridView.setOnItemClickListener(this);
            mGridView.setSelection(1);
        }


        mDialog = new AlertDialog.Builder(mContext)
            .setTitle(R.string.action_assign_condition)
            .setView(frameLayout)
            .create();
        mDialog.show();
    }

    /**
     * Notifies the dialog that updating the patient's location has failed.
     * @param event The EncounterAddFailedEvent for the current failure.
     */
    public void onEncounterAddFailed(EncounterAddFailedEvent event) {
        mAdapter.setSelectedConditionUuid(mCurrentConditionUuid);

        int messageId =
            event.reason == Reason.FAILED_TO_AUTHENTICATE ?
                R.string.encounter_add_failed_to_authenticate :
            event.reason == Reason.FAILED_TO_FETCH_SAVED_OBSERVATION ?
                R.string.encounter_add_failed_to_fetch_saved :
            event.reason == Reason.FAILED_TO_SAVE_ON_SERVER ?
                R.string.encounter_add_failed_to_saved_on_server :
            event.reason == Reason.FAILED_TO_VALIDATE ?
                R.string.encounter_add_failed_invalid_encounter :
            event.reason == Reason.INTERRUPTED ?
                R.string.encounter_add_failed_interrupted :
            event.reason == Reason.INVALID_NUMBER_OF_OBSERVATIONS_SAVED ?
                // Hard to communicate to the user.
                R.string.encounter_add_failed_unknown_server_error :
            event.reason == Reason.UNKNOWN_SERVER_ERROR ?
                R.string.encounter_add_failed_unknown_server_error :
                R.string.encounter_add_failed_unknown_reason;

        String message = event.exception.getMessage();
        if (event.reason == Reason.FAILED_TO_VALIDATE) {
            // Validation reason typically starts after the message below.
            message = message.replaceFirst(".*failed to validate with reason: .*: ", "");
        }
        BigToast.show(messageId, message);
    }

    @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mAdapter == null) return;

        Utils.logUserAction("condition_assigned");
        String newConditionUuid = mAdapter.getItem(position);
        mAdapter.setSelectedConditionUuid(newConditionUuid);
        if (!isCurrentCondition(newConditionUuid)) {
            mConditionSelectedCallback.onNewConditionSelected(newConditionUuid);
        }
        dismiss();
    }

    private boolean isCurrentCondition(String newConditionUuid) {
        return mCurrentConditionUuid != null
            && mCurrentConditionUuid.equals(newConditionUuid);
    }

    /** Dismisses the dialog. */
    public void dismiss() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    // TODO: Consider adding the ability to re-enable buttons if a server request fails.

    /** Returns true iff the dialog is currently displayed. */
    public boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }
}
