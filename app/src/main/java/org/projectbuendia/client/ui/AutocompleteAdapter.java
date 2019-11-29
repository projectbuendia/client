package org.projectbuendia.client.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;

public class AutocompleteAdapter<T> extends ArrayAdapter<T> {
    private final LayoutInflater inflater;
    private final int resourceId;
    private final Filter filter;
    private final CompletionAdapter<T> adapter;

    public AutocompleteAdapter(Context context, int resourceId, CompletionAdapter<T> adapter) {
        super(context, resourceId, 0, new ArrayList<>());
        this.inflater = LayoutInflater.from(context);
        this.resourceId = resourceId;
        this.adapter = adapter;
        this.filter = new Filter() {
            @Override protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint != null) {
                    List<T> completions = adapter.suggestCompletions(constraint);
                    results.values = completions;
                    results.count = completions.size();
                }
                return results;
            }

            @Override public CharSequence convertResultToString(Object result) {
                return adapter.getCompletedText((T) result);
            }

            @Override protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    clear();
                    addAll((List<T>) results.values);
                }
            }
        };
    }

    @Override public @NonNull View getView(int position, View reusableView, @NonNull ViewGroup parent) {
        View view = reusableView;
        if (view == null) {
            view = inflater.inflate(resourceId, parent, false);
        }
        adapter.showInView(view, getItem(position));
        return view;
    }

    @Override public long getItemId(int position) {
        return position;
    }

    @Override public @NonNull Filter getFilter() {
        return filter;
    }

    public interface CompletionAdapter<T> {
        List<T> suggestCompletions(CharSequence constraint);
        void showInView(View itemView, T item);
        @NonNull String getCompletedText(T item);
    }
}
