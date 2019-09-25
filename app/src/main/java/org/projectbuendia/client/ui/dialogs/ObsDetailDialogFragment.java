package org.projectbuendia.client.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.sync.ConceptService;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.projectbuendia.client.utils.Utils.DateStyle.HOUR_MINUTE;
import static org.projectbuendia.client.utils.Utils.DateStyle.MONTH_DAY;
import static org.projectbuendia.client.utils.Utils.DateStyle.RELATIVE_MONTH_DAY_HOUR_MINUTE;
import static org.projectbuendia.client.utils.Utils.eq;

public class ObsDetailDialogFragment extends BaseDialogFragment<ObsDetailDialogFragment> {

    private static String EN_DASH = "\u2013";
    private static String EM_DASH = "\u2014";
    private static String BULLET = "\u2022";

    private ConceptService concepts;
    private Interval interval;
    private String[] queriedConceptUuids;
    private String[] conceptOrdering;
    private List<Obs> observations;
    private SortedMap<Group, List<Obs>> observationsBySection;
    private List<View> items;

    public static ObsDetailDialogFragment create(
        Interval interval, String[] queriedConceptUuids,
        String[] conceptOrdering, List<Obs> observations) {
        Bundle args = new Bundle();
        args.putString("interval", Utils.toNullableString(interval));
        args.putStringArray("queriedConceptUuids", queriedConceptUuids);
        args.putStringArray("conceptOrdering", conceptOrdering);
        args.putParcelableArrayList("observations", new ArrayList<>(observations));
        return new ObsDetailDialogFragment().withArgs(args);
    }

    @Override public @Nonnull Dialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.obs_detail_dialog_fragment);
    }

    @Override public void onOpen(Bundle args) {
        dialog.setTitle(R.string.obs_details_title);
        concepts = App.getConceptService();
        interval = Utils.toNullableInterval(args.getString("interval"));
        queriedConceptUuids = Utils.orDefault(
            args.getStringArray("queriedConceptUuids"), new String[0]);
        conceptOrdering = args.getStringArray("conceptOrdering");
        observations = args.getParcelableArrayList("observations");

        Map<Group, List<Obs>> groupObs = new HashMap<>();
        for (Obs obs : observations) {
            Group group = new Group(obs.time.toLocalDate(), obs.conceptUuid);
            if (!groupObs.containsKey(group)) {
                groupObs.put(group, new ArrayList<>());
            }
            groupObs.get(group).add(obs);
        }

        TextView message = dialog.findViewById(R.id.message);
        message.setText(describeQuery());


        List<Group> groups = new ArrayList<>(groupObs.keySet());
        items = new ArrayList<>();
        Collections.sort(groups, new GroupComparator(conceptOrdering));

        ViewGroup list = dialog.findViewById(R.id.obs_list);
        list.addView(u.inflate(R.layout.group_spacer, list));
        for (Group group : groups) {
            // No need to show a heading if there's only one date and one concept.
            if (interval == null || queriedConceptUuids.length != 1) {
                View heading = u.inflate(R.layout.heading, list);
                TextView headingText = u.findView(R.id.heading);
                headingText.setText(formatHeading(group));
                list.addView(heading);
            }

            for (Obs obs : groupObs.get(group)) {
                View item = u.inflate(R.layout.checkable_item, list);
                TextView itemText = u.findView(R.id.text);
                itemText.setText(formatValue(obs));
                App.getUserManager().showChip(u.findView(R.id.user_initials), obs.providerUuid);
                list.addView(item);

                item.setTag(obs);
                items.add(item);

                final CheckBox checkbox = u.findView(R.id.checkbox);
                checkbox.setOnCheckedChangeListener((view, checked) -> updateUi());
                item.setOnClickListener(view -> {
                    if (checkbox.isEnabled()) checkbox.setChecked(!checkbox.isChecked());
                });
            }

            list.addView(u.inflate(R.layout.group_spacer, list));
        }
    }

    private CharSequence formatHeading(Group group) {
        if (queriedConceptUuids.length != 1) return concepts.getName(group.conceptUuid);
        if (interval == null) return Utils.format(group.date, MONTH_DAY);
        return "";
    }

    private CharSequence formatValue(Obs obs) {
        return Html.fromHtml(
            "<span style='color: #33b5e5'>" + Utils.format(obs.time, HOUR_MINUTE) + ":</span> " +
                Html.escapeHtml(obs.valueName));
    }

    private void updateUi() {

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
                    Utils.hasItems(observations)
                        ? R.string.obs_details_concept_interval
                        : R.string.obs_details_concept_interval_empty,
                    conceptNames, start, stop
                );
            } else {
                return getString(
                    Utils.hasItems(observations)
                        ? R.string.obs_details_interval
                        : R.string.obs_details_interval_empty,
                    start, stop
                );
            }
        } else if (conceptNames != null) {
            return getString(
                Utils.hasItems(observations)
                    ? R.string.obs_details_concept
                    : R.string.obs_details_concept_empty,
                conceptNames
            );
        } else {
            // Should never get here (no concepts and no interval).
            return "";
        }
    }

    @Override protected void onSubmit() {
        dialog.dismiss();
    }

    /** Observations are grouped by date and concept. */
    public static class Group {
        public final @Nonnull LocalDate date;
        public final @Nonnull String conceptUuid;

        public Group(Obs obs) {
            this(obs.time.toLocalDate(), obs.conceptUuid);
        }

        public Group(@Nonnull LocalDate date, @Nonnull String conceptUuid) {
            this.date = date;
            this.conceptUuid = conceptUuid;
        }

        @Override public boolean equals(Object other) {
            if (other instanceof Group) {
                Group o = (Group) other;
                return eq(date, o.date) && eq(conceptUuid, o.conceptUuid);
            }
            return false;
        }

        @Override public int hashCode() {
            return Objects.hash(date, conceptUuid);
        }
    }

    /** Arranges groups in order by date and by concept according to a given ordering. */
    public static class GroupComparator implements Comparator<Group> {
        private final Map<String, Integer> orderingByUuid = new HashMap<>();
        private final int orderingMax;

        public GroupComparator(@Nullable String[] conceptOrdering) {
            if (conceptOrdering != null) {
                for (int i = 0; i < conceptOrdering.length; i++) {
                    orderingByUuid.put(conceptOrdering[i], i);
                }
                orderingMax = conceptOrdering.length;
            } else {
                orderingMax = 0;
            }
        }

        @Override public int compare(Group a, Group b) {
            int result = a.date.compareTo(b.date);
            if (result != 0) return result;

            result = Integer.compare(getOrdering(a.conceptUuid), getOrdering(b.conceptUuid));
            if (result != 0) return result;

            return a.conceptUuid.compareTo(b.conceptUuid);
        }

        private int getOrdering(String conceptUuid) {
            return Utils.getOrDefault(orderingByUuid, conceptUuid, orderingMax);
        }
    }
}

