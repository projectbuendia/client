package org.msf.records.ui;

import java.util.List;

import javax.inject.Inject;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.net.model.User;
import org.msf.records.ui.dialogs.AddNewUserDialogFragment;
import org.msf.records.utils.Colorizer;
import org.msf.records.utils.EventBusWrapper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;
import de.greenrobot.event.EventBus;

/**
 * Screen allowing the user to login by selecting their name.
 */
public class UserLoginActivity extends FragmentActivity {

    @Inject Colorizer mUserColorizer;
    @InjectView(R.id.users) GridView mUserListView;
    private UserLoginController mController;
    private UserListAdapter mUserListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);

        setContentView(R.layout.fragment_user_login);
        ButterKnife.inject(this);

        mUserListAdapter = new UserListAdapter(this);
        mUserListView.setAdapter(mUserListAdapter);
        mController = new UserLoginController(
        		App.getUserManager(),
        		new EventBusWrapper(EventBus.getDefault()),
        		new MyUi());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login, menu);

        menu.findItem(R.id.action_add_user).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                    	mController.onAddUserPressed();
                        return true;
                    }
                }
        );

        menu.findItem(R.id.settings).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                    	mController.onSettingPressed();
                        return true;
                    }
                }
        );

        return true;
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	mController.init();
    }

    @Override
    protected void onPause() {
    	mController.suspend();
    	super.onPause();
    }

    @OnItemClick(R.id.users)
    void onUsersItemClick(int position) {
    	mController.onUserSelected(mUserListAdapter.getItem(position));
    }

    private final class UserListAdapter extends ArrayAdapter<User> {
        public UserListAdapter(Context context) {
            super(context, R.layout.grid_item_user);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            UserListItemViewHolder holder;
            if (view != null) {
                holder = (UserListItemViewHolder) view.getTag();
            } else {
                view = LayoutInflater.from(getContext())
                        .inflate(R.layout.grid_item_user, parent, false);
                holder = new UserListItemViewHolder(view);
                view.setTag(holder);
            }

            User user = getItem(position);
            holder.initials
                    .setBackgroundColor(mUserColorizer.getColorArgb(user.getId()));
            holder.initials.setText(user.getInitials());
            holder.fullName.setText(user.getFullName());

            return view;
        }
    }

    static final class UserListItemViewHolder {
        @InjectView(R.id.user_initials) public TextView initials;
        @InjectView(R.id.user_name) public TextView fullName;

        public UserListItemViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }

    private final class MyUi implements UserLoginController.Ui {
    	@Override
    	public void showAddNewUserDialog() {
            FragmentManager fm = getSupportFragmentManager();
            AddNewUserDialogFragment dialogFragment = AddNewUserDialogFragment.newInstance();
            dialogFragment.show(fm, null);
    	}

    	@Override
    	public void showSettings() {
            Intent settingsIntent =
                    new Intent(UserLoginActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
    	}

    	@Override
    	public void showErrorToast(int stringResourceId) {
    		Toast toast = Toast.makeText(
    				UserLoginActivity.this,
    				getResources().getString(stringResourceId),
    				Toast.LENGTH_SHORT);
    		toast.show();
    	}

    	@Override
    	public void showTentSelectionScreen() {
            startActivity(new Intent(UserLoginActivity.this, TentSelectionActivity.class));
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
