package org.projectbuendia.client.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.resolvables.ResStatus;

import javax.annotation.Nonnull;

public class ContextUtils extends ContextWrapper {
    public final Context context;
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


    // ==== Views

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

    public void setText(int id, String text) {
        ((TextView) findView(id)).setText(text);
    }

    public void setTextViewColors(int id, ResStatus.Resolved status) {
        ((TextView) findView(id)).setTextColor(status.getForegroundColor());
        ((TextView) findView(id)).setBackgroundColor(status.getBackgroundColor());
    }

    // ==== String formatting

    /** Strings are always available in the app-wide resources. */
    public String str(int id, Object... args) {
        return App.str(id, args);
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

    public interface ContextProvider {
        Context getContext();
    }
}
