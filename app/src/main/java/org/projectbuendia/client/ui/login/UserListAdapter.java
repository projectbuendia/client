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

package org.projectbuendia.client.ui.login;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.utils.Colorizer;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** {@link ArrayAdapter} for a grid of users. */
final class UserListAdapter extends ArrayAdapter<JsonUser> {

    private final Colorizer mColorizer;

    public UserListAdapter(Context context, Colorizer colorizer) {
        super(context, R.layout.login_grid_user_item);
        mColorizer = colorizer;
    }

    @Override public View getView(int position, View view, ViewGroup parent) {
        ItemViewHolder holder;
        if (view != null) {
            holder = (ItemViewHolder) view.getTag();
        } else {
            view = LayoutInflater.from(getContext())
                .inflate(R.layout.login_grid_user_item, parent, false);
            holder = new ItemViewHolder(view);
            view.setTag(holder);
        }

        JsonUser user = getItem(position);
        holder.initials.setBackgroundColor(mColorizer.getColorArgb(user.fullName.trim()));
        holder.initials.setText(user.getInitials());
        holder.fullName.setText(user.fullName);

        return view;
    }

    static final class ItemViewHolder {
        @InjectView(R.id.user_initials) public TextView initials;
        @InjectView(R.id.user_name) public TextView fullName;

        public ItemViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
