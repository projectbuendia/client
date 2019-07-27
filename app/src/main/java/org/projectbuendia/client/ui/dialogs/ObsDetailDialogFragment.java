package org.projectbuendia.client.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import org.joda.time.LocalDate;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.ui.lists.ExpandableObsRowAdapter;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import butterknife.ButterKnife;

public class ObsDetailDialogFragment extends DialogFragment {
    private static final String KEY_OBS_ROWS = "obsRows";
    private static final String KEY_CONCEPT_UUIDS = "conceptUuids";

    private LayoutInflater mInflater;
    private SortedMap<Section, List<ObsRow>> rowsBySection;

    private static String EN_DASH = "\u2013";
    private static String EM_DASH = "\u2014";
    private static String BULLET = "\u2022";

    public static ObsDetailDialogFragment newInstance(
        List<ObsRow> obsRows, List<String> orderedConceptUuids) {
        Bundle args = new Bundle();
        args.putParcelableArrayList(KEY_OBS_ROWS, new ArrayList<>(obsRows));
        args.putStringArrayList(KEY_CONCEPT_UUIDS, new ArrayList<>(orderedConceptUuids));
        ObsDetailDialogFragment fragment = new ObsDetailDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.obs_detail_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        List<String> conceptUuids = getArguments().getStringArrayList(KEY_CONCEPT_UUIDS);
        List<ObsRow> obsRows = getArguments().getParcelableArrayList(KEY_OBS_ROWS);

        rowsBySection = new TreeMap<>(new SectionComparator(conceptUuids));
        if (obsRows != null) {
            for (ObsRow row : obsRows) {
                if (row.valueName != null) {
                    Section section = new Section(row);
                    if (!rowsBySection.containsKey(section)) {
                        rowsBySection.put(section, new ArrayList<ObsRow>());
                    }
                    rowsBySection.get(section).add(row);
                }
            }
        }

        ExpandableListAdapter adapter = new ExpandableObsRowAdapter(
            App.getInstance().getApplicationContext(), rowsBySection);
        ExpandableListView listView = fragment.findViewById(R.id.obs_list);
        listView.setAdapter(adapter);
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            listView.expandGroup(i);
        }

        // Omit the "void observations switch" for now.  (Ping, 2019-03-19)
        //
        // LinearLayout listFooterView = (LinearLayout)mInflater.inflate(R.layout.void_observations_switch, null);
        // listView.addFooterView(listFooterView);

        return new AlertDialog.Builder(getActivity())
            .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
            .setView(fragment)
            .create();

        // Omit the "void observations switch" for now.  (Ping, 2019-03-19)
        /*
        final Switch swVoid = (Switch) fragment.findViewById(R.id.swVoid);
        swVoid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (swVoid.isChecked()){
                    VoidObservationsDialogFragment.newInstance(obsrows)
                            .show(getActivity().getSupportFragmentManager(), null);
                }
                dialog.dismiss();
            }
        });
        */
    }

    @Override public void show(FragmentManager manager, String tag) {
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag);
        }
    }

    /**
     * The listing is divided into sections, with each section showing all the
     * observations on a particular date for a particular concept.
     */
    public static class Section {
        public final LocalDate date;
        public final @Nonnull String conceptUuid;
        public final @Nonnull String conceptName;

        public Section(ObsRow row) {
            this(row.time.toLocalDate(), row.conceptUuid, row.conceptName);
        }

        public Section(LocalDate date, String conceptUuid, String conceptName) {
            this.conceptUuid = Utils.toNonnull(conceptUuid);
            this.conceptName = Utils.toNonnull(conceptName);
            this.date = date;
        }

        @Override public boolean equals(Object other) {
            if (other instanceof Section) {
                Section o = (Section) other;
                return date.equals(o.date)
                    && conceptUuid.equals(o.conceptUuid)
                    && conceptName.equals(o.conceptName);
            }
            return false;
        }

        @Override public int hashCode() {
            return Objects.hash(date, conceptUuid, conceptName);
        }
    }

    public static class SectionComparator implements Comparator<Section> {
        private final Map<String, Integer> orderingByUuid = new HashMap<>();
        private final int orderingMax;

        public SectionComparator(@Nullable List<String> conceptUuids) {
            if (conceptUuids != null) {
                for (int i = 0; i < conceptUuids.size(); i++) {
                    orderingByUuid.put(conceptUuids.get(i), i);
                }
                orderingMax = conceptUuids.size();
            } else {
                orderingMax = 0;
            }
        }

        @Override public int compare(Section a, Section b) {
            int result = a.date.compareTo(b.date);
            if (result != 0) return result;

            result = Integer.compare(getOrdering(a.conceptUuid), getOrdering(b.conceptUuid));
            if (result != 0) return result;

            result = a.conceptName.compareTo(b.conceptName);
            if (result != 0) return result;

            return a.conceptUuid.compareTo(b.conceptUuid);
        }

        private int getOrdering(String conceptUuid) {
            return Utils.getOrDefault(orderingByUuid, conceptUuid, orderingMax);
        }
    }
}

