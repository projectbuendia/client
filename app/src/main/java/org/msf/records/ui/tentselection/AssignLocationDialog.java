package org.msf.records.ui.tentselection;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.Toast;

import com.google.common.base.Optional;

import org.msf.records.R;
import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationComparator;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.model.Zone;
import org.msf.records.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A dialog that allows users to assign or change a patient's location.
 */
public final class AssignLocationDialog
        implements DialogInterface.OnDismissListener, AdapterView.OnItemClickListener {

    private static final Logger LOG = Logger.create();

    public static final boolean DEBUG = true;

    @Nullable private AlertDialog mDialog;
    @Nullable private GridView mGridView;
    @Nullable private TentListAdapter mAdapter;

    private final Context mContext;
    private final AppModel mAppModel;
    private final String mLocale;
    private final Runnable mOnDismiss;
    private final CrudEventBus mEventBus;
    private final EventBusSubscriber mEventBusSubscriber = new EventBusSubscriber();
    private final Optional<String> mCurrentLocationUuid;
    private final TentSelectedCallback mTentSelectedCallback;
    private ProgressDialog mProgressDialog;

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
            AppModel appModel,
            String locale,
            Runnable onDismiss,
            CrudEventBus eventBus,
            Optional<String> currentLocationUuid,
            TentSelectedCallback tentSelectedCallback) {
        mContext = checkNotNull(context);
        mAppModel = checkNotNull(appModel);
        mLocale = checkNotNull(locale);
        this.mOnDismiss = checkNotNull(onDismiss);
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
        mAdapter.setmSelectedLocationUuid(mCurrentLocationUuid);

        Toast.makeText( mContext, "Failed to update patient, reason: " + Integer.toString( reason ), Toast.LENGTH_SHORT ).show();
        mProgressDialog.dismiss();
    }

    private void startListeningForLocations() {
        mEventBus.register(mEventBusSubscriber);
        mAppModel.fetchLocationTree(mEventBus, mLocale);
    }

    private void setTents(AppLocationTree locationTree) {
        if (mGridView != null) {
            List<AppLocation> locations = new ArrayList(
                    locationTree.getDescendantsAtDepth(AppLocationTree.ABSOLUTE_DEPTH_TENT));
            Collections.sort(locations, new AppLocationComparator(locationTree));
            AppLocation dischargedZone = locationTree.findByUuid(Zone.DISCHARGED_ZONE_UUID);
            locations.add(dischargedZone);
            mAdapter = new TentListAdapter(mContext, locations, locationTree, mCurrentLocationUuid);
            mGridView.setAdapter(mAdapter);
            mGridView.setOnItemClickListener(this);
            mGridView.setSelection(1);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        String newTentUuid = mAdapter.getItem(position).uuid;
        mAdapter.setmSelectedLocationUuid(Optional.fromNullable(newTentUuid));
        mProgressDialog = ProgressDialog.show(mContext,
                mContext.getResources().getString(R.string.title_updating_patient),
                mContext.getResources().getString(R.string.please_wait), true);
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
        return mCurrentLocationUuid.equals(mAdapter.getmSelectedLocationUuid());
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mEventBus.unregister(mEventBusSubscriber);
        mOnDismiss.run();
    }

    private final class EventBusSubscriber {

        public void onEventMainThread(AppLocationTreeFetchedEvent event) {
            if (event.tree.getRoot() == null) {
                LOG.d("AppLocationTree has a null root, suggesting something went wrong.");
                return;
            }

            setTents(event.tree);
        }
    }
}
