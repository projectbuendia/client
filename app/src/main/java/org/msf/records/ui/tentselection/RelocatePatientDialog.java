package org.msf.records.ui.tentselection;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridView;

import com.google.common.base.Preconditions;

import org.msf.records.R;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.utils.EventBusRegistrationInterface;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Popup dialog showing tents in order to relocate patients into one of them.
 */
public class RelocatePatientDialog {

    public static final boolean DEBUG = true;
    public static final String TAG = RelocatePatientDialog.class.getSimpleName();

    @Nullable private GridView gridView;
    private final Context context;
    private final LocationManager locationManager;
    private final EventBusRegistrationInterface eventBus;

    public RelocatePatientDialog(
            Context context,
            LocationManager locationManager,
            EventBusRegistrationInterface eventBus) {
        this.context = Preconditions.checkNotNull(context);
        this.locationManager = Preconditions.checkNotNull(locationManager);
        this.eventBus = Preconditions.checkNotNull(eventBus);
    }

    public void show() {
        FrameLayout frameLayout = new FrameLayout(context); // needed for outer margins to just work
        View.inflate(context,R.layout.tent_grid, frameLayout);
        gridView = (GridView) frameLayout.findViewById(R.id.tent_selection_tents);
        startListeningForLocations();

        new AlertDialog.Builder(context)
            .setTitle(R.string.action_assign_location)
            .setView(frameLayout)
            .create()
            .show();
    }

    private void startListeningForLocations() {
        eventBus.register(new EventBusSubscriber());
        locationManager.loadLocations();
    }

    private void setTents(List<LocationTree.LocationSubtree> tents) {
        if (gridView != null) {
            TentListAdapter adapter = new TentListAdapter(context, tents);
            gridView.setAdapter(adapter);
        }
    }

    private final class EventBusSubscriber {
        public void onEventMainThread(LocationsLoadFailedEvent event) {
            if (DEBUG) {
                Log.d(TAG, "Error loading location tree");
            }
        }

        public void onEventMainThread(LocationsLoadedEvent event) {
            setTents(event.locationTree.getTents());
        }
    }
}
