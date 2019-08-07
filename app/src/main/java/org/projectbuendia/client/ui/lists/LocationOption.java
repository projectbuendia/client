package org.projectbuendia.client.ui.lists;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;

/** Data model for a button shown as an option in a LocationOptionList. */
public class LocationOption {
    public final String uuid;
    public final String name;
    public final int numPatients;
    public final int fgColor;
    public final int bgColor;
    public final double relWidth;
    public final boolean wrapBefore;

    public static final int DEFAULT_FG = App.getInstance().getResources().getColor(R.color.vital_fg_light);
    public static final int DEFAULT_BG = App.getInstance().getResources().getColor(R.color.zone_confirmed);

    public LocationOption(
        String uuid, String name, long numPatients, int fgColor, int bgColor, double relWidth, boolean wrapBefore) {
        this.uuid = uuid;
        this.name = name;
        this.numPatients = (int) numPatients;
        this.fgColor = fgColor;
        this.bgColor = bgColor;
        this.relWidth = relWidth;
        this.wrapBefore = wrapBefore;
    }
}
