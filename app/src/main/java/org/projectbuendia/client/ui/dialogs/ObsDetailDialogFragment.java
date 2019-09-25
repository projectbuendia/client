package org.projectbuendia.client.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.sync.ConceptService;
import org.projectbuendia.client.ui.lists.ExpandableObsRowAdapter;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.projectbuendia.client.utils.Utils.DateStyle.RELATIVE_MONTH_DAY_HOUR_MINUTE;

public class ObsDetailDialogFragment extends BaseDialogFragment<ObsDetailDialogFragment> {
    private SortedMap<Section, List<ObsRow>> rowsBySection;

    private static String EN_DASH = "\u2013";
    private static String EM_DASH = "\u2014";
    private static String BULLET = "\u2022";

    private Interval interval;
    private String[] queriedConceptUuids;
    private String[] conceptOrdering;
    private List<ObsRow> obsRows;

    public static ObsDetailDialogFragment create(
        Interval interval, String[] queriedConceptUuids,
        String[] conceptOrdering, List<ObsRow> obsRows) {
        Bundle args = new Bundle();
        args.putString("interval", Utils.toNullableString(interval));
        args.putStringArray("queriedConceptUuids", queriedConceptUuids);
        args.putStringArray("conceptOrdering", conceptOrdering);
        args.putParcelableArrayList("obsRows", new ArrayList<>(obsRows));
        return new ObsDetailDialogFragment().withArgs(args);
    }

    @Override public @Nonnull Dialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.obs_detail_dialog_fragment);
    }

    @Override public void onOpen(Bundle args) {
        dialog.setTitle(R.string.obs_details_title);
        interval = Utils.toNullableInterval(args.getString("interval"));
        queriedConceptUuids = args.getStringArray("queriedConceptUuids");
        conceptOrdering = args.getStringArray("conceptOrdering");
        obsRows = args.getParcelableArrayList("obsRows");

        rowsBySection = new TreeMap<>(new SectionComparator(conceptOrdering));
        if (obsRows != null) {
            for (ObsRow row : obsRows) {
                if (row.valueName != null) {
                    Section section = new Section(row);
                    if (!rowsBySection.containsKey(section)) {
                        rowsBySection.put(section, new ArrayList<>());
                    }
                    rowsBySection.get(section).add(row);
                }
            }
        }

        TextView message = dialog.findViewById(R.id.message);
        message.setText(describeQuery());

        ExpandableListAdapter adapter = new ExpandableObsRowAdapter(u, rowsBySection);
        ExpandableListView listView = dialog.findViewById(R.id.obs_list);
        listView.setAdapter(adapter);
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            listView.expandGroup(i);
        }

        // Omit the "void observations switch" for now.  (Ping, 2019-03-19)
        //
        // LinearLayout listFooterView = (LinearLayout)mInflater.inflate(R.layout.void_observations_switch, null);
        // listView.addFooterView(listFooterView);

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

    private String describeQuery() {
        String conceptNames = null;
        if (Utils.hasItems(queriedConceptUuids)) {
            ConceptService concepts = App.getConceptService();
            Locale locale = App.getSettings().getLocale();
            String[] names = new String[queriedConceptUuids.length];
            for (int i = 0; i < queriedConceptUuids.length; i++) {
                names[i] = getString(R.string.quoted_text, concepts.getName(queriedConceptUuids[i], locale));
            }
            conceptNames = u.formatItems(names);
        }
        if (interval != null) {
            String start = Utils.format(interval.getStart(), RELATIVE_MONTH_DAY_HOUR_MINUTE);
            String stop = Utils.format(interval.getEnd(), RELATIVE_MONTH_DAY_HOUR_MINUTE);
            if (conceptNames != null) {
                return getString(
                    Utils.hasItems(obsRows)
                        ? R.string.obs_details_concept_interval
                        : R.string.obs_details_concept_interval_empty,
                    conceptNames, start, stop
                );
            } else {
                return getString(
                    Utils.hasItems(obsRows)
                        ? R.string.obs_details_interval
                        : R.string.obs_details_interval_empty,
                    start, stop
                );
            }
        } else if (conceptNames != null) {
            return getString(
                Utils.hasItems(obsRows)
                    ? R.string.obs_details_concept
                    : R.string.obs_details_concept_empty,
                conceptNames
            );
        } else {
            // Should never get here (no concepts and no interval).
            return "";
        }
    }

    @Override public void show(FragmentManager manager, String tag) {
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag);
        }
    }

    @Override protected void onSubmit() {
        dialog.dismiss();
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

        public SectionComparator(@Nullable String[] conceptOrdering) {
            if (conceptOrdering != null) {
                for (int i = 0; i < conceptOrdering.length; i++) {
                    orderingByUuid.put(conceptOrdering[i], i);
                }
                orderingMax = conceptOrdering.length;
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

