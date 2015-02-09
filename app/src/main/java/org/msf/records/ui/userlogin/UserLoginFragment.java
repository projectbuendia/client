package org.msf.records.ui.userlogin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.net.model.User;
import org.msf.records.ui.ProgressFragment;
import org.msf.records.utils.Colorizer;
import org.msf.records.utils.Logger;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;

/**
 * Fragment for {@link UserLoginActivity}.
 */
public class UserLoginFragment extends ProgressFragment {
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

    @OnItemClick(R.id.users)
    void onUsersItemClick(int position) {
        UserLoginController controller =
                ((UserLoginActivity)getActivity()).getUserLoginController();
        if (controller != null) {
            controller.onUserSelected(mUserListAdapter.getItem(position));
        } else {
            LOG.e("No UserLoginController available. This should never happen.");
        }
    }

    /**
     * Returns the {@link FragmentUi} for interfacing with the {@link UserLoginController}.
     */
    public FragmentUi getFragmentUi() {
        return mFragmentUi;
    }

    private class FragmentUi implements UserLoginController.FragmentUi {

        @Override
        public void showSpinner(boolean show) {
            changeState(show ? State.LOADING : State.LOADED);
        }

        @Override
        public void showUsers(List<User> users) {
            mUserListAdapter.setNotifyOnChange(false);
            mUserListAdapter.clear();
            mUserListAdapter.addAll(users);
            mUserListAdapter.notifyDataSetChanged();
        }
    }
}
