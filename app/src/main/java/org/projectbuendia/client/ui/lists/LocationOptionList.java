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

import javax.annotation.Nullable;

/**
 * An arrangement of selectable, nested buttons representing locations,
 * with the number of patients shown for each location.
 */
public class LocationOptionList {
    protected final ContextUtils c;
    protected final FlexboxLayout container;
    protected final List<Location> locations;
    protected final Map<String, View> itemsByUuid;
    protected final boolean highlightSelection;
    protected Location selectedLocation;
    protected OnItemSelectedListener onLocationSelectedListener;

    public LocationOptionList(FlexboxLayout container, boolean highlightSelection) {
        c = ContextUtils.from(container);
        this.container = container;
        this.locations = new ArrayList<>();
        this.itemsByUuid = new HashMap<>();
        this.selectedLocation = null;
        this.onLocationSelectedListener = null;
        this.highlightSelection = highlightSelection;
    }

    public void setLocations(LocationForest forest, Iterable<Location> locations) {
        clear();

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
        lastParent = null;
        for (Location location : locations) {
            double size = forest.isLeaf(location) ? 0.49 : 1;
            Location parent = forest.getParent(location);
            String parentUuid = parent != null ? parent.uuid : null;

            if (parent != lastParent) colorIndex++;
            int bgColor = getBgColor(colorIndex / colorIndexMax);
            int fgColor = getFgColor(bgColor);
            lastParent = parent;

            int numPatients = forest.countPatientsIn(location);
            View item = createItem(
                location, location.name, numPatients, parentUuid, fgColor, bgColor, size);

            item.setTag(location);
            item.setOnClickListener(clickedItem -> {
                setSelectedLocation((Location) clickedItem.getTag());
            });

            this.locations.add(location);
            itemsByUuid.put(location.uuid, item);
        }
    }

    public void clear() {
        locations.clear();
        for (View item : itemsByUuid.values()) {
            item.setOnClickListener(null);
        }
        itemsByUuid.clear();
        while (container.getChildCount() > 0) {  // ViewGroup doesn't have a child iterator
            container.removeViewAt(0);
        }
    }

    protected View createItem(
        Location location, String label, int numPatients, String parentUuid,
        int fgColor, int bgColor, double relativeWidth) {
        ViewGroup parent = findContainer(parentUuid);
        View item = c.inflate(R.layout.location_list_item, parent);
        View button = item.findViewById(R.id.button);
        TextView title = item.findViewById(R.id.title);
        TextView content = item.findViewById(R.id.content);

        title.setText(label);
        content.setText("" + numPatients);

        title.setTextColor(fgColor);
        content.setTextColor(Utils.colorWithOpacity(fgColor, numPatients > 0 ? 1 : 0.25));
        button.setBackgroundColor(bgColor);

        FlexboxLayout.LayoutParams params = (FlexboxLayout.LayoutParams) item.getLayoutParams();
        // The method refers to "percent", but it takes a value between 0 and 1, not 0 and 100!
        params.setFlexBasisPercent((float) relativeWidth);
        item.setLayoutParams(params);
        parent.addView(item);
        parent.setVisibility(View.VISIBLE);
        return item;
    }

    private ViewGroup findContainer(String uuid) {
        View item = itemsByUuid.get(uuid);
        if (item != null) {
            return item.findViewById(R.id.child_container);
        }
        return container;
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

    public void setSelectedLocation(Location selectedLocation) {
        this.selectedLocation = selectedLocation;

        if (highlightSelection) {
            for (Location location : locations) {
                itemsByUuid.get(location.uuid).setSelected(location.equals(selectedLocation));
            }
        }
        if (onLocationSelectedListener != null) {
            onLocationSelectedListener.onSelected(selectedLocation);
        }
    }

    public Location getSelectedLocation() {
        return selectedLocation;
    }

    public void setOnLocationSelectedListener(OnItemSelectedListener listener) {
        onLocationSelectedListener = listener;
    }

    public interface OnItemSelectedListener {
        void onSelected(@Nullable Location option);
    }
}
