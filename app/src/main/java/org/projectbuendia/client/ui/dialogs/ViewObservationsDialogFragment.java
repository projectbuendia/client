package org.projectbuendia.client.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;

import org.joda.time.LocalDate;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.ui.lists.ExpandableObsRowAdapter;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ViewObservationsDialogFragment extends DialogFragment {

    private static final String PATIENT_UUID_KEY = "patient_uuid";
    private static final String CONCEPT_UUID_KEY = "concept_uuid";
    private static final String START_TIME_KEY = "start_time";
    private static final String END_TIME_KEY = "end_time";

    public static ViewObservationsDialogFragment newInstance(
            String patientUuid, String conceptUuid,
            @Nullable Long startTime, @Nullable Long endTime) {
        ViewObservationsDialogFragment f = new ViewObservationsDialogFragment();
        Bundle args = new Bundle();
        args.putString(PATIENT_UUID_KEY, patientUuid);
        args.putString(CONCEPT_UUID_KEY, conceptUuid);
        if (startTime != null) {
            args.putLong(START_TIME_KEY, startTime);
        }
        if (endTime != null) {
            args.putLong(END_TIME_KEY, endTime);
        }
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    @NonNull
    // It's ok to inflate with a null parent because the dialog has no parent.
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        // TODO: keep this off the main thread and add a spinner whilst the data is loading /
        // processing.
        View v = inflater.inflate(R.layout.view_observations_dialog_fragment, null);
        ExpandableListView obsList = (ExpandableListView) v.findViewById(R.id.obs_list);
        obsList.setEmptyView(v.findViewById(R.id.empty));
        List<Pair<LocalDate, List<Obs>>> mData = loadData();
        ExpandableObsRowAdapter adapter = new ExpandableObsRowAdapter(mData);
        obsList.setAdapter(adapter);
        // Expand all groups by default.
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            obsList.expandGroup(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setView(v);

        return builder.create();
    }

    private List<Pair<LocalDate, List<Obs>>> loadData() {
        Bundle args = getArguments();
        // TODO: Can we / should we inject this?
        ChartDataHelper chartDataHelper =
                new ChartDataHelper(getContext().getContentResolver());
        List<Obs> rawData = chartDataHelper.getPatientObservationsByConceptAndTime(
                args.getString(PATIENT_UUID_KEY),
                args.getString(CONCEPT_UUID_KEY),
                Utils.getLong(args, START_TIME_KEY),
                Utils.getLong(args, END_TIME_KEY));
        if (rawData.size() == 0) {
            return new ArrayList<>();
        }
        List<Pair<LocalDate, List<Obs>>> result = new ArrayList<>();
        List<Obs> obsForDay = new ArrayList<>();
        LocalDate lastDay = rawData.get(0).time.toLocalDate();
        for (Obs obs : rawData) {
            LocalDate date = obs.time.toLocalDate();
            if (!Objects.equals(lastDay, date)) {
                result.add(Pair.create(lastDay, obsForDay));
                obsForDay = new ArrayList<>();
                lastDay = date;
            }
            obsForDay.add(obs);
        }
        result.add(Pair.create(lastDay, obsForDay));
        return result;
    }
}

