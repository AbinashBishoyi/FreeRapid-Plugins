package cz.vity.freerapid.plugins.services.canalplus;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
enum VideoQuality {
    Lowest(1),
    _200(200),
    _400(400),
    _800(800),
    _1000(1000),
    _1500(1500),
    _2000(2000),
    Highest(100000);

    private final int bitrate;

    private VideoQuality(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getBitrate() {
        return bitrate;
    }

    @Override
    public String toString() {
        switch (this) {
            case Lowest:
                return "Lowest bitrate";
            case Highest:
                return "Highest bitrate";
            default:
                return bitrate + " Kbps";
        }
    }

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}