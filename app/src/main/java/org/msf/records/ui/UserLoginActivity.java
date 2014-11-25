package org.msf.records.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import org.msf.records.R;
import org.msf.records.net.Constants;

/**
 * A {@link FragmentActivity} that allows a user to login.
 */
public class UserLoginActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_login);

        UserLoginFragment fragment = new UserLoginFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.user_login_container, fragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login, menu);

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
}
