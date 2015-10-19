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
import android.app.ProgressDialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Conditions;

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
    private ProgressDialog mProgressDialog;

    // TODO: Consider making this an event bus event rather than a callback so that we don't
    // have to worry about Activity context leaks.
    public interface ConditionSelectedCallback {
        /**
         * Called when then user selects a general condition that is not the currently selected one.
         * @return whether to immediately dismiss the dialog. If {@code false}, the dialog will
         * disable further button presses and display a progress spinner until
         * {@link org.projectbuendia.client.ui.chart.AssignGeneralConditionDialog#dismiss} is called.
         */
        boolean onNewConditionSelected(String newConditionUuid);
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
        mGridView = (GridView) frameLayout.findViewById(R.id.condition_selection_conditions);

        if (mGridView != null) {
            mAdapter = new GeneralConditionAdapter(
                mContext, Conditions.UUIDS, mCurrentConditionUuid);
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

    @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mAdapter == null) return;

        String newConditionUuid = mAdapter.getItem(position);
        mAdapter.setSelectedConditionUuid(newConditionUuid);
        mProgressDialog = ProgressDialog.show(mContext,
            mContext.getResources().getString(R.string.title_updating_patient),
            mContext.getResources().getString(R.string.please_wait), true);
        if (isCurrentCondition(newConditionUuid)
            || mConditionSelectedCallback.onNewConditionSelected(newConditionUuid)) {
            dismiss();
        }
    }

    private boolean isCurrentCondition(String newConditionUuid) {
        return mCurrentConditionUuid != null
            && mCurrentConditionUuid.equals(newConditionUuid);
    }

    /** Dismisses the dialog. */
    public void dismiss() {
        mProgressDialog.dismiss();

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
