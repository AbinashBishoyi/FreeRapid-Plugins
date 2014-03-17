package cz.vity.freerapid.plugins.services.liveleak;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
public enum VideoQuality {
    SD(270),
    HD(720);

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
            case SD:
                return "Standard Definition (SD)";
            default:
                return "High Definition (HD)";
        }
    }

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}
