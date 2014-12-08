package org.msf.records.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.events.user.UserAddFailedEvent;
import org.msf.records.events.user.UserAddedEvent;
import org.msf.records.ui.dialogs.AddNewUserDialogFragment;

import de.greenrobot.event.EventBus;

/**
 * A {@link FragmentActivity} that allows a user to login.
 */
public class UserLoginActivity extends FragmentActivity {
    private UserLoginFragment mFragment = null;
    private static final String TAG = "UserLoginActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_login);

        mFragment = new UserLoginFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.user_login_container, mFragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login, menu);

        menu.findItem(R.id.action_add_user).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        EventBus.getDefault().register(new UserAddEventSubscriber());
                        EventBus.getDefault().register(new UserAddFailedEventSubscriber());
                        FragmentManager fm = getSupportFragmentManager();
                        AddNewUserDialogFragment dialogFragment =
                                AddNewUserDialogFragment.newInstance();
                        dialogFragment.show(fm, null);
                        return true;
                    }
                }
        );

        menu.findItem(R.id.settings).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Intent settingsIntent =
                                new Intent(UserLoginActivity.this, SettingsActivity.class);
                        startActivity(settingsIntent);

                        return true;
                    }
                }
        );

        return true;
    }

    private class UserAddEventSubscriber {
        public UserAddEventSubscriber() {}

        public void onEventMainThread(final UserAddedEvent event) {
            App.getUserManager().loadKnownUsers();
        }
    }

    private class UserAddFailedEventSubscriber {
        public UserAddFailedEventSubscriber() {}

        public void onEventMainThread(UserAddFailedEvent event) {
            Toast toast = Toast.makeText(
                    UserLoginActivity.this, toErrorString(event), Toast.LENGTH_SHORT);
            toast.show();
        }

        private String toErrorString(UserAddFailedEvent event) {
            int errorResource;
            switch (event.mReason) {
                case UserAddFailedEvent.REASON_UNKNOWN:
                    errorResource = R.string.add_user_unknown_error;
                    break;
                case UserAddFailedEvent.REASON_INVALID_USER:
                    errorResource = R.string.add_user_invalid_user;
                    break;
                case UserAddFailedEvent.REASON_USER_EXISTS_LOCALLY:
                    errorResource = R.string.add_user_user_exists_locally;
                    break;
                case UserAddFailedEvent.REASON_USER_EXISTS_ON_SERVER:
                    errorResource = R.string.add_user_user_exists_on_server;
                    break;
                case UserAddFailedEvent.REASON_SERVER_ERROR:
                    errorResource = R.string.add_user_server_error;
                    break;
                default:
                    errorResource = R.string.add_user_unknown_error;
            }
            return getResources().getString(errorResource);
        }
    }
}
