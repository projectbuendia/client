// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.models;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/** A {@link BaseAdapter} backed by a {@link TypedCursor}. */
public abstract class TypedCursorAdapter<T extends Model> extends BaseAdapter {

    private final Context mContext;
    private TypedCursor<T> mTypedCursor;

    public TypedCursorAdapter(Context context, TypedCursor<T> typedCursor) {
        mContext = context;
        mTypedCursor = typedCursor;
    }

    @Override public int getCount() {
        return mTypedCursor != null ? mTypedCursor.getCount() : 0;
    }

    @Override public long getItemId(int position) {
        if (mTypedCursor == null) {
            return 0;
        }
        T item = mTypedCursor.get(position);
        return item != null ? item.hashCode() : 0;
    }

    @Override public boolean hasStableIds() {
        return true;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        if (mTypedCursor == null) {
            throw new IllegalStateException(
                "Cannot get a view when no backing lazy array has been set.");
        }

        View view =
            convertView == null
                ? newView(mContext, mTypedCursor.get(position), parent)
                : convertView;
        bindView(mContext, view, getItem(position));

        return view;
    }

    protected abstract View newView(Context context, T item, ViewGroup parent);

    protected abstract void bindView(Context context, View view, T item);

    // TODO: Provide a mechanism to filter, similar to Cursor.

    @Override public T getItem(int position) {
        return mTypedCursor == null ? null : mTypedCursor.get(position);
    }

    /**
     * Changes the attached {@link TypedCursor} to the specified value, closing the currently
     * attached {@link TypedCursor} if it exists.
     */
    public void changeTypedCursor(TypedCursor<T> newTypedCursor) {
        if (mTypedCursor == newTypedCursor) return;

        if (mTypedCursor != null) {
            mTypedCursor.close();
        }

        mTypedCursor = newTypedCursor;
        if (mTypedCursor == null) {
            notifyDataSetInvalidated();
        } else {
            notifyDataSetChanged();
        }
    }
}
