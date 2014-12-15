package org.msf.records.ui.tentselection;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.Toast;

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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A dialog that allows users to assign or change a patient's location.
 */
public final class AssignLocationDialog
        implements DialogInterface.OnDismissListener, AdapterView.OnItemClickListener {

    public static final boolean DEBUG = true;
    public static final String TAG = AssignLocationDialog.class.getSimpleName();

    @Nullable private AlertDialog mDialog;
    @Nullable private GridView mGridView;
    @Nullable private TentListAdapter mAdapter;

    private final Context mContext;
    private final LocationManager mLocationManager;
    private final EventBusRegistrationInterface mEventBus;
    private final EventBusSubscriber mEventBusSubscriber = new EventBusSubscriber();
    private final Optional<String> mCurrentLocationUuid;
    private final TentSelectedCallback mTentSelectedCallback;
    private ProgressDialog mProgressDialog;
    private View mPreviousView;

    // TODO(dxchen): Consider making this an event bus event rather than a callback so that we don't
    // have to worry about Activity context leaks.
    public interface TentSelectedCallback {
        /**
         * Called when then user selects a tent that is not the currently selected one.
         *
         * @return whether to immediately dismiss the dialog. If {@code false}, the dialog will
         *         disable further button presses and display a progress spinner until
         *         {@link AssignLocationDialog#dismiss} is called.
         */
        boolean onNewTentSelected(String newTentUuid);
    }

    public AssignLocationDialog(
            Context context,
            LocationManager locationManager,
            EventBusRegistrationInterface eventBus,
            Optional<String> currentLocationUuid,
            TentSelectedCallback tentSelectedCallback) {
        mContext = checkNotNull(context);
        mLocationManager = checkNotNull(locationManager);
        mEventBus = checkNotNull(eventBus);
        mCurrentLocationUuid = currentLocationUuid;
        mTentSelectedCallback = checkNotNull(tentSelectedCallback);
    }

    public void show() {
        FrameLayout frameLayout = new FrameLayout(mContext); // needed for outer margins to just work
        View.inflate(mContext,R.layout.tent_grid, frameLayout);
        mGridView = (GridView) frameLayout.findViewById(R.id.tent_selection_tents);
        startListeningForLocations();

        mDialog = new AlertDialog.Builder(mContext)
            .setTitle(R.string.action_assign_location)
            .setView(frameLayout)
            .setOnDismissListener(this)
            .create();
        mDialog.show();
    }

    public void onPatientUpdateFailed( int reason )
    {
        mAdapter.setSelectedView( mPreviousView );
        mPreviousView = null;

        Toast.makeText( mContext, "Failed to update patient, reason: " + Integer.toString( reason ), Toast.LENGTH_SHORT ).show();
        mProgressDialog.dismiss();
        //dismiss();
    }

    private void startListeningForLocations() {
        mEventBus.register(mEventBusSubscriber);
        mLocationManager.loadLocations();
    }

    private void setTents(LocationTree locationTree) {
        if (mGridView != null) {
            List<LocationSubtree> locations = locationTree.getTents();
            LocationSubtree dischargedZone = locationTree.getZoneForUuid(Zone.DISCHARGED_ZONE_UUID);
            locations.add(dischargedZone);
            mAdapter = new TentListAdapter(
                    mContext, locations, mCurrentLocationUuid, true /*shouldAbbreviate*/);
            mGridView.setAdapter(mAdapter);
            mGridView.setOnItemClickListener(this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String newTentUuid = mAdapter.getItem(position).getLocation().uuid;
        mPreviousView = mAdapter.getSelectedView();
        mAdapter.setSelectedView( view );
        mProgressDialog = ProgressDialog.show(mContext, "Updating Patient",
                "Please wait...", true);
        if (isCurrentTent(newTentUuid) || mTentSelectedCallback.onNewTentSelected(newTentUuid)) {
            dismiss();
        }

        // TODO(kpy): Show a progress spinner somewhere on the dialog.

    }

    public void dismiss() {
        mProgressDialog.dismiss();
        mDialog.dismiss();
    }

    // TODO(dxchen): Consider adding the ability to re-enable buttons if a server request fails.

    private boolean isCurrentTent(String newTentUuid) {
        Optional<String> selectedTentUuid = mAdapter.getSelectedLocationUuid();
        return selectedTentUuid.isPresent() &&
                newTentUuid.equals(selectedTentUuid.get());
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mEventBus.unregister(mEventBusSubscriber);
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
