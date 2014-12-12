package org.msf.records.ui.tentselection;

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;

import com.google.common.base.Optional;

import org.msf.records.R;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Zone;
import org.msf.records.utils.EventBusRegistrationInterface;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Popup dialog showing tents in order to relocate patients into one of them.
 */
public final class RelocatePatientDialog
        implements DialogInterface.OnDismissListener, AdapterView.OnItemClickListener {
    public static final boolean DEBUG = true;
    public static final String TAG = RelocatePatientDialog.class.getSimpleName();

    @Nullable private AlertDialog dialog;
    @Nullable private GridView gridView;
    @Nullable private TentListAdapter adapter;
    private final Context context;
    private final LocationManager locationManager;
    private final EventBusRegistrationInterface eventBus;
    private final EventBusSubscriber eventBusSubscriber = new EventBusSubscriber();
    private final String patientId;
    private final String initialPatientLocationUuid;
    private final TentSelectedCallback tentSelectedCallback;

    public interface TentSelectedCallback {
        /**
         * Called when then user selects a tent that is not the currently selected one.
         */
        void onNewTentSelected(String newTentUuid);
    }

    public RelocatePatientDialog(
            Context context,
            LocationManager locationManager,
            EventBusRegistrationInterface eventBus,
            String patientId,
            String patientLocationUuid,
            TentSelectedCallback tentSelectedCallback) {
        this.context = checkNotNull(context);
        this.locationManager = checkNotNull(locationManager);
        this.eventBus = checkNotNull(eventBus);
        this.patientId = checkNotNull(patientId);
        this.initialPatientLocationUuid = checkNotNull(patientLocationUuid);
        this.tentSelectedCallback = checkNotNull(tentSelectedCallback);
    }

    public void show() {
        FrameLayout frameLayout = new FrameLayout(context); // needed for outer margins to just work
        View.inflate(context,R.layout.tent_grid, frameLayout);
        gridView = (GridView) frameLayout.findViewById(R.id.tent_selection_tents);
        startListeningForLocations();

        dialog = new AlertDialog.Builder(context)
            .setTitle(R.string.action_assign_location)
            .setView(frameLayout)
            .setOnDismissListener(this)
            .create();
        dialog.show();
    }

    private void startListeningForLocations() {
        eventBus.register(eventBusSubscriber);
        locationManager.loadLocations();
    }

    private void setTents(LocationTree locationTree) {
        if (gridView != null) {
            LocationSubtree initialTent =
                    locationTree.getTentForUuid(initialPatientLocationUuid);
            Optional<String> initialTentUuid =
                    initialTent == null
                            ? Optional.<String>absent()
                            : Optional.of(initialTent.getLocation().uuid);
            List<LocationSubtree> locations = locationTree.getTents();
            LocationSubtree dischargedZone = locationTree.getZoneForUuid(Zone.DISCHARGED_ZONE_UUID);
            locations.add(dischargedZone);
            adapter = new TentListAdapter(
                    context, locations,
                    initialTentUuid);
            gridView.setAdapter(adapter);
            gridView.setOnItemClickListener(this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String newTentUuid = adapter.getItem(position).getLocation().uuid;
        if (!isCurrentTent(newTentUuid)) {
            tentSelectedCallback.onNewTentSelected(newTentUuid);
        }
        dialog.dismiss();
    }

    private boolean isCurrentTent(String newTentUuid) {
        Optional<String> selectedTentUuid = adapter.getSelectedTentUuid();
        return selectedTentUuid.isPresent() &&
                newTentUuid.equals(selectedTentUuid.get());
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        eventBus.unregister(eventBusSubscriber);
    }

    private final class EventBusSubscriber {
        public void onEventMainThread(LocationsLoadFailedEvent event) {
            if (DEBUG) {
                Log.d(TAG, "Error loading location tree");
            }
        }

        public void onEventMainThread(LocationsLoadedEvent event) {
            setTents(event.locationTree);
        }
    }
}
