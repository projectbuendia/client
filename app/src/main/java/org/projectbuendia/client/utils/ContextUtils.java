package org.projectbuendia.client.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Joiner;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.Sex;
import org.projectbuendia.client.resolvables.ResStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

public class ContextUtils extends ContextWrapper {
    private static final String EN_DASH = "\u2013";

    private final Context context;
    private LayoutInflater inflater = null;
    private View lastView = null;

    public static ContextUtils from(Context context) {
        return (context instanceof ContextUtils) ? (ContextUtils) context : new ContextUtils(context);
    }

    public static ContextUtils from(ContextProvider provider) {
        return from(provider.getContext());
    }

    public static ContextUtils from(View view) {
        return from(view.getContext());
    }

    private ContextUtils(Context context) {
        super(context);
        this.context = context;
    }

    /** Queries the ContentResolver. */
    public Cursor query(Uri uri, String[] columns, String selection, String... args) {
        return getContentResolver().query(uri, columns, selection, args, null);
    }


    // ==== Views ====

    public View reuseOrInflate(View view, int layoutId, ViewGroup parent) {
        lastView = view != null ? view : inflate(layoutId, parent);
        return lastView;
    }

    /** Always use this method, never the awful, confusing LayoutInflater.inflate(). */
    public View inflate(int layoutId, @Nonnull ViewGroup layoutParent) {
        if (inflater == null) {
            inflater = LayoutInflater.from(this);
        }
        // Provide the parent so that the proper LayoutParams are created, but
        // set attachToRoot = false so that the returned value is the inflated
        // view, not a useless reference to its parent.
        lastView = inflater.inflate(layoutId, layoutParent, false);
        return lastView;
    }

    /** Constructs an AlertDialog.Builder inflated from a given layout. */
    public AlertDialog.Builder buildDialog(int layoutId) {
        // This is the only situation where it's okay to pass null for the parent.
        return new AlertDialog.Builder(this).setView(inflate(layoutId, null));
    }

    /** The parent can be null only when inflating a view for a dialog. */
    public View inflateForDialog(int id) {
        return inflate(id, null);
    }

    public void setContainer(View view) {
        lastView = view;
    }

    /** Sets the view in which findView() will search. */
    public ContextUtils inView(View view) {
        lastView = view;
        return this;
    }

    /** Finds a view in the last view that was inflated. */
    public <T extends View> T findView(int id) {
        return lastView != null ? lastView.findViewById(id) : null;
    }

    public void show(int id, boolean visible) {
        findView(id).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void show(int id) {
        findView(id).setVisibility(View.VISIBLE);
    }

    public void hide(int id) {
        findView(id).setVisibility(View.GONE);
    }

    public void setText(int id, CharSequence text) {
        ((TextView) findView(id)).setText(text);
    }

    public void setTextViewColors(int id, ResStatus.Resolved status) {
        ((TextView) findView(id)).setTextColor(status.getForegroundColor());
        ((TextView) findView(id)).setBackgroundColor(status.getBackgroundColor());
    }

    // ==== String formatting ====

    /** Strings are always available in the app-wide resources. */
    public String str(int id, Object... args) {
        return App.str(id, args);
    }

    /** Formats a list of items in a localized fashion. */
    public String formatItems(String... items) {
        int n = items.length;
        return n == 0 ? ""
            : n == 1 ? items[0]
            : n == 2 ? str(R.string.two_items, items[0], items[1])
            : str(
                R.string.more_than_two_items,
                Joiner.on(", ").join(Arrays.copyOfRange(items, 0, n - 1)),
                items[n - 1]
            );
    }

    /** Formats a location heading with an optional patient count. */
    public String formatLocationHeading(String locationUuid, long patientCount) {
        LocationForest forest = App.getModel().getForest();
        Location location = forest.get(locationUuid);
        String locationName = location != null ? location.name : str(R.string.unknown_location);
        // If no patient count is available, only show the location name.
        if (patientCount < 0) return locationName;
        return locationName + "  \u00b7  " + formatPatientCount(patientCount);
    }

    /** Formats a localized patient count. */
    public String formatPatientCount(long count) {
        return count == 0 ? str(R.string.no_patients)
            : count == 1 ? str(R.string.one_patient)
            : str(R.string.n_patients, count);
    }

    /** Formats a patient name, using an en-dash if either part is missing. */
    public String formatPatientName(Patient patient) {
        String given = Utils.orDefault(patient.givenName, EN_DASH);
        String family = Utils.orDefault(patient.familyName, EN_DASH);
        return given + " " + family;
    }

    public enum FormatStyle { NONE, SHORT, LONG };

    /** Formats the sex, pregnancy status, and/or age of a patient. */
    public String formatPatientDetails(
        Patient patient, FormatStyle sex, FormatStyle pregnancy, FormatStyle age) {
        List<String> labels = new ArrayList<>();
        if (patient.sex != null && (sex == FormatStyle.SHORT || sex == FormatStyle.LONG)) {
            String abbrev = Sex.getAbbreviation(patient.sex);
            labels.add(Utils.isChild(patient.birthdate) ? abbrev.toLowerCase() : abbrev);
        }
        if (patient.sex == null && sex == FormatStyle.LONG) {
            labels.add(str(R.string.sex_unknown));
        }
        if (patient.pregnancy) {
            if (pregnancy == FormatStyle.SHORT) labels.add(str(R.string.pregnant_abbreviation));
            if (pregnancy == FormatStyle.LONG) labels.add(str(R.string.pregnant).toLowerCase());
        }
        if (patient.birthdate != null && (age == FormatStyle.SHORT || age == FormatStyle.LONG)) {
            labels.add(Utils.birthdateToAge(patient.birthdate));
        }
        if (patient.birthdate == null && (age == FormatStyle.LONG)) {
            labels.add(str(R.string.age_unknown));
        }
        return Joiner.on(", ").join(labels);
    }


    // ==== User interface ====

    public void prompt(int titleId, int messageId, int actionId, Runnable action) {
        new AlertDialog.Builder(this)
            .setTitle(titleId)
            .setMessage(messageId)
            .setPositiveButton(actionId, (d, i) -> action.run())
            .setNegativeButton(R.string.cancel, null)
            .create()
            .show();
    }

    public Dialog showProgressDialog(String title, String message) {
        ProgressDialog d = new ProgressDialog(this);
        d.setIcon(android.R.drawable.ic_dialog_info);
        d.setTitle(title);
        d.setMessage(message);
        d.setIndeterminate(true);
        d.setCancelable(false);
        d.show();
        return d;
    }

    public interface ContextProvider {
        Context getContext();
    }
}
