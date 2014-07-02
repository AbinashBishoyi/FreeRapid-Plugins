package cz.vity.freerapid.plugins.services.firedrive;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
public enum VideoQuality {
    Mobile(1),
    SD(4),
    HD(5),
    Original(10);

    private final int quality;

    VideoQuality(int quality) {
        this.quality = quality;
    }

    public int getQuality() {
        return quality;
    }

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }

    @Override
    public String toString() {
        switch (this) {
            case SD:
                return "Standard Definition";
            case HD:
                return "High Definition";
            default:
                return name();
        }
    }
}
