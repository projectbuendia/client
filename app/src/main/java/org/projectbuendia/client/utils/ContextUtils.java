package org.projectbuendia.client.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.annotation.Nonnull;

public class ContextUtils extends ContextWrapper {
    private LayoutInflater inflater = null;
    private View lastView = null;

    public static ContextUtils from(Context context) {
        return (context instanceof ContextUtils) ? (ContextUtils) context : new ContextUtils(context);
    }

    public static ContextUtils from(Provider provider) {
        return from(provider.getContext());
    }

    public static ContextUtils from(View view) {
        return from(view.getContext());
    }

    private ContextUtils(Context context) {
        super(context);
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

    /** Finds a view in the last view that was inflated. */
    public <T extends View> T findView(int id) {
        return lastView != null ? lastView.findViewById(id) : null;
    }

    /** getColor() doesn't exist for API < 23, but it's final, so this can't be named getColor(). */
    public int color(int id) {
        return getResources().getColor(id);
    }

    public interface Provider {
        Context getContext();
    }
}
