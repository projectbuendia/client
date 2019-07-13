package org.projectbuendia.client.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;

public class AutocompleteAdapter extends ArrayAdapter<AutocompleteAdapter.Completion> {
    private final LayoutInflater inflater;
    private final int resourceId;
    private final Filter filter;

    public AutocompleteAdapter(Context context, int resourceId, final Completer completer) {
        super(context, resourceId, 0, new ArrayList<Completion>());
        this.inflater = LayoutInflater.from(context);
        this.resourceId = resourceId;
        this.filter = new Filter() {
            @Override protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint != null) {
                    Collection<? extends Completion> completions = completer.suggestCompletions(constraint);
                    results.values = completions;
                    results.count = completions.size();
                }
                return results;
            }

            @Override public CharSequence convertResultToString(Object result) {
                return (result instanceof Completion) ? ((Completion) result).getValue() : "";
            }

            @Override protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    clear();
                    addAll((Collection<? extends Completion>) results.values);
                }
            }
        };
    }

    @Override public @NonNull View getView(int position, View reusableView, @NonNull ViewGroup parent) {
        View view = reusableView;
        if (view == null) {
            view = inflater.inflate(resourceId, parent, false);
        }
        if (view instanceof TextView) {
            Completion completion = getItem(position);
            ((TextView) view).setText(completion != null ? completion.getLabel() : "");
        }
        return view;
    }

    @Override public long getItemId(int position) {
        return position;
    }

    @Override public @NonNull Filter getFilter() {
        return filter;
    }

    public interface Completer {
        Collection<? extends Completion> suggestCompletions(CharSequence constraint);
    }

    public interface Completion {
        @NonNull String getLabel();
        @NonNull String getValue();
    }
}
