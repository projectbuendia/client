package org.projectbuendia.client.ui.lists;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

import org.projectbuendia.client.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

public class LocationOptionList {
    protected final Context context;
    protected final LayoutInflater inflater;
    protected final FlexboxLayout container;
    protected final List<LocationOption> options;
    protected int selectedIndex;
    protected OnItemSelectedListener onItemSelectedListener;

    private static final int DEFAULT_FG_COLOR_ID = R.color.vital_fg_light;
    private static final int DEFAULT_BG_COLOR_ID = R.color.zone_confirmed;

    public LocationOptionList(Context context, FlexboxLayout container) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.container = container;
        this.options = new ArrayList<>();
        this.selectedIndex = -1;
        this.onItemSelectedListener = null;
    }

    public void setOptions(List<LocationOption> newOptions) {
        options.clear();
        int i = 0;
        for (LocationOption option : newOptions) {
            options.add(option);
            container.addView(createItem(option), i);
            i++;
        }
        while (i < container.getChildCount()) {
            container.getChildAt(i).setOnClickListener(null);
            container.removeViewAt(i);
        }
    }

    protected View createItem(LocationOption option) {
        View item = inflater.inflate(R.layout.location_list_item, null);
        View box = item.findViewById(R.id.box);
        TextView title = item.findViewById(R.id.title);
        TextView content = item.findViewById(R.id.content);

        title.setText(option.name);
        content.setText("" + option.numPatients);

        int titleColor = getColor(DEFAULT_FG_COLOR_ID);
        int contentColor = option.numPatients > 0 ?
            titleColor : (0x40_00_00_00 | titleColor & 0x00_ff_ff_ff);
        int bgColor = getColor(DEFAULT_BG_COLOR_ID);

        title.setTextColor(titleColor);
        content.setTextColor(contentColor);
        box.setBackgroundColor(bgColor);
        item.setBackgroundResource(R.drawable.border_grey_1dp);

        item.setOnClickListener(clickedItem -> {
            int index = container.indexOfChild(clickedItem);
            if (index >= 0) setSelectedIndex(index);
        });
        return item;
    }

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= options.size()) {
            index = -1;
        }
        selectedIndex = index;

        for (int i = 0; i < container.getChildCount(); i++) {
            container.getChildAt(i).setBackgroundResource(
                (i == selectedIndex) ? R.color.zone_location_selected_padding
                    : R.drawable.location_selector);
        }
        if (onItemSelectedListener != null) {
            onItemSelectedListener.onSelected(options.get(index));
        }
    }

    public void setSelectedUuid(String uuid) {
        for (int i = 0; i < options.size(); i++) {
            if (Objects.equals(options.get(i).locationUuid, uuid)) {
                setSelectedIndex(i);
                return;
            }
        }
        setSelectedIndex(-1);
    }

    private int getColor(int id) {
        return context.getResources().getColor(id);
    }

    public interface OnItemSelectedListener {
        void onSelected(@Nullable LocationOption option);
    }
}
