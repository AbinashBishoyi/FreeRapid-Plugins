package cz.vity.freerapid.plugins.services.canalplus;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
public enum VideoQuality {
    _200(200),
    _400(400),
    _800(800),
    _1500(1500);

    private final int bitrate;
    private final String name;

    private VideoQuality(int bitrate) {
        this.bitrate = bitrate;
        this.name = bitrate + " kbps";
    }

    public int getBitrate() {
        return bitrate;
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
