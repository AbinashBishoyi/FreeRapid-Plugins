package cz.vity.freerapid.plugins.services.hulu;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
public enum VideoQuality {
    Lowest(0),
    _240(240),
    _360(360),
    _480(480),
    Highest(10000);

    private final int quality;
    private final String name;

    private VideoQuality(int quality) {
        this.quality = quality;
        switch (quality) {
            case 0:
                this.name = "Lowest available";
                break;
            case 10000:
                this.name = "Highest available";
                break;
            default:
                this.name = quality + "p";
                break;
        }
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
