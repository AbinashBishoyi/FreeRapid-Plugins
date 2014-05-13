package cz.vity.freerapid.plugins.services.streamcz;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
public enum VideoQuality {
    _240(240),
    _360(360),
    _480(480),
    _720(720);

    private final int quality;
    private final String name;

    private VideoQuality(int quality) {
        this.quality = quality;
        this.name = quality + "p";
    }

    public int getQuality() {
        return quality;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}
