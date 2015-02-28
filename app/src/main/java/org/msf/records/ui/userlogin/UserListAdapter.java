package org.msf.records.ui.userlogin;

import org.msf.records.R;
import org.msf.records.net.model.User;
import org.msf.records.utils.Colorizer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Adapter for a grid of users.
 */
final class UserListAdapter extends ArrayAdapter<User> {

	private final Colorizer mColorizer;

	public UserListAdapter(Context context, Colorizer colorizer) {
        super(context, R.layout.grid_item_user);
        mColorizer = colorizer;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ItemViewHolder holder;
        if (view != null) {
            holder = (ItemViewHolder) view.getTag();
        } else {
            view = LayoutInflater.from(getContext())
                    .inflate(R.layout.grid_item_user, parent, false);
            holder = new ItemViewHolder(view);
            view.setTag(holder);
        }

        User user = getItem(position);
        holder.initials.setBackgroundColor(mColorizer.getColorArgb(user.id));
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