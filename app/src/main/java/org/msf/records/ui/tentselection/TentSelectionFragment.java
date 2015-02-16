package org.msf.records.ui.tentselection;

import java.util.List;

import org.msf.records.R;
import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.ui.ProgressFragment;
import org.msf.records.ui.patientlist.PatientListActivity;
import org.msf.records.utils.PatientCountDisplay;
import org.msf.records.widget.SubtitledButtonView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.google.common.base.Optional;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemClick;

/**
 * Displays a list of all tents.
 */
public final class TentSelectionFragment extends ProgressFragment {
    @InjectView(R.id.tent_selection_tents) GridView mTentGrid;
    @InjectView(R.id.tent_selection_all_patients) SubtitledButtonView mAllPatientsButton;
    @InjectView(R.id.tent_selection_triage) SubtitledButtonView mTriageButton;
    @InjectView(R.id.tent_selection_discharged) SubtitledButtonView mDischargedButton;

    private TentSelectionController mController;
    private final MyUi mMyUi = new MyUi();
    private TentListAdapter mAdapter;

    //private ProgressDialog mLoadingDialog;

    public TentSelectionFragment() {
        // Required empty public constructor
    }

    @OnItemClick(R.id.tent_selection_tents) void onTentGridClicked(int position) {
        mController.onTentSelected(mAdapter.getItem(position));
    }

    @OnClick(R.id.tent_selection_all_patients) void onAllPatientsClicked(View v) {
        Intent listIntent = new Intent(getActivity(), PatientListActivity.class);
        startActivity(listIntent);
    }

    @OnClick(R.id.tent_selection_discharged) void onDischargedClicked(View v) {
        mController.onDischargedPressed();
    }

    @OnClick(R.id.tent_selection_triage) void onTriageClicked(View v) {
        mController.onTriagePressed();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*mLoadingDialog = new ProgressDialog(getActivity());
        mLoadingDialog.setIcon(android.R.drawable.ic_dialog_info);
        mLoadingDialog.setTitle(getString(R.string.tent_selection_dialog_title));
        mLoadingDialog.setMessage(getString(R.string.tent_selection_dialog_message));
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.setIndeterminate(true);*/
        setContentView(R.layout.fragment_tent_selection);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mController = ((TentSelectionActivity) getActivity()).getController();
        mController.attachFragmentUi(mMyUi);
    }

    @Override
    public void onDestroyView() {
        mController.detachFragmentUi(mMyUi);
        super.onDestroyView();
    }

    private final class MyUi implements TentSelectionController.TentFragmentUi {
        @Override
        public void setDischargedPatientCount(int patientCount) {
            mDischargedButton.setSubtitle(
                    PatientCountDisplay.getPatientCountSubtitle(
                            getActivity(), patientCount));
        }

        @Override
        public void setTriagePatientCount(int patientCount) {
            mTriageButton.setSubtitle(
                    PatientCountDisplay.getPatientCountSubtitle(
                            getActivity(), patientCount));
        }

        @Override
        public void setPresentPatientCount(int patientCount) {
            mAllPatientsButton.setSubtitle(
                    PatientCountDisplay.getPatientCountSubtitle(
                            getActivity(), patientCount, true));
        }

        @Override
        public void setTents(AppLocationTree locationTree, List<AppLocation> tents) {
            mAdapter = new TentListAdapter(
                    getActivity(), tents, locationTree, Optional.<String>absent());
            mTentGrid.setAdapter(mAdapter);
        }

        @Override
        public void setBusyLoading(boolean busy) {
            changeState(busy ? State.LOADING : State.LOADED);
            /*if (mLoadingDialog != null && busy != mLoadingDialog.isShowing()) {
                if (busy) {
                    mLoadingDialog.show();
                } else {
                    mLoadingDialog.hide();
                }
            }*/
        }

        @Override
        public void showIncrementalSyncProgress(int progress, @Nullable String label) {
            incrementProgressBy(progress);
            if (label != null) {
                setProgressLabel(label);
            }
        }

        @Override
        public void resetSyncProgress() {
            switchToCircularProgressBar();
        }

        @Override
        public void showSyncCancelRequested() {
            setProgressLabel(getString(R.string.cancelling_sync));
        }
    }
}
