package org.msf.records.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.TextView;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.drawable.TextDrawable;
import org.msf.records.events.user.ActiveUserUnsetEvent;
import org.msf.records.model.User;
import org.msf.records.utils.Constants;

import de.greenrobot.event.EventBus;

/**
 * An abstract {@link FragmentActivity} that is the base for all activities.
 */
public abstract class BaseActivity extends FragmentActivity {

    private User lastActiveUser;
    private Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
    }

    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        onExtendOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.base, menu);

        mMenu
                .getItem(mMenu.size() -1)
                .getActionView()
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(BaseActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    }
                });

        updateActiveUser();

        return true;
    }

    public void onExtendOptionsMenu(Menu menu) {}

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    private void updateActiveUser() {
        User user = App.getUserManager().getActiveUser();
        if (!user.equals(lastActiveUser)) {
            // TODO(dxchen): Handle a user switch.
        }
        lastActiveUser = user;

        TextView initials = (TextView) mMenu
                .getItem(mMenu.size() - 1)
                .getActionView()
                .findViewById(R.id.user_initials);

        initials.setBackgroundColor(Constants.USER_COLORIZER.getColorArgb(user.getId()));
        initials.setText(user.getInitials());
    }

    public void onEvent(ActiveUserUnsetEvent event) {
        // TODO(dxchen): Implement this in one way or another!
    }
}
