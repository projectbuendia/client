package org.msf.records.ui.tentselection;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridView;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.msf.records.R;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.utils.EventBusRegistrationInterface;

import javax.annotation.Nullable;

/**
 * Popup dialog showing tents in order to relocate patients into one of them.
 */
public class RelocatePatientDialog implements DialogInterface.OnDismissListener {

    public static final boolean DEBUG = true;
    public static final String TAG = RelocatePatientDialog.class.getSimpleName();

    @Nullable private GridView gridView;
    private final Context context;
    private final LocationManager locationManager;
    private final EventBusRegistrationInterface eventBus;
    private final EventBusSubscriber eventBusSubscriber = new EventBusSubscriber();
    private final String initialPatientLocationUuid;

    public RelocatePatientDialog(
            Context context,
            LocationManager locationManager,
            EventBusRegistrationInterface eventBus,
            String patientLocationUuid) {
        this.context = Preconditions.checkNotNull(context);
        this.locationManager = Preconditions.checkNotNull(locationManager);
        this.eventBus = Preconditions.checkNotNull(eventBus);
        this.initialPatientLocationUuid = Preconditions.checkNotNull(patientLocationUuid);
    }

    public void show() {
        FrameLayout frameLayout = new FrameLayout(context); // needed for outer margins to just work
        View.inflate(context,R.layout.tent_grid, frameLayout);
        gridView = (GridView) frameLayout.findViewById(R.id.tent_selection_tents);
        startListeningForLocations();

        new AlertDialog.Builder(context)
            .setTitle(R.string.action_assign_location)
            .setView(frameLayout)
            .setOnDismissListener(this)
            .create()
            .show();
    }

    private void startListeningForLocations() {
        eventBus.register(eventBusSubscriber);
        locationManager.loadLocations();
    }

    private void setTents(LocationTree locationTree) {
        if (gridView != null) {
            LocationTree.LocationSubtree initialTent =
                    locationTree.getTentForUuid(initialPatientLocationUuid);
            Optional<String> initialTentUuid =
                    initialTent == null
                            ? Optional.<String>absent()
                            : Optional.of(initialTent.getLocation().uuid);
            TentListAdapter adapter = new TentListAdapter(
                    context, locationTree.getTents(),
                    initialTentUuid);
            gridView.setAdapter(adapter);
        }
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
