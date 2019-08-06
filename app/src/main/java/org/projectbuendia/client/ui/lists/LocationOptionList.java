package org.projectbuendia.client.ui.lists;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * An arrangement of selectable, nested buttons representing locations,
 * with the number of patients shown for each location.
 */
public class LocationOptionList {
    protected final ContextUtils c;
    protected final FlexboxLayout container;
    protected final List<LocationOption> options;
    protected final Map<String, View> itemsByUuid;
    protected final boolean highlightSelection;
    protected int selectedIndex;
    protected OnItemSelectedListener onItemSelectedListener;

    public LocationOptionList(FlexboxLayout container, boolean highlightSelection) {
        c = ContextUtils.from(container);
        this.container = container;
        this.options = new ArrayList<>();
        this.itemsByUuid = new HashMap<>();
        this.selectedIndex = -1;
        this.onItemSelectedListener = null;
        this.highlightSelection = highlightSelection;
    }

    public void setLocations(LocationForest forest, Iterable<Location> locations) {
        setOptions(getLocationOptions(forest, locations));
    }

    public void clear() {
        options.clear();
        for (View item : itemsByUuid.values()) {
            item.setOnClickListener(null);
        }
        itemsByUuid.clear();
        while (container.getChildCount() > 0) {  // ViewGroup doesn't have a child iterator
            container.removeViewAt(0);
        }
    }

    public void setOptions(List<LocationOption> newOptions) {
        clear();
        for (LocationOption option : newOptions) {
            View item = createItem(option);
            item.setTag(options.size());
            item.setOnClickListener(clickedItem -> {
                setSelectedIndex((Integer) clickedItem.getTag());
            });

            options.add(option);
            itemsByUuid.put(option.uuid, item);
        }
    }

    protected void onItemClick(View item) {
        setSelectedIndex((Integer) item.getTag());
    }

    protected View createItem(LocationOption option) {
        ViewGroup parent = findParent(option);
        View item = c.inflate(R.layout.location_list_item, parent);
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
        params.setWrapBefore(option.wrapBefore);
        item.setLayoutParams(params);
        parent.addView(item);
        parent.setVisibility(View.VISIBLE);
        return item;
    }

    protected ViewGroup findParent(LocationOption option) {
        View parentItem = itemsByUuid.get(option.parentUuid);
        if (parentItem != null) {
            return parentItem.findViewById(R.id.child_container);
        }
        return container;
    }

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= options.size()) {
            index = -1;
        }
        selectedIndex = index;

        if (highlightSelection) {
            for (int i = 0; i < container.getChildCount(); i++) {
                container.getChildAt(i).setSelected(i == selectedIndex);
            }
        }
        if (onItemSelectedListener != null) {
            onItemSelectedListener.onSelected(options.get(index));
        }
    }

    public void setSelectedUuid(String uuid) {
        for (int i = 0; i < options.size(); i++) {
            if (Objects.equals(options.get(i).uuid, uuid)) {
                setSelectedIndex(i);
                return;
            }
        }
        setSelectedIndex(-1);
    }

    public String getSelectedUuid() {
        return selectedIndex >= 0 ? options.get(selectedIndex).uuid : null;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        onItemSelectedListener = listener;
    }

    public interface OnItemSelectedListener {
        void onSelected(@Nullable LocationOption option);
    }

    private List<LocationOption> getLocationOptions(LocationForest forest, Iterable<Location> locations) {
        // First count how many colours we'll need.  Leaf nodes that are siblings
        // have the same colour; otherwise, we advance to a new colour.
        int colorIndexMax = 0;
        Location lastParent = null;
        for (Location location : locations) {
            Location parent = forest.getParent(location);
            if (parent != lastParent) colorIndexMax++;
            lastParent = parent;
        }
        if (colorIndexMax == 0) colorIndexMax = 1;  // avoid division by zero

        // Then create the option buttons with appropriate contents and colours.
        float colorIndex = 0;
        List<LocationOption> options = new ArrayList<>();
        lastParent = null;
        for (Location location : locations) {
            double size = 0.99 / (1 << (location.depth - 1));
            boolean wrapBefore = location.depth < 2;
            Location parent = forest.getParent(location);
            String parentUuid = parent != null ? parent.uuid : null;

            if (parent != lastParent) colorIndex++;
            int bg = getBgColor(colorIndex / colorIndexMax);
            int fg = getFgColor(bg);
            lastParent = parent;

            options.add(new LocationOption(
                location.uuid, parentUuid, location.name,
                forest.countPatientsIn(location), fg, bg, size, false));
        }
        return options;
    }

    private int getBgColor(float fraction) {
        float rStart = 200;
        float gStart = 200;
        float bStart = 200;
        float rEnd = 10;
        float gEnd = 92;
        float bEnd = 80;
        int r = (int) (rStart + (rEnd - rStart) * fraction);
        int g = (int) (gStart + (gEnd - gStart) * fraction);
        int b = (int) (bStart + (bEnd - bStart) * fraction);
        return (0xff << 24) + (r << 16)+ (g << 8) + b;
    }

    private int getFgColor(int bg) {
        int r = (bg >> 16) & 0xff;
        int g = (bg >> 8) & 0xff;
        int b = bg & 0xff;
        double luminance = 0.3 * r + 0.58 * g + 0.12 * b;
        return luminance > 128 ? 0xff_00_00_00 : 0xff_ff_ff_ff;
    }
}
