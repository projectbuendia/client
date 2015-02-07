package org.msf.records.ui.userlogin;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.SettingsActivity;
import org.msf.records.ui.dialogs.AddNewUserDialogFragment;
import org.msf.records.ui.tentselection.TentSelectionActivity;
import org.msf.records.utils.EventBusWrapper;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import de.greenrobot.event.EventBus;

/**
 * Screen allowing the user to login by selecting their name.
 */
public class UserLoginActivity extends BaseActivity {

    private UserLoginController mController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        UserLoginFragment fragment =
                (UserLoginFragment)getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_user_login);
        mController = new UserLoginController(
        		App.getUserManager(),
        		new EventBusWrapper(EventBus.getDefault()),
        		new MyUi(),
                fragment.getFragmentUi());
    }

    /**
     * Returns the {@link UserLoginController} used by this activity. After onCreate, this should
     * never be null.
     */
    public UserLoginController getUserLoginController() {
        return mController;
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
                    	mController.onSettingsPressed();
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
    }
}
