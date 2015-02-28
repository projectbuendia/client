package org.msf.records.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import org.msf.records.BuildConfig;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.utils.Logger;

public class GenericAccountService extends Service {

    private static final Logger LOG = Logger.create();

    private static final String ACCOUNT_TYPE = BuildConfig.ACCOUNT_TYPE;
    public static final String ACCOUNT_NAME = "sync";
    private static final long SYNC_FREQUENCY = 5 * 60;  // 5 minutes (in seconds)
    private static final String CONTENT_AUTHORITY = Contracts.CONTENT_AUTHORITY;
    private static final String PREF_SETUP_COMPLETE = "setup_complete";
    private Authenticator mAuthenticator;

    /**
     * Obtain a handle to the {@link android.accounts.Account} used for sync in this application.
     *
     * @return Handle to application's account (not guaranteed to resolve unless registerSyncAccount()
     *         has been called)
     */
    public static Account getAccount() {
        // Note: Normally the account name is set to the user's identity (username or email
        // address). However, since we aren't actually using any user accounts, it makes more sense
        // to use a generic string in this case.
        //
        // This string should *not* be localized. If the user switches locale, we would not be
        // able to locate the old account, and may erroneously register multiple accounts.
        final String accountName = ACCOUNT_NAME;
        return new Account(accountName, ACCOUNT_TYPE);
    }

    /**
     * Helper method to trigger an immediate sync ("refresh").
     *
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     *
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public static void triggerRefresh(SharedPreferences prefs) {
        Bundle b = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        b.putBoolean(SyncAdapter.FULL_SYNC, true);
        b.putBoolean(SyncAdapter.SYNC_PATIENTS, true);
        b.putBoolean(SyncAdapter.SYNC_CONCEPTS, true);
        b.putBoolean(SyncAdapter.SYNC_CHART_STRUCTURE, true);
        b.putBoolean(SyncAdapter.SYNC_LOCATIONS, true);
        b.putBoolean(SyncAdapter.SYNC_OBSERVATIONS, true);
        b.putBoolean(SyncAdapter.SYNC_USERS, true);
        // For manual update we might want to allow complete update, but for now do it
        // incrementally.

        if (prefs.getBoolean("incremental_observation_update", true)) {
            b.putBoolean(SyncAdapter.INCREMENTAL_OBSERVATIONS_UPDATE, true);
        }
        LOG.i("Requesting sync");
        ContentResolver.requestSync(
                getAccount(),      // Sync account
                Contracts.CONTENT_AUTHORITY, // Content authority
                b);                                      // Extras
    }

    /** Starts an incremental update of observations.  No-op if incremental update is disabled. */
    static void triggerIncrementalObservationSync(SharedPreferences prefs) {
        // TODO: Remove this setting and merge this function with forceIncrementalObservationSync.
        if (prefs.getBoolean("incremental_observation_update", true)) {
            forceIncrementalObservationSync();
        }
    }

    /** Starts (and forces) an incremental update of observations. */
    public static void forceIncrementalObservationSync() {
        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        b.putBoolean(SyncAdapter.SYNC_OBSERVATIONS, true);
        b.putBoolean(SyncAdapter.INCREMENTAL_OBSERVATIONS_UPDATE, true);
        ContentResolver.requestSync(getAccount(), Contracts.CONTENT_AUTHORITY, b);
    }

    /**
     * Create an entry for this application in the system account list, if it isn't already there.
     *
     * @param context Context
     */
    public static void registerSyncAccount(Context context) {
        boolean newAccount = false;
        boolean setupComplete = PreferenceManager
                .getDefaultSharedPreferences(context).getBoolean(PREF_SETUP_COMPLETE, false);

        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = getAccount();
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(account, null, null)) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(
                    account, CONTENT_AUTHORITY, getExtrasForPeriodicSync(), SYNC_FREQUENCY);
            newAccount = true;
        }

        // Schedule an initial sync if we detect problems with either our account or our local
        // data has been deleted. (Note that it's possible to clear app data WITHOUT affecting
        // the account list, so wee need to check both.)
        if (newAccount || !setupComplete) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            triggerRefresh(prefs);
            prefs.edit().putBoolean(PREF_SETUP_COMPLETE, true).commit();
        }
    }

    private static Bundle getExtrasForPeriodicSync() {
        Bundle extras = new Bundle();
        extras.putBoolean(SyncAdapter.SYNC_PATIENTS, true);
        extras.putBoolean(SyncAdapter.SYNC_CONCEPTS, true);
        extras.putBoolean(SyncAdapter.SYNC_CHART_STRUCTURE, true);
        extras.putBoolean(SyncAdapter.SYNC_LOCATIONS, true);
        extras.putBoolean(SyncAdapter.SYNC_OBSERVATIONS, true);
        extras.putBoolean(SyncAdapter.SYNC_USERS, true);

        return extras;
    }

    /**
     * Removes the periodic sync that is started when this app registers its account.
     */
    public static void removePeriodicSync() {
        ContentResolver.removePeriodicSync(
                GenericAccountService.getAccount(),
                Contracts.CONTENT_AUTHORITY,
                getExtrasForPeriodicSync());
    }

    @Override
    public void onCreate() {
        LOG.i("Service created");
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public void onDestroy() {
        LOG.i("Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }

    private static class Authenticator extends AbstractAccountAuthenticator {
        public Authenticator(Context context) {
            super(context);
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                     String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                 String s, String s2, String[] strings, Bundle bundle)
                throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                         Account account, Bundle bundle)
                throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                   Account account, String s, Bundle bundle)
                throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAuthTokenLabel(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                        Account account, String s, Bundle bundle)
                throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                  Account account, String[] strings)
                throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }
    }

}

