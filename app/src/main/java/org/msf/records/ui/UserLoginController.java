package org.msf.records.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.msf.records.R;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.events.user.UserAddFailedEvent;
import org.msf.records.events.user.UserAddedEvent;
import org.msf.records.net.model.User;
import org.msf.records.user.UserManager;
import org.msf.records.utils.EventBusRegistrationInterface;

import android.util.Log;

import com.google.common.collect.Ordering;

/**
 * Controller for {@link UserLoginActivity}.
 *
 * <p>Don't add untestable dependencies to this class.
 */
final class UserLoginController {

	private static final String TAG = UserLoginController.class.getSimpleName();
	private static final boolean DEBUG = true;

    public interface Ui {
    	void showAddNewUserDialog();
    	void showSettings();
    	void showErrorToast(int stringResourceId);
    	void showUsers(List<User> users);
    	void showTentSelectionScreen();
    }

    private final EventBusRegistrationInterface mEventBus;
    private final Ui mUi;
    private final UserManager mUserManager;
    private final List<User> mUsersSortedByName = new ArrayList<User>();
	private final BusEventSubscriber mSubscriber = new BusEventSubscriber();

    public UserLoginController(
    		UserManager userManager,
    		EventBusRegistrationInterface eventBus,
    		Ui ui) {
    	mUserManager = userManager;
    	mEventBus = eventBus;
    	mUi = ui;
    }

    public void init() {
    	mEventBus.register(mSubscriber);
    	mUserManager.loadKnownUsers();
    }

    public void suspend() {
    	mEventBus.unregister(mSubscriber);
    }

    /** Call when the user presses the 'add user' button. */
    public void onAddUserPressed() {
    	mUi.showAddNewUserDialog();
    }

    /** Call when the user presses the settings button. */
    public void onSettingPressed() {
		mUi.showSettings();
    }

    /** Call when the user taps to select a user. */
    public void onUserSelected(User user) {
        mUserManager.setActiveUser(user);
        mUi.showTentSelectionScreen();
    }

    @SuppressWarnings("unused") // Called by reflection from event bus.
    private final class BusEventSubscriber {
    	/** Updates the UI when the list of users is loaded. */
    	public void onEventMainThread(KnownUsersLoadedEvent event) {
    		if (DEBUG) {
    			Log.d(TAG, "Loaded list of " + event. mKnownUsers.size() + " users");
    		}
    		mUsersSortedByName.clear();
    		mUsersSortedByName.addAll(Ordering.from(User.COMPARATOR_BY_NAME).sortedCopy(event.mKnownUsers));
    		mUi.showUsers(mUsersSortedByName);
        }

        public void onEventMainThread(UserAddedEvent event) {
    		if (DEBUG) {
    			Log.d(TAG, "User added");
    		}
            insertIntoSortedList(mUsersSortedByName, User.COMPARATOR_BY_NAME, event.mAddedUser);
            mUi.showUsers(mUsersSortedByName);
        }

        public void onEventMainThread(UserAddFailedEvent event) {
        	if (DEBUG) {
    			Log.d(TAG, "Failed to add user");
    		}
        	mUi.showErrorToast(errorToStringId(event));
        }
    }

    /** Converts a {@link UserAddFailedEvent} to an error string resource id. */
    private static int errorToStringId(UserAddFailedEvent event) {
        switch (event.mReason) {
            case UserAddFailedEvent.REASON_UNKNOWN:
                return R.string.add_user_unknown_error;
            case UserAddFailedEvent.REASON_INVALID_USER:
                return R.string.add_user_invalid_user;
            case UserAddFailedEvent.REASON_USER_EXISTS_LOCALLY:
                return R.string.add_user_user_exists_locally;
            case UserAddFailedEvent.REASON_USER_EXISTS_ON_SERVER:
                return R.string.add_user_user_exists_on_server;
            case UserAddFailedEvent.REASON_SERVER_ERROR:
                return R.string.add_user_server_error;
            default:
                return R.string.add_user_unknown_error;
        }
    }

    /** Given a sorted list, inserts a new element in the correct position to maintain the sorted order. */
    private static <T> void insertIntoSortedList(List<T> list, Comparator<T> comparator, T newItem) {
    	 int i;
         for (i = 0; i < list.size(); i++) {
             if (comparator.compare(list.get(i),  newItem) == 1) {
            	 break;
             }
         }
         list.add(i, newItem);
    }
}
