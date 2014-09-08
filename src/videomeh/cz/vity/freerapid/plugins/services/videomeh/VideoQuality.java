package cz.vity.freerapid.plugins.services.videomeh;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */

enum VideoQuality {
    Lowest(1),
    _240(240),
    _360(360),
    _480(480),
    //_720(720),
    //_1080(1080),
    Highest(100000);

    private final int quality;

    private VideoQuality(int quality) {
        this.quality = quality;
    }

    public int getQuality() {
        return quality;
    }

    @Override
    public String toString() {
        switch (this) {
            case Lowest:
                return "Lowest quality";
            case Highest:
                return "Highest quality";
            default:
                return quality + "p";
        }
    }

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}
