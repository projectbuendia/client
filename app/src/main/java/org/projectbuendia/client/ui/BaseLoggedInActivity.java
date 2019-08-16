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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.PatientChartRequestedEvent;
import org.projectbuendia.client.events.user.ActiveUserUnsetEvent;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.ui.chart.PatientChartActivity;
import org.projectbuendia.client.ui.dialogs.GoToPatientDialogFragment;
import org.projectbuendia.client.ui.login.LoginActivity;
import org.projectbuendia.client.utils.Colorizer;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import static org.projectbuendia.client.utils.Utils.eq;

/** A {@link BaseActivity} that requires that there currently be a logged-in user. */
public abstract class BaseLoggedInActivity extends BaseActivity {

    private static final Logger LOG = Logger.create();

    @Inject Colorizer mUserColorizer;

    private JsonUser mLastActiveUser;
    private Menu mMenu;
    private MenuPopupWindow mPopupWindow;

    private boolean mIsCreated = false;

    protected UpdateNotificationController mUpdateNotificationController = null;

    private ReadyState mReadyState = ReadyState.READY;

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
            BigToast.show(this, "Please login to continue"); // TODO/i18n

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Utils.logEvent("redirected_to_login");
            return;
        }

        // Turn the action bar icon into a "back" arrow that goes back in the activity stack.
        getActionBar().setIcon(R.drawable.ic_back_36dp);
        getActionBar().setDisplayHomeAsUpEnabled(true);

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

        MenuItem searchByIdItem = menu.findItem(R.id.action_go_to);
        setMenuBarIcon(searchByIdItem, FontAwesomeIcons.fa_search);

        searchByIdItem.setOnMenuItemClickListener(menuItem -> {
            Utils.logUserAction("go_to_patient_pressed");
            GoToPatientDialogFragment.newInstance().show(getSupportFragmentManager(), null);
            return true;
        });

        mPopupWindow = new MenuPopupWindow();

        final View userView = mMenu.getItem(mMenu.size() - 1).getActionView();
        userView.setOnClickListener(view -> mPopupWindow.showAsDropDown(userView));

        updateActiveUser();

        return true;
    }

    public void onExtendOptionsMenu(Menu menu) {
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // Go back rather than reloading the activity, so that the patient list retains its
            // filter state.
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateActiveUser() {
        JsonUser user = App.getUserManager().getActiveUser();

        if (!eq(mLastActiveUser, user)) {
            LOG.w("User has switched from %s to %s", mLastActiveUser, user);
        }
        mLastActiveUser = user;

        TextView initials = mMenu
            .getItem(mMenu.size() - 1)
            .getActionView()
            .findViewById(R.id.user_initials);

        initials.setBackgroundColor(mUserColorizer.getColorArgb(user.fullName));
        initials.setText(user.getInitials());
    }

    public void onEvent(ActiveUserUnsetEvent event) {
        // TODO: Implement this in one way or another!
    }

    public void onEventMainThread(PatientChartRequestedEvent event) {
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

    protected ReadyState getReadyState() {
        return mReadyState;
    }

    /** Changes the activity's ready state and updates the set of available buttons. */
    protected void setReadyState(ReadyState state) {
        if (mReadyState != state) {
            mReadyState = state;
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

