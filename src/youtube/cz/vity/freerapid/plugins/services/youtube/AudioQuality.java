package cz.vity.freerapid.plugins.services.youtube;

import java.util.Arrays;
import java.util.Collections;

enum AudioQuality {
    _48(48),
    _64(64),
    _96(96),
    _128(128),
    _192(192),
    _256(256);

    private final int bitrate;
    private final String name;

    private AudioQuality(int quality) {
        this.bitrate = quality;
        this.name = String.valueOf(quality) + " kbps";
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

    public static AudioQuality[] getItems() {
        final AudioQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}
