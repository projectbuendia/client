package org.msf.records.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.collect.Lists;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.events.user.UserAddedEvent;
import org.msf.records.model.User;
import org.msf.records.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;
import de.greenrobot.event.EventBus;

/**
 * A {@link Fragment} that allows a user to login.
 */
public class UserLoginFragment extends Fragment {

    @InjectView(R.id.users) GridView mUsersGrid;
    private UserListAdapter mUserListAdapter;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        RelativeLayout layout =
                (RelativeLayout) inflater.inflate(R.layout.fragment_user_login, null);
        ButterKnife.inject(this, layout);

        mUserListAdapter = new UserListAdapter(getActivity());
        mUsersGrid.setAdapter(mUserListAdapter);

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);

        App.getUserManager().loadKnownUsers();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    public void onEventMainThread(KnownUsersLoadedEvent event) {
        List<User> users = Lists.newArrayList(event.mKnownUsers);
        Collections.sort(users, User.COMPARATOR_BY_NAME);

        mUserListAdapter.clear();
        mUserListAdapter.addAll(users);

        // TODO(dxchen): Center the damn GridView, because it won't center itself.
    }

    public synchronized void onUserAdded(UserAddedEvent event) {
        List<User> users = new ArrayList<User>();

        // Create a list of users from the list of current users with the new user inserted in
        // the right place.
        User current;
        int i;
        for (i = 0; i < mUserListAdapter.getCount(); i++) {
            current = mUserListAdapter.getItem(i);

            if (current.compareTo(event.mAddedUser) == 1) {
                users.add(event.mAddedUser);
            }
            users.add(mUserListAdapter.getItem(i));
        }
        for (int j = i+1; j < mUserListAdapter.getCount(); j++) {
            users.add(mUserListAdapter.getItem(j));
        }

        // Update the UI.
        mUserListAdapter.clear();
        mUserListAdapter.addAll(users);
    }

    @OnItemClick(R.id.users)
    void onUsersItemClick(int position) {
        // TODO(dxchen): Error handling.
        App.getUserManager().setActiveUser(mUserListAdapter.getItem(position));

        getActivity().startActivity(new Intent(getActivity(), TentSelectionActivity.class));
    }

    static class UserListAdapter extends ArrayAdapter<User> {

        public UserListAdapter(Context context) {
            super(context, R.layout.grid_item_user);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder holder;
            if (view != null) {
                holder = (ViewHolder) view.getTag();
            } else {
                view = LayoutInflater.from(getContext())
                        .inflate(R.layout.grid_item_user, parent, false);
                holder = new ViewHolder(view);
                view.setTag(holder);
            }

            User user = getItem(position);
            holder.mInitials
                    .setBackgroundColor(Constants.USER_COLORIZER.getColorArgb(user.getId()));
            holder.mInitials.setText(user.getInitials());
            holder.mName.setText(user.getFullName());

            return view;
        }

        static class ViewHolder {
            @InjectView(R.id.user_initials) TextView mInitials;
            @InjectView(R.id.user_name) TextView mName;

            public ViewHolder(View view) {
                ButterKnife.inject(this, view);
            }
        }
    }
}
