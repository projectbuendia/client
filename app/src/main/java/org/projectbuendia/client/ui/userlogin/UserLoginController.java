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

package org.projectbuendia.client.ui.userlogin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.projectbuendia.client.R;
import org.projectbuendia.client.diagnostics.Troubleshooter;
import org.projectbuendia.client.events.diagnostics.TroubleshootingActionsChangedEvent;
import org.projectbuendia.client.events.user.KnownUsersLoadFailedEvent;
import org.projectbuendia.client.events.user.KnownUsersLoadedEvent;
import org.projectbuendia.client.events.user.UserAddFailedEvent;
import org.projectbuendia.client.events.user.UserAddedEvent;
import org.projectbuendia.client.net.model.User;
import org.projectbuendia.client.ui.dialogs.AddNewUserDialogFragment;
import org.projectbuendia.client.user.UserManager;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import com.google.common.collect.Ordering;

/** Controller for {@link UserLoginActivity}. */
public final class UserLoginController {

    private static final Logger LOG = Logger.create();

    public interface Ui {

        void showAddNewUserDialog();

        void showSettings();

        void showErrorToast(int stringResourceId);

        void showSyncFailedDialog(boolean show);

        void showTentSelectionScreen();
    }

    public interface FragmentUi {

        void showSpinner(boolean show);

        void showUsers(List<User> users);
    }

    private final EventBusRegistrationInterface mEventBus;
    private final Ui mUi;
    private final FragmentUi mFragmentUi;
    private final DialogActivityUi mDialogUi = new DialogActivityUi();
    private final UserManager mUserManager;
    private final List<User> mUsersSortedByName = new ArrayList<>();
    private final BusEventSubscriber mSubscriber = new BusEventSubscriber();
    private final Troubleshooter mTroubleshooter;

    /**
     * Instantiates a {@link UserLoginController}.
     * @param userManager a {@link UserManager} from which users will be fetched
     * @param eventBus an {@link EventBusRegistrationInterface} for listening to user fetch and
     *                 modification events
     * @param troubleshooter a {@link Troubleshooter} for monitoring server health; if the server
     *                       becomes available and the controller has no data available, the
     *                       controller will automatically retry fetching users
     * @param ui a {@link Ui} for handling activity changes
     * @param fragmentUi a {@link FragmentUi} for displaying users
     */
    public UserLoginController(
            UserManager userManager,
            EventBusRegistrationInterface eventBus,
            Troubleshooter troubleshooter,
            Ui ui,
            FragmentUi fragmentUi) {
        mUserManager = userManager;
        mEventBus = eventBus;
        mTroubleshooter = troubleshooter;
        mUi = ui;
        mFragmentUi = fragmentUi;
    }

    /**
     * Requests any necessary resources. Note that some resources may be fetched asynchronously
     * after this function returns.
     */
    public void init() {
        mEventBus.register(mSubscriber);
        mFragmentUi.showSpinner(true);
        mUserManager.loadKnownUsers();
    }

    /** Attempts to reload users. */
    public void onSyncRetry() {
        mFragmentUi.showSpinner(true);
        mUserManager.loadKnownUsers();
    }

    public void suspend() {
        mEventBus.unregister(mSubscriber);
    }

    /** Call when the user presses the 'add user' button. */
    public void onAddUserPressed() {
        Utils.logEvent("add_user_button_pressed");
        mUi.showAddNewUserDialog();
    }

    /** Call when the user presses the settings button. */
    public void onSettingsPressed() {
        Utils.logEvent("settings_button_pressed");
        mUi.showSettings();
    }

    /** Call when the user taps to select a user. */
    public void onUserSelected(User user) {
        mUserManager.setActiveUser(user);
        Utils.logUserAction("logged_in");
        mUi.showTentSelectionScreen();
    }

    @SuppressWarnings("unused") // Called by reflection from event bus.
    private final class BusEventSubscriber {
        /** Restart user fetch if we have no users and the Buendia API just became available. */
        public void onEventMainThread(TroubleshootingActionsChangedEvent event) {
            if (mUsersSortedByName.isEmpty() && mTroubleshooter.isServerHealthy()) {
                LOG.d("Buendia API is available and users are not, retrying sync.");
                onSyncRetry();
            }
        }

        /** Updates the UI when the list of users is loaded. */
        public void onEventMainThread(KnownUsersLoadedEvent event) {
            LOG.d("Loaded list of " + event.knownUsers.size() + " users");
            mUsersSortedByName.clear();
            mUsersSortedByName
                    .addAll(Ordering.from(User.COMPARATOR_BY_NAME).sortedCopy(event.knownUsers));
            mFragmentUi.showUsers(mUsersSortedByName);
            mFragmentUi.showSpinner(false);
            mUi.showSyncFailedDialog(false);
        }

        public void onEventMainThread(KnownUsersLoadFailedEvent event) {
            LOG.e("Failed to load list of users");
            mUi.showSyncFailedDialog(true);
        }

        public void onEventMainThread(UserAddedEvent event) {
            mUi.showSyncFailedDialog(false);  // Just in case.
            LOG.d("User added");
            insertIntoSortedList(mUsersSortedByName, User.COMPARATOR_BY_NAME, event.addedUser);
            mFragmentUi.showUsers(mUsersSortedByName);
            mFragmentUi.showSpinner(false);
        }

        public void onEventMainThread(UserAddFailedEvent event) {
            LOG.d("Failed to add user");
            mUi.showErrorToast(errorToStringId(event));
            mFragmentUi.showSpinner(false);
        }
    }

    public AddNewUserDialogFragment.ActivityUi getDialogUi() {
        return mDialogUi;
    }

    public final class DialogActivityUi implements AddNewUserDialogFragment.ActivityUi {

        @Override
        public void showSpinner(boolean show) {
            mFragmentUi.showSpinner(show);
        }
    }

    /** Converts a {@link UserAddFailedEvent} to an error string resource id. */
    private static int errorToStringId(UserAddFailedEvent event) {
        switch (event.reason) {
            case UserAddFailedEvent.REASON_UNKNOWN:
                return R.string.add_user_unknown_error;
            case UserAddFailedEvent.REASON_INVALID_USER:
                return R.string.add_user_invalid_user;
            case UserAddFailedEvent.REASON_USER_EXISTS_LOCALLY:
                return R.string.add_user_user_exists_locally;
            case UserAddFailedEvent.REASON_USER_EXISTS_ON_SERVER:
                return R.string.add_user_user_exists_on_server;
            case UserAddFailedEvent.REASON_CONNECTION_ERROR:
                return R.string.add_user_connection_error;
            default:
                return R.string.add_user_unknown_error;
        }
    }

    /**
     * Given a sorted list, inserts a new element in the correct position to maintain the sorted
     * order.
     */
    private static <T> void insertIntoSortedList(
            List<T> list, Comparator<T> comparator, T newItem) {
        int index;
        for (index = 0; index < list.size(); index++) {
            if (comparator.compare(list.get(index), newItem) > 0) {
                break;
            }
        }
        list.add(index, newItem);
    }
}
