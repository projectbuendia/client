package org.msf.records.ui.chart;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.Toast;

import org.msf.records.R;
import org.msf.records.model.Concepts;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A dialog that allows users to assign or change a patient's location.
 */
public final class AssignGeneralConditionDialog
        implements AdapterView.OnItemClickListener {

    @Nullable private AlertDialog mDialog;
    @Nullable private GridView mGridView;
    @Nullable private GeneralConditionAdapter mAdapter;

    private final Context mContext;
    @Nullable private final String mCurrentConditionUuid;
    private final ConditionSelectedCallback mConditionSelectedCallback;
    private ProgressDialog mProgressDialog;

    // TODO(dxchen): Consider making this an event bus event rather than a callback so that we don't
    // have to worry about Activity context leaks.
    public interface ConditionSelectedCallback {
        /**
         * Called when then user selects a general condition that is not the currently selected one.
         *
         * @return whether to immediately dismiss the dialog. If {@code false}, the dialog will
         *         disable further button presses and display a progress spinner until
         *         {@link org.msf.records.ui.chart.AssignGeneralConditionDialog#dismiss} is called.
         */
        boolean onNewConditionSelected(String newConditionUuid);
    }

    public AssignGeneralConditionDialog(
            Context context,
            @Nullable String currentConditionUuid,
            ConditionSelectedCallback conditionSelectedCallback) {
        mContext = checkNotNull(context);
        mCurrentConditionUuid = currentConditionUuid;
        mConditionSelectedCallback = checkNotNull(conditionSelectedCallback);
    }

    public void show() {
        FrameLayout frameLayout = new FrameLayout(mContext); // needed for outer margins to just work
        View.inflate(mContext, R.layout.condition_grid, frameLayout);
        mGridView = (GridView) frameLayout.findViewById(R.id.condition_selection_conditions);

        if (mGridView != null) {
            mAdapter = new GeneralConditionAdapter(
                    mContext, Concepts.GENERAL_CONDITION_UUIDS, mCurrentConditionUuid);
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

    public void onPatientUpdateFailed( int reason )
    {
        if (mCurrentConditionUuid != null) {
            mAdapter.setSelectedConditionUuid(mCurrentConditionUuid);
        }

        Toast.makeText( mContext, "Failed to update patient, reason: " + Integer.toString( reason ),
                Toast.LENGTH_SHORT ).show();
        mProgressDialog.dismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        String newConditionUuid = mAdapter.getItem(position);
        mAdapter.setSelectedConditionUuid(newConditionUuid);
        mProgressDialog = ProgressDialog.show(mContext,
                mContext.getResources().getString(R.string.title_updating_patient),
                mContext.getResources().getString(R.string.please_wait), true);
        if (isCurrentCondition(newConditionUuid) ||
                mConditionSelectedCallback.onNewConditionSelected(newConditionUuid)) {
            dismiss();
        }
    }

    public void dismiss() {
        mProgressDialog.dismiss();
        mDialog.dismiss();
    }

    // TODO(dxchen): Consider adding the ability to re-enable buttons if a server request fails.

    private boolean isCurrentCondition(String newConditionUuid) {
        return mCurrentConditionUuid != null &&
                mCurrentConditionUuid.equals(mAdapter.getSelectedConditionUuid());
    }
}
