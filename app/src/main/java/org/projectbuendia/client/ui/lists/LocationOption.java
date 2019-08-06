package org.projectbuendia.client.ui.lists;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Location;

/** Data model for a button shown as an option in a LocationOptionList. */
public class LocationOption {
    public final String locationUuid;
    public final String name;
    public final int numPatients;
    public final int fgColor;
    public final int bgColor;
    public final double relWidth;

    public static final int DEFAULT_FG = App.getInstance().getResources().getColor(R.color.vital_fg_light);
    public static final int DEFAULT_BG = App.getInstance().getResources().getColor(R.color.zone_confirmed);

    public LocationOption(Location location, long numPatients) {
        this(location.uuid, location.name, numPatients, DEFAULT_FG, DEFAULT_BG, 1);
    }

    public LocationOption(
        String locationUuid, String name, long numPatients, int fgColor, int bgColor, double relWidth) {
        this.locationUuid = locationUuid;
        this.name = name;
        this.numPatients = (int) numPatients;
        this.fgColor = fgColor;
        this.bgColor = bgColor;
        this.relWidth = relWidth;
    }
}
