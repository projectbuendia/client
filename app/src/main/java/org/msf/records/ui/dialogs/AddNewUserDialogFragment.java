package org.msf.records.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.net.model.NewUser;

import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * A {@link android.support.v4.app.DialogFragment} for adding a new user.
 */
public class AddNewUserDialogFragment extends DialogFragment {
    public static AddNewUserDialogFragment newInstance(Ui ui) {
        AddNewUserDialogFragment fragment = new AddNewUserDialogFragment();
        fragment.setUi(ui);
        return fragment;
    }

    @InjectView(R.id.add_user_given_name_tv) EditText mGivenName;
    @InjectView(R.id.add_user_family_name_tv) EditText mFamilyName;

    private LayoutInflater mInflater;
    // Optional UI for exposing a spinner.
    @Nullable private Ui mUi;

    public interface Ui {

        void showSpinner(boolean show);
    }

    public void setUi(Ui ui) {
        mUi = ui;
    }

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
                                        if (isNullOrWhitespace(mGivenName)) {
                                            setError(
                                                    mGivenName,
                                                    R.string.given_name_cannot_be_null);
                                            return;
                                        }
                                        if (isNullOrWhitespace(mFamilyName)) {
                                            setError(
                                                    mFamilyName,
                                                    R.string.family_name_cannot_be_null);
                                            return;
                                        }

                                        App.getUserManager().addUser(new NewUser(
                                                mGivenName.getText().toString().trim(),
                                                mFamilyName.getText().toString().trim()
                                        ));
                                        if (mUi != null) {
                                            mUi.showSpinner(true);
                                        }
                                        dialog.dismiss();
                                    }
                                });
            }
        });

        return dialog;
    }

    private boolean isNullOrWhitespace(EditText field) {
        return field.getText() == null || field.getText().toString().trim().isEmpty();
    }

    private void setError(EditText field, int resourceId) {
        field.setError(getResources().getString(resourceId));
        field.invalidate();
        field.requestFocus();
    }
}
