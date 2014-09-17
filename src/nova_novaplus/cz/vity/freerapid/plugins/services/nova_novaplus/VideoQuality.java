package cz.vity.freerapid.plugins.services.nova_novaplus;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */

enum VideoQuality {
    LQ(240), HQ(720);

    private final int quality;

    VideoQuality(int quality) {
        this.quality = quality;
    }

    public int getQuality() {
        return quality;
    }

    @Override
    public String toString() {
        switch (this) {
            case LQ:
                return "Low Quality";
            case HQ:
                return "High Quality";
        }
        return null;
    }

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}
