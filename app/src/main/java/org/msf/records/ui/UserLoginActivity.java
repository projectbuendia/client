package org.msf.records.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;

import org.msf.records.R;

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
}
