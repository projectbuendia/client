package org.projectbuendia.client.ui.lists;

import android.view.View;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

import org.projectbuendia.client.R;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

public class LocationOptionList {
    protected final ContextUtils c;
    protected final FlexboxLayout container;
    protected final List<LocationOption> options;
    protected int selectedIndex;
    protected OnItemSelectedListener onItemSelectedListener;

    public LocationOptionList(FlexboxLayout container) {
        c = ContextUtils.from(container);
        this.container = container;
        this.options = new ArrayList<>();
        this.selectedIndex = -1;
        this.onItemSelectedListener = null;
    }

    public void setOptions(List<LocationOption> newOptions) {
        options.clear();
        while (container.getChildCount() > 0) {  // ViewGroup doesn't have a child iterator
            container.getChildAt(0).setOnClickListener(null);
            container.removeViewAt(0);
        }
        for (LocationOption option : newOptions) {
            options.add(option);
            container.addView(createItem(option));
        }
    }

    protected View createItem(LocationOption option) {
        View item = c.inflate(R.layout.location_list_item, container);
        View button = item.findViewById(R.id.button);
        TextView title = item.findViewById(R.id.title);
        TextView content = item.findViewById(R.id.content);

        title.setText(option.name);
        content.setText("" + option.numPatients);

        title.setTextColor(option.fgColor);
        content.setTextColor(Utils.colorWithOpacity(option.fgColor, option.numPatients > 0 ? 1 : 0.25));
        button.setBackgroundColor(option.bgColor);

        FlexboxLayout.LayoutParams params = (FlexboxLayout.LayoutParams) item.getLayoutParams();
        // The method refers to "percent", but it takes a value between 0 and 1, not 0 and 100!
        params.setFlexBasisPercent((float) option.relWidth);
        item.setLayoutParams(params);

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
            container.getChildAt(i).setSelected(i == selectedIndex);
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

    public interface OnItemSelectedListener {
        void onSelected(@Nullable LocationOption option);
    }
}
