package org.msf.records.ui;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.events.user.ActiveUserUnsetEvent;
import org.msf.records.net.model.User;
import org.msf.records.utils.Colorizer;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/**
 * An abstract {@link FragmentActivity} that is the base for all activities.
 */
public abstract class BaseActivity extends ControllableActivity {

    @Inject Colorizer mUserColorizer;

    private User lastActiveUser;
    private Menu mMenu;
    private MenuPopupWindow mPopupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.getInstance().inject(this);
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

        mPopupWindow = new MenuPopupWindow();
        final View userView = mMenu.getItem(mMenu.size() - 1).getActionView();
        userView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mPopupWindow.showAsDropDown(userView);
            }
        });

        updateActiveUser();

        return true;
    }

    public void onExtendOptionsMenu(Menu menu) {}

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);

        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }

        super.onPause();
    }

    private void updateActiveUser() {
        User user = App.getUserManager().getActiveUser();
        if (user == null) {
            // TODO(dxchen): Handle no user.
            return;
        }

        if (lastActiveUser == null || user.compareTo(lastActiveUser) != 0) {
            // TODO(dxchen): Handle a user switch.
        }
        lastActiveUser = user;

        TextView initials = (TextView) mMenu
                .getItem(mMenu.size() - 1)
                .getActionView()
                .findViewById(R.id.user_initials);

        initials.setBackgroundColor(mUserColorizer.getColorArgb(user.getId()));
        initials.setText(user.getInitials());
    }

    public void onEvent(ActiveUserUnsetEvent event) {
        // TODO(dxchen): Implement this in one way or another!
    }

    class MenuPopupWindow extends PopupWindow {

        private final LinearLayout mLayout;

        @InjectView(R.id.user_name) TextView mUserName;
        @InjectView(R.id.button_settings) ImageButton mSettings;
        @InjectView(R.id.button_log_out) ImageButton mLogOut;

        public MenuPopupWindow() {
            super();

            mLayout = (LinearLayout) getLayoutInflater()
                    .inflate(R.layout.popup_window_user, null);
            setContentView(mLayout);

            ButterKnife.inject(this, mLayout);

            setWindowLayoutMode(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            setFocusable(true);
            setOutsideTouchable(true);
            setBackgroundDrawable(new BitmapDrawable());
        }

        @Override
        public void showAsDropDown(View anchor) {
            super.showAsDropDown(anchor);

            User user = App.getUserManager().getActiveUser();
            if (user == null) {
                // TODO(dxchen): Handle no user.
                return;
            }

            mUserName.setText(App.getUserManager().getActiveUser().getFullName());
        }

        @OnClick(R.id.button_settings)
        public void onSettingsClick() {
            Intent settingsIntent = new Intent(BaseActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        @OnClick(R.id.button_log_out)
        public void onLogOutClick() {
            App.getUserManager().setActiveUser(null);

            Intent settingsIntent = new Intent(BaseActivity.this, UserLoginActivity.class);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsIntent);
        }
    }
}

