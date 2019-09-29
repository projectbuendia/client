package org.projectbuendia.client.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.google.common.base.Joiner;

import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.ObsDeleteRequestedEvent;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.sync.ConceptService;
import org.projectbuendia.client.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.utils.Utils.DateStyle.HOUR_MINUTE;
import static org.projectbuendia.client.utils.Utils.DateStyle.MONTH_DAY;
import static org.projectbuendia.client.utils.Utils.DateStyle.RELATIVE_HOUR_MINUTE;
import static org.projectbuendia.client.utils.Utils.eq;

public class ObsDetailDialogFragment extends BaseDialogFragment<ObsDetailDialogFragment, ObsDetailDialogFragment.Args> {
    static class Args implements Serializable {
        Interval interval;
        String[] queriedConceptUuids;
        String[] conceptOrdering;
        List<Obs> observations;
    }

    private ViewGroup obsList;
    private ConceptService concepts;
    private boolean empty;
    private SortedMap<Group, List<Obs>> observationsBySection;
    private List<View> items;
    private Set<String> obsUuidsToDelete = new HashSet<>();

    public static ObsDetailDialogFragment create(
        Interval interval, String[] queriedConceptUuids,
        String[] conceptOrdering, List<Obs> observations) {
        Args args = new Args();
        args.interval = interval;
        args.queriedConceptUuids = Utils.orDefault(queriedConceptUuids, new String[0]);
        args.conceptOrdering = conceptOrdering;
        args.observations = observations;
        return new ObsDetailDialogFragment().withArgs(args);
    }

    @Override public @Nonnull Dialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.obs_detail_dialog_fragment);
    }

    @Override public void onOpen() {
        obsList = u.findView(R.id.obs_list);
        concepts = App.getConceptService();
        empty = args.observations.isEmpty();

        dialog.setTitle(R.string.obs_detail_title);
        dialog.getButton(BUTTON_NEUTRAL).setText(R.string.delete_selected);
        u.show(dialog.getButton(BUTTON_NEUTRAL), !empty);
        u.show(dialog.getButton(BUTTON_NEGATIVE), !empty);

        u.setText(R.id.message, Html.fromHtml(describeQueryHtml()));
        u.show(R.id.body, !empty);

        Map<Group, List<Obs>> groupObs = new HashMap<>();
        for (Obs obs : args.observations) {
            Group group = new Group(obs.time.toLocalDate(), obs.conceptUuid);
            if (!groupObs.containsKey(group)) {
                groupObs.put(group, new ArrayList<>());
            }
            groupObs.get(group).add(obs);
        }

        List<Group> groups = new ArrayList<>(groupObs.keySet());
        items = new ArrayList<>();
        Collections.sort(groups, new GroupComparator(args.conceptOrdering));

        u.addInflated(R.layout.group_spacer, obsList);
        for (Group group : groups) {
            if (args.interval == null || args.queriedConceptUuids.length != 1) {
                View heading = u.addInflated(R.layout.heading, obsList);
                u.setText(R.id.heading, formatHeading(group));
            }

            for (Obs obs : groupObs.get(group)) {
                View item = u.addInflated(R.layout.checkable_item, obsList);
                u.setText(R.id.text, formatValue(obs));
                App.getUserManager().showChip(u.findView(R.id.user_initials), obs.providerUuid);
                item.setTag(obs.uuid);
                items.add(item);

                final CheckBox checkbox = u.findView(R.id.checkbox);
                checkbox.setOnCheckedChangeListener((view, checked) -> updateUi());
                item.setOnClickListener(view -> {
                    if (checkbox.isEnabled()) checkbox.setChecked(!checkbox.isChecked());
                });
            }

            u.addInflated(R.layout.group_spacer, obsList);
        }
        dialog.getButton(BUTTON_NEUTRAL).setOnClickListener(v -> deleteSelected());
        updateUi();
    }

    private CharSequence formatHeading(Group group) {
        if (args.queriedConceptUuids.length != 1) {
            return Html.fromHtml(toBoldHtml(concepts.getName(group.conceptUuid)));
        }
        if (args.interval == null) {
            return Html.fromHtml(toAccentHtml(Utils.format(group.date, MONTH_DAY)));
        }
        return "";
    }

    private CharSequence formatValue(Obs obs) {
        return Html.fromHtml(
            "<span style='color: #33b5e5'>" + Utils.format(obs.time, HOUR_MINUTE)
                + "</span>" + "&nbsp;&nbsp;&nbsp;"
                + Html.escapeHtml(obs.valueName));
    }

    private void updateUi() {
        boolean anyItemsChecked = false;
        for (View item : items) {
            CheckBox checkbox = item.findViewById(R.id.checkbox);
            if (checkbox.isChecked()) anyItemsChecked = true;
            if (obsUuidsToDelete.contains(item.getTag())) {
                strikeCheckableItem(item);
            }
        }
        dialog.getButton(BUTTON_NEUTRAL).setEnabled(anyItemsChecked);
    }

    private String describeQueryHtml() {
        String htmlConceptNames = null;
        if (args.queriedConceptUuids.length > 0) {
            Locale locale = App.getSettings().getLocale();
            String[] htmlNames = new String[args.queriedConceptUuids.length];
            for (int i = 0; i < args.queriedConceptUuids.length; i++) {
                htmlNames[i] = toBoldHtml(
                    concepts.getName(args.queriedConceptUuids[i], locale));
            }
            // For this to work, R.string.two_items and R.string.more_than_two_items
            // must not contain any HTML special characters.
            htmlConceptNames = u.formatItems(htmlNames);
        }
        if (args.interval != null) {
            LocalDate day = args.interval.getStart().toLocalDate();
            String htmlDate = toAccentHtml(Utils.format(day, MONTH_DAY));
            if (eq(args.interval, day.toInterval())) {
                return htmlConceptNames != null ? getString(
                    empty ? R.string.obs_detail_concept_day_empty
                        : R.string.obs_detail_concept_day,
                    htmlConceptNames, htmlDate
                ) : getString(
                    empty ? R.string.obs_detail_day_empty
                        : R.string.obs_detail_day,
                    htmlDate
                );
            } else {
                String htmlStart = toAccentHtml(Utils.format(
                    args.interval.getStart(), RELATIVE_HOUR_MINUTE));
                String htmlStop = toAccentHtml(Utils.format(
                    args.interval.getEnd(), RELATIVE_HOUR_MINUTE));
                if (eq(args.interval.getEnd(), day.toInterval().getEnd())) {
                    htmlStop = toAccentHtml(getString(R.string.end_of_day_hour_minute));
                }
                return htmlConceptNames != null ? getString(
                    empty ? R.string.obs_detail_concept_interval_empty
                        : R.string.obs_detail_concept_interval,
                    htmlConceptNames, htmlDate, htmlStart, htmlStop
                ) : getString(
                    empty ? R.string.obs_detail_interval_empty
                        : R.string.obs_detail_interval,
                    htmlDate, htmlStart, htmlStop
                );
            }
        } else if (htmlConceptNames != null) {
            return getString(
                empty ? R.string.obs_detail_concept_empty
                    : R.string.obs_detail_concept,
                htmlConceptNames
            );
        }
        // Should never get here (no concepts and no interval).
        return "";
    }

    /** Marks the checked items for deletion. */
    private void deleteSelected() {
        for (View item : items) {
            CheckBox checkbox = item.findViewById(R.id.checkbox);
            if (checkbox.isChecked()) {
                obsUuidsToDelete.add((String) item.getTag());
                checkbox.setChecked(false);
            }
        }
        updateUi();
        // This button doesn't dismiss the dialog.
    }

    @Override protected void onSubmit() {
        if (obsUuidsToDelete.size() > 0) {
            List<Obs> observationsToDelete = new ArrayList<>();
            for (Obs obs : args.observations) {
                if (obsUuidsToDelete.contains(obs.uuid)) {
                    observationsToDelete.add(obs);
                }
            }
            Utils.logUserAction("obs_deleted",
                "obsUuids", Joiner.on(",").join(obsUuidsToDelete));
            EventBus.getDefault().post(
                new ObsDeleteRequestedEvent(observationsToDelete));
        }
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

