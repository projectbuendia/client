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
import android.widget.Toast;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.net.model.NewUser;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * A {@link android.support.v4.app.DialogFragment} for adding a new user.
 */
public class AddNewUserDialogFragment extends DialogFragment {
    public static AddNewUserDialogFragment newInstance() {
        return new AddNewUserDialogFragment();
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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.dialog_fragment_add_new_user, null);
        ButterKnife.inject(this, fragment);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
                .setCancelable(false) // Disable auto-cancel.
                .setTitle(getResources().getString(R.string.title_add_new_user))
                .setPositiveButton(getResources().getString(R.string.ok), null)
                .setNegativeButton(getResources().getString(R.string.cancel), null)
                .setView(fragment);

        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(
                                new View.OnClickListener() {

                                    @Override
                                    public void onClick(View view) {
                                        // Validate the user.
                                        if (mUsername.getText() == null
                                                || mUsername.getText().toString().equals("")) {
                                            Toast.makeText(
                                                    getActivity(),
                                                    "Username must not be null",
                                                    Toast.LENGTH_LONG).show();
                                            mUsername.invalidate();
                                            return;
                                        }
                                        if (mGivenName.getText() == null
                                                || mGivenName.getText().toString().equals("")) {
                                            Toast.makeText(
                                                    getActivity(),
                                                    "Given name must not be null",
                                                    Toast.LENGTH_LONG).show();
                                            mGivenName.invalidate();
                                            return;
                                        }
                                        if (mFamilyName.getText() == null
                                                || mFamilyName.getText().toString().equals("")) {
                                            Toast.makeText(
                                                    getActivity(),
                                                    "Family name must not be null",
                                                    Toast.LENGTH_LONG).show();
                                            mFamilyName.invalidate();
                                            return;
                                        }

                                        App.getUserManager().addUser(NewUser.create(
                                                mUsername.getText().toString(),
                                                mGivenName.getText().toString(),
                                                mFamilyName.getText().toString()
                                        ));
                                        dialog.dismiss();
                                    }
                                });
            }
        });

        return dialog;
    }
}
