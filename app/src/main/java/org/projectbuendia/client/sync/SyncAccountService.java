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

package org.projectbuendia.client.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.BuildConfig;
import org.projectbuendia.client.utils.Logger;

import javax.inject.Inject;

/**
 * A {@link Service} that manages the app's sync account, including static
 * functions for account registration and sync requests. For a detailed
 * description of the app's sync architecture, see:
 * https://github.com/projectbuendia/buendia/wiki/Client-Sync
 */
public class SyncAccountService extends Service {

    public static final String ACCOUNT_NAME = "sync";
    private static final Logger LOG = Logger.create();
    @Inject static AppSettings sSettings;
    @Inject static SyncManager sSyncManager;

    private Authenticator mAuthenticator;

    /** Sets up the sync account for this app. */
    public static void initialize(Context context) {
        if (createAccount(context) || !sSettings.getSyncAccountInitialized()) {
            sSyncManager.startFullSync();
            sSettings.setSyncAccountInitialized(true);
        }
    }

    /**
     * Creates the sync account for this app if it doesn't already exist.
     * @return true if a new account was created
     */
    private static boolean createAccount(Context context) {
        Account account = getAccount();
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(account, null, null)) {
            sSyncManager.initPeriodicSyncs();
            return true;
        }
        return false;
    }

    /** Gets the app's sync account (call initialize() before using this). */
    public static Account getAccount() {
        return new Account(ACCOUNT_NAME, BuildConfig.ACCOUNT_TYPE);
    }

    @Override public void onCreate() {
        LOG.i("Service created");
        mAuthenticator = new Authenticator(this);
    }

    @Override public void onDestroy() {
        LOG.i("Service destroyed");
    }

    @Override public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }

    /** A dummy authenticator. */
    private static class Authenticator extends AbstractAccountAuthenticator {
        public Authenticator(Context context) {
            super(context);
        }

        public Bundle addAccount(
            AccountAuthenticatorResponse r, String s1, String s2, String[] ss, Bundle b) {
            return null;
        }

        public Bundle confirmCredentials(AccountAuthenticatorResponse r, Account a, Bundle b) {
            return null;
        }

        public Bundle editProperties(AccountAuthenticatorResponse r, String s) {
            throw new UnsupportedOperationException();
        }

        public Bundle getAuthToken(AccountAuthenticatorResponse r, Account a, String s, Bundle b) {
            throw new UnsupportedOperationException();
        }

        public String getAuthTokenLabel(String s) {
            throw new UnsupportedOperationException();
        }

        public Bundle updateCredentials(
            AccountAuthenticatorResponse r, Account a, String s, Bundle b) {
            throw new UnsupportedOperationException();
        }

        public Bundle hasFeatures(AccountAuthenticatorResponse r, Account a, String[] ss) {
            throw new UnsupportedOperationException();
        }
    }
}
