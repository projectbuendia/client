package org.projectbuendia.client.ui.lists;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Location;

/** Data model for a button shown as an option in a LocationOptionList. */
public class LocationOption {
    public final String locationUuid;
    public final String name;
    public final long numPatients;
    public final int fgColor;
    public final int bgColor;

    public static final int DEFAULT_FG = App.getInstance().getResources().getColor(R.color.vital_fg_light);
    public static final int DEFAULT_BG = App.getInstance().getResources().getColor(R.color.zone_confirmed);

    public LocationOption(Location location, long numPatients) {
        this(location.uuid, location.name, numPatients, DEFAULT_FG, DEFAULT_BG);
    }

    public LocationOption(
        String locationUuid, String name, long numPatients, int fgColor, int bgColor) {
        this.locationUuid = locationUuid;
        this.name = name;
        this.numPatients = numPatients;
        this.fgColor = fgColor;
        this.bgColor = bgColor;
    }
}
