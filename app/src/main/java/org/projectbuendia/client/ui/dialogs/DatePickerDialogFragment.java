package org.projectbuendia.client.ui.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;

/**
 * A {@link DialogFragment} that displays a {@link DatePickerDialog}. Basic usage:
 * <ul>
 *     <li>call {@link #create(Date)} with a starting date.
 *     <li>call {@link #setListener(DateChosenListener)} to set a listener that will be triggered
 *         when a value has been set.
 * </ul>
 */
public class DatePickerDialogFragment extends DialogFragment {
    public interface DateChosenListener {
        void onDateChosen(Date date);
    }

    private static final String YEAR_KEY = "year";
    private static final String MONTH_KEY = "month";
    private static final String DAY_KEY = "day";

    @Nullable
    private DateChosenListener mListener;

    public static DatePickerDialogFragment create(@Nullable Date startingDate) {
        if (startingDate == null) {
            startingDate = new Date();
        }
        Bundle args = new Bundle();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startingDate);
        args.putInt(YEAR_KEY, calendar.get(Calendar.YEAR));
        args.putInt(MONTH_KEY, calendar.get(Calendar.MONTH));
        args.putInt(DAY_KEY, calendar.get(Calendar.DAY_OF_MONTH));
        DatePickerDialogFragment fragment = new DatePickerDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(@Nullable DateChosenListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int year = getArguments().getInt(YEAR_KEY);
        int month = getArguments().getInt(MONTH_KEY);
        int day = getArguments().getInt(DAY_KEY);
        return new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, monthOfYear, dayOfMonth);
                Date date = calendar.getTime();
                if (mListener != null) {
                    mListener.onDateChosen(date);
                }
            }
        }, year, month, day);
    }
}
