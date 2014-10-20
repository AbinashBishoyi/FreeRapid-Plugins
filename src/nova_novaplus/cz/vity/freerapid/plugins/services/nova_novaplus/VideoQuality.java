package cz.vity.freerapid.plugins.services.nova_novaplus;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */

enum VideoQuality {
    LQ(240, "Low Quality"),
    HQ(480, "High Quality"),
    HD(720, "High Definition");

    private final int quality;
    private final String name;

    VideoQuality(int quality, String name) {
        this.quality = quality;
        this.name = name;
    }

    public int getQuality() {
        return quality;
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
