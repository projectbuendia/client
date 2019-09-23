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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.user.FetchUsersTaskFailedEvent;
import org.projectbuendia.client.events.user.UsersFetchedEvent;
import org.projectbuendia.client.ui.chart.ChartRenderer;
import org.projectbuendia.client.ui.login.LoginActivity;
import org.projectbuendia.client.user.FetchUsersTask;
import org.projectbuendia.client.utils.Utils;

/**
 * The starting activity for the app.  The app cannot navigate to any other
 * activities until a successful authorization taken place.
 */
public class AuthorizationActivity extends BaseActivity {
    private EditText serverField;
    private EditText usernameField;
    private EditText passwordField;
    private Button authorizeButton;

    private Subscriber subscriber = new Subscriber();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (settings.isAuthorized()) startActivity(new Intent(this, LoginActivity.class));

        getActionBar().setDisplayUseLogoEnabled(false);
        getActionBar().setIcon(R.drawable.ic_launcher);  // don't show the back arrow
        getActionBar().setDisplayHomeAsUpEnabled(false);  // don't behave like a back button

        // This is the starting activity for the app, so show the app name and version.
        setTitle(getString(R.string.app_name) + " " + getString(R.string.app_version));

        setContentView(R.layout.authorization_activity);
        serverField = findViewById(R.id.server_field);
        usernameField = findViewById(R.id.openmrs_user_field);
        passwordField = findViewById(R.id.openmrs_password_field);
        authorizeButton = findViewById(R.id.authorize_button);

        new EditTextWatcher(serverField, usernameField, passwordField).onChange(this::updateUi);
        authorizeButton.setOnClickListener(this::submit);

        populateFields();
        updateUi();
        Utils.focusFirstEmptyField(serverField, usernameField, passwordField);

        ChartRenderer.backgroundCompileTemplate();
    }

    private void populateFields() {
        serverField.setText(settings.getServer());
        usernameField.setText(settings.getOpenmrsUser());
        passwordField.setText(settings.getOpenmrsPassword());
    }

    private void updateUi() {
        String server = serverField.getText().toString().trim();
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();
        authorizeButton.setEnabled(Utils.hasChars(server)
            && Utils.hasChars(username) && Utils.hasChars(password));
    }

    private void submit(View view) {
        String server = serverField.getText().toString().trim();
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();
        new FetchUsersTask(App.getCrudEventBus(), server, username, password).execute();
    }

    protected void onResume() {
        super.onResume();
        App.getCrudEventBus().register(subscriber);
    }

    protected void onPause() {
        App.getCrudEventBus().unregister(subscriber);
        super.onPause();
    }

    class Subscriber {
        public void onEventMainThread(UsersFetchedEvent event) {
            BigToast.show(R.string.authorization_successful);
            settings.authorize(event.server, event.username, event.password);
            startActivity(new Intent(AuthorizationActivity.this, LoginActivity.class));
        }

        public void onEventMainThread(FetchUsersTaskFailedEvent event) {
            BigToast.show(R.string.authorization_failed);
            settings.deauthorize();
        }
    }
}
