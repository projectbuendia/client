// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui;

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

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.PatientChartRequestedEvent;
import org.projectbuendia.client.events.user.ActiveUserUnsetEvent;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.ui.chart.PatientChartActivity;
import org.projectbuendia.client.ui.login.LoginActivity;
import org.projectbuendia.client.utils.Colorizer;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/** A {@link BaseActivity} that requires that there currently be a logged-in user. */
public abstract class BaseLoggedInActivity extends BaseActivity {

    private static final Logger LOG = Logger.create();

    @Inject Colorizer mUserColorizer;

    private JsonUser mLastActiveUser;
    private Menu mMenu;
    private MenuPopupWindow mPopupWindow;

    private boolean mIsCreated = false;

    protected UpdateNotificationController mUpdateNotificationController = null;

    private LoadingState mLoadingState = LoadingState.LOADED;

    /**
     * {@inheritDoc}
     * <p/>
     * <p>Instead of overriding this method, override {@link #onCreateImpl}.
     */
    @Override public final void onCreate(Bundle savedInstanceState) {
        JsonUser user = App.getUserManager().getActiveUser();
        if (user == null) {
            super.onCreate(savedInstanceState);

            // If there is no active user, then return the user to the user login activity.
            BigToast.show(this, R.string.toast_please_login);

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Utils.logEvent("redirected_to_login");
            return;
        }

        onCreateImpl(savedInstanceState);
        mIsCreated = true;
    }

    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override public final boolean onCreateOptionsMenu(Menu menu) {
        if (!mIsCreated) {
            return true;
        }

        mMenu = menu;
        onExtendOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.base, menu);

        mPopupWindow = new MenuPopupWindow();

        final View userView = mMenu.getItem(mMenu.size() - 1).getActionView();
        userView.setOnClickListener(new View.OnClickListener() {

            @Override public void onClick(View view) {
                mPopupWindow.showAsDropDown(userView);
            }
        });

        updateActiveUser();

        return true;
    }

    public void onExtendOptionsMenu(Menu menu) {
    }

    private void updateActiveUser() {
        JsonUser user = App.getUserManager().getActiveUser();

        if (mLastActiveUser == null || mLastActiveUser.compareTo(user) != 0) {
            LOG.w("The user has switched. I don't know how to deal with that right now");
            // TODO: Handle.
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
        // TODO: Implement this in one way or another!
    }

    public void onEvent(PatientChartRequestedEvent event) {
        PatientChartActivity.start(this, event.uuid);
    }

    @Override protected final void onStart() {
        if (!mIsCreated) {
            super.onStart();

            return;
        }

        onStartImpl();
    }

    protected void onStartImpl() {
        super.onStart();
    }

    @Override protected final void onResume() {
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

    @Override protected final void onPause() {
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

    @Override protected final void onStop() {
        if (!mIsCreated) {
            super.onStop();

            return;
        }

        onStopImpl();
    }

    protected void onStopImpl() {
        super.onStop();
    }

    protected LoadingState getLoadingState() {
        return mLoadingState;
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

    class MenuPopupWindow extends PopupWindow {

        private final LinearLayout mLayout;

        @InjectView(R.id.user_name) TextView mUserName;
        @InjectView(R.id.button_settings) ImageButton mSettings;
        @InjectView(R.id.button_log_out) ImageButton mLogOut;

        @SuppressLint("InflateParams")
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

        @Override public void showAsDropDown(View anchor) {
            super.showAsDropDown(anchor);

            JsonUser user = App.getUserManager().getActiveUser();
            if (user == null) {
                // TODO: Handle no user.
                return;
            }

            mUserName.setText(App.getUserManager().getActiveUser().fullName);
        }

        @OnClick(R.id.button_settings)
        public void onSettingsClick() {
            Utils.logUserAction("popup_settings_button_pressed");
            SettingsActivity.start(BaseLoggedInActivity.this);
        }

        @OnClick(R.id.button_log_out)
        public void onLogOutClick() {
            Utils.logUserAction("popup_logout_button_pressed");
            App.getUserManager().setActiveUser(null);

            Intent intent = new Intent(BaseLoggedInActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}

