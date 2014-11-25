package org.msf.records.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.events.PatientLocationEditedEvent;
import org.msf.records.events.user.UserAddedEvent;
import org.msf.records.model.Location2;
import org.msf.records.model.NewUser;
import org.msf.records.model.User;
import org.msf.records.view.InstantAutoCompleteTextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/**
 * A {@link android.support.v4.app.DialogFragment} for adding a new user.
 */
public class AddNewUserDialogFragment extends DialogFragment {
    public static AddNewUserDialogFragment newInstance() {
        AddNewUserDialogFragment fragment = new AddNewUserDialogFragment();
        return fragment;
    }

    @InjectView(R.id.add_user_username_tv) EditText mUsername;
    @InjectView(R.id.add_user_given_name_tv) EditText mGivenName;
    @InjectView(R.id.add_user_family_name_tv) EditText mFamilyName;

    private LayoutInflater mInflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInflater = LayoutInflater.from(getActivity());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.dialog_fragment_add_new_user, null);
        ButterKnife.inject(this, fragment);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.title_add_new_user))
                .setPositiveButton(
                        getResources().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                App.getUserManager().addUser(NewUser.create(
                                        mUsername.getText().toString(),
                                        mGivenName.getText().toString(),
                                        mFamilyName.getText().toString()
                                ));
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.cancel), null)
                .setView(fragment);

        return dialogBuilder.create();
    }
}
