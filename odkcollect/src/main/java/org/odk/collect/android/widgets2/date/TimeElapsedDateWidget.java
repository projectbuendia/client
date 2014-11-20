package org.odk.collect.android.widgets2.date;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.javarosa.core.model.data.DateData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.DateTime;
import org.odk.collect.android.R;
import org.odk.collect.android.widgets2.Appearance;
import org.odk.collect.android.widgets2.TypedWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DateData} {@link TypedWidget} that allows you to specify the amount of time elapsed to
 * determine a date.
 *
 * <p>For example, if you want to specify an age, you can specify the time elapsed in years, which
 * will then be subtracted from the current time to determine the (approximate) date of birth.
 */
public class TimeElapsedDateWidget extends TypedWidget<DateData> {

    private static final String YEARS_TAG = "years";
    private static final String MONTHS_TAG = "months";
    private static final String DAYS_TAG = "days";

    private final LinearLayout mLayout;
    private final EditText mValue;
    private final RadioGroup mUnits;
    private final RadioButton mYears;
    private final RadioButton mMonths;
    private final RadioButton mDays;
    private final List<RadioButton> mVisibleUnits;

    private final DateTime mStartDateTime;

    public TimeElapsedDateWidget(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        // TODO(dxchen): Handle initial values.

        super(context, prompt, appearance, forceReadOnly);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        mLayout = (LinearLayout) inflater.inflate(R.layout.template_time_elapsed_date_widget, null);
        mValue = (EditText) mLayout.findViewById(R.id.value);
        mUnits = (RadioGroup) mLayout.findViewById(R.id.units);
        mYears = (RadioButton) mLayout.findViewById(R.id.years);
        mMonths = (RadioButton) mLayout.findViewById(R.id.months);
        mDays = (RadioButton) mLayout.findViewById(R.id.days);
        mVisibleUnits = new ArrayList<RadioButton>();

        boolean showYears = appearance.hasQualifier("show_years");
        boolean showMonths = appearance.hasQualifier("show_months");
        boolean showDays = appearance.hasQualifier("show_days");
        boolean showAll = !(showYears || showMonths || showYears);

        if (showAll || showYears) {
            mYears.setVisibility(View.VISIBLE);
            mVisibleUnits.add(mYears);
        }
        if (showAll || showMonths) {
            mMonths.setVisibility(View.VISIBLE);
            mVisibleUnits.add(mMonths);
        }
        if (showAll || showDays) {
            mDays.setVisibility(View.VISIBLE);
            mVisibleUnits.add(mDays);
        }

        mVisibleUnits.get(0).setChecked(true);

        addView(mLayout);

        mStartDateTime = DateTime.now();
    }

    @Override
    public DateData getAnswer() {
        String valueString = mValue.getText().toString();
        if ("".equals(valueString)) {
            return null;
        }
        int value = Integer.valueOf(valueString);

        RadioButton checkedButton =
                (RadioButton) mUnits.findViewById(mUnits.getCheckedRadioButtonId());

        if (YEARS_TAG.equals(checkedButton.getTag())) {
            return new DateData(mStartDateTime.minusYears(value).toDate());
        } else if (MONTHS_TAG.equals(checkedButton.getTag())) {
            return new DateData(mStartDateTime.minusMonths(value).toDate());
        } else {
            return new DateData(mStartDateTime.minusDays(value).toDate());
        }
    }

    @Override
    public void clearAnswer() {
        mValue.setText("");
        mVisibleUnits.get(0).setChecked(true);
    }

    @Override
    public void setFocus(Context context) {}

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {}
}
