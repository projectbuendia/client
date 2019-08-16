// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui.lists;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.ui.ProgressFragment;
import org.projectbuendia.client.ui.ReadyState;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.widgets.SubtitledButtonView;

import javax.inject.Inject;

/** Displays a list of all locations. */
public final class LocationListFragment extends ProgressFragment {
    private static final Logger LOG = Logger.create();

    @Inject AppModel mModel;
    @Inject AppSettings mSettings;
    private ContextUtils c;

    private LocationListController mController;
    private final Ui mUi = new Ui();
    private LocationOptionList mList;
    private ScrollView mScroll;

    public LocationListFragment() {
        // Required empty public constructor
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        c = ContextUtils.from(getActivity());
        setContentView(R.layout.fragment_location_selection);
    }

    @Override public View onCreateView(
        LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mScroll = view.findViewById(R.id.scroll_container);
        mList = new LocationOptionList(view.findViewById(R.id.list_container), false);
        LocationForest forest = mModel.getForest(mSettings.getLocaleTag());
        if (forest != null) mList.setLocations(forest, forest.allNodes());
        return view;
    }

    @Override public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mController = ((LocationListActivity) getActivity()).getController();
        if (mController == null) {
            LOG.w("No controller for " + getActivity().getClass().getSimpleName());
        } else {
            mController.attachFragmentUi(mUi);
        }
    }

    @Override public void onResume() {
        super.onResume();
        if (mController != null) mController.init();
        mList.setOnLocationSelectedListener(location -> mController.onLocationSelected(location));
        ViewGroup.LayoutParams lp = mScroll.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        mScroll.setLayoutParams(lp);
    }

    @Override public void onPause() {
        mList.setOnLocationSelectedListener(null);
        if (mController != null) mController.suspend();
        super.onPause();
    }

    @Override public void onDestroyView() {
        if (mController == null) {
            LOG.w("No controller for " + getActivity().getClass().getSimpleName());
        } else {
            mController.detachFragmentUi(mUi);
        }
        super.onDestroyView();
    }

    public void setPatientCount(SubtitledButtonView button, long count) {
        button.setSubtitle("" + count);
        button.setTextColor(0xff000000);
        button.setSubtitleColor(count == 0 ? 0x40000000 : 0xff000000);
    }

    private final class Ui implements LocationListController.LocationListFragmentUi {
        @Override public void setLocations(LocationForest forest, Iterable<Location> locations) {
            mList.setLocations(forest, locations);
        }

        @Override public void setReadyState(ReadyState state) {
            LocationListFragment.this.setReadyState(state);
        }

        @Override public void setSyncProgress(int numerator, int denominator, Integer messageId) {
            setProgress(numerator, denominator);
            if (messageId != null) setProgressMessage(messageId);
        }

        @Override public void showSyncCancelRequested() {
            setProgressMessage(R.string.cancelling_sync);
        }
    }
}
