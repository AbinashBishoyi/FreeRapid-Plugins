package cz.vity.freerapid.plugins.services.pbskids;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
public enum VideoQuality {
    _400(400),
    _800(800),
    _1200(1200),
    _2500(2500);

    private final int quality;

    private VideoQuality(int quality) {
        this.quality = quality;
    }

    public int getQuality() {
        return quality;
    }

    @Override
    public String toString() {
        return quality + " kbps";
    }

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}