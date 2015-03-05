package org.msf.records.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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
import org.msf.records.ui.userlogin.UserLoginActivity;
import org.msf.records.utils.Colorizer;
import org.msf.records.utils.Logger;
import org.msf.records.utils.Utils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * An activity that requires that there currently be a logged-in user.
 */
public abstract class BaseLoggedInActivity extends BaseActivity {

    private static final Logger LOG = Logger.create();

    @Inject Colorizer mUserColorizer;

    private User mLastActiveUser;
    private Menu mMenu;
    private MenuPopupWindow mPopupWindow;

    private boolean mIsCreated = false;

    protected UpdateNotificationController mUpdateNotificationController = null;

    private LoadingState mLoadingState = LoadingState.LOADED;

    /**
     * {@inheritDoc}
     *
     * <p>Instead of overriding this method, override {@link #onCreateImpl}.
     */
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        User user = App.getUserManager().getActiveUser();
        if (user == null) {
            super.onCreate(savedInstanceState);

            // If there is no active user, then return the user to the user login activity.
            BigToast.show(this, "Please login to continue");

            Intent intent = new Intent(this, UserLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            return;
        }

        onCreateImpl(savedInstanceState);
        mIsCreated = true;
    }

    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        if (!mIsCreated) {
            return true;
        }

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
    protected final void onStart() {
        if (!mIsCreated) {
            super.onStart();

            return;
        }

        onStartImpl();
    }

    protected void onStartImpl() {
        super.onStart();
    }

    @Override
    protected final void onResume() {
        if (!mIsCreated) {
            super.onResume();

            return;
        }

        onResumeImpl();
        if (mUpdateNotificationController != null) {
            mUpdateNotificationController.init();
        }
    }

    protected void onResumeImpl() {
        super.onResume();
    }

    @Override
    protected final void onPause() {
        if (!mIsCreated) {
            super.onPause();

            return;
        }

        if (mUpdateNotificationController != null) {
            mUpdateNotificationController.suspend();
        }
        onPauseImpl();
    }

    protected void onPauseImpl() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }

        super.onPause();
    }

    @Override
    protected final void onStop() {
        if (!mIsCreated) {
            super.onStop();

            return;
        }

        onStopImpl();
    }

    protected void onStopImpl() {
        super.onStop();
    }

    private void updateActiveUser() {
        User user = App.getUserManager().getActiveUser();

        if (mLastActiveUser == null || mLastActiveUser.compareTo(user) != 0) {
            LOG.w("The user has switched. I don't know how to deal with that right now");
            // TODO(dxchen): Handle.
        }
        mLastActiveUser = user;

        TextView initials = (TextView) mMenu
                .getItem(mMenu.size() - 1)
                .getActionView()
                .findViewById(R.id.user_initials);

        initials.setBackgroundColor(mUserColorizer.getColorArgb(user.id));
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

        @SuppressLint("InflateParams") public MenuPopupWindow() {
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

            mUserName.setText(App.getUserManager().getActiveUser().fullName);
        }

        @OnClick(R.id.button_settings)
        public void onSettingsClick() {
            Intent settingsIntent = new Intent(BaseLoggedInActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        @OnClick(R.id.button_log_out)
        public void onLogOutClick() {
            Utils.logUserAction("logged_out");
            App.getUserManager().setActiveUser(null);

            Intent settingsIntent = new Intent(BaseLoggedInActivity.this, UserLoginActivity.class);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsIntent);
        }
    }

    /**
     * Changes the state of this activity, changing the set of available buttons if necessary.
     * @param loadingState the new activity state
     */
    protected void setLoadingState(LoadingState loadingState) {
        if (mLoadingState != loadingState) {
            mLoadingState = loadingState;
            invalidateOptionsMenu();
        }
    }

    protected LoadingState getLoadingState() {
        return mLoadingState;
    }
}

