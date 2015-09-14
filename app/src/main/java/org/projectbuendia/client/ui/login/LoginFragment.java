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

package org.projectbuendia.client.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.net.json.JsonUser;
import org.projectbuendia.client.ui.ProgressFragment;
import org.projectbuendia.client.utils.Colorizer;
import org.projectbuendia.client.utils.Logger;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;

/** Fragment for {@link LoginActivity}. */
public class LoginFragment extends ProgressFragment {
    private static final Logger LOG = Logger.create();

    private FragmentUi mFragmentUi = new FragmentUi();
    private UserListAdapter mUserListAdapter;

    @Inject Colorizer mUserColorizer;
    @InjectView(R.id.users) GridView mUserGrid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);

        setContentView(R.layout.fragment_user_login);

        mUserListAdapter = new UserListAdapter(getActivity(), mUserColorizer);
    }

    @Override
    public View onCreateView(
        LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ButterKnife.inject(this, v);
        mUserGrid.setAdapter(mUserListAdapter);
        return v;
    }

    /** Returns the {@link FragmentUi} for interfacing with the {@link LoginController}. */
    public FragmentUi getFragmentUi() {
        return mFragmentUi;
    }

    @OnItemClick(R.id.users) void onUsersItemClick(int position) {
        LoginController controller =
            ((LoginActivity) getActivity()).getUserLoginController();
        if (controller != null) {
            controller.onUserSelected(mUserListAdapter.getItem(position));
        } else {
            LOG.e("No LoginController available. This should never happen.");
        }
    }

    private class FragmentUi implements LoginController.FragmentUi {

        @Override
        public void showSpinner(boolean show) {
            changeState(show ? State.LOADING : State.LOADED);
        }

        @Override
        public void showUsers(List<JsonUser> users) {
            mUserListAdapter.setNotifyOnChange(false);
            mUserListAdapter.clear();
            mUserListAdapter.addAll(users);
            mUserListAdapter.notifyDataSetChanged();
        }
    }
}
